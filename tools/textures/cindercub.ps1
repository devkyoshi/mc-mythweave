# Paints the Cindercub entity texture (128 x 128) to match CindercubModel's UV layout, plus its
# glow map. The look follows the concept art in docs/reference/cindercub/ (the "In-Game Preview"
# of Design Sheet-3 is the target for how it should look in a world). The model is built at
# 1.5x detail (see CindercubModel), so one texel here is 2/3 of a Minecraft pixel in game.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\cindercub.ps1
#
# Outputs (in src/main/resources/assets/mythweave/textures/entity/cindercub/):
#   cindercub.png       the main texture
#   cindercub_glow.png  same layout, transparent except the ember tail. PulsingGlowLayer draws
#                       it with the "eyes" render type, which ADDS it on top of the lit texture
#                       (and shader packs bloom it), so it holds only a dimmed copy of the
#                       hottest colors: the tail keeps its orange-red and only its gold core
#                       glows. Painting full colors here washes the tail out to white.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z. A top face's last row touches the front face; on the sides, the columns
# next to the front face are u+d-1 and u+d+w.
#
# Layout (u, v, w, h, d):
#   head       (0,  0,  10, 8, 9)    snout      (38, 0,  4, 3, 3)    cheek      (52, 0,  1, 3, 3)
#   ear base   (60, 0,  6, 3, 3)     ear middle (78, 0,  4, 3, 2)    ear tip    (90, 0,  2, 2, 2)
#   tail tuft  (110, 0, 2, 3, 2)     tail flame (98, 0,  3, 4, 3)    chest      (38, 8,  6, 5, 2)
#   leg        (54, 8,  3, 7, 3)     paw        (66, 8,  4, 3, 4)    tail root  (82, 8,  5, 5, 4)
#   body       (0,  20, 9, 8, 15)    tail mid   (48, 20, 11, 11, 8)  tail upper (86, 20, 9, 9, 7)
#   tail tip   (0,  44, 6, 6, 5)     eyelid     (110, 8, 2, 3, 0)   (a flat plane: front 2x3 at 110,8)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\entity\cindercub\cindercub.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette, from the concept art's in-game look: deep ember orange fur shading through
# red-orange and red to maroon underneath; warm cream snout and chest (pale cream turns white
# under shader lighting); warm near-black browns for ears and paws (a purple-grey charcoal
# reads purple in game); a tail from maroon through orange to a gold core.
$orangeLight = @(250, 146, 64)
$orange      = @(236, 104, 42)
$orangeDeep  = @(204, 66, 30)
$red         = @(160, 46, 26)
$maroon      = @(108, 32, 24)
$gold        = @(255, 190, 80)
$hot         = @(255, 226, 150)
$cream       = @(240, 207, 160)
$creamLight  = @(247, 222, 182)
$creamShade  = @(217, 168, 120)
$brownLit    = @(90, 59, 48)
$brown       = @(62, 42, 37)
$brownDark   = @(42, 29, 27)
$white       = @(255, 250, 240)

$size = 128
$bmp     = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowBmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# A deterministic 0..65535 hash of a pixel position (plus a salt), for noise.
function Hash([int]$x, [int]$y, [int]$salt) {
	[long]$n = ([long]$x * 374761393 + [long]$y * 668265263 + [long]$salt * 1013904223) -band 0xFFFFFFFFL
	$n = (($n -bxor ($n -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
	return [int](($n -bxor ($n -shr 16)) -band 0xFFFF)
}

# Sets one pixel. Fur gets a deterministic brightness jitter (+/-5%) so it doesn't look like
# flat plastic; -Flat skips it (eyes, nose, flames). -Glow also writes the pixel into the glow
# map, scaled by -GlowAmt (the glow is added on top in game, so less is more). A later call
# on the same pixel replaces both.
function Px([int]$x, [int]$y, $rgb, [switch]$Flat, [switch]$Glow, [double]$GlowAmt = 1.0) {
	$f = 1.0
	if (-not $Flat) { $f = 0.95 + ((Hash $x $y 0) -band 0xFF) / 255.0 * 0.1 }
	$c = $rgb | ForEach-Object { [Math]::Max(0, [Math]::Min(255, [int]($_ * $f))) }
	$bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2]))
	if ($Glow -and $GlowAmt -gt 0) {
		$g = $c | ForEach-Object { [int]($_ * $GlowAmt) }
		$glowBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $g[0], $g[1], $g[2]))
	} else {
		$glowBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
	}
}

function Fill([int]$x, [int]$y, [int]$w, [int]$h, $rgb, [switch]$Flat) {
	for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { Px ($x + $i) ($y + $j) $rgb -Flat:$Flat } }
}

