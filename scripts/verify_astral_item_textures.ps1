$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$namespace = 'deeprealm_4th'
$modelDirectory = Join-Path $root "src/main/resources/assets/$namespace/models/item"
$textureDirectory = Join-Path $root "src/main/resources/assets/$namespace/textures/item"
$registrationPath = Join-Path $root 'src/main/java/com/tonywww/deeprealm4th/astral/content/AstralItemCatalog.java'

Add-Type -AssemblyName System.Drawing
$registration = Get-Content -Raw -Encoding utf8 $registrationPath
$itemIds = [regex]::Matches($registration, '(?:registerItem|scoreGem|regionalGem|gem|percentGem|conditionGem)\("([a-z0-9_]+)"') |
    ForEach-Object { $_.Groups[1].Value } |
    Sort-Object -Unique

foreach ($id in $itemIds) {
    $modelPath = Join-Path $modelDirectory "$id.json"
    $texturePath = Join-Path $textureDirectory "$id.png"
    if (-not (Test-Path -LiteralPath $modelPath)) { throw "Missing item model: $modelPath" }
    if (-not (Test-Path -LiteralPath $texturePath)) { throw "Missing item texture: $texturePath" }
    $model = Get-Content -Raw -Encoding utf8 $modelPath | ConvertFrom-Json
    $dynamicFrame = $id -eq 'projection_frame' -and $model.parent -eq 'minecraft:builtin/entity'
    if (-not $dynamicFrame -and $model.textures.layer0 -ne "$namespace`:item/$id") {
        throw "Item model $id does not use its own texture"
    }

    $bitmap = [System.Drawing.Bitmap]::new($texturePath)
    try {
        if ($bitmap.Width -ne 16 -or $bitmap.Height -ne 16) {
            throw "Item texture $id must be exactly 16x16"
        }
        $palette = [System.Collections.Generic.HashSet[int]]::new()
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $color = $bitmap.GetPixel($x, $y)
                if ($color.A -ne 0 -and $color.A -ne 255) {
                    throw "Item texture $id contains a semi-transparent pixel at $x,$y"
                }
                if ($color.A -eq 255) { [void]$palette.Add($color.ToArgb()) }
            }
        }
        if ($palette.Count -gt 24) { throw "Item texture $id exceeds 24 opaque colors" }
        Write-Output "$id`: 16x16, binary alpha, $($palette.Count) colors"
    } finally { $bitmap.Dispose() }
}
