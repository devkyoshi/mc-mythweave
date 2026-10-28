# Paints the Cindercub entity texture (64 x 64) to match CindercubModel's UV layout, plus its
# glow map. The look follows the concept sheets in docs/reference/cindercub/.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\cindercub.ps1
#
# Outputs (in src/main/resources/assets/mythweave/textures/entity/cindercub/):
#   cindercub.png       the main texture
#   cindercub_glow.png  same layout, transparent except the glowing parts (the ember tail and
#                       the eye shine). PulsingGlowLayer draws it full-bright with a slow
#                       pulse, so those parts light up in the dark.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z. A top face's last row touches the front face; on the sides, the columns
# next to the front face are u+d-1 and u+d+w.
#
# Layout (u, v, w, h, d):
#   head       (0,  0,  8, 6, 6)    muzzle     (28, 0,  4, 2, 1)
#   ear base   (48, 0,  3, 3, 3)    ear tip    (56, 12, 2, 2, 2)
#   body       (0,  12, 6, 5, 8)    leg        (28, 12, 2, 3, 2)    paw        (50, 17, 3, 2, 3)
#   tail root  (36, 12, 4, 4, 3)    tail tuft  (52, 12, 1, 2, 1)
#   tail mid   (0,  25, 8, 8, 7)    tail tip   (30, 25, 6, 6, 5)    tail flame (0,  40, 4, 4, 3)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\entity\cindercub\cindercub.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette, from the reference sheets: ember orange fur with red-orange and deep red shading,
# cream muzzle, cheeks, chest and belly; charcoal ears and paws; a tail that runs from deep
# red through orange and gold to a cream-hot tip.
$orangeLight = @(255, 162, 84)
$orange      = @(255, 138, 61)    # #FF8A3D
$orangeDeep  = @(255, 90, 31)     # #FF5A1F
$red         = @(200, 58, 30)
$gold        = @(255, 204, 102)   # #FFCC66
$cream       = @(255, 233, 176)   # #FFE9B0
$peach       = @(255, 178, 128)
$charcoal    = @(59, 47, 63)      # #3B2F3F
$ink         = @(31, 26, 37)      # #1F1A25
$white       = @(255, 255, 250)