# Fills all six faces of a box with one color; details are painted on top afterwards.
function Fill-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $rgb, [switch]$Flat) {
	Fill ($u + $d) $v (2 * $w) $d $rgb -Flat:$Flat
	Fill $u ($v + $d) (2 * ($d + $w)) $h $rgb -Flat:$Flat
}

# Calls $paint for every used pixel of a box's net with: x, y, the face ('top', 'bottom',
# 'side', 'front', 'back'), how far along the box's depth it is (0 at the front, 1 at the
# back) and how far down (0 at the top row, 1 at the bottom row). $paint runs in the caller's
# scope chain, so it can read the caller's variables.
function For-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, [scriptblock]$paint) {
	$dSpan = [Math]::Max(1, $d - 1); $hSpan = [Math]::Max(1, $h - 1)
	for ($i = 0; $i -lt 2 * ($d + $w); $i++) {
		for ($j = 0; $j -lt ($d + $h); $j++) {
			$px = $u + $i; $py = $v + $j
			if ($j -lt $d) {
				if ($i -lt $d -or $i -ge $d + 2 * $w) { continue }                   # unused corners
				$face = if ($i -lt $d + $w) { 'top' } else { 'bottom' }
				& $paint $px $py $face (($d - 1 - $j) / $dSpan) 0.0
				continue
			}
			$down = ($j - $d) / $hSpan
			if ($i -lt $d)                { & $paint $px $py 'side' (($d - 1 - $i) / $dSpan) $down }
			elseif ($i -lt $d + $w)       { & $paint $px $py 'front' 0.0 $down }
			elseif ($i -lt 2 * $d + $w)   { & $paint $px $py 'side' (($i - $d - $w) / $dSpan) $down }
			else                          { & $paint $px $py 'back' 1.0 $down }
		}
	}
}

# Painted shading, like the concept art: light on top faces, sides darkening row by row toward
# the bottom, dark undersides. $ramp runs light to dark; a hashed offset breaks up the bands.
function Shade-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $ramp, [double]$noise = 0.9) {
	For-Box $u $v $w $h $d {
		param($x, $y, $face, $along, $down)
		$t = switch ($face) { 'top' { 0.0 } 'bottom' { 1.0 } default { 0.15 + 0.85 * $down } }
		$k = $t * ($ramp.Count - 1) + ((Hash $x $y 11) / 65535.0 - 0.5) * $noise
		Px $x $y $ramp[[int][Math]::Round([Math]::Max(0, [Math]::Min($ramp.Count - 1, $k)))]
	}
}

# Ember tail, colored by "heat". $from is the heat where the box joins the previous segment
# (its front face), $to at its far end; tops run hotter and undersides cooler, so the plume is
# gold above and maroon beneath. Only the hotter colors go into the glow map, dimmed
# ($heatGlow per color), so the tail glows from its core instead of washing out.
$heatRamp = @($maroon, $red, $orangeDeep, $orange, $gold, $hot)
$heatGlow = @(0.0, 0.0, 0.12, 0.25, 0.5, 0.6)
function Heat-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d, [double]$from, [double]$to) {
	For-Box $u $v $w $h $d {
		param($x, $y, $face, $along, $down)
		$vert = switch ($face) { 'top' { 0.15 } 'bottom' { -0.35 } default { 0.15 - 0.45 * $down } }
		$t = $from + ($to - $from) * $along + $vert + ((Hash $x $y 3) / 65535.0 - 0.5) * 0.3
		$k = [int][Math]::Round([Math]::Max(0.0, [Math]::Min(1.0, $t)) * ($heatRamp.Count - 1))
		Px $x $y $heatRamp[$k] -Flat -Glow -GlowAmt $heatGlow[$k]
	}
}

$fur     = @($orangeLight, $orange, $orange, $orangeDeep, $red)
$faceFur = @($orangeLight, $orange, $orange, $orangeDeep)
$creamy  = @($creamLight, $cream, $creamShade)
$dark    = @($brownLit, $brown, $brownDark)

