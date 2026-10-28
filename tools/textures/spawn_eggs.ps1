# Paints the original Mythling spawn egg item textures (16 x 16 each) into
# src/main/resources/assets/mythweave/textures/item/:
#   cindercub_spawn_egg.png  ember-orange egg, charcoal and deep-red spots, a cream shine, an ember fleck
#   emberlynx_spawn_egg.png  deeper red egg, gold spots, a brighter ember fleck
#   ashenox_spawn_egg.png    obsidian egg with magma-orange and red spots
#   solflare_spawn_egg.png   cream egg with sun-gold and red spots
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\spawn_eggs.ps1
#
# All use the same egg shape (dark outline, a shine on the upper left) and spot layout, so
# every form of the line reads as one family; only the colors change.

param(
	[string]$OutDir = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\item')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$ember    = @(255, 190, 50)
$emberHot = @(255, 241, 168)

# Egg: an ellipse that narrows toward the top. Center (7.5, 8.5), up to 5.5 wide, 7 tall.
function Test-Inside([int]$x, [int]$y) {
	$cx = 7.5; $cy = 8.5; $ry = 7.0
	$rx = 5.5 - [Math]::Max(0.0, $cy - $y) * 0.2
	return ([Math]::Pow(($x - $cx) / $rx, 2) + [Math]::Pow(($y - $cy) / $ry, 2)) -le 1.0
}

function New-Egg([string]$name, $outline, $base, $light, $spotA, $spotB) {
	$bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$set = { param($x, $y, $rgb) $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2])) }

	for ($x = 0; $x -lt 16; $x++) {
		for ($y = 0; $y -lt 16; $y++) {
			if (-not (Test-Inside $x $y)) { continue }
			$edge = -not ((Test-Inside ($x - 1) $y) -and (Test-Inside ($x + 1) $y) -and (Test-Inside $x ($y - 1)) -and (Test-Inside $x ($y + 1)))
			if ($edge) { & $set $x $y $outline }
			elseif (($x -eq 5 -and $y -ge 3 -and $y -le 6) -or ($x -eq 6 -and $y -eq 3)) { & $set $x $y $light }   # shine
			else { & $set $x $y $base }
		}
	}
	# Spots (only where the egg is, never on the outline).
	foreach ($p in @(@(9, 5, $spotA), @(10, 6, $spotA), @(5, 10, $spotB), @(6, 10, $spotB), @(5, 11, $spotB),
			@(10, 11, $spotA), @(11, 11, $spotA), @(10, 12, $spotA), @(8, 13, $spotB))) {
		& $set $p[0] $p[1] $p[2]
	}
	# Ember fleck, like the tail flame.
	& $set 8 8 $ember; & $set 9 8 $ember; & $set 8 9 $ember; & $set 9 9 $emberHot

	New-Item -ItemType Directory -Force $OutDir | Out-Null
	$path = Join-Path $OutDir "$name.png"
	$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
	$bmp.Dispose()
	Write-Output "Wrote $((Resolve-Path $path).Path)"
}

#          name                   outline         base             shine            spots A          spots B
New-Egg 'cindercub_spawn_egg' @(74, 34, 20) @(255, 138, 61) @(255, 233, 176) @(59, 47, 63)  @(200, 58, 30)
New-Egg 'emberlynx_spawn_egg' @(60, 20, 14) @(178, 52, 28)  @(222, 104, 60) @(255, 200, 80) @(44, 36, 38)
New-Egg 'ashenox_spawn_egg'   @(16, 10, 12) @(46, 32, 36)   @(82, 62, 66)   @(255, 140, 40) @(220, 70, 26)
New-Egg 'solflare_spawn_egg'  @(128, 66, 26)  @(250, 230, 194) @(255, 248, 230) @(246, 160, 48) @(214, 64, 30)
