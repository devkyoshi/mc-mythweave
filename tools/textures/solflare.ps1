# Paints the original Solflare entity texture (64 x 64) to match SolflareModel's UV layout,
# plus its glow map.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\solflare.ps1
#
# Outputs (in src/main/resources/assets/mythweave/textures/entity/solflare/):
#   solflare.png       the main texture: cream and sun-gold fur, red-orange markings, flame wings
#   solflare_glow.png  same layout, transparent except the glowing parts (eyes, sun rune,
#                      crest, wings, tail, paws), drawn full-bright with a slow pulse.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z, the way the creature looks. A top face's last row touches the front face.
#
# Layout (u, v, w, h, d):
#   head        (0,  0,  6, 5, 6)     muzzle      (24, 0,  3, 2, 3)
#   right ear   (36, 0,  2, 5, 1)     left ear    (42, 0,  2, 5, 1)
#   crest, big  (48, 0,  1, 4, 1)     crest, small(52, 0,  1, 3, 1)
#   flame tongue(56, 0,  1, 2, 1)     tail plume  (60, 0,  1, 2, 1)
#   body        (0,  11, 5, 5, 11)    chest ruff  (32, 11, 6, 3, 3)
#   wing blades (50, 11, 1, 10, 2) (56, 11, 1, 8, 2)  (32, 17, 1, 6, 2)
#   tail base   (38, 17, 3, 3, 3)     tail segment(0,  27, 2, 2, 4)   tail tip (12, 27, 2, 2, 3)
#   legs        (0,  33, 2, 8, 2)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\entity\solflare\solflare.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette (from the concept sheet's Solflare swatches): cream and sun-gold fur, red-orange
# markings, and the line's ember/flame colors for everything that burns.
$cream        = @(252, 234, 200)
$creamLight   = @(255, 246, 222)
$creamShade   = @(232, 206, 164)
$gold         = @(246, 190, 86)
$goldDeep     = @(236, 146, 52)
$marking      = @(214, 72, 34)
$markingDeep  = @(160, 40, 28)
$eyeDark      = @(52, 26, 20)
$ember        = @(255, 184, 46)
$emberHot     = @(255, 240, 160)
$flameOrange  = @(255, 128, 32)
$flameRed     = @(226, 70, 26)
$flameDeep    = @(196, 44, 24)

$bmp     = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowBmp = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Deterministic per-pixel hash in 0..65535 (mixed so it shows no diagonal streaks).
function Hash([int]$x, [int]$y, [int]$salt) {
	[long]$h = ([long]$x * 374761393 + [long]$y * 668265263 + [long]$salt * 1013904223) -band 0xFFFFFFFFL
	$h = (($h -bxor ($h -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
	return ($h -bxor ($h -shr 16)) -band 0xFFFF
}

# Sets one pixel. Fur gets a soft brightness jitter (+/-4%); -Flat skips it.
# -Glow also copies the pixel into the glow map. A later call on the same pixel replaces both.
function Px([int]$x, [int]$y, $rgb, [switch]$Flat, [switch]$Glow) {
	$f = 1.0
	if (-not $Flat) { $f = 0.96 + ((Hash $x $y 0) -band 0xFF) / 255.0 * 0.08 }
	$c = $rgb | ForEach-Object { [Math]::Max(0, [Math]::Min(255, [int]($_ * $f))) }
	$color = [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2])
	$bmp.SetPixel($x, $y, $color)
	if ($Glow) { $glowBmp.SetPixel($x, $y, $color) } else { $glowBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent) }
}

function Fill([int]$x, [int]$y, [int]$w, [int]$h, $rgb, [switch]$Flat, [switch]$Glow) {
	for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { Px ($x + $i) ($y + $j) $rgb -Flat:$Flat -Glow:$Glow } }
}

function Fill-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $rgb, [switch]$Flat, [switch]$Glow) {
	Fill ($u + $d) $v (2 * $w) $d $rgb -Flat:$Flat -Glow:$Glow
	Fill $u ($v + $d) (2 * ($d + $w)) $h $rgb -Flat:$Flat -Glow:$Glow
}

function Flame-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill-Box $u $v $w $h $d $ember -Flat -Glow
	Fill ($u + $d) $v $w $d $emberHot -Flat -Glow
	Fill $u ($v + $d) (2 * ($d + $w)) 1 $emberHot -Flat -Glow
}

$flameMix = @($emberHot, $ember, $ember, $flameOrange, $flameOrange, $flameRed)
function Fire-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	for ($i = 0; $i -lt 2 * ($d + $w); $i++) {
		for ($j = 0; $j -lt ($d + $h); $j++) {
			if ($j -lt $d -and ($i -lt $d -or $i -ge $d + 2 * $w)) { continue }   # unused corners
			$x = $u + $i; $y = $v + $j
			Px $x $y $flameMix[(Hash $x $y 7) % $flameMix.Count] -Flat -Glow
		}
	}
}

# A flame blade (wing feather or crest): white-hot at the tip (top), through gold and orange,
# to deep red where it grows from the body. All of it glows.
$bladeRamp = @($emberHot, $ember, $ember, $flameOrange, $flameOrange, $flameRed, $flameRed, $flameDeep)
function Blade-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill ($u + $d) $v (2 * $w) $d $emberHot -Flat -Glow                   # top and bottom faces
	for ($j = 0; $j -lt $h; $j++) {
		$c = $bladeRamp[[int][Math]::Floor($j * $bladeRamp.Count / $h)]
		Fill $u ($v + $d + $j) (2 * ($d + $w)) 1 $c -Flat -Glow
	}
}

