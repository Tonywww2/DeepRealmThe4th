param([ValidateSet('forge','neoforge')][string]$Loader='forge')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$javaRoot=if($Loader -eq 'forge'){'C:\Program Files\Java\jdk-17.0.5'}else{'C:\Program Files\Java\graalvm-jdk-21.0.5+9.1'}
$classes=Join-Path $projectRoot "build/standalone-worldgen/$Loader/classes"
[void](New-Item -ItemType Directory -Force -Path $classes)
$package=Join-Path $projectRoot 'src/main/java/com/tonywww/deeprealm4th'
$sources=@(foreach($part in @('worldgen/terrain','worldgen/layout','worldgen/hydrology')) {
    Get-ChildItem -LiteralPath (Join-Path $package $part) -Filter '*.java' | Select-Object -ExpandProperty FullName
})
$sources+=@('worldgen/biome/BiomeClimate.java','worldgen/biome/BaseBiomeResolver.java','travel/LandingSearch.java') | ForEach-Object {Join-Path $package $_}
$sources+=@('CurrentTerrainVerification.java','GlobalHydrologyVerification.java','WorldgenOptimizationVerification.java') | ForEach-Object {
    Join-Path $projectRoot "src/test/java/com/tonywww/deeprealm4th/worldgen/$_"
}
# Intentionally compiles only dependency-free worldgen code. This does NOT validate
# Minecraft integration, loader APIs, unrelated modules, or create a release jar.
& (Join-Path $javaRoot 'bin/javac.exe') --release 17 -encoding UTF-8 -d $classes $sources
if($LASTEXITCODE -ne 0){throw 'Standalone worldgen compilation failed'}
$classpath=$classes+';'+(Join-Path $projectRoot 'src/main/resources')
$out=Join-Path $projectRoot "build/reports/standalone-worldgen/$Loader"
$climate=Join-Path $projectRoot "run/$Loader-server/biome-compat-v5/climate.tsv"
& (Join-Path $javaRoot 'bin/java.exe') -Xmx1G -cp $classpath 'com.tonywww.deeprealm4th.worldgen.WorldgenOptimizationVerification'
if($LASTEXITCODE -ne 0){throw 'Worldgen optimization parity verification failed'}
& (Join-Path $javaRoot 'bin/java.exe') -Xmx1G -cp $classpath 'com.tonywww.deeprealm4th.worldgen.CurrentTerrainVerification'
if($LASTEXITCODE -ne 0){throw 'Current terrain verification failed'}
& (Join-Path $javaRoot 'bin/java.exe') -Xmx1G -cp $classpath 'com.tonywww.deeprealm4th.worldgen.GlobalHydrologyVerification' $climate "$out/hydrology"
if($LASTEXITCODE -ne 0){throw 'Current hydrology verification failed'}
Write-Output 'STANDALONE_WORLDGEN_OK (not a release-build result)'
