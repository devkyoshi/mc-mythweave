# Paints the original Emberlynx entity texture (64 x 64) to match EmberlynxModel's UV layout,
# plus its glow map.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\emberlynx.ps1
#
# Outputs (in src/main/resources/assets/mythweave/textures/entity/emberlynx/):
#   emberlynx.png       the main texture
#   emberlynx_glow.png  same layout, transparent except the glowing parts (runes, eye shine,
#                       crystals, flames), drawn full-bright with a slow pulse by PulsingGlowLayer.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z, the way the creature looks. A top face's last row touches the front face.
#
# Layout (u, v, w, h, d):
#   head        (0,  0,  7, 5, 6)     snout       (26, 0,  3, 2, 2)
#   right ear   (36, 0,  2, 5, 1)     left ear    (42, 0,  2, 5, 1)
#   ear tuft    (48, 0,  1, 2, 1)     flame tongue(48, 4,  1, 2, 1)   (tail + mane)
#   horn        (52, 0,  1, 4, 1)     spine shard (56, 0,  1, 2, 1)   tail plume (60, 0, 1, 2, 1)
#   body        (0,  12, 5, 5, 10)    fire mane   (30, 12, 6, 3, 3)
#   tail base   (30, 18, 3, 3, 3)     tail segment(42, 18, 2, 2, 3)   tail tip   (52, 18, 2, 2, 2)
#   legs        (0,  28, 2, 6, 2)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\entity\emberlynx\emberlynx.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette: the Cindercub's colors, a shade deeper (it's grown up), plus the same ember, flame
# and crystal colors so the line reads as one family.
$orange       = @(228, 94, 36)
$orangeLight  = @(244, 136, 64)
$red          = @(176, 46, 28)
$cream        = @(248, 214, 168)
$charcoal     = @(44, 36, 38)
$eyeDark      = @(36, 22, 20)
$white        = @(255, 255, 250)
$ember        = @(255, 184, 46)
$emberHot     = @(255, 240, 160)
$flameOrange  = @(255, 128, 32)
$flameRed     = @(226, 70, 26)
$crystalDeep  = @(232, 136, 40)
$crystal      = @(255, 200, 80)
$crystalLight = @(255, 238, 170)

