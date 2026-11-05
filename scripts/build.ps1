#Requires -Version 5.1
<#
.SYNOPSIS
    Package the Forge 1.20.1 and NeoForge 1.21.1 release jars.
.DESCRIPTION
    Runs buildAllVersions (Forge remapJar + NeoForge jar), without verification tasks.
    Prints the current version's exact release paths; excludes stale and source jars.
.PARAMETER JavaHome
    Optional Gradle launcher JDK path. The daemon JDK 25 is selected by Gradle's configuration.
.EXAMPLE
    .\scripts\build.ps1
#>
[CmdletBinding()]
param([string]$JavaHome)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Release-Common.ps1')
$release = Get-ReleaseConfiguration -JavaHome $JavaHome

Push-Location -LiteralPath $release.RepoRoot
try {
    Write-Host "== Building $($release.ModName) $($release.Version) (all loaders) ==" -ForegroundColor Cyan
    Invoke-ReleaseGradle -GradleArguments @('buildAllVersions')
    Show-ReleaseArtifacts -Release $release
} finally {
    Pop-Location
}