$bmp     = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowBmp = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# A deterministic 0..65535 hash of a pixel position (plus a salt), for noise.
function Hash([int]$x, [int]$y, [int]$salt) {
	[long]$n = ([long]$x * 374761393 + [long]$y * 668265263 + [long]$salt * 1013904223) -band 0xFFFFFFFFL
	$n = (($n -bxor ($n -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
	return [int](($n -bxor ($n -shr 16)) -band 0xFFFF)
}

# Sets one pixel. Fur gets a deterministic brightness jitter (+/-6%) so it doesn't look like
# flat plastic; -Flat skips it (eyes, nose, flames). -Glow also copies the pixel into the glow
# map. A later call on the same pixel replaces both.
function Px([int]$x, [int]$y, $rgb, [switch]$Flat, [switch]$Glow) {
	$f = 1.0
	if (-not $Flat) { $f = 0.94 + ((Hash $x $y 0) -band 0xFF) / 255.0 * 0.12 }
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

# Fur: the blocky, patchy orange of the reference (light, mid and red-orange voxels mixed by a
# hash), over every face of a box.
$furMix = @($orangeLight, $orange, $orange, $orange, $orangeDeep)
function Fur-Px([int]$x, [int]$y) { Px $x $y $furMix[(Hash $x $y 7) % $furMix.Count] }
function Fur-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	for ($i = 0; $i -lt 2 * ($d + $w); $i++) {
		for ($j = 0; $j -lt ($d + $h); $j++) {
			if ($j -lt $d -and ($i -lt $d -or $i -ge $d + 2 * $w)) { continue }   # unused corners
			Fur-Px ($u + $i) ($v + $j)
		}
	}
}

# Ember tail: every pixel glows, colored by "heat" along the tail. $from is the heat where the
# box joins the previous segment (its front face), $to at its far end (its back face). A hashed
# offset breaks the bands up so the fire looks like flickering voxels, not stripes.
$heatRamp = @($red, $orangeDeep, $orange, $gold, $cream)
function Heat-Px([int]$x, [int]$y, [double]$heat) {
	$t = $heat + ((Hash $x $y 3) / 65535.0 - 0.5) * 0.35
	$i = [int][Math]::Round([Math]::Max(0.0, [Math]::Min(1.0, $t)) * ($heatRamp.Count - 1))
	Px $x $y $heatRamp[$i] -Flat -Glow
}
function Heat-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, [double]$from, [double]$to) {
	$span = [Math]::Max(1, $d - 1)
	for ($i = 0; $i -lt 2 * ($d + $w); $i++) {
		for ($j = 0; $j -lt ($d + $h); $j++) {
			$x = $u + $i; $y = $v + $j
			if ($j -lt $d) {
				if ($i -lt $d -or $i -ge $d + 2 * $w) { continue }               # unused corners
				$along = ($d - 1 - $j) / $span                                   # top and bottom
			} elseif ($i -lt $d) {
				$along = ($d - 1 - $i) / $span                                   # first side
			} elseif ($i -lt $d + $w) {
				$along = 0.0                                                     # front
			} elseif ($i -lt 2 * $d + $w) {
				$along = ($i - $d - $w) / $span                                  # second side
			} else {
				$along = 1.0                                                     # back
			}
			Heat-Px $x $y ($from + ($to - $from) * $along)
		}
	}
}

# ---- Head (0, 0, 8, 6, 6): top (6,0) 8x6, bottom (14,0) 8x6, side (0,6) 6x6,
#      front (6,6) 8x6, side (14,6) 6x6, back (20,6) 8x6.
Fur-Box 0 0 8 6 6
Fill 14 0 8 6 $cream                               # chin / underside
# Face (front, x 6..13, y 6..11). Rows 0-1: forehead; rows 2-3: eyes; rows 4-5: cream around
# the muzzle (the muzzle box covers its middle four columns).
Fill 7 7 2 1 $orangeDeep; Fill 11 7 2 1 $orangeDeep   # shadow over the eyes
foreach ($e in @(@(7, 8), @(11, 11))) {            # eyes at columns 1-2 and 5-6, shine on the inner top corner
	Fill $e[0] 8 2 2 $ink -Flat
	Px $e[1] 8 $white -Flat -Glow
}
Fill 6 8 1 2 $red; Fill 13 8 1 2 $red              # dark eye patches, outer side
Fill 8 10 4 2 $cream; Fill 7 11 6 1 $cream         # cream lower face around the muzzle
Fill 4 11 2 1 $cream; Fill 14 11 2 1 $cream        # ...just reaching round to the sides
Fill 0 11 2 1 $red; Fill 18 11 2 1 $red            # shadow under the jaw, toward the back
Fill 20 11 8 1 $red                                # back of the head: darker at the nape

# ---- Muzzle (28, 0, 4, 2, 1): top (29,0) 4x1, bottom (33,0), side (28,1) 1x2,
#      front (29,1) 4x2, side (33,1) 1x2, back (34,1) 4x2. Cream with a dark nose on the tip.
Fill-Box 28 0 4 2 1 $cream
Fill 30 1 2 1 $ink -Flat                           # nose, front face
Fill 30 0 2 1 $ink -Flat                           # nose, top face

# ---- Ear base (48, 0, 3, 3, 3): top (51,0) 3x3, side (48,3) 3x3, front (51,3) 3x3,
#      side (54,3) 3x3, back (57,3) 3x3. Charcoal, with a cream inner ear edged in red on the
#      front and a red band where it meets the head.
Fill-Box 48 0 3 3 3 $charcoal
Fill 51 4 3 2 $red                                 # inner ear...
Fill 52 3 1 3 $cream                               # ...with a cream centre
Fill 48 5 12 1 $red                                # base row on every side
Fill 52 5 1 1 $peach

# ---- Ear tip (56, 12, 2, 2, 2): top (58,12) 2x2, side (56,14) 2x2, front (58,14) 2x2,
#      side (60,14) 2x2, back (62,14) 2x2. Charcoal, darkest at the very top.
Fill-Box 56 12 2 2 2 $charcoal
Fill 58 12 2 2 $ink                                # top

# ---- Body (0, 12, 6, 5, 8): top (8,12) 6x8, bottom (14,12) 6x8, side (0,20) 8x5,
#      front (8,20) 6x5, side (14,20) 8x5, back (22,20) 6x5.
Fur-Box 0 12 6 5 8
Fill 14 12 6 8 $cream                              # belly
Fill 9 20 4 5 $cream; Fill 8 22 6 3 $cream         # cream chest bib
Fill 0 24 8 1 $red; Fill 14 24 8 1 $red            # red-orange shadow along the lower sides
Fill 5 23 3 2 $cream; Fill 14 23 3 2 $cream        # ...except where the belly fur shows at the front
Fill 22 24 6 1 $red

# ---- Legs (28, 12, 2, 3, 2): orange at the top, red then charcoal toward the paw.
Fur-Box 28 12 2 3 2
Fill 28 15 8 1 $red
Fill 28 16 8 1 $charcoal

# ---- Paws (50, 17, 3, 2, 3): top (53,17) 3x3, bottom (56,17) 3x3, sides (50,20) 12x2.
#      Chunky and charcoal, with ink pads.
Fill-Box 50 17 3 2 3 $charcoal
Fill 56 17 3 3 $ink                                # pads (bottom face)
Fill 50 21 12 1 $ink                               # bottom edge

# ---- Ember tail: a heat gradient from a deep-red root to a gold, cream-hot flame.
Heat-Box 36 12 4 4 3 0.0 0.2                       # root
Heat-Box 0 25 8 8 7 0.2 0.65                       # fluffy middle
Heat-Box 30 25 6 6 5 0.5 0.8                        # tip
Heat-Box 0 40 4 4 3 0.75 0.92                      # flame
Heat-Box 52 12 1 2 1 0.6 0.9                       # side tufts

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$glowOut = Join-Path (Split-Path $Out) 'cindercub_glow.png'
$glowBmp.Save($glowOut, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $glowBmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
Write-Output "Wrote $((Resolve-Path $glowOut).Path)"