$bmp     = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowBmp = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Deterministic per-pixel hash in 0..65535 (mixed so it shows no diagonal streaks).
function Hash([int]$x, [int]$y, [int]$salt) {
	[long]$h = ([long]$x * 374761393 + [long]$y * 668265263 + [long]$salt * 1013904223) -band 0xFFFFFFFFL
	$h = (($h -bxor ($h -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
	return ($h -bxor ($h -shr 16)) -band 0xFFFF
}

# Sets one pixel. Fur gets a brightness jitter (+/-6%); -Flat skips it. -Glow also copies the
# pixel into the glow map. A later call on the same pixel replaces both.
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

# A small flame box: hot top, gold sides.
function Flame-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill-Box $u $v $w $h $d $ember -Flat -Glow
	Fill ($u + $d) $v $w $d $emberHot -Flat -Glow
	Fill $u ($v + $d) (2 * ($d + $w)) 1 $emberHot -Flat -Glow
}

# A crystal shard: light top and left edge, deep amber at the base.
function Crystal-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill-Box $u $v $w $h $d $crystal -Flat -Glow
	Fill ($u + $d) $v $w $d $crystalLight -Flat -Glow
	Fill $u ($v + $d) 1 $h $crystalLight -Flat -Glow
	Fill $u ($v + $d + $h - 1) (2 * ($d + $w)) 1 $crystalDeep -Flat -Glow
}

# A box of moving fire: each pixel picked from the flame colors by hash (no stripes).
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

# ---- Head (0, 0, 7, 5, 6): top (6,0) 7x6, side (0,6) 6x5, front (6,6) 7x5, side (13,6),
#      back (19,6) 7x5, bottom (13,0) 7x6.
Fill-Box 0 0 7 5 6 $orange
Fill 13 0 7 6 $cream                               # chin
Fill 0 9 6 2 $cream; Fill 13 9 6 2 $cream          # cheek fluff on both sides
# Face (front, x 6..12, y 6..10): row 0 brow + rune; rows 1-2 eyes; rows 3-4 cheeks/muzzle.
# Eyes 2x2 at columns 1-2 and 4-5: white shine and dark on top, glowing ember iris below.
foreach ($ex in 7, 10) {
	Px $ex 7 $white -Flat -Glow
	Px ($ex + 1) 7 $eyeDark -Flat
	Px $ex 8 $ember -Flat -Glow; Px ($ex + 1) 8 $ember -Flat -Glow
}
Px 9 6 $ember -Flat -Glow                          # forehead rune: down the brow...
Px 9 5 $ember -Flat -Glow; Px 9 4 $ember -Flat -Glow   # ...back over the head top...
Px 8 3 $ember -Flat -Glow; Px 10 3 $ember -Flat -Glow  # ...forking like a flame
Fill 6 9 2 2 $cream; Fill 11 9 2 2 $cream          # cheeks
Fill 19 6 7 1 $red                                 # back of the head: crest

# ---- Snout (26, 0, 3, 2, 2): cream muzzle, dark nose.
Fill-Box 26 0 3 2 2 $cream
Fill 28 0 3 2 $orangeLight                         # top blends into the face
Px 29 2 $charcoal -Flat                            # nose

# ---- Ears (36/42, 0, 2, 5, 1): orange, burnt-red inside, ember tips (the flame tuft sits on top).
foreach ($u in 36, 42) {
	Fill-Box $u 0 2 5 1 $orange
	Fill ($u + 1) 2 2 4 $red
	Fill $u 1 6 1 $ember -Flat -Glow
	Fill ($u + 1) 0 4 1 $ember -Flat -Glow
}

# ---- Flames and crystals.
Flame-Box 48 0 1 2 1                               # ear tufts
Flame-Box 48 4 1 2 1                               # flame tongues (tail, mane)
Flame-Box 60 0 1 2 1                               # tail plume
Crystal-Box 52 0 1 4 1                             # horns
Crystal-Box 56 0 1 2 1                             # spine shards

# ---- Body (0, 12, 5, 5, 10): top (10,12) 5x10, bottom (15,12), side (0,22) 10x5,
#      front (10,22) 5x5, side (15,22) 10x5, back (25,22) 5x5.
Fill-Box 0 12 5 5 10 $orange
Fill 15 12 5 10 $cream                             # belly
Fill 10 22 5 5 $cream                              # chest
Fill 12 12 1 10 $red                               # burnt-red stripe down the back
Fill 0 26 10 1 $cream; Fill 15 26 10 1 $cream      # pale lower edge of the flanks
# Flank runes: a flame glyph, mirrored the same on both sides.
foreach ($sx in 0, 15) {
	foreach ($p in @(@(4, 0), @(3, 1), @(5, 1), @(4, 2), @(6, 2), @(7, 3))) { Px ($sx + $p[0]) (22 + $p[1]) $ember -Flat -Glow }
}
Px 12 23 $ember -Flat -Glow                        # chest rune

# ---- Fire: mane and tail.
Fire-Box 30 12 6 3 3                               # mane collar
Fire-Box 30 18 3 3 3                               # tail base
Fire-Box 42 18 2 2 3                               # tail segments
Flame-Box 52 18 2 2 2                              # tail tip

# ---- Legs (0, 28, 2, 6, 2): orange with charcoal socks and pads.
Fill-Box 0 28 2 6 2 $orange
Fill 0 34 8 2 $charcoal                            # socks (bottom two rows of every side)
Fill 4 28 2 2 $charcoal                            # paw pads (bottom face)

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$glowOut = Join-Path (Split-Path $Out) 'emberlynx_glow.png'
$glowBmp.Save($glowOut, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $glowBmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
Write-Output "Wrote $((Resolve-Path $glowOut).Path)"
