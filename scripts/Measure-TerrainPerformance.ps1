param([ValidateSet('forge','neoforge')][string]$Loader='forge')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $node=if($Loader -eq 'forge'){'1.20.1-forge'}else{'1.21.1-neoforge'}
    $minecraft=if($Loader -eq 'forge'){'1.20.1'}else{'1.21.1'}
    $java=if($Loader -eq 'forge'){'C:\Program Files\Java\jdk-17.0.5\bin\java.exe'}else{'C:\Program Files\Java\graalvm-jdk-21.0.5+9.1\bin\java.exe'}
    $testClasses="versions/$node/build/classes/java/test"
    $oldJar="versions/$node/build/libs/deep_realm_the_forth-$Loader-0.1.6+$minecraft.jar"
    if(!(Test-Path $oldJar)){throw 'Preserved 0.1.6 baseline jar is missing.'}
    $mainClasses="versions/$node/build/classes/java/main"
    $folder="build/reports/terrain-performance/$Loader"
    $snapshot="run/$Loader-server/hydrology-prototype/climate.tsv"
    # Sequential isolated JVMs: no concurrent build/server workload during these measurements.
    foreach($pair in 1,2) {
        foreach($variant in 'baseline','optimized','v4') {
            $label="paired-$pair-$variant"
            $version=if($variant -eq 'v4'){4}else{3}
            $implementation=if($variant -eq 'baseline'){$oldJar}else{$mainClasses}
            & $java "-Djava.io.tmpdir=$projectRoot\build" -Xmx1G -cp "$implementation;$testClasses" `
                com.tony.deeprealmtheforth.worldgen.TerrainPerformanceBenchmark $snapshot $folder $label $version
            if($LASTEXITCODE -ne 0){throw "Benchmark failed: $label"}
        }
    }
    $results=@{}
    foreach($variant in 'baseline','optimized','v4') {
        $results[$variant]=@(foreach($pair in 1,2){Import-Csv "$folder/paired-$pair-$variant.tsv" -Delimiter "`t"})
    }
    foreach($scenario in 'H0','columns-cold','columns-repeat','columns-parallel4') {
        $reference=@($results.baseline | Where-Object scenario -eq $scenario | Select-Object -ExpandProperty fingerprint -Unique)
        $optimized=@($results.optimized | Where-Object scenario -eq $scenario | Select-Object -ExpandProperty fingerprint -Unique)
        if($reference.Count -ne 1 -or $optimized.Count -ne 1 -or $reference[0] -ne $optimized[0]) {
            throw "Output-preserving optimization changed $scenario fingerprints."
        }
    }
    $summary=foreach($variant in 'baseline','optimized','v4') {
        foreach($scenario in 'H0','columns-cold','columns-repeat','columns-parallel4') {
            $rows=@($results[$variant] | Where-Object scenario -eq $scenario)
            $values=@($rows | ForEach-Object {[double]$_.ms} | Sort-Object)
            [pscustomobject]@{variant=$variant;scenario=$scenario;samples=$values.Count;medianMs=($values[4]+$values[5])/2;
                minMs=$values[0];maxMs=$values[-1];mainThreadAllocatedMiB=($rows | Measure-Object mainThreadAllocatedMiB -Average).Average;
                fingerprints=($rows | Select-Object -ExpandProperty fingerprint -Unique) -join ','}
        }
    }
    $summary | Export-Csv "$folder/paired-summary.tsv" -NoTypeInformation -Delimiter "`t" -Encoding UTF8
    $summary | Format-Table -AutoSize
    'PAIRED_BENCHMARK_OK outputPreserved=True'
} finally {Pop-Location}
