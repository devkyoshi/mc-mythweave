# Paints the original Cindercub entity texture (64 x 32) to match CindercubModel's UV layout.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\generate_cindercub_texture.ps1
#
# This is a placeholder-quality texture. For hand-painted art, open the PNG in Blockbench
# or any pixel editor; the layout below is what the model expects.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z, which is the direction the creature looks.
#
# Layout (u, v, w, h, d):
#   head      (0,  0,  5, 4, 4)    snout     (20, 0,  3, 2, 2)
#   right ear (32, 0,  2, 3, 1)    left ear  (40, 0,  2, 3, 1)
#   body      (0,  10, 4, 4, 7)    tail      (24, 10, 3, 3, 5)
#   ember tip (42, 10, 2, 2, 2)    legs      (0,  24, 2, 3, 2)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\src\main\resources\assets\mythweave\textures\entity\cindercub.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette (spec §3: warm orange, burnt red, charcoal accents, ember glow).
$orange   = @(232, 118, 43)
$red      = @(181, 64, 30)
$charcoal = @(46, 42, 43)
$cream    = @(243, 211, 163)
$ember    = @(255, 190, 50)
$emberHot = @(255, 241, 168)

$bmp = New-Object System.Drawing.Bitmap 64, 32, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Deterministic per-pixel brightness jitter (about +/-6%) so faces read as fur, not flat plastic.
function Set-Px([int]$x, [int]$y, $rgb, [bool]$flat = $false) {
	$f = 1.0
	if (-not $flat) { $f = 0.94 + ((($x * 73856093) -bxor ($y * 19349663)) -band 0xFF) / 255.0 * 0.12 }
	$c = $rgb | ForEach-Object { [Math]::Max(0, [Math]::Min(255, [int]($_ * $f))) }
	$bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2]))
}

function Fill([int]$x, [int]$y, [int]$w, [int]$h, $rgb, [bool]$flat = $false) {
	for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { Set-Px ($x + $i) ($y + $j) $rgb $flat } }
}

# Fills all six faces of a box with one color; details are painted on top afterwards.
function Fill-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $rgb) {
	Fill ($u + $d) $v $w $d $rgb
	Fill ($u + $d + $w) $v $w $d $rgb
	Fill $u ($v + $d) (2 * ($d + $w)) $h $rgb
}

# Head: orange, cream underside, dark eyes, cream cheeks.
Fill-Box 0 0 5 4 4 $orange
Fill 9 0 5 4 $cream                      # bottom
Set-Px 5 5 $charcoal $true; Set-Px 7 5 $charcoal $true   # eyes (front face starts at 4,4)
Fill 4 7 5 1 $cream                      # cheeks / jaw line
Fill 0 4 4 1 $red; Fill 9 4 4 1 $red     # burnt-red streak across both sides of the head

# Snout: cream with a charcoal nose on the front.
Fill-Box 20 0 3 2 2 $cream
Fill 22 0 3 2 $orange                    # top of the snout blends into the head
Set-Px 23 2 $charcoal $true

# Ears: orange outside, burnt-red inside, charcoal tips.
foreach ($u in 32, 40) {
	Fill-Box $u 0 2 3 1 $orange
	Fill ($u + 1) 2 2 2 $red             # inner ear (front face, below the tip row)
	Fill $u 1 6 1 $charcoal              # tip row on every side face
	Fill ($u + 1) 0 4 1 $charcoal        # top and bottom faces
}

# Body: orange, cream belly and chest, burnt-red stripe along the back.
Fill-Box 0 10 4 4 7 $orange
Fill 11 10 4 7 $cream                    # belly (bottom face)
Fill 8 10 2 7 $red                       # back stripe (centre of top face)
Fill 7 17 4 4 $cream                     # chest (front face)

# Tail: burnt red, orange on top; the end that meets the ember tip glows.
Fill-Box 24 10 3 3 5 $red
Fill 29 10 3 5 $orange                   # top
Fill 37 15 3 3 $ember                    # back face, under the ember tip

# Ember tip: bright and flat (no fur jitter) so it reads as light, with a hot core.
Fill-Box 42 10 2 2 2 $ember
for ($x = 42; $x -lt 50; $x++) { for ($y = 10; $y -lt 14; $y++) { Set-Px $x $y $ember $true } }
Fill 44 10 2 2 $emberHot $true           # top
Fill 48 12 2 2 $emberHot $true           # back (the very tip)

# Legs: orange tops fading into charcoal "socks".
Fill-Box 0 24 2 3 2 $orange
Fill 0 27 8 2 $charcoal                  # lower two rows of all side faces
Fill 4 24 2 2 $charcoal                  # paw pads (bottom face)

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
