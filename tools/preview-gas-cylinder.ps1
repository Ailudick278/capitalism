$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
public static class CylinderTexturePreview {
 public static void Triangle(Bitmap target, Bitmap texture, PointF a, PointF b, PointF c,
   PointF ta, PointF tb, PointF tc, double shade) {
  double d=(b.Y-c.Y)*(a.X-c.X)+(c.X-b.X)*(a.Y-c.Y);
  if(Math.Abs(d)<0.0001)return;
  int minX=Math.Max(0,(int)Math.Floor(Math.Min(a.X,Math.Min(b.X,c.X))));
  int maxX=Math.Min(target.Width-1,(int)Math.Ceiling(Math.Max(a.X,Math.Max(b.X,c.X))));
  int minY=Math.Max(0,(int)Math.Floor(Math.Min(a.Y,Math.Min(b.Y,c.Y))));
  int maxY=Math.Min(target.Height-1,(int)Math.Ceiling(Math.Max(a.Y,Math.Max(b.Y,c.Y))));
  for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
   double p=((b.Y-c.Y)*(x+.5-c.X)+(c.X-b.X)*(y+.5-c.Y))/d;
   double q=((c.Y-a.Y)*(x+.5-c.X)+(a.X-c.X)*(y+.5-c.Y))/d;
   double r=1-p-q;
   if(p<-.001||q<-.001||r<-.001)continue;
   double u=(p*ta.X+q*tb.X+r*tc.X)/16, v=(p*ta.Y+q*tb.Y+r*tc.Y)/16;
   int tx=Math.Max(0,Math.Min(texture.Width-1,(int)(u*texture.Width)));
   int ty=Math.Max(0,Math.Min(texture.Height-1,(int)(v*texture.Height)));
   Color s=texture.GetPixel(tx,ty);
   target.SetPixel(x,y,Color.FromArgb(255,(int)(s.R*shade),(int)(s.G*shade),(int)(s.B*shade)));
  }
 }
}
'@
$root=Split-Path -Parent $PSScriptRoot
$model=Get-Content (Join-Path $root 'src/main/resources/assets/capitalismmod/models/item/gas_cylinder.json') -Raw | ConvertFrom-Json
$colors=@{body=@(30,119,129);rim=@(50,56,61);brass=@(192,147,53);steel=@(184,194,200);wheel=@(167,39,38);black=@(25,27,28);white=@(227,230,224);green=@(104,186,57)}
$colors.label=@(255,255,255)
$textureImages=@{}
foreach($t in $model.textures.PSObject.Properties){
 if($t.Value.StartsWith('capitalismmod:')){
  $path=Join-Path $root ('src/main/resources/assets/capitalismmod/textures/'+$t.Value.Replace('capitalismmod:','')+'.png')
  if(Test-Path -LiteralPath $path){$textureImages[$t.Name]=[Drawing.Bitmap]::new($path)}
 }
}
$polygons=@()
foreach($e in $model.elements){
 $a=$e.from;$b=$e.to
 $vertices=@(@($a[0],$a[1],$a[2]),@($b[0],$a[1],$a[2]),@($b[0],$b[1],$a[2]),@($a[0],$b[1],$a[2]),@($a[0],$a[1],$b[2]),@($b[0],$a[1],$b[2]),@($b[0],$b[1],$b[2]),@($a[0],$b[1],$b[2]))
 $faces=@{north=@(0,1,2,3);south=@(5,4,7,6);west=@(4,0,3,7);east=@(1,5,6,2);up=@(3,2,6,7);down=@(4,5,1,0)}
 foreach($face in $e.faces.PSObject.Properties){
  $normals=@{north=@(0,0,-1);south=@(0,0,1);west=@(-1,0,0);east=@(1,0,0);up=@(0,1,0);down=@(0,-1,0)}
  $normal=$normals[$face.Name];$nx=$normal[0];$nz=$normal[2]
  if($e.rotation){$angle=$e.rotation.angle*[math]::PI/180;$nx=$normal[0]*[math]::Cos($angle)+$normal[2]*[math]::Sin($angle);$nz=-$normal[0]*[math]::Sin($angle)+$normal[2]*[math]::Cos($angle)}
  if($nx*.4-$nz*.8+$normal[1]*.3 -le 0){continue}
  $points=@();$depth=0
  foreach($index in $faces[$face.Name]){
   $v=$vertices[$index];$x=[double]$v[0];$y=[double]$v[1];$z=[double]$v[2]
   if($e.rotation){$o=$e.rotation.origin;$angle=$e.rotation.angle*[math]::PI/180;$px=$x-$o[0];$pz=$z-$o[2];$x=$o[0]+$px*[math]::Cos($angle)+$pz*[math]::Sin($angle);$z=$o[2]-$px*[math]::Sin($angle)+$pz*[math]::Cos($angle)}
   $points += [Drawing.PointF]::new([single](300+($x-8)*13+($z-8)*7),[single](610-$y*16+($x-8)*2.5-($z-8)*4))
   $depth += $x*.4-$z*.8+$y*.3
  }
  $c=$colors[$face.Value.texture.TrimStart('#')]
  $factor=switch($face.Name){north{.88}south{.7}west{.7}east{.8}up{1.0}down{.55}}
  $polygons += [pscustomobject]@{Points=[Drawing.PointF[]]$points;Depth=$depth;Texture=$face.Value.texture.TrimStart('#');UV=$face.Value.uv;Shade=$factor;Color=[Drawing.Color]::FromArgb([int]($c[0]*$factor),[int]($c[1]*$factor),[int]($c[2]*$factor))}
 }
}
$bmp=[Drawing.Bitmap]::new(600,700)
$g=[Drawing.Graphics]::FromImage($bmp)
$g.Clear([Drawing.Color]::FromArgb(37,41,47))
$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
foreach($poly in ($polygons | Sort-Object Depth)){
 if($textureImages.ContainsKey($poly.Texture)){
  $u=$poly.UV
  if(!$u){$u=@(0,0,16,16)}
  $uv=@([Drawing.PointF]::new($u[0],$u[3]),[Drawing.PointF]::new($u[2],$u[3]),[Drawing.PointF]::new($u[2],$u[1]),[Drawing.PointF]::new($u[0],$u[1]))
  $p=$poly.Points;$t=$textureImages[$poly.Texture]
  [CylinderTexturePreview]::Triangle($bmp,$t,$p[0],$p[1],$p[2],$uv[0],$uv[1],$uv[2],$poly.Shade)
  [CylinderTexturePreview]::Triangle($bmp,$t,$p[0],$p[2],$p[3],$uv[0],$uv[2],$uv[3],$poly.Shade)
  continue
 }
 $brush=[Drawing.SolidBrush]::new($poly.Color)
 $g.FillPolygon($brush,$poly.Points)
 $brush.Dispose()
}
New-Item -ItemType Directory -Force -Path (Join-Path $root 'tmp/gas-cylinder') | Out-Null
$bmp.Save((Join-Path $root 'tmp/gas-cylinder/model-preview.png'),[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$bmp.Dispose()
foreach($t in $textureImages.Values){$t.Dispose()}
