param(
    [Parameter(Mandatory=$true)][ValidateSet('forge', 'neoforge')][string]$Loader,
    [Parameter(Mandatory=$true)][ValidateSet('Prime', 'Check')][string]$Phase
)

# Mutates disposable verification worlds only. Prime, wait at least 20 seconds, then Check.
# A failed Check replaces escaped flowing fluid with barriers to expose the failing positions.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$properties = Get-Content -Encoding UTF8 (Join-Path $projectRoot "run/$Loader-server/server.properties")
if (-not ($properties -match '^level-name=verification-world-vortex-v8-shores$') -or -not ($properties -match '^level-seed=42$')) {
    throw 'Refusing to modify anything except the disposable seed-42 vortex-v8-shores verification worlds.'
}
$probes = Get-Content -Raw (Join-Path $projectRoot 'build/reports/terrain/fluid-probes.json') | ConvertFrom-Json
$commands = [Collections.Generic.List[string]]::new()
$prefix = 'execute in deeprealm_4th:fourth_layer run '
foreach ($probe in $probes) {
    $x = [int]$probe.x; $y = [int]$probe.y; $z = [int]$probe.z
    if ($Phase -eq 'Prime') {
        $commands.Add($prefix + "forceload add $($x-16) $($z-16) $($x+16) $($z+16)")
        $commands.Add($prefix + "fill $($x-12) $($y+1) $($z-12) $($x+12) $($y+1) $($z+12) minecraft:structure_void replace minecraft:air")
        $commands.Add($prefix + "fill $($x-12) $($y+1) $($z-12) $($x+12) $($y+1) $($z+12) minecraft:air replace minecraft:structure_void")
    } else {
        $fluid = ([string]$probe.fluid).ToLowerInvariant()
        $commands.Add($prefix + "execute if block $x $y $z minecraft:$fluid[level=0]")
        for ($level = 1; $level -le 15; $level++) {
            $commands.Add($prefix + "fill $($x-12) $($y-5) $($z-12) $($x+12) $($y+2) $($z+12) minecraft:barrier replace minecraft:$fluid[level=$level]")
        }
    }
}
try {
    $result = & (Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1') -Loader $Loader -Command $commands.ToArray()
    $result | Write-Output
    if ($Phase -eq 'Check') {
        $responses = ($result | Where-Object { -not $_.StartsWith('> ') }) -join "`n"
        $sourceCount = [regex]::Matches($responses, 'Test passed').Count
        $emptyCount = [regex]::Matches($responses, 'No blocks were filled').Count
        if ($sourceCount -ne $probes.Count -or $emptyCount -ne 15 * $probes.Count) {
            throw "Fluid probe failed: sources=$sourceCount/$($probes.Count), empty-flow checks=$emptyCount/$($probes.Count*15)"
        }
        Write-Output "FLUID_TICK_VERIFICATION_OK probes=$($probes.Count) flowingStateChecks=$emptyCount"
    }
} finally {
    if ($Phase -eq 'Check') {
        & (Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1') -Loader $Loader -Command ($prefix + 'forceload remove all')
    }
}
