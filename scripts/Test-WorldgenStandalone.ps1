param([ValidateSet('forge','neoforge')][string]$Loader='forge')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$javaRoot=if($Loader -eq 'forge'){'C:\Program Files\Java\jdk-17.0.5'}else{'C:\Program Files\Java\graalvm-jdk-21.0.5+9.1'}
$classes=Join-Path $projectRoot "build/standalone-worldgen/$Loader/classes"
[void](New-Item -ItemType Directory -Force -Path $classes)
$package=Join-Path $projectRoot 'src/main/java/com/tony/deeprealmtheforth'
$sources=@(foreach($part in @('worldgen/terrain','worldgen/layout','worldgen/hydrology')) {
    Get-ChildItem -LiteralPath (Join-Path $package $part) -Filter '*.java' | Select-Object -ExpandProperty FullName
})
$sources+=@('worldgen/biome/BiomeClimate.java','worldgen/biome/BaseBiomeResolver.java','travel/LandingSearch.java') | ForEach-Object {Join-Path $package $_}
$sources+=Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/test/java/com/tony/deeprealmtheforth/worldgen') -Filter '*.java' | Select-Object -ExpandProperty FullName
# Intentionally compiles only dependency-free worldgen code. This does NOT validate
# Minecraft integration, loader APIs, unrelated modules, or create a release jar.
& (Join-Path $javaRoot 'bin/javac.exe') --release 17 -encoding UTF-8 -d $classes $sources
if($LASTEXITCODE -ne 0){throw 'Standalone worldgen compilation failed'}
$classpath=$classes+';'+(Join-Path $projectRoot 'src/main/resources')
$out=Join-Path $projectRoot "build/reports/standalone-worldgen/$Loader"
$oldClimate=Join-Path $projectRoot "run/$Loader-server/hydrology-prototype/climate.tsv"
$climate=Join-Path $projectRoot "run/$Loader-server/biome-compat-v5/climate.tsv"
$runs=@(
    @('TerrainVerification',"$out/legacy"),
    @('GlobalHydrologyVerification',$oldClimate,"$out/v3",'3'),
    @('GlobalHydrologyVerification',$oldClimate,"$out/v4",'4'),
    @('MarineAridVerification',$climate,"$out/v5-marine"),
    @('GlobalHydrologyVerification',$climate,"$out/v5-hydrology",'5')
)
foreach($entry in $runs) {
    Write-Output "Standalone $Loader / $($entry[0]) / $($entry[-1])"
    & (Join-Path $javaRoot 'bin/java.exe') -Xmx1G '-Djava.awt.headless=true' -cp $classpath ("com.tony.deeprealmtheforth.worldgen."+$entry[0]) $entry[1..($entry.Count-1)]
    if($LASTEXITCODE -ne 0){throw "Worldgen verification failed: $($entry[0])"}
}
Write-Output 'STANDALONE_WORLDGEN_OK (not a release-build result)'
