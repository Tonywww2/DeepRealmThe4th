#Requires -Version 5.1
<#
.SYNOPSIS
    Build both release jars and preview CurseForge publishing without uploading.
.DESCRIPTION
    Runs publishAllVersions -Ppublish.dryRun=true. This builds the jars and writes
    publication previews under each loader's build directory. No token is needed.
.PARAMETER JavaHome
    Optional Gradle launcher JDK path. The daemon JDK 25 is selected by Gradle's configuration.
.EXAMPLE
    .\scripts\dryrun.ps1
#>
[CmdletBinding()]
param([string]$JavaHome)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Release-Common.ps1')
$release = Get-ReleaseConfiguration -JavaHome $JavaHome

Push-Location -LiteralPath $release.RepoRoot
try {
    Write-Host "== $($release.ModName) $($release.Version) - DRY RUN (no upload) ==" -ForegroundColor Cyan
    Write-Host "CurseForge project: $($release.ProjectId)"
    Invoke-ReleaseGradle -GradleArguments @('publishAllVersions', '-Ppublish.dryRun=true')
    Show-ReleaseArtifacts -Release $release
    Write-Host 'Dry run OK. Nothing was uploaded.' -ForegroundColor Green
} finally {
    Pop-Location
}
