# Paints the original Ashenox entity texture (64 x 64) to match AshenoxModel's UV layout,
# plus its glow map.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\ashenox.ps1
#
# Outputs (in src/main/resources/assets/mythweave/textures/entity/ashenox/):
#   ashenox.png       the main texture: obsidian armor cracked with magma
#   ashenox_glow.png  same layout, transparent except the glowing parts (magma veins, eyes,
#                     horn tips, lava paws, fire), drawn full-bright with a slow pulse.
#
# How Minecraft maps a box (width w, height h, depth d) at texOffs(u, v):
#   top    (u+d,     v)    w x d     bottom (u+d+w,   v)    w x d
#   side   (u,       v+d)  d x h     front  (u+d,     v+d)  w x h
#   side   (u+d+w,   v+d)  d x h     back   (u+2d+w,  v+d)  w x h
# "front" faces -z, the way the creature looks. A top face's last row touches the front face.
#
# Layout (u, v, w, h, d):
#   head        (0,  0,  7, 6, 7)     jaw         (28, 0,  4, 3, 3)
#   right ear   (42, 0,  2, 3, 1)     left ear    (48, 0,  2, 3, 1)
#   horn base   (54, 0,  2, 4, 2)     horn tip    (28, 6,  1, 3, 1)
#   flame tongue(32, 6,  1, 2, 1)     tail plume  (36, 6,  1, 2, 1)
#   body        (0,  13, 7, 6, 12)    back plate  (38, 13, 8, 2, 4)   magma mane (38, 19, 8, 4, 4)
#   tail base   (0,  31, 4, 4, 4)     tail mid    (16, 31, 3, 3, 4)
#   tail mid 2  (30, 31, 3, 3, 3)     tail tip    (42, 31, 2, 2, 2)
#   legs        (0,  39, 3, 6, 3)

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\entity\ashenox\ashenox.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Palette: obsidian and ash for the armor, the line's ember/flame/crystal colors for the magma.
$obsidian      = @(42, 30, 48)
$obsidianLight = @(70, 54, 80)
$ash           = @(92, 80, 84)
$ashLight      = @(130, 116, 116)
$eyeDark       = @(24, 14, 16)
$magmaDeep     = @(200, 60, 20)
$ember         = @(255, 184, 46)
$emberHot      = @(255, 240, 160)
$flameOrange   = @(255, 128, 32)
$flameRed      = @(226, 70, 26)
$crystalDeep   = @(232, 136, 40)
$crystal       = @(255, 200, 80)
$crystalLight  = @(255, 238, 170)

