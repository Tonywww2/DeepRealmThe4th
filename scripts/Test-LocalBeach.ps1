param([Parameter(Mandatory=$true)][ValidateSet('forge','neoforge')][string]$Loader)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$properties = Get-Content (Join-Path $root "run/$Loader-server/server.properties")
if (-not ($properties -match '^level-name=verification-world-vortex-v8-shores$') -or -not ($properties -match '^level-seed=42$')) {
    throw 'Only the disposable seed-42 v8 test world is permitted.'
}
$rcon = Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1'
$prefix = 'execute in deeprealm_4th:fourth_layer run '
try {
    & $rcon -Loader $Loader -Command ($prefix + 'forceload add -2016 1080 -1984 1112')
    $queries = @()
    for ($x=-2016; $x -le -1984; $x+=8) { for ($z=1080; $z -le 1112; $z+=8) {
        $queries += "fourthlayer inspect $x $z"
    } }
    $model = & $rcon -Loader $Loader -Command $queries
    $checks = @()
    foreach ($line in $model) {
        if ($line -match 'Fourth layer \[(-?\d+), (-?\d+)\] arm=7 theme=COAST top=(-?\d+) ') {
            $checks += $prefix + "execute if block $($Matches[1]) $($Matches[3]) $($Matches[2]) minecraft:sand"
        }
    }
    if ($checks.Count -ne 25) { throw 'Expected 25 coastal model probes.' }
    $result = & $rcon -Loader $Loader -Command $checks
    $result | Tee-Object -FilePath (Join-Path $root "build/reports/vortex-v8/$Loader-Beach.txt")
    if (@($result | Where-Object { $_.Trim() -eq 'Test passed' }).Count -ne 25) {
        throw 'Actual generated beach surface disagrees with the model.'
    }
    'ACTUAL_BEACH_OK seed=42 center=-2000,1096 area=33x33 grid=5x5 matches=25' |
        Tee-Object -Append -FilePath (Join-Path $root "build/reports/vortex-v8/$Loader-Beach.txt")
} finally {
    & $rcon -Loader $Loader -Command ($prefix + 'forceload remove all')
}
