$ErrorActionPreference = 'Stop'
throw 'Retired: this template generator overwrites individual artwork. Use industrial-art-v3-prompts.json and import-industrial-art.ps1 instead.'
$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root 'src/main/resources/assets/capitalismmod/textures/item'
$modelDir = Join-Path $root 'src/main/resources/assets/capitalismmod/models/item'
New-Item -ItemType Directory -Force -Path $itemDir, $modelDir | Out-Null
Add-Type -AssemblyName System.Drawing

$source = Get-Content (Join-Path $root 'src/main/java/com/ailudick/capitalismmod/init/ModItems.java') -Raw
$start = $source.IndexOf('ITEMS.register("steel_sheet"')
$industrialSource = $source.Substring($start)
$ids = [regex]::Matches($industrialSource, 'ITEMS\.register\("([a-z0-9_]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$ids = @($ids + 'copper_wire','steel_sheet','laptop','smartphone','television','wireless_router' | Sort-Object -Unique)

function Fill([Drawing.Graphics]$g, [Drawing.Brush]$b, [int]$x, [int]$y, [int]$w, [int]$h) { $g.FillRectangle($b,$x,$y,$w,$h) }
function Brush([int]$r,[int]$g,[int]$b) { return [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(255,$r,$g,$b)) }

function Get-Theme([string]$id) {
    if ($id -match 'cobalt') { return @(45,130,220) }
    if ($id -match 'manganese') { return @(170,85,70) }
    if ($id -match 'chromium') { return @(175,205,220) }
    if ($id -match 'titanium') { return @(100,190,185) }
    if ($id -match 'tungsten') { return @(95,105,125) }
    if ($id -match 'molybdenum') { return @(120,135,210) }
    if ($id -match 'fluorite') { return @(170,85,220) }
    if ($id -match 'rare_earth') { return @(225,95,175) }
    if ($id -match 'uranium') { return @(100,205,90) }
    if ($id -match 'copper') { return @(205,105,55) }
    if ($id -match 'lead') { return @(100,110,135) }
    if ($id -match 'nickel') { return @(155,180,125) }
    if ($id -match 'zinc') { return @(115,185,205) }
    if ($id -match 'tin') { return @(180,190,200) }
    if ($id -match 'aluminum|alumina') { return @(190,205,220) }
    if ($id -match 'graphite|carbon') { return @(70,75,95) }
    if ($id -match 'sulfur') { return @(235,195,45) }
    if ($id -match 'phosphate|phosphoric|fertilizer') { return @(220,175,65) }
    if ($id -match 'acid|chlor|oxide|peroxide|ammonia|nitrogen|oxygen|hydrogen|gas|air|argon|solvent|water|brine|ethanol|methanol|acetone') { return @(80,180,220) }
    if ($id -match 'wafer|chip|pcb|electronic|photoresist|resin|poly|plastic|rubber|fiber|film|foam|tire') { return @(120,100,205) }
    if ($id -match 'tablet|pharmaceutical|medical|culture|yeast|glucose|pesticide|herbicide|fungicide') { return @(75,185,115) }
    return @(200,125,60)
}

foreach ($id in $ids) {
    $bmp = [Drawing.Bitmap]::new(16,16)
    $g = [Drawing.Graphics]::FromImage($bmp)
    $g.Clear([Drawing.Color]::Transparent)
    $t = Get-Theme $id; $seed=[math]::Abs($id.GetHashCode())
    $outline=Brush 20 24 34; $shadow=Brush ([math]::Max(20,$t[0]-65)) ([math]::Max(20,$t[1]-65)) ([math]::Max(20,$t[2]-65)); $mid=Brush $t[0] $t[1] $t[2]; $hi=Brush ([math]::Min(255,$t[0]+65)) ([math]::Min(255,$t[1]+65)) ([math]::Min(255,$t[2]+65)); $glint=Brush 245 235 175
    $ore = $id -match 'ore$|^raw_|graphite$|quartz_sand|phosphate$|potash$|limestone$|tailings|sludge|carbon_black'
    $metal = $id -match 'ingot|cathode|foil|wire|frame|laminate|coil|casing|alumina|silicon_ingot|concentrate'
    $powder = $id -match 'powder|fertilizer|carbonate|activated_carbon|sulfur$|phosphate$|potash$'
    $fluid = $id -match 'acid|gas|oil|diesel|gasoline|lpg|benzene|ethanol|methanol|acetone|water|brine|solvent|ammonia|oxygen|nitrogen|hydrogen|chlorine|oxide|peroxide|soda_ash|sodium_'
    $chip = $id -match 'chip|wafer|pcb|electronic|photoresist|display|module|battery|smartphone|television|laptop|router|speaker|adapter|power_bank|circuit'
    if ($ore) {
        Fill $g $shadow 2 3 12 10; Fill $g $mid 3 4 10 8; Fill $g $hi 4 5 3 2
        for($i=0;$i -lt 10;$i++){ $x=3+(($seed+$i*5)%10);$y=4+(($seed/7+$i*3)%8);$fleck=$outline;if($i%3 -eq 0){$fleck=$hi};$fw=1;if($i%4 -eq 0){$fw=2};Fill $g $fleck $x $y $fw 1 }
    } elseif ($metal) {
        Fill $g $outline 2 5 12 7; Fill $g $shadow 3 4 10 7; Fill $g $mid 4 4 8 5; Fill $g $hi 5 4 5 1; Fill $g $glint 5 5 2 1; Fill $g $outline 4 10 8 1
    } elseif ($powder) {
        Fill $g $outline 2 9 12 4; Fill $g $shadow 3 8 10 4; Fill $g $mid 4 7 8 4; Fill $g $hi 5 7 3 1; Fill $g $glint 8 8 1 1; Fill $g $shadow 3 12 10 1
    } elseif ($fluid) {
        Fill $g $outline 6 2 4 3; Fill $g $outline 4 5 8 9; Fill $g $shadow 5 6 6 7; Fill $g $mid 6 7 4 5; Fill $g $hi 6 7 2 2; Fill $g $glint 7 7 1 1
    } elseif ($chip) {
        Fill $g $outline 2 3 12 11; Fill $g $shadow 3 4 10 9; Fill $g $mid 4 5 8 7; Fill $g $hi 5 5 3 1; Fill $g $glint 5 7 1 1
        for($i=0;$i -lt 4;$i++){Fill $g $hi (4+$i*2) 12 1 1; Fill $g $outline (3+$i*3) 4 1 1}
    } else {
        Fill $g $outline 2 3 12 11; Fill $g $shadow 3 4 10 9; Fill $g $mid 4 5 8 6; Fill $g $hi 5 5 4 1; Fill $g $glint 5 7 1 1; Fill $g $outline 4 12 8 1
    }
    $g.Dispose(); $out=Join-Path $itemDir "$id.png"; $bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
    Set-Content -LiteralPath (Join-Path $modelDir "$id.json") -Value ('{"parent":"minecraft:item/generated","textures":{"layer0":"capitalismmod:item/' + $id + '"}}') -Encoding UTF8
}
Write-Output "Redrew $($ids.Count) industrial item textures with category-specific pixel art."