# ---- Head (0, 0, 10, 8, 9): top (9,0) 10x9, bottom (19,0) 10x9, side (0,9) 9x8,
#      front (9,9) 10x8, side (19,9) 9x8, back (28,9) 10x8.
Shade-Box 0 0 10 8 9 $fur
Fill 19 0 10 9 $cream                              # chin / underside
# Face (front, x 9..18, y 9..16): bright orange. Row 1: red brow; rows 2-4: eyes at columns
# 2-3 and 6-7 with dark red patches beside them; rows 6-7: cream cheeks beside the snout
# (the snout box covers columns 3-6 of rows 5-7).
for ($x = 9; $x -le 18; $x++) { for ($y = 9; $y -le 14; $y++) { Px $x $y $faceFur[(Hash $x $y 5) % $faceFur.Count] } }
Fill 10 10 3 1 $red; Fill 15 10 3 1 $red           # brow
Fill 10 11 1 3 $red; Fill 17 11 1 3 $red           # eye patches, outer side
Fill 8 11 1 3 $red; Fill 19 11 1 3 $red            # ...wrapping round the head's front corners
foreach ($e in @(@(11, 12), @(15, 15))) {          # 2x3 eyes, a white shine on the inner top corner
	Fill $e[0] 11 2 3 $brownDark -Flat
	Px $e[1] 11 $white -Flat
}
Fill 9 15 10 2 $orange                             # lower face, beside the snout
Fill 10 15 2 2 $cream; Fill 16 15 2 2 $cream       # cream cheeks
Fill 7 15 2 2 $cream; Fill 19 15 2 2 $cream        # ...just reaching round to the sides

# ---- Snout (38, 0, 4, 3, 3): top (41,0) 4x3, bottom (45,0), side (38,3) 3x3,
#      front (41,3) 4x3, side (45,3) 3x3, back (48,3) 4x3. Warm cream with a dark nose on its
#      tip; the top blends back into the orange forehead.
Shade-Box 38 0 4 3 3 $creamy 0.6
Fill 41 0 4 1 $orangeLight                         # top face, back row
Fill 42 2 2 1 $brownDark -Flat                     # nose, top face (front row)
Fill 42 3 2 1 $brownDark -Flat                     # nose, front face

# ---- Cheek ruffs (52, 0, 1, 3, 3): orange fur, darker underneath.
Shade-Box 52 0 1 3 3 $fur

# ---- Ears: dark brown, with a red-orange inner ear on the front of the lower two tiers and a
#      warm base where they grow out of the head.
# Base (60, 0, 6, 3, 3): top (63,0) 6x3, side (60,3) 3x3, front (63,3) 6x3, side (69,3) 3x3,
#      back (72,3) 6x3.
Shade-Box 60 0 6 3 3 $dark 0.5
Fill 64 3 4 3 $orangeDeep                          # inner ear
Fill 65 4 2 2 $orange
Fill 60 5 3 1 $red; Fill 69 5 3 1 $red             # warm base on the sides
# Middle (78, 0, 4, 3, 2): top (80,0) 4x2, side (78,2) 2x3, front (80,2) 4x3, side (84,2),
#      back (86,2).
Shade-Box 78 0 4 3 2 $dark 0.5
Fill 81 3 2 2 $red                                 # inner ear
# Tip (90, 0, 2, 2, 2): darkest.
Fill-Box 90 0 2 2 2 $brownDark

# ---- Eyelids (110, 8, 2, 3, 0): flat planes over the eyes, shown only while it sleeps. Orange
#      fur with the closed eye as a dark line along the bottom; front (110,8), back (112,8).
Fill 110 8 4 2 $orange
Fill 110 9 4 1 $orangeDeep
Fill 110 10 4 1 $brownDark

# ---- Chest bib (38, 8, 6, 5, 2): warm cream, shading down.
Shade-Box 38 8 6 5 2 $creamy 0.6

# ---- Body (0, 20, 9, 8, 15): top (15,20) 9x15, bottom (24,20) 9x15, side (0,35) 15x8,
#      front (15,35) 9x8, side (24,35) 15x8, back (39,35) 9x8.
Shade-Box 0 20 9 8 15 $fur

# ---- Leg (54, 8, 3, 7, 3): orange at the shoulder, through red into dark socks.
Shade-Box 54 8 3 7 3 @($orange, $orange, $orangeDeep, $red, $maroon, $brown, $brownDark) 0.5
# ---- Paw (66, 8, 4, 3, 4): chunky and dark, with darker pads underneath.
Shade-Box 66 8 4 3 4 $dark 0.5
Fill 74 8 4 4 $brownDark                           # pads (bottom face)

# ---- Ember tail: a heat gradient from a maroon root to a gold flame.
Heat-Box 82 8 5 5 4 0.0 0.15                       # root
Heat-Box 48 20 11 11 8 0.12 0.45                   # bushy middle
Heat-Box 86 20 9 9 7 0.4 0.7                       # upper
Heat-Box 0 44 6 6 5 0.62 0.85                      # tip
Heat-Box 98 0 3 4 3 0.8 1.0                        # flame
Heat-Box 110 0 2 3 2 0.55 0.9                      # side tufts

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$glowOut = Join-Path (Split-Path $Out) 'cindercub_glow.png'
$glowBmp.Save($glowOut, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $glowBmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
Write-Output "Wrote $((Resolve-Path $glowOut).Path)"
