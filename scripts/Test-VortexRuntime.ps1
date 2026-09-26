param(
    [Parameter(Mandatory=$true)][ValidateSet('forge', 'neoforge')][string]$Loader,
    [Parameter(Mandatory=$true)][ValidateSet('Geometry', 'Biomes', 'Ecology', 'Structures', 'Prime', 'Fluids')][string]$Phase
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$reportRoot = Join-Path $projectRoot 'build/reports/vortex-v8'
[void](New-Item -ItemType Directory -Force -Path $reportRoot)
if ($Phase -eq 'Structures') {
    foreach ($structure in @('desert_pyramid', 'jungle_pyramid', 'shipwreck', 'fortress', 'village_plains')) {
        $result = & (Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1') -Loader $Loader -Command "fourthlayer verifystructures $structure"
        $result | Tee-Object -FilePath (Join-Path $reportRoot "$Loader-Structure-$structure.txt")
        $body = $result -join "`n"
        if (!$body.Contains('STRUCTURE_VERIFY_OK') -or $body.Contains('FAILED')) { throw "Natural structure verification failed: $structure" }
    }
} elseif ($Phase -eq 'Geometry' -or $Phase -eq 'Ecology' -or $Phase -eq 'Biomes') {
    $command = switch ($Phase) { 'Geometry' { 'fourthlayer verify' } 'Biomes' { 'fourthlayer verifybiomes' } default { 'fourthlayer verifyecology' } }
    $result = & (Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1') -Loader $Loader -Command $command
    $result | Tee-Object -FilePath (Join-Path $reportRoot "$Loader-$Phase.txt")
    $body = $result -join "`n"
    $marker = switch ($Phase) { 'Geometry' { 'FOURTH_LAYER_VERIFY_OK' } 'Biomes' { 'BIOME_VERIFY_OK' } default { 'NATURAL_SPAWN_PROBE OK' } }
    if (!$body.Contains($marker) -or $body.Contains('FAILED')) { throw "Runtime $Phase verification failed." }
    if ($Phase -eq 'Ecology' -and !$body.Contains('failedFeatureIds=[]')) { throw 'Feature integration raised exceptions.' }
} else {
    $fluidPhase = if ($Phase -eq 'Prime') { 'Prime' } else { 'Check' }
    & (Join-Path $PSScriptRoot 'Test-LocalFluids.ps1') -Loader $Loader -Phase $fluidPhase |
        Tee-Object -FilePath (Join-Path $reportRoot "$Loader-$Phase.txt")
}
