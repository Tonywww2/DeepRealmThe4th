param([ValidateSet('forge','neoforge')][string]$Loader='forge',
      [ValidateSet('overworld-first','fourth-first')][string]$Order='overworld-first',
      [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunLabel='ab-normal-20260928')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$save="verification-world-vortex-v13-perf-comparison-$Loader-$RunLabel"
$properties=Get-Content -LiteralPath (Join-Path $root "run/$Loader-server/server.properties")
if(-not ($properties -contains 'level-seed=42')) {throw 'Comparison requires seed 42'}
if(-not ($properties -contains "level-name=$save")) {throw "Comparison requires level-name=$save"}
$levelType=($properties | Where-Object {$_ -like 'level-type=*'} | Select-Object -First 1) -replace '^level-type=',''
$worldPath=Join-Path $root "run/$Loader-server/$save"
if(-not (Test-Path -LiteralPath $worldPath -PathType Container)) {throw "Comparison save not running: $worldPath"}
$report=Join-Path $root "build/reports/worldgen-performance/$Loader/overworld-comparison-$RunLabel.txt"
if(Test-Path -LiteralPath $report){throw 'Comparison already recorded; use a fresh save and report name'}
$helper=Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1'
$sites=@(@(-6208,4800),@(-2560,-1792),@(3968,-2944),@(-1664,640))
$dimensions=if($Order -eq 'overworld-first'){@('minecraft:overworld','deeprealm_4th:fourth_layer')}
            else{@('deeprealm_4th:fourth_layer','minecraft:overworld')}
function Commands-For([string]$dimension,[int]$originX,[int]$originZ,[int]$size) {
    [string[]]$commands=@()
    for($dx=0;$dx -lt $size;$dx++) {
        for($dz=0;$dz -lt $size;$dz++) {
            $x=$originX+$dx*16+8;$z=$originZ+$dz*16+8
            # Only reports whether the ticketed FULL chunk is available; changes no blocks.
            $commands+="execute in $dimension if block $x 80 $z minecraft:air"
        }
    }
    return ,$commands
}
function Run-Probes([string[]]$commands) {
    [string[]]$responses=@(& $helper -Loader $Loader -Command $commands)
    if($responses -match 'Unknown or incomplete command|An unexpected error occurred|Internal exception|No such dimension')
        {throw "Probe command failed: $($responses | Select-String 'Unknown or incomplete command|An unexpected error occurred|Internal exception|No such dimension' | Select-Object -First 1)"}
    return ,$responses
}
function Run-Patch([string]$dimension,[int]$originX,[int]$originZ,[int]$size) {
    $maxX=$originX+$size*16-1;$maxZ=$originZ+$size*16-1
    $prefix=if($dimension -eq 'minecraft:overworld'){''}else{"execute in $dimension run "}
    $add="${prefix}forceload add $originX $originZ $maxX $maxZ"
    $remove="${prefix}forceload remove $originX $originZ $maxX $maxZ"
    [string[]]$queries=Commands-For $dimension $originX $originZ $size
    $timer=[System.Diagnostics.Stopwatch]::StartNew()
    try {
        [string[]]$added=@(& $helper -Loader $Loader -Command @($add))
        if($added -match 'already|Unknown or incomplete command|No such dimension'){throw "Forceload failed: $added"}
        for($poll=0;$poll -lt 80;$poll++) {
            [string[]]$responses=Run-Probes $queries
            if(-not ($responses -match 'That position is not loaded')){break}
            Start-Sleep -Milliseconds 100
        }
        if($responses -match 'That position is not loaded'){throw 'FULL chunks did not finish within 80 polls'}
        $timer.Stop()
        return $timer.Elapsed.TotalMilliseconds
    }finally {
        [string[]]$removed=@(& $helper -Loader $Loader -Command @($remove))
        if($removed -match 'Unknown or incomplete command|No such dimension'){throw "Forceload cleanup failed: $removed"}
    }
}
$lines=[Collections.Generic.List[string]]::new()
$lines.Add("RCON_FULL_CHUNK_COMPARISON loader=$Loader seed=42 levelType=$levelType targetChunksPerDimension=256 order=$Order runLabel=$RunLabel")
$totals=@{}
foreach($dimension in $dimensions) {
    $label=if($dimension -eq 'minecraft:overworld'){'overworld'}else{'fourth_layer'}
    $null=Run-Patch $dimension 512 512 4
    $freshTotal=0.0;$cachedTotal=0.0
    for($site=0;$site -lt $sites.Count;$site++) {
        $fresh=Run-Patch $dimension $sites[$site][0] $sites[$site][1] 8
        $cached=Run-Patch $dimension $sites[$site][0] $sites[$site][1] 8
        $freshTotal+=$fresh;$cachedTotal+=$cached
        $row="dimension=$label site=$site freshMs=$([math]::Round($fresh,3)) cachedMs=$([math]::Round($cached,3)) estimatedGenerationMs=$([math]::Round($fresh-$cached,3))"
        $lines.Add($row);Write-Output $row
    }
    $totals[$label]=$freshTotal-$cachedTotal
    $row="dimension=$label freshTotalMs=$([math]::Round($freshTotal,3)) cachedTotalMs=$([math]::Round($cachedTotal,3)) estimatedGenerationMs=$([math]::Round($totals[$label],3))"
    $lines.Add($row);Write-Output $row
}
$ratio=$totals['fourth_layer']/$totals['overworld']
$lines.Add("ratioFourthToOverworld=$([math]::Round($ratio,4))")
[void](New-Item -ItemType Directory -Force -Path (Split-Path $report -Parent))
[IO.File]::WriteAllLines($report,$lines)
Write-Output "COMPARE_OK ratioFourthToOverworld=$([math]::Round($ratio,4)) report=$report"