$bmp     = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowBmp = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Deterministic per-pixel hash in 0..65535 (mixed so it shows no diagonal streaks).
function Hash([int]$x, [int]$y, [int]$salt) {
	[long]$h = ([long]$x * 374761393 + [long]$y * 668265263 + [long]$salt * 1013904223) -band 0xFFFFFFFFL
	$h = (($h -bxor ($h -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
	return ($h -bxor ($h -shr 16)) -band 0xFFFF
}

# Sets one pixel. Armor gets a brightness jitter (+/-8%, rougher than fur); -Flat skips it.
# -Glow also copies the pixel into the glow map. A later call on the same pixel replaces both.
function Px([int]$x, [int]$y, $rgb, [switch]$Flat, [switch]$Glow) {
	$f = 1.0
	if (-not $Flat) { $f = 0.92 + ((Hash $x $y 0) -band 0xFF) / 255.0 * 0.16 }
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

# A magma crack: one pixel per column, walking right from (x, y) for $len columns and
# drifting up or down a pixel now and then, staying inside rows y0..y1 and columns up to x1.
# Glows. Always moving forward keeps it a thin line, so it reads as a vein.
function Crack([int]$x, [int]$y, [int]$len, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [int]$salt) {
	for ($i = 0; $i -lt $len -and $x -le $x1; $i++) {
		$hot = ((Hash $x $y $salt) % 5) -eq 0
		if ($hot) { Px $x $y $emberHot -Flat -Glow } else { Px $x $y $ember -Flat -Glow }
		$r = (Hash $x $y ($salt + 1)) % 3
		if ($r -eq 0 -and $y -gt $y0) { $y-- } elseif ($r -eq 1 -and $y -lt $y1) { $y++ }
		$x++
	}
}

function Flame-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	Fill-Box $u $v $w $h $d $ember -Flat -Glow
	Fill ($u + $d) $v $w $d $emberHot -Flat -Glow
	Fill $u ($v + $d) (2 * ($d + $w)) 1 $emberHot -Flat -Glow
}

$flameMix = @($emberHot, $ember, $ember, $flameOrange, $flameOrange, $flameRed, $magmaDeep)
function Fire-Box([int]$u, [int]$v, [int]$w, [int]$h, [int]$d) {
	for ($i = 0; $i -lt 2 * ($d + $w); $i++) {
		for ($j = 0; $j -lt ($d + $h); $j++) {
			if ($j -lt $d -and ($i -lt $d -or $i -ge $d + 2 * $w)) { continue }   # unused corners
			$x = $u + $i; $y = $v + $j
			Px $x $y $flameMix[(Hash $x $y 7) % $flameMix.Count] -Flat -Glow
		}
	}
}

# ---- Head (0, 0, 7, 6, 7): top (7,0) 7x7, side (0,7) 7x6, front (7,7) 7x6, side (14,7),
#      back (21,7) 7x6, bottom (14,0) 7x7.
Fill-Box 0 0 7 6 7 $obsidian
Fill 14 0 7 7 $ash                                  # underside
Fill 0 11 7 2 $ash; Fill 14 11 7 2 $ash             # ashen cheeks, low on both sides
Fill 7 7 7 1 $obsidianLight                         # heavy brow
# Eyes: glowing ember slits at columns 1-2 and 4-5 of row 2, each with a hot inner pixel.
foreach ($ex in 8, 11) { Px $ex 9 $ember -Flat -Glow; Px ($ex + 1) 9 $emberHot -Flat -Glow }
Fill 7 10 7 1 $obsidianLight                        # cheekbones under the eyes
Px 10 7 $ember -Flat -Glow; Px 10 6 $ember -Flat -Glow; Px 10 5 $ember -Flat -Glow   # forehead vein
Px 9 4 $ember -Flat -Glow; Px 11 4 $ember -Flat -Glow                              # ...forking
Crack 0 8 7 0 7 6 10 11; Crack 14 8 7 14 7 20 10 12     # veins along both sides of the head
Fill 21 7 7 1 $magmaDeep                            # back of the head: a deep magma crest

# ---- Jaw (28, 0, 4, 3, 3): ash muzzle, dark nose, a magma line along the mouth.
Fill-Box 28 0 4 3 3 $ash
Fill 31 0 4 3 $obsidian                             # top of the muzzle blends into the face
Px 32 3 $eyeDark -Flat; Px 33 3 $eyeDark -Flat      # nose
Fill 31 5 4 1 $flameRed -Flat -Glow                 # glowing mouth line (front face, bottom row)
Fill 28 5 3 1 $flameRed -Flat -Glow; Fill 35 5 3 1 $flameRed -Flat -Glow

# ---- Ears (42/48, 0, 2, 3, 1): obsidian outside, glowing magma inside.
foreach ($u in 42, 48) {
	Fill-Box $u 0 2 3 1 $obsidian
	Fill ($u + 1) 1 2 3 $magmaDeep -Flat -Glow       # inner ear (front face)
	Fill ($u + 1) 1 2 1 $ember -Flat -Glow           # bright rim at the top
}

# ---- Horns (base 54, 0, 2, 4, 2): obsidian with a vein up two sides, glowing crystal tip.
Fill-Box 54 0 2 4 2 $obsidian
Fill 56 0 2 2 $obsidianLight                        # top
Px 55 3 $ember -Flat -Glow; Px 55 4 $ember -Flat -Glow   # a vein on the outer side...
Px 59 4 $ember -Flat -Glow; Px 59 5 $ember -Flat -Glow   # ...and the inner side
Fill-Box 28 6 1 3 1 $crystal -Flat -Glow
Fill 29 6 1 1 $crystalLight -Flat -Glow; Fill 28 7 1 3 $crystalLight -Flat -Glow
Fill 28 9 4 1 $crystalDeep -Flat -Glow

# ---- Small flames.
Flame-Box 32 6 1 2 1                                # flame tongues (mane, tail)
Flame-Box 36 6 1 2 1                                # tail plume

# ---- Body (0, 13, 7, 6, 12): top (12,13) 7x12, bottom (19,13), side (0,25) 12x6,
#      front (12,25) 7x6, side (19,25) 12x6, back (31,25) 7x6.
Fill-Box 0 13 7 6 12 $obsidian
Fill 19 13 7 12 $ash                                # belly
Fill 12 25 7 6 $ash; Fill 12 25 7 1 $obsidian       # chest (ashen), obsidian collar edge
Fill 0 30 12 1 $ash; Fill 19 30 12 1 $ash           # ashen lower flanks
Fill 15 13 1 12 $obsidianLight                      # spine ridge
Crack 0 26 12 0 25 11 29 21; Crack 0 28 8 0 25 11 29 22       # magma veins, left flank
Crack 19 27 12 19 25 30 29 23; Crack 22 29 8 19 25 30 29 24   # ...right flank
Crack 12 15 7 12 13 18 24 25; Crack 12 20 7 12 13 18 24 26    # ...across the back
Px 15 27 $ember -Flat -Glow; Px 15 28 $ember -Flat -Glow       # chest rune

# ---- Back plates (38, 13, 8, 2, 4): polished obsidian, a glowing magma seam on every edge.
Fill-Box 38 13 8 2 4 $obsidianLight
Fill 42 13 8 1 $obsidian                            # top: darker band at the back edge
Fill 42 16 8 1 $ember -Flat -Glow                   # top: glowing seam at the front edge
Fill 38 18 24 1 $flameRed -Flat -Glow               # lower edge of every side glows

# ---- Fire: mane and tail.
Fire-Box 38 19 8 4 4                                # magma mane
Fire-Box 0 31 4 4 4                                 # tail base
Fire-Box 16 31 3 3 4                                # tail mid
Fire-Box 30 31 3 3 3                                # tail mid 2
Flame-Box 42 31 2 2 2                               # tail tip

# ---- Legs (0, 39, 3, 6, 3): obsidian with a vein, glowing lava paws.
Fill-Box 0 39 3 6 3 $obsidian
Crack 0 43 12 0 42 11 45 31
Fill 0 47 12 1 $flameRed -Flat -Glow                # paws: bottom row of every side glows
Fill 6 39 3 3 $ember -Flat -Glow                    # paw pads (bottom face)

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$glowOut = Join-Path (Split-Path $Out) 'ashenox_glow.png'
$glowBmp.Save($glowOut, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $glowBmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
Write-Output "Wrote $((Resolve-Path $glowOut).Path)"
