#Requires -Version 5.1
# Shared setup for build.ps1, dryrun.ps1, and publish.ps1. Never prints credentials.

function Get-ReleaseConfiguration {
    param([string]$JavaHome)

    $repoRoot = Split-Path -Parent $PSScriptRoot
    if ($JavaHome) {
        $javaExecutable = Join-Path $JavaHome 'bin\java.exe'
        if (-not (Test-Path -LiteralPath $javaExecutable -PathType Leaf)) {
            throw "JavaHome '$JavaHome' does not contain bin\java.exe."
        }
        $env:JAVA_HOME = (Get-Item -LiteralPath $JavaHome).FullName
    }

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath (Join-Path $repoRoot 'gradle.properties') -Encoding UTF8) {
        if ($line -match '^\s*([^#!\s][^=]*?)\s*=\s*(.*)$') {
            $properties[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    foreach ($key in @('mod.id', 'mod.name', 'mod.version', 'curseforge.projectId')) {
        if ([string]::IsNullOrWhiteSpace($properties[$key])) {
            throw "Missing '$key' in gradle.properties."
        }
    }
    if ($properties['curseforge.projectId'] -notmatch '^\d+$') {
        throw 'curseforge.projectId must be a numeric CurseForge project id.'
    }

    $nodes = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'versions') -Directory |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'gradle.properties') } |
        Sort-Object Name | ForEach-Object {
            $separator = $_.Name.LastIndexOf('-')
            if ($separator -lt 1) { throw "Invalid loader node '$($_.Name)'." }
            $minecraft = $_.Name.Substring(0, $separator)
            $loader = $_.Name.Substring($separator + 1)
            $fileName = '{0}-{1}-{2}+{3}.jar' -f $properties['mod.id'], $loader, $properties['mod.version'], $minecraft
            [pscustomobject]@{
                Name = $_.Name
                Artifact = Join-Path $_.FullName (Join-Path 'build\libs' $fileName)
            }
        })
    if ($nodes.Count -eq 0) { throw 'No loader nodes found under versions/.' }

    [pscustomobject]@{
        RepoRoot = $repoRoot
        ModName = $properties['mod.name']
        Version = $properties['mod.version']
        ProjectId = $properties['curseforge.projectId']
        Nodes = $nodes
    }
}

function Invoke-ReleaseGradle {
    param([string[]]$GradleArguments)

    & .\gradlew.bat @GradleArguments '--console=plain'
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed (exit $LASTEXITCODE)."
    }
}

function Show-ReleaseArtifacts {
    param($Release)

    Write-Host ''
    Write-Host 'Release jars:' -ForegroundColor Green
    foreach ($node in $Release.Nodes) {
        if (-not (Test-Path -LiteralPath $node.Artifact -PathType Leaf)) {
            throw "Expected release jar was not generated: $($node.Artifact)"
        }
        $artifact = Get-Item -LiteralPath $node.Artifact
        Write-Host ('  {0} ({1:N0} bytes)' -f $artifact.FullName, $artifact.Length)
    }
}
