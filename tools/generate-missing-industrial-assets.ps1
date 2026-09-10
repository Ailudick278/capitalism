$ErrorActionPreference = 'Stop'
throw 'Retired: this placeholder generator overwrites individual artwork. Use industrial-art-v3-prompts.json and import-industrial-art.ps1 instead.'

$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root 'src/main/resources/assets/capitalismmod/textures/item'
$modelDir = Join-Path $root 'src/main/resources/assets/capitalismmod/models/item'
$blockTextureDir = Join-Path $root 'src/main/resources/assets/capitalismmod/textures/block'
$blockModelDir = Join-Path $root 'src/main/resources/assets/capitalismmod/models/block'
$blockStateDir = Join-Path $root 'src/main/resources/assets/capitalismmod/blockstates'
New-Item -ItemType Directory -Force -Path $itemDir, $modelDir, $blockTextureDir, $blockModelDir, $blockStateDir | Out-Null

$source = Get-Content (Join-Path $root 'src/main/java/com/ailudick/capitalismmod/init/ModItems.java') -Raw
$registered = [regex]::Matches($source, 'ITEMS\.register\("([a-z0-9_]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$script:broken = @('copper_wire','steel_sheet','laptop','smartphone','television','wireless_router')
$ids = @($registered | Where-Object { -not (Test-Path (Join-Path $modelDir "$_.json")) })
$ids = @($ids + $script:broken | Sort-Object -Unique)

Add-Type -AssemblyName System.Drawing

function Get-Color([int]$h, [int]$s, [int]$v) {
    $hi = [math]::Floor($h / 60) % 6
    $f = $h / 60 - [math]::Floor($h / 60)
    $p = $v * (1 - $s / 255)
    $q = $v * (1 - $s / 255 * $f)
    $t = $v * (1 - $s / 255 * (1 - $f))
    switch ($hi) {
        0 { return [Drawing.Color]::FromArgb(255,[int]$v,[int]$t,[int]$p) }
        1 { return [Drawing.Color]::FromArgb(255,[int]$q,[int]$v,[int]$p) }
        2 { return [Drawing.Color]::FromArgb(255,[int]$p,[int]$v,[int]$t) }
        3 { return [Drawing.Color]::FromArgb(255,[int]$p,[int]$q,[int]$v) }
        4 { return [Drawing.Color]::FromArgb(255,[int]$t,[int]$p,[int]$v) }
        default { return [Drawing.Color]::FromArgb(255,[int]$v,[int]$p,[int]$q) }
    }
}

function Get-Palette([string]$id) {
    if ($id -match 'ore|raw_|bauxite|limestone|phosphate|potash|fluorite|graphite|quartz|tailings|sludge|carbon_black') { return @(32, 160, 200) }
    if ($id -match 'acid|chlor|oxide|peroxide|ammonia|nitrogen|oxygen|hydrogen|gas|air|argon|solvent|water|brine|ethanol|methanol|acetone|formaldehyde|ethylene|propylene|sulfur') { return @(185, 190, 220) }
    if ($id -match 'ingot|copper|nickel|cobalt|manganese|chromium|titanium|tungsten|molybdenum|zinc|lead|silicon|cathode|foil|frame|laminate') { return @(210, 150, 190) }
    if ($id -match 'wafer|chip|pcb|electronic|photoresist|resin|poly|plastic|rubber|fiber|film|foam|tire') { return @(250, 150, 245) }
    if ($id -match 'tablet|pharmaceutical|medical|culture|yeast|glucose|fertilizer|pesticide|herbicide|fungicide') { return @(95, 175, 120) }
    return @(45, 165, 240)
}

foreach ($id in $ids) {
    $bmp = New-Object Drawing.Bitmap 16,16
    $base = Get-Palette $id
    $seed = [math]::Abs($id.GetHashCode())
    $hue = ($base[0] + ($seed % 35)) % 360
    $sat = $base[1]
    $val = $base[2]
    $dark = Get-Color $hue $sat ([math]::Max(35, $val - 70))
    $mid = Get-Color $hue $sat $val
    $light = Get-Color $hue ([math]::Max(70, $sat - 35)) ([math]::Min(255, $val + 45))
    $accent = Get-Color (($hue + 42) % 360) ([math]::Max(80, $sat - 20)) ([math]::Min(255, $val + 20))
    for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $bmp.SetPixel($x,$y,[Drawing.Color]::Transparent) } }
    for ($y=3; $y -le 12; $y++) {
        for ($x=3; $x -le 12; $x++) {
            $edge = ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12)
            $c = if ($edge) { $dark } elseif ((($x*13 + $y*7 + $seed) % 17) -lt 3) { $light } else { $mid }
            $bmp.SetPixel($x,$y,$c)
        }
    }
    for ($i=0; $i -lt 5; $i++) {
        $x = 4 + (($seed + $i*3) % 8); $y = 4 + (($seed/7 + $i*5) % 8)
        $bmp.SetPixel($x,$y,$accent)
    }
    $bmp.SetPixel(4,4,$light); $bmp.SetPixel(11,11,$dark)
    $out = Join-Path $itemDir "$id.png"
    $bmp.Save($out, [Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $json = '{"parent":"minecraft:item/generated","textures":{"layer0":"capitalismmod:item/' + $id + '"}}'
    Set-Content -LiteralPath (Join-Path $modelDir "$id.json") -Value $json -Encoding UTF8
}

# Complete the world-facing assets for the newly registered industrial ores.
$oreBlocks = @(
    'cobalt_ore','deepslate_cobalt_ore','manganese_ore','deepslate_manganese_ore',
    'chromium_ore','deepslate_chromium_ore','titanium_ore','deepslate_titanium_ore',
    'tungsten_ore','deepslate_tungsten_ore','molybdenum_ore','deepslate_molybdenum_ore',
    'fluorite_ore','deepslate_fluorite_ore','rare_earth_ore','deepslate_rare_earth_ore',
    'uranium_ore','deepslate_uranium_ore'
)
foreach ($id in $oreBlocks) {
    $bmp = New-Object Drawing.Bitmap 16,16
    $base = Get-Palette $id
    $seed = [math]::Abs($id.GetHashCode())
    $hue = ($base[0] + ($seed % 35)) % 360
    $dark = Get-Color $hue $base[1] ([math]::Max(28, $base[2] - 85))
    $mid = Get-Color $hue $base[1] ([math]::Max(45, $base[2] - 30))
    $light = Get-Color $hue ([math]::Max(70, $base[1] - 25)) ([math]::Min(255, $base[2] + 20))
    for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
        $c = if ((($x*11 + $y*17 + $seed) % 13) -lt 3) { $light } elseif ((($x+$y+$seed) % 7) -lt 2) { $dark } else { $mid }
        $bmp.SetPixel($x,$y,$c)
    }}
    $bmp.Save((Join-Path $blockTextureDir "$id.png"), [Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Set-Content -LiteralPath (Join-Path $blockModelDir "$id.json") -Value ('{"parent":"minecraft:block/cube_all","textures":{"all":"capitalismmod:block/' + $id + '"}}') -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $blockStateDir "$id.json") -Value ('{"variants":{"":{"model":"capitalismmod:block/' + $id + '"}}}') -Encoding UTF8
}

Write-Output "Generated $($ids.Count) industrial item textures/models and $($oreBlocks.Count) ore block assets."
