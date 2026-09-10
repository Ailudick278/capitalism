param([Parameter(Mandatory=$true)][ValidateSet('paint','brass','steel','wheel','base','label')][string]$Material,
      [Parameter(Mandatory=$true)][string]$Source)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$destination=Join-Path $root 'src/main/resources/assets/capitalismmod/textures/block/gas_cylinder'
$archive=Join-Path $root 'tmp/gas-cylinder/material-originals'
New-Item -ItemType Directory -Force -Path $destination,$archive | Out-Null
Copy-Item -LiteralPath $Source -Destination (Join-Path $archive "$Material.png") -Force
$inputImage=[Drawing.Bitmap]::new($Source)
$height=128
if($Material -eq 'label'){$height=192}
$outputImage=[Drawing.Bitmap]::new(128,$height,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g=[Drawing.Graphics]::FromImage($outputImage)
try {
 $g.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy
 $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
 $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::HighQuality
 $attributes=[Drawing.Imaging.ImageAttributes]::new()
 try {
  $attributes.SetWrapMode([Drawing.Drawing2D.WrapMode]::TileFlipXY)
  $g.DrawImage($inputImage,[Drawing.Rectangle]::new(0,0,128,$height),0,0,$inputImage.Width,$inputImage.Height,[Drawing.GraphicsUnit]::Pixel,$attributes)
 } finally {$attributes.Dispose()}
 $outputImage.Save((Join-Path $destination "$Material.png"),[Drawing.Imaging.ImageFormat]::Png)
} finally {$g.Dispose();$outputImage.Dispose();$inputImage.Dispose()}
Write-Output "Imported cylinder material: $Material (128 x $height)"