# ---- Head (0, 0, 6, 5, 6): top (6,0) 6x6, bottom (12,0) 6x6, side (0,6) 6x5, front (6,6) 6x5,
#      side (12,6) 6x5, back (18,6) 6x5.
Fill-Box 0 0 6 5 6 $cream
Fill 6 0 6 6 $gold                                   # golden crown...
Fill 8 0 2 6 $marking                                # ...with a red stripe under the crest
Fill 12 0 6 6 $creamLight                            # underside
Fill 0 6 6 1 $gold; Fill 12 6 6 1 $gold; Fill 18 6 6 1 $gold   # gold along the top edge
Fill 6 6 6 1 $goldDeep                               # brow
# Eyes: glowing ember eyes at columns 0-1 and 4-5 of row 2, a hot pixel on the inner side,
# and a red marking sweeping down and back from each.
Px 6 8 $ember -Flat -Glow; Px 7 8 $emberHot -Flat -Glow
Px 10 8 $emberHot -Flat -Glow; Px 11 8 $ember -Flat -Glow
Px 6 9 $marking; Px 11 9 $marking
Fill 2 8 4 1 $marking; Px 2 9 $marking               # ...continuing back along both sides (the
Fill 12 8 4 1 $marking; Px 15 9 $marking             # -x side's front is its last column)
Px 8 7 $ember -Flat -Glow; Px 9 7 $ember -Flat -Glow  # sun mark on the forehead
Fill 18 7 6 4 $creamShade                            # back of the head, in shadow under the crest

# ---- Muzzle (24, 0, 3, 2, 3): cream, dark nose on the top front.
Fill-Box 24 0 3 2 3 $creamLight
Px 28 2 $eyeDark -Flat                               # nose (top face, front row)
Px 28 3 $eyeDark -Flat                               # ...and the top of the front face

# ---- Ears (36/42, 0, 2, 5, 1): cream outside, glowing orange inside, flame-tipped.
foreach ($u in 36, 42) {
	Fill-Box $u 0 2 5 1 $cream
	Fill ($u + 1) 1 2 5 $flameOrange -Flat -Glow      # inner ear (front face)
	Fill ($u + 1) 4 2 2 $goldDeep                     # fur at the base of the inner ear
	Fill $u 1 6 1 $ember -Flat -Glow                  # flame tip, every side
	Fill ($u + 1) 0 2 1 $emberHot -Flat -Glow         # top
}

# ---- Crest blades and small flames.
Blade-Box 48 0 1 4 1
Blade-Box 52 0 1 3 1
Flame-Box 56 0 1 2 1                                 # flame tongues (tail)
Flame-Box 60 0 1 2 1                                 # tail plume

# ---- Body (0, 11, 5, 5, 11): top (11,11) 5x11, bottom (16,11) 5x11, side (0,22) 11x5,
#      front (11,22) 5x5, side (16,22) 11x5, back (27,22) 5x5.
Fill-Box 0 11 5 5 11 $cream
Fill 11 11 5 11 $gold                                # golden saddle along the back
Fill 16 11 5 11 $creamLight                          # belly
Fill 0 22 11 1 $gold; Fill 16 22 11 1 $gold; Fill 27 22 5 1 $gold   # saddle edge on the sides
# Sun rune on the back: a glowing ring with a hot centre, between the wings.
foreach ($p in @(@(13, 14), @(12, 15), @(14, 15), @(13, 16))) { Px $p[0] $p[1] $ember -Flat -Glow }
Px 13 15 $emberHot -Flat -Glow
Fill 13 11 1 3 $marking                              # red stripe down the spine to the tail
# Flank markings: a red flame shape on each side, mid-flank.
foreach ($s in 0, 16) {
	Fill ($s + 3) 23 5 1 $goldDeep
	Fill ($s + 3) 24 5 1 $marking
	Fill ($s + 4) 25 3 1 $markingDeep
}
Fill 11 22 5 5 $creamLight                           # chest
Fill 27 23 5 4 $creamShade                           # rump

# ---- Chest ruff (32, 11, 6, 3, 3): soft cream fluff, gold at the top.
Fill-Box 32 11 6 3 3 $creamLight
Fill 32 14 18 1 $cream

# ---- Wings and tail: fire.
Blade-Box 50 11 1 10 2
Blade-Box 56 11 1 8 2
Blade-Box 32 17 1 6 2
Fire-Box 38 17 3 3 3                                 # tail base
Fire-Box 0 27 2 2 4                                  # tail segments
Flame-Box 12 27 2 2 3                                # tail tip

# ---- Legs (0, 33, 2, 8, 2): cream above, gold-orange stockings, glowing flame paws.
Fill-Box 0 33 2 8 2 $cream
Fill 0 39 8 2 $gold
Fill 0 41 8 2 $goldDeep
Fill 0 42 8 1 $flameRed -Flat -Glow                  # paws: bottom row of every side glows
Fill 4 33 2 2 $ember -Flat -Glow                     # paw pads (bottom face)

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$glowOut = Join-Path (Split-Path $Out) 'solflare_glow.png'
$glowBmp.Save($glowOut, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $glowBmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
Write-Output "Wrote $((Resolve-Path $glowOut).Path)"
