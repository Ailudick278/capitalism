param(
    [Parameter(Mandatory=$true)][string]$Id,
    [Parameter(Mandatory=$true)][string]$Source
)
$ErrorActionPreference = 'Stop'
if ($Id -notmatch '^[a-z0-9_]+$') { throw 'Invalid asset ID' }
$project = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $project 'src/main/resources/assets/capitalismmod'
$archive = Join-Path $project 'tmp/industrial-art-v3'
$backup = Join-Path $archive 'backup'
$originals = Join-Path $archive 'originals'
New-Item -ItemType Directory -Force -Path $backup,$originals | Out-Null
$destination = Join-Path $assets "textures/item/$Id.png"
$backupFile = Join-Path $backup "$Id.png"
if ((Test-Path -LiteralPath $destination) -and !(Test-Path -LiteralPath $backupFile)) {
    Copy-Item -LiteralPath $destination -Destination $backupFile
}
Copy-Item -LiteralPath $Source -Destination (Join-Path $originals "$Id.png") -Force
Add-Type -AssemblyName System.Drawing
$sourceImage = [Drawing.Bitmap]::new($Source)
$outputImage = [Drawing.Bitmap]::new(64,64,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [Drawing.Graphics]::FromImage($outputImage)
try {
    $graphics.Clear([Drawing.Color]::Transparent)
    $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($sourceImage,[Drawing.Rectangle]::new(0,0,64,64),0,0,$sourceImage.Width,$sourceImage.Height,[Drawing.GraphicsUnit]::Pixel)
    $transparent = 0
    for ($y=0;$y -lt 64;$y++) { for ($x=0;$x -lt 64;$x++) { if ($outputImage.GetPixel($x,$y).A -eq 0) { $transparent++ } } }
    if ($transparent -lt 100) { throw "Generated asset $Id lacks transparent background; requires review" }
    $outputImage.Save($destination,[Drawing.Imaging.ImageFormat]::Png)
    Write-Output "$Id imported at 64x64; transparent pixels=$transparent"
} finally {
    $graphics.Dispose()
    $outputImage.Dispose()
    $sourceImage.Dispose()
}
