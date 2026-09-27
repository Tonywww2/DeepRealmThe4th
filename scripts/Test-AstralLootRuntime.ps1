param(
    [Parameter(Mandatory=$true)][ValidateSet('forge', 'neoforge')][string]$Loader
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$properties = Get-Content -Encoding UTF8 -LiteralPath (Join-Path $projectRoot "run/$Loader-server/server.properties")
if ($properties -notcontains 'level-name=verification-world-astral-loot' -or
        $properties -notcontains 'level-seed=42') {
    throw 'Astral loot verification requires its isolated seed-42 test world.'
}

$rcon = Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1'
$tables = [ordered]@{
    'chests/desert_pyramid' = 'arid_ridge_gem'
    'chests/nether_bridge' = 'magma_vein_gem'
    'chests/jungle_temple' = 'canopy_gem'
    'chests/shipwreck_map' = 'tidal_gem'
    'chests/shipwreck_supply' = 'tidal_gem'
    'chests/shipwreck_treasure' = 'tidal_gem'
}
$base = @('strength_gem','agility_gem','intelligence_gem','constitution_gem','perception_gem','magic_gem')
$themed = @('arid_ridge_gem','magma_vein_gem','canopy_gem','tidal_gem')
$rareByTable = @{
    'chests/desert_pyramid' = @('facet_bridge_gem','twin_mirror_gem','balance_shift_gem',
        'split_edge_gem','vein_amplitude_gem','folded_reflection_gem','cluster_mirror_gem',
        'etched_step_gem','last_edge_gem','nightglow_gem')
    'chests/nether_bridge' = @('facet_bridge_gem','ember_remnant_gem','returning_ray_gem',
        'split_edge_gem','vein_amplitude_gem','linked_vein_gem','looped_trace_gem','last_edge_gem')
    'chests/jungle_temple' = @('wandering_stripe_gem','ember_remnant_gem','balance_shift_gem',
        'steady_anchor_gem','wandering_shadow_gem','linked_vein_gem','cluster_mirror_gem',
        'etched_step_gem','full_breath_gem','well_fed_glow_gem')
    'chests/shipwreck_map' = @('twin_mirror_gem','wandering_stripe_gem','returning_ray_gem',
        'steady_anchor_gem','wandering_shadow_gem','folded_reflection_gem','looped_trace_gem',
        'full_breath_gem','well_fed_glow_gem','nightglow_gem')
}
$rareByTable['chests/shipwreck_supply'] = $rareByTable['chests/shipwreck_map']
$rareByTable['chests/shipwreck_treasure'] = $rareByTable['chests/shipwreck_map']
$allRare = @($rareByTable.Values | ForEach-Object { $_ } | Sort-Object -Unique)
$report = [System.Collections.Generic.List[string]]::new()

function Rcon([string[]]$commands) {
    return & $rcon -Loader $Loader -Command $commands
}

function Present([string]$dimension, [string]$gem) {
    $command = 'execute in ' + $dimension +
            ' if entity @e[type=item,nbt={Item:{id:"deeprealm_4th:' + $gem + '"}}]'
    $reply = Rcon @($command)
    return ($reply -join "`n").Contains('Test passed')
}

foreach ($dimension in @('deeprealm_4th:fourth_layer','minecraft:overworld',
        'minecraft:the_nether','minecraft:the_end')) {
    $setup = Rcon @(
        "execute in $dimension run forceload add 0 0",
        "execute in $dimension run fill -5 101 -5 5 110 5 air",
        "execute in $dimension run fill -5 100 -5 5 100 5 stone")
    if (($setup -join "`n") -match 'Unknown dimension|Unknown command|not loaded') {
        throw "Cannot prepare $dimension for loot verification"
    }
    try {
        foreach ($table in $tables.Keys) {
            $rolls = if ($dimension -eq 'deeprealm_4th:fourth_layer') { 80 } else { 12 }
            $commands = @(
                for ($i = 0; $i -lt $rolls; $i++) {
                    "execute in $dimension run loot spawn 0 102 0 loot minecraft:$table"
                })
            $lootResult = Rcon $commands
            $dropped = @($lootResult | Where-Object { $_ -match 'Dropped [0-9]+ items?' }).Count
            if ($dropped -ne $rolls) {
                throw "Only $dropped/$rolls loot commands succeeded for $dimension $table"
            }

            $foundBase = @($base | Where-Object { Present $dimension $_ })
            $foundThemed = @($themed | Where-Object { Present $dimension $_ })
            $foundRare = @($allRare | Where-Object { Present $dimension $_ })
            if ($dimension -eq 'deeprealm_4th:fourth_layer') {
                if ($foundBase.Count -eq 0 -or $foundThemed.Count -ne 1 -or
                        $foundThemed[0] -ne $tables[$table] -or $foundRare.Count -eq 0 -or
                        @($foundRare | Where-Object { $rareByTable[$table] -notcontains $_ }).Count -ne 0) {
                    throw "Wrong Fourth Layer gems for $table`: base=$foundBase themed=$foundThemed rare=$foundRare"
                }
            } elseif ($foundBase.Count -ne 0 -or $foundThemed.Count -ne 0 -or $foundRare.Count -ne 0) {
                throw "Gem loot escaped into $dimension $table`: base=$foundBase themed=$foundThemed rare=$foundRare"
            }
            $report.Add("$dimension $table rolls=$rolls base=$($foundBase -join ',') themed=$($foundThemed -join ',') rare=$($foundRare -join ',')")
            [void](Rcon @("execute in $dimension run kill @e[type=item]"))
        }
    } finally {
        [void](Rcon @(
            "execute in $dimension run kill @e[type=item]",
            "execute in $dimension run forceload remove 0 0"))
    }
}

$output = Join-Path $projectRoot "build/reports/astral-loot/$Loader-runtime.txt"
$report | Set-Content -LiteralPath $output -Encoding UTF8
$report
Write-Output "ASTRAL_LOOT_VERIFY_OK loader=$Loader"
