# Paints the original Ember Brazier block textures (16 x 16 each) into
# src/main/resources/assets/mythweave/textures/block/.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\generate_brazier_textures.ps1
#
# Files and where the block models use them:
#   ember_brazier_metal.png      base plate, post, bowl underside (dark iron, rivets)
#   ember_brazier_bowl.png       bowl sides; the models read rows 0-3 (rim + band)
#   ember_brazier_coals.png      bowl top, unlit (charcoal and ash)
#   ember_brazier_coals_lit.png  bowl top, lit (glowing coals)
#   ember_brazier_flame.png      flame on the lit model: 16 x 64, four animation frames
#                                (ember_brazier_flame.png.mcmeta sets the frame time)
#
# Every pixel is either fully opaque or fully transparent, so 26.3 renders the flame as
# cutout automatically (it picks the render layer from the texture's transparency).

param(
	[string]$OutDir = (Join-Path $PSScriptRoot '..\src\main\resources\assets\mythweave\textures\block')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $OutDir | Out-Null

# Palette: iron greys, charcoal, and the same ember colors as Cindercub's tail tip.
$ironDark  = @(52, 50, 54)
$iron      = @(78, 76, 82)
$ironLight = @(118, 116, 124)
$charcoal  = @(33, 30, 31)
$ash       = @(96, 90, 88)
$red       = @(181, 64, 30)
$orange    = @(232, 118, 43)
$ember     = @(255, 190, 50)
$emberHot  = @(255, 241, 168)

# Deterministic per-pixel noise in [0, 1), so re-running gives the same textures.
# (An integer hash with mixing steps; a plain XOR of products shows diagonal streaks.)
function Noise([int]$x, [int]$y, [int]$salt) {
	[long]$h = ([long]$x * 374761393 + [long]$y * 668265263 + [long]$salt * 982451653) -band 0xFFFFFFFFL
	$h = (($h -bxor ($h -shr 13)) * 1274126177L) -band 0xFFFFFFFFL
	$h = $h -bxor ($h -shr 16)
	return ($h -band 0xFFFF) / 65536.0
}

function New-Bitmap([int]$w, [int]$h) {
	return New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Set-Px($bmp, [int]$x, [int]$y, $rgb, [double]$jitter = 0.08) {
	$f = 1.0 - $jitter + (Noise $x $y 1) * 2 * $jitter
	$c = $rgb | ForEach-Object { [Math]::Max(0, [Math]::Min(255, [int]($_ * $f))) }
	$bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2]))
}

function Save($bmp, [string]$name) {
	$path = Join-Path $OutDir $name
	$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
	$bmp.Dispose()
	Write-Host "Wrote $path"
}

# --- metal: dark iron with a lighter top edge and a few rivets ---
$bmp = New-Bitmap 16 16
for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { Set-Px $bmp $x $y $iron } }
for ($x = 0; $x -lt 16; $x++) { Set-Px $bmp $x 14 $ironLight; Set-Px $bmp $x 15 $ironDark }
foreach ($p in @(@(5, 15), @(10, 15), @(7, 11), @(8, 13), @(5, 5), @(10, 10))) { Set-Px $bmp $p[0] $p[1] $ironLight 0 }
Save $bmp 'ember_brazier_metal.png'

# --- bowl side: bright rim on row 0, dark band with an ember-red stripe, then iron ---
$bmp = New-Bitmap 16 16
for ($x = 0; $x -lt 16; $x++) {
	Set-Px $bmp $x 0 $ironLight
	Set-Px $bmp $x 1 $ironDark
	if (($x % 4) -eq 1) { Set-Px $bmp $x 2 $ironLight 0 } else { Set-Px $bmp $x 2 $red }
	Set-Px $bmp $x 3 $ironDark
	for ($y = 4; $y -lt 16; $y++) { Set-Px $bmp $x $y $iron }
}
Save $bmp 'ember_brazier_bowl.png'

# --- coals (unlit): charcoal lumps with grey ash, a darker iron border ---
$bmp = New-Bitmap 16 16
for ($x = 0; $x -lt 16; $x++) {
	for ($y = 0; $y -lt 16; $y++) {
		$n = Noise $x $y 7
		if ($x -le 2 -or $y -le 2 -or $x -ge 13 -or $y -ge 13) { Set-Px $bmp $x $y $ironDark }
		elseif ($n -lt 0.22) { Set-Px $bmp $x $y $ash }
		else { Set-Px $bmp $x $y $charcoal 0.15 }
	}
}
Save $bmp 'ember_brazier_coals.png'

# --- coals (lit): same layout, glowing hotter toward the middle ---
$bmp = New-Bitmap 16 16
for ($x = 0; $x -lt 16; $x++) {
	for ($y = 0; $y -lt 16; $y++) {
		$n = Noise $x $y 7
		$dist = [Math]::Sqrt([Math]::Pow($x - 7.5, 2) + [Math]::Pow($y - 7.5, 2))
		if ($x -le 2 -or $y -le 2 -or $x -ge 13 -or $y -ge 13) { Set-Px $bmp $x $y $ironDark }
		elseif ($n -lt 0.18) { Set-Px $bmp $x $y $charcoal }
		elseif ($dist -lt 2.2) { Set-Px $bmp $x $y $emberHot 0.04 }
		elseif ($dist -lt 3.6 -or $n -gt 0.85) { Set-Px $bmp $x $y $ember }
		elseif ($n -gt 0.5) { Set-Px $bmp $x $y $orange }
		else { Set-Px $bmp $x $y $red }
	}
}
Save $bmp 'ember_brazier_coals_lit.png'

# --- flame: 4 frames stacked vertically (16 x 64). Each column burns up to a height that
# peaks in the middle and flickers per frame; color runs hot-white at the base to red tips.
$bmp = New-Bitmap 16 64
$transparent = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
for ($frame = 0; $frame -lt 4; $frame++) {
	for ($x = 0; $x -lt 16; $x++) {
		$profile = 1.0 - [Math]::Pow(($x - 7.5) / 8.0, 2)          # 1 in the middle, ~0 at the edges
		$flicker = 0.75 + (Noise $x $frame 3) * 0.35
		$height = [int]([Math]::Round(14 * $profile * $flicker))
		for ($y = 0; $y -lt 16; $y++) {
			$py = $frame * 16 + $y
			$fromBottom = 15 - $y
			if ($fromBottom -ge $height -or ($x -lt 2) -or ($x -gt 13)) { $bmp.SetPixel($x, $py, $transparent); continue }
			$t = $fromBottom / [Math]::Max(1, $height)                 # 0 at base, 1 at the tip
			$rgb = if ($t -lt 0.25) { $emberHot } elseif ($t -lt 0.55) { $ember } elseif ($t -lt 0.8) { $orange } else { $red }
			Set-Px $bmp $x $py $rgb 0.05
		}
	}
}
Save $bmp 'ember_brazier_flame.png'

$mcmeta = Join-Path $OutDir 'ember_brazier_flame.png.mcmeta'
[IO.File]::WriteAllText($mcmeta, "{`n  `"animation`": {`n    `"frametime`": 3,`n    `"interpolate`": false`n  }`n}`n")
Write-Host "Wrote $mcmeta"
