param(
    [string]$SourceDirectory = (Join-Path $PSScriptRoot '..\art\source\item'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\src\main\resources\assets\deeprealm_4th\textures\item'),
    [string[]]$ItemIds = @()
)

Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

function ColorDistanceSquared($a, $b) {
    $dr = [double]$a.R - [double]$b.R
    $dg = [double]$a.G - [double]$b.G
    $db = [double]$a.B - [double]$b.B
    return $dr * $dr + $dg * $dg + $db * $db
}

function Simplify-PixelPalette([System.Drawing.Bitmap]$sprite) {
    $visible = [System.Collections.Generic.List[System.Drawing.Color]]::new()
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $color = $sprite.GetPixel($x, $y)
            if ($color.A -ge 128) { $visible.Add($color) }
        }
    }
    if ($visible.Count -eq 0) { throw 'Generated sprite has no visible pixels' }

    # A small palette and binary alpha keep the exported sprite readable at 16x16.
    $paletteSize = [Math]::Min(20, $visible.Count)
    $centers = [System.Collections.Generic.List[object]]::new()
    $centers.Add($visible[0])
    while ($centers.Count -lt $paletteSize) {
        $bestColor = $visible[0]
        $bestDistance = -1.0
        foreach ($color in $visible) {
            $nearest = [double]::PositiveInfinity
            foreach ($center in $centers) {
                $nearest = [Math]::Min($nearest, (ColorDistanceSquared $color $center))
            }
            if ($nearest -gt $bestDistance) {
                $bestDistance = $nearest
                $bestColor = $color
            }
        }
        if ($bestDistance -le 0) { break }
        $centers.Add($bestColor)
    }

    for ($pass = 0; $pass -lt 12; $pass++) {
        $sums = @()
        for ($i = 0; $i -lt $centers.Count; $i++) {
            $sums += [pscustomobject]@{ R = 0.0; G = 0.0; B = 0.0; Count = 0 }
        }
        foreach ($color in $visible) {
            $nearestIndex = 0
            $nearestDistance = [double]::PositiveInfinity
            for ($i = 0; $i -lt $centers.Count; $i++) {
                $distance = ColorDistanceSquared $color $centers[$i]
                if ($distance -lt $nearestDistance) {
                    $nearestDistance = $distance
                    $nearestIndex = $i
                }
            }
            $sums[$nearestIndex].R += $color.R
            $sums[$nearestIndex].G += $color.G
            $sums[$nearestIndex].B += $color.B
            $sums[$nearestIndex].Count++
        }
        for ($i = 0; $i -lt $centers.Count; $i++) {
            if ($sums[$i].Count -gt 0) {
                $centers[$i] = [System.Drawing.Color]::FromArgb(
                    [int][Math]::Round($sums[$i].R / $sums[$i].Count),
                    [int][Math]::Round($sums[$i].G / $sums[$i].Count),
                    [int][Math]::Round($sums[$i].B / $sums[$i].Count))
            }
        }
    }

    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $color = $sprite.GetPixel($x, $y)
            if ($color.A -lt 128) {
                $sprite.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                continue
            }
            $nearestColor = $centers[0]
            $nearestDistance = [double]::PositiveInfinity
            foreach ($center in $centers) {
                $distance = ColorDistanceSquared $color $center
                if ($distance -lt $nearestDistance) {
                    $nearestDistance = $distance
                    $nearestColor = $center
                }
            }
            $sprite.SetPixel($x, $y,
                [System.Drawing.Color]::FromArgb(255, $nearestColor.R, $nearestColor.G, $nearestColor.B))
        }
    }
}

Get-ChildItem -LiteralPath $SourceDirectory -Filter '*.png' -File |
    Where-Object { $ItemIds.Count -eq 0 -or $ItemIds -contains $_.BaseName } |
    ForEach-Object {
    $source = [System.Drawing.Bitmap]::new($_.FullName)
    try {
        if ($source.Width -eq 16 -and $source.Height -eq 16) {
            # Hand-authored pixel sprites are already final resolution. Rescaling them would blur shapes.
            Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $OutputDirectory $_.Name) -Force
            Write-Output "$($_.Name): 16x16 source copied"
            return
        }
        $left = $source.Width
        $top = $source.Height
        $right = -1
        $bottom = -1
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                if ($source.GetPixel($x, $y).A -gt 16) {
                    $left = [Math]::Min($left, $x)
                    $top = [Math]::Min($top, $y)
                    $right = [Math]::Max($right, $x)
                    $bottom = [Math]::Max($bottom, $y)
                }
            }
        }
        if ($right -lt $left) { throw "No visible pixels in $($_.FullName)" }

        $width = $right - $left + 1
        $height = $bottom - $top + 1
        $scale = [Math]::Min(13.0 / $width, 13.0 / $height)
        $drawWidth = [Math]::Max(1, [int][Math]::Round($width * $scale))
        $drawHeight = [Math]::Max(1, [int][Math]::Round($height * $scale))
        $drawLeft = [int][Math]::Floor((16 - $drawWidth) / 2)
        $drawTop = [int][Math]::Floor((16 - $drawHeight) / 2)

        $sprite = [System.Drawing.Bitmap]::new(16, 16,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($sprite)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.DrawImage($source,
                    [System.Drawing.Rectangle]::new($drawLeft, $drawTop, $drawWidth, $drawHeight),
                    [System.Drawing.Rectangle]::new($left, $top, $width, $height),
                    [System.Drawing.GraphicsUnit]::Pixel)
            } finally { $graphics.Dispose() }
            Simplify-PixelPalette $sprite
            $outputPath = Join-Path $OutputDirectory $_.Name
            $sprite.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Output "$($_.Name): 16x16"
        } finally { $sprite.Dispose() }
    } finally { $source.Dispose() }
}
