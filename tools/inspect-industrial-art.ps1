$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$project = Split-Path -Parent $PSScriptRoot
$archive = Join-Path $project 'tmp/industrial-art-v3'
$originals = Join-Path $archive 'originals'
$files = @(Get-ChildItem -LiteralPath $originals -Filter '*.png' | Sort-Object Name)
$font = [Drawing.Font]::new('Consolas',9)
$brush = [Drawing.SolidBrush]::new([Drawing.Color]::White)
$completed = 0
for ($page=0; $page -lt [math]::Ceiling($files.Count/48); $page++) {
    $sheet=[Drawing.Bitmap]::new(1024,1008)
    $g=[Drawing.Graphics]::FromImage($sheet)
    $g.Clear([Drawing.Color]::FromArgb(42,45,52))
    $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
    try {
        for($i=0;$i -lt 48;$i++) {
            $index=$page*48+$i
            if($index -ge $files.Count){break}
            $id=$files[$index].BaseName
            $path=Join-Path $project "src/main/resources/assets/capitalismmod/textures/item/$id.png"
            $bmp=[Drawing.Bitmap]::new($path)
            try {
                if($bmp.Width -ne 64 -or $bmp.Height -ne 64){throw "Wrong size: $id"}
                $x=($i%8)*128
                $y=[math]::Floor($i/8)*168
                $g.DrawImage($bmp,[Drawing.Rectangle]::new($x,$y,128,128),0,0,64,64,[Drawing.GraphicsUnit]::Pixel)
                $g.DrawString($id.Replace('_',' '),$font,$brush,[Drawing.RectangleF]::new($x+3,$y+128,123,39))
                $completed++
            } finally { $bmp.Dispose() }
        }
        $sheet.Save((Join-Path $archive "contact-$page.png"),[Drawing.Imaging.ImageFormat]::Png)
    } finally {$g.Dispose();$sheet.Dispose()}
}
$brush.Dispose();$font.Dispose()
Write-Output "Inspected $completed 64x64 item textures; contact sheets in $archive"
