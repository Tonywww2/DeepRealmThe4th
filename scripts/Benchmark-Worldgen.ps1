param([ValidateSet('forge','neoforge')][string]$Loader='forge',
      [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label='baseline', [int]$Repeats=3,
      [switch]$NoJfr)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$javaRoot=if($Loader -eq 'forge'){'C:\Program Files\Java\jdk-17.0.5'}else{'C:\Program Files\Java\graalvm-jdk-21.0.5+9.1'}
$out=Join-Path $projectRoot "build/reports/worldgen-performance/$Loader/$Label"
if(Test-Path (Join-Path $out 'timing.txt')){throw 'This label already has a result; use a new label to preserve baseline bytecode and measurements'}
$classes=Join-Path $out 'classes'
[void](New-Item -ItemType Directory -Force -Path $classes)
$temp=Join-Path $out 'jfr-temp'
[void](New-Item -ItemType Directory -Force -Path $temp)
$package=Join-Path $projectRoot 'src/main/java/com/tonywww/deeprealm4th'
$sources=@(foreach($part in @('worldgen/terrain','worldgen/layout','worldgen/hydrology')) {
    Get-ChildItem -LiteralPath (Join-Path $package $part) -Filter '*.java' | Select-Object -ExpandProperty FullName
})
$sources+=@('worldgen/biome/BiomeClimate.java','worldgen/biome/BaseBiomeResolver.java') | ForEach-Object {Join-Path $package $_}
$sources+=Join-Path $projectRoot 'src/test/java/com/tonywww/deeprealm4th/worldgen/WorldgenPerformanceBenchmark.java'
& (Join-Path $javaRoot 'bin/javac.exe') --release 17 -encoding UTF-8 -d $classes $sources
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$recording="-XX:StartFlightRecording=filename=$out/profile.jfr,settings=profile,dumponexit=true"
if($NoJfr) {
    & (Join-Path $javaRoot 'bin/java.exe') -Xms512m -Xmx1G -cp $classes 'com.tonywww.deeprealm4th.worldgen.WorldgenPerformanceBenchmark' (Join-Path $projectRoot "run/$Loader-server/biome-compat-v5/climate.tsv") $out $Repeats
} else {
    & (Join-Path $javaRoot 'bin/java.exe') -Xms512m -Xmx1G "-Djava.io.tmpdir=$temp" $recording -cp $classes 'com.tonywww.deeprealm4th.worldgen.WorldgenPerformanceBenchmark' (Join-Path $projectRoot "run/$Loader-server/biome-compat-v5/climate.tsv") $out $Repeats
}
if($LASTEXITCODE -ne 0){throw 'Benchmark failed'}
