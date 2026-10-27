# Paints the original Cindercub entity texture (64 x 32) to match CindercubModel's UV layout,
# plus its glow map.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\generate_cindercub_texture.ps1
#
# Outputs (in src/main/resources/assets/mythweave/textures/entity/):
#   cindercub.png       the main texture
#   cindercub_glow.png  same layout, transparent except the glowing parts (rune markings,
#                       eye shine, crystals, flames). CindercubGlowLayer draws it full-bright
#                       with a slow pulse, so those parts light up in the dark.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z, the way the creature looks. A top face's last row touches the front face.
#
# Layout (u, v, w, h, d):
#   head        (0,  0,  7, 5, 5)    snout       (24, 0, 3, 2, 1)
#   right ear   (34, 0,  2, 4, 1)    left ear    (40, 0, 2, 4, 1)
#   horn        (50, 0,  1, 2, 1)
#   spine shard (54, 0,  1, 2, 1)    tail plume  (58, 0, 1, 2, 1)
#   body        (0,  11, 4, 4, 6)    fur tail    (22, 11, 3, 3, 5)   (wild)
#   fire base   (22, 20, 3, 3, 3)    fire mid    (36, 20, 2, 2, 3)   (bonded)
#   fire tip    (40, 11, 2, 2, 2)    fire tongue (48, 11, 1, 2, 1)
#   legs        (0,  23, 2, 3, 2)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\src\main\resources\assets\mythweave\textures\entity\cindercub.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette: warm orange fur, cream, burnt red, charcoal paws; ember-gold runes and flames;
# amber crystal.
$orange       = @(236, 124, 48)
$orangeLight  = @(247, 158, 84)
$red          = @(186, 68, 34)
$cream        = @(250, 222, 178)
$blush        = @(246, 150, 120)
$charcoal     = @(48, 40, 42)
$eyeDark      = @(40, 26, 24)
$white        = @(255, 255, 250)
$ember        = @(255, 184, 46)
$emberHot     = @(255, 240, 160)
$flameOrange  = @(255, 128, 32)
$flameRed     = @(226, 70, 26)
$crystalDeep  = @(232, 136, 40)
$crystal      = @(255, 200, 80)
$crystalLight = @(255, 238, 170)

$bmp  = New-Object System.Drawing.Bitmap 64, 32, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowBmp = New-Object System.Drawing.Bitmap 64, 32, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Sets one pixel. Fur gets a deterministic brightness jitter (+/-6%) so it doesn't look like
# flat plastic; -Flat skips it (eyes, runes, crystals, flames). -Glow also copies the pixel
# into the glow map. A later call on the same pixel replaces both.
function Px([int]$x, [int]$y, $rgb, [switch]$Flat, [switch]$Glow) {
	$f = 1.0
	if (-not $Flat) {
		[long]$h = ([long]$x * 374761393 + [long]$y * 668265263) -band 0xFFFFFFFFL
		$h = (($h -bxor ($h -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
		$f = 0.94 + (($h -shr 8) -band 0xFF) / 255.0 * 0.12
	}
	$c = $rgb | ForEach-Object { [Math]::Max(0, [Math]::Min(255, [int]($_ * $f))) }
	$color = [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2])
	$bmp.SetPixel($x, $y, $color)
	if ($Glow) { $glowBmp.SetPixel($x, $y, $color) } else { $glowBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent) }
}

function Fill([int]$x, [int]$y, [int]$w, [int]$h, $rgb, [switch]$Flat, [switch]$Glow) {
	for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { Px ($x + $i) ($y + $j) $rgb -Flat:$Flat -Glow:$Glow } }
}

# Fills all six faces of a box with one color; details are painted on top afterwards.
function Fill-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $rgb, [switch]$Flat, [switch]$Glow) {
	Fill ($u + $d) $v (2 * $w) $d $rgb -Flat:$Flat -Glow:$Glow
	Fill $u ($v + $d) (2 * ($d + $w)) $h $rgb -Flat:$Flat -Glow:$Glow
}

# Paints a small flame box: hot top, gold sides fading to deeper ember at the base.
function Flame-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill-Box $u $v $w $h $d $ember -Flat -Glow
	Fill ($u + $d) $v $w $d $emberHot -Flat -Glow                         # top
	Fill $u ($v + $d) (2 * ($d + $w)) 1 $emberHot -Flat -Glow             # top row of the sides
}

# Paints a crystal shard: light top and left edge, deep amber at the base.
function Crystal-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill-Box $u $v $w $h $d $crystal -Flat -Glow
	Fill ($u + $d) $v $w $d $crystalLight -Flat -Glow                     # top
	Fill $u ($v + $d) 1 $h $crystalLight -Flat -Glow                      # a lit facet
	Fill $u ($v + $d + $h - 1) (2 * ($d + $w)) 1 $crystalDeep -Flat -Glow # base row
}

