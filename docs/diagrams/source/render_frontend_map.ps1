param([double]$Scale = 1.6)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$sceneFile = Join-Path $PSScriptRoot 'frontend-map.scene.json'
$scene = Get-Content -LiteralPath $sceneFile -Encoding UTF8 -Raw | ConvertFrom-Json
$outDir = Split-Path -Parent $PSScriptRoot
$pngPath = Join-Path $outDir 'frontend-mindmap.png'
$previewPath = Join-Path $PSScriptRoot 'frontend-map-preview.png'
$bitmap = New-Object System.Drawing.Bitmap([int]($scene.width * $Scale), [int]($scene.height * $Scale))
$bitmap.SetResolution(144, 144)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.ScaleTransform([single]$Scale, [single]$Scale)
$format = [System.Drawing.StringFormat]::GenericTypographic.Clone()
$format.FormatFlags = $format.FormatFlags -bor [System.Drawing.StringFormatFlags]::MeasureTrailingSpaces -bor [System.Drawing.StringFormatFlags]::NoWrap
$fontCache = @{}
$overflows = @()

function Get-MapColor([string]$Hex) { return [System.Drawing.ColorTranslator]::FromHtml($Hex) }
function Get-MapPoints($Values) {
    $result = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    foreach ($value in $Values) { $result.Add([System.Drawing.PointF]::new([single]$value[0], [single]$value[1])) }
    return ,$result.ToArray()
}

foreach ($s in $scene.shapes) {
    switch ($s.type) {
        'rect' {
            $path = New-Object System.Drawing.Drawing2D.GraphicsPath
            $r = [single]$s.radius
            $x = [single]$s.x; $y = [single]$s.y; $w = [single]$s.w; $h = [single]$s.h
            if ($r -gt 0) {
                $d = 2 * $r
                $path.AddArc($x, $y, $d, $d, 180, 90)
                $path.AddArc(($x + $w - $d), $y, $d, $d, 270, 90)
                $path.AddArc(($x + $w - $d), ($y + $h - $d), $d, $d, 0, 90)
                $path.AddArc($x, ($y + $h - $d), $d, $d, 90, 90)
                $path.CloseFigure()
            } else { $path.AddRectangle([System.Drawing.RectangleF]::new($x, $y, $w, $h)) }
            $brush = [System.Drawing.SolidBrush]::new((Get-MapColor $s.fill))
            $pen = [System.Drawing.Pen]::new((Get-MapColor $s.stroke), [single]$s.sw)
            $g.FillPath($brush, $path); $g.DrawPath($pen, $path)
            $pen.Dispose(); $brush.Dispose(); $path.Dispose()
        }
        'line' {
            $pen = [System.Drawing.Pen]::new((Get-MapColor $s.color), [single]$s.sw)
            $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
            if ($s.dashed) { $pen.DashPattern = @([single]3, [single]3) }
            $points = Get-MapPoints $s.points
            $g.DrawLines($pen, $points)
            $pen.Dispose()
        }
        'polygon' {
            $points = Get-MapPoints $s.points
            $brush = [System.Drawing.SolidBrush]::new((Get-MapColor $s.fill))
            $pen = [System.Drawing.Pen]::new((Get-MapColor $s.stroke), [single]$s.sw)
            $g.FillPolygon($brush, $points); $g.DrawPolygon($pen, $points)
            $pen.Dispose(); $brush.Dispose()
        }
        'text' {
            $fontKey = "$($s.font)|$($s.size)|$($s.bold)"
            if (-not $fontCache.ContainsKey($fontKey)) {
                $style = if ($s.bold) { [System.Drawing.FontStyle]::Bold } else { [System.Drawing.FontStyle]::Regular }
                $fontCache[$fontKey] = [System.Drawing.Font]::new($s.font, [single]$s.size, $style, [System.Drawing.GraphicsUnit]::Pixel)
            }
            $font = $fontCache[$fontKey]
            $extent = $g.MeasureString($s.text, $font, [System.Drawing.PointF]::new(0, 0), $format)
            if ($s.max_width -and $extent.Width -gt ([double]$s.max_width + 1)) {
                $overflows += [PSCustomObject]@{ text=$s.text; width=[Math]::Round($extent.Width,1); limit=$s.max_width; x=$s.x; y=$s.y }
            }
            if (($s.x + $extent.Width) -gt $scene.width -or ($s.y + $extent.Height) -gt $scene.height) { throw "Text exceeds canvas: $($s.text)" }
            $brush = [System.Drawing.SolidBrush]::new((Get-MapColor $s.color))
            $g.DrawString($s.text, $font, $brush, [System.Drawing.PointF]::new([single]$s.x, [single]$s.y), $format)
            $brush.Dispose()
        }
    }
}
$g.Dispose()
$bitmap.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
$previewWidth = 1500
$previewHeight = [int]($bitmap.Height * $previewWidth / $bitmap.Width)
$preview = New-Object System.Drawing.Bitmap($previewWidth, $previewHeight)
$pg = [System.Drawing.Graphics]::FromImage($preview)
$pg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$pg.DrawImage($bitmap, 0, 0, $previewWidth, $previewHeight)
$preview.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$pg.Dispose(); $preview.Dispose()
foreach ($font in $fontCache.Values) { $font.Dispose() }
$format.Dispose(); $bitmap.Dispose()
if ($overflows.Count -gt 0) { $overflows | ConvertTo-Json; throw "$($overflows.Count) text rows exceed their allocated width." }
Get-Item -LiteralPath $pngPath | Select-Object FullName, Length
Write-Output "Rendered $([int]($scene.width * $Scale)) x $([int]($scene.height * $Scale)); checked all text widths."
