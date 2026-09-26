param(
    [Parameter(Mandatory=$true)][ValidateSet('forge','neoforge')][string]$Loader,
    [Parameter(Mandatory=$true)][ValidateSet('Climate','Geometry','Biomes','Compat','Ecology','Prepare','Check','Cleanup','Structures')][string]$Phase,
    [ValidateSet(3,4,5)][int]$GenerationVersion=4,
    [ValidateSet('default','compat')][string]$Profile='default'
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$runName=if($Profile -eq 'compat'){"$Loader-compat-server"}else{"$Loader-server"}
$properties=Get-Content -Encoding UTF8 -LiteralPath (Join-Path $projectRoot "run/$runName/server.properties")
$expectedWorld=if($GenerationVersion -eq 5){if($Profile -eq 'compat'){'verification-world-vortex-v11-compat'}else{'verification-world-vortex-v11-marine'}}elseif($GenerationVersion -eq 4){'verification-world-vortex-v10-edges'}else{'verification-world-vortex-v9-hydrology'}
if ($properties -notcontains "level-name=$expectedWorld") {
    throw "Only the $expectedWorld disposable verification world is permitted."
}
$reportRoot=Join-Path $projectRoot "build/reports/hydrology-v$GenerationVersion/$Loader/$Profile/runtime"
[void](New-Item -ItemType Directory -Force -Path $reportRoot)
$commands=switch($Phase) {
    'Climate' { 'fourthlayer verifyclimate' }
    'Geometry' { 'fourthlayer verify' }
    'Biomes' { 'fourthlayer verifybiomes' }
    'Compat' { 'fourthlayer verifycompat' }
    'Ecology' { 'fourthlayer verifyecology' }
    'Prepare' { 'fourthlayer verifyhydrology prepare' }
    'Check' { 'fourthlayer verifyhydrology check' }
    'Cleanup' { 'fourthlayer verifyhydrology cleanup' }
    'Structures' { 'fourthlayer verifystructures desert_pyramid'; 'fourthlayer verifystructures jungle_pyramid'; 'fourthlayer verifystructures village_plains' }
}
$marker=switch($Phase) {
    'Climate' { 'CLIMATE_VERIFY_OK' }
    'Geometry' { 'FOURTH_LAYER_VERIFY_OK' }
    'Biomes' { 'BIOME_VERIFY_OK' }
    'Compat' { 'BIOME_COMPAT_OK' }
    'Ecology' { 'NATURAL_SPAWN_PROBE OK' }
    'Prepare' { 'HYDROLOGY_PREPARED' }
    'Check' { 'HYDROLOGY_RUNTIME_OK' }
    'Cleanup' { 'HYDROLOGY_CLEANUP_OK' }
    'Structures' { 'STRUCTURE_VERIFY_OK' }
}
foreach($command in $commands) {
    $result=& (Join-Path $PSScriptRoot 'Invoke-LocalRcon.ps1') -Loader $Loader -Profile $Profile -Command $command
    $name=($command -replace ' ','-')+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt'
    $result | Tee-Object -FilePath (Join-Path $reportRoot $name)
    $body=$result -join "`n"
    if (!$body.Contains($marker) -or $body.Contains('FAILED')) { throw "Runtime verification failed: $command" }
    if ($Phase -eq 'Ecology' -and !$body.Contains('failedFeatureIds=[]')) { throw 'Native feature exceptions.' }
}