# ---- Head (0, 0, 7, 5, 5): faces top (5,0) 7x5, side (0,5) 5x5, front (5,5) 7x5,
#      side (12,5) 5x5, back (17,5) 7x5, bottom (12,0) 7x5.
Fill-Box 0 0 7 5 5 $orange
Fill 12 0 7 5 $cream                              # chin / underside
Fill 0 8 5 2 $cream; Fill 12 8 5 2 $cream         # cheek fluff low on both sides
# Face (front, x 5..11, y 5..9). Row 0: brow; rows 1-2: eyes; rows 3-4: cheeks and muzzle.
# Big glossy eyes, 2x2 each, at columns 1-2 and 4-5: dark, with a glowing white shine in
# the top-left corner. Column 3 between them carries the forehead rune.
foreach ($ex in 6, 9) {
	Fill $ex 6 2 2 $eyeDark -Flat
	Px $ex 6 $white -Flat -Glow
}
Px 8 5 $ember -Flat -Glow                         # forehead rune: a short line from the brow
Px 8 4 $ember -Flat -Glow                         # ...back over the head top
Fill 5 8 2 2 $cream; Fill 10 8 2 2 $cream          # cheeks
Px 5 8 $blush; Px 11 8 $blush                      # blush
Fill 17 5 7 1 $red                                 # back of the head: a burnt-red crest

# ---- Snout (24, 0, 3, 2, 1): cream muzzle with a dark nose on top of the front face.
Fill-Box 24 0 3 2 1 $cream
Fill 25 0 3 1 $orangeLight                         # top blends into the face
Px 26 1 $charcoal -Flat                            # nose

# ---- Ears (34/40, 0, 2, 4, 1): orange outside, burnt-red inside, glowing ember tips.
foreach ($u in 34, 40) {
	Fill-Box $u 0 2 4 1 $orange
	Fill ($u + 1) 2 2 3 $red                       # inner ear (front face, below the tip)
	Fill $u 1 6 1 $ember -Flat -Glow               # tip row on every side face
	Fill ($u + 1) 0 4 1 $ember -Flat -Glow         # top and bottom faces
}

# ---- Small flame and crystals.
Flame-Box 58 0 1 2 1                               # tail plume
Crystal-Box 50 0 1 2 1                             # horns
Crystal-Box 54 0 1 2 1                             # spine shards

# ---- Body (0, 11, 4, 4, 6): top (6,11) 4x6, bottom (10,11), side (0,17) 6x4,
#      front (6,17) 4x4, side (10,17) 6x4, back (16,17) 4x4.
Fill-Box 0 11 4 4 6 $orange
Fill 10 11 4 6 $cream                              # belly
Fill 6 17 4 4 $cream                               # chest
Fill 7 11 2 6 $red                                 # burnt-red stripe down the back
# Side runes: a small diamond on each flank (same on both sides).
foreach ($sx in 0, 10) {
	foreach ($p in @(@(3, 0), @(2, 1), @(4, 1), @(3, 2))) { Px ($sx + $p[0]) (17 + $p[1]) $ember -Flat -Glow }
}

# ---- Wild fur tail (22, 11, 3, 3, 5): top (27,11) 3x5, bottom (30,11), side (22,16) 5x3,
#      front (27,16) 3x3, side (30,16) 5x3, back (35,16) 3x3. A plain fox tail: orange,
#      darker underneath, with a cream tip. On the top/bottom faces row 11 is the tip end; on
#      the sides it's column 22 (min-x side) and column 34 (max-x side).
Fill-Box 22 11 3 3 5 $orange
Fill 30 11 3 5 $red                                # underside
Fill 22 18 5 1 $red; Fill 30 18 5 1 $red           # lower row of the sides
Fill 27 11 3 1 $cream; Fill 30 11 3 1 $cream       # tip, top and bottom
Fill 22 16 1 3 $cream; Fill 34 16 1 3 $cream       # tip, sides
Fill 35 16 3 3 $cream                              # tip, end face

# ---- Fire tail (shown once bonded), all glowing. Each pixel is picked from the flame
# colors by a hash, so the fire looks broken up rather than striped; with the model's
# flicker and the glow's breathing it reads as moving fire.
$flameMix = @($emberHot, $ember, $ember, $flameOrange, $flameOrange, $flameRed)
function Fire-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	$cols = 2 * ($d + $w)
	for ($i = 0; $i -lt $cols; $i++) {
		for ($j = 0; $j -lt ($d + $h); $j++) {
			$x = $u + $i; $y = $v + $j
			if ($j -lt $d -and ($i -lt $d -or $i -ge $d + 2 * $w)) { continue }   # unused corners
			[long]$n = ([long]$x * 374761393 + [long]$y * 668265263 + 1013904223) -band 0xFFFFFFFFL
			$n = (($n -bxor ($n -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
			$n = ($n -bxor ($n -shr 16)) -band 0xFFFF
			Px $x $y $flameMix[$n % $flameMix.Count] -Flat -Glow
		}
	}
}
Fire-Box 22 20 3 3 3                               # base segment
Fire-Box 36 20 2 2 3                               # middle segment
Flame-Box 40 11 2 2 2                              # tip segment (hotter)
Flame-Box 48 11 1 2 1                              # flame tongues

# ---- Legs (0, 23, 2, 3, 2): orange with charcoal paws.
Fill-Box 0 23 2 3 2 $orange
Fill 0 27 8 1 $charcoal                            # paws
Fill 4 23 2 2 $charcoal                            # paw pads (bottom face)

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$glowOut = Join-Path (Split-Path $Out) 'cindercub_glow.png'
$glowBmp.Save($glowOut, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $glowBmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
Write-Output "Wrote $((Resolve-Path $glowOut).Path)"
