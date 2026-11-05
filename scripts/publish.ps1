#Requires -Version 5.1
<#
.SYNOPSIS
    Build both loader jars and publish the release to CurseForge.
.DESCRIPTION
    Reads mod.version and curseforge.projectId from gradle.properties. Displays
    the exact release artifacts, asks for confirmation, then runs publishAllVersions.
    The upload token comes from CURSEFORGE_TOKEN or user-level Gradle properties
    (curseforge.token). Both jars are built before the first upload.
.PARAMETER Yes
    Skip the interactive confirmation, for CI or an already approved release.
.PARAMETER JavaHome
    Optional Gradle launcher JDK path. The daemon JDK 25 is selected by Gradle's configuration.
.EXAMPLE
    .\scripts\publish.ps1
.EXAMPLE
    .\scripts\publish.ps1 -Yes
#>
[CmdletBinding()]
param(
    [switch]$Yes,
    [string]$JavaHome
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Release-Common.ps1')
$release = Get-ReleaseConfiguration -JavaHome $JavaHome

Push-Location -LiteralPath $release.RepoRoot
try {
    Write-Host "== Publish $($release.ModName) $($release.Version) to CurseForge ==" -ForegroundColor Yellow
    Write-Host "Project: $($release.ProjectId); release type: Release; client + server"
    foreach ($node in $release.Nodes) {
        Write-Host "  $($node.Name): $($node.Artifact)"
    }
    if (-not $Yes) {
        $answer = Read-Host 'Build and upload these files now? [y/N]'
        if ($answer -notmatch '^(y|yes)$') {
            Write-Host 'Cancelled. Nothing was uploaded.'
            return
        }
    }

    Invoke-ReleaseGradle -GradleArguments @('publishAllVersions', '-Ppublish.dryRun=false')
    Write-Host "Published $($release.ModName) $($release.Version) to CurseForge." -ForegroundColor Green
} finally {
    Pop-Location
}
