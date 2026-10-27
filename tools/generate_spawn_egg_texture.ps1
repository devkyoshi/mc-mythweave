# Paints the original Cindercub spawn egg item texture (16 x 16):
# src/main/resources/assets/mythweave/textures/item/cindercub_spawn_egg.png
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\generate_spawn_egg_texture.ps1
#
# An orange egg in Cindercub's colors: dark outline, highlight on the upper left,
# charcoal and burnt-red spots, and one glowing ember fleck.

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\src\main\resources\assets\mythweave\textures\item\cindercub_spawn_egg.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outline  = @(74, 34, 20)
$orange   = @(232, 118, 43)
$light    = @(246, 160, 86)
$red      = @(181, 64, 30)
$charcoal = @(46, 42, 43)
$ember    = @(255, 190, 50)
$emberHot = @(255, 241, 168)

# Egg: an ellipse that narrows toward the top. Center (7.5, 8.5), up to 5.5 wide, 7 tall.
function Test-Inside([int]$x, [int]$y) {
	$cx = 7.5; $cy = 8.5; $ry = 7.0
	$rx = 5.5 - [Math]::Max(0.0, $cy - $y) * 0.2
	return ([Math]::Pow(($x - $cx) / $rx, 2) + [Math]::Pow(($y - $cy) / $ry, 2)) -le 1.0
}

$bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
function Set-Px([int]$x, [int]$y, $rgb) {
	$bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2]))
}

for ($x = 0; $x -lt 16; $x++) {
	for ($y = 0; $y -lt 16; $y++) {
		if (-not (Test-Inside $x $y)) { continue }
		$edge = -not ((Test-Inside ($x - 1) $y) -and (Test-Inside ($x + 1) $y) -and (Test-Inside $x ($y - 1)) -and (Test-Inside $x ($y + 1)))
		if ($edge) { Set-Px $x $y $outline }
		elseif (($x -eq 5 -and $y -ge 3 -and $y -le 6) -or ($x -eq 6 -and $y -eq 3)) { Set-Px $x $y $light }   # shine
		else { Set-Px $x $y $orange }
	}
}

# Spots (only where the egg is, never on the outline).
foreach ($p in @(@(9, 5, $charcoal), @(10, 6, $charcoal), @(5, 10, $red), @(6, 10, $red), @(5, 11, $red),
		@(10, 11, $charcoal), @(11, 11, $charcoal), @(10, 12, $charcoal), @(8, 13, $red))) {
	Set-Px $p[0] $p[1] $p[2]
}
# Ember fleck, like the tail tip.
Set-Px 8 8 $ember; Set-Px 9 8 $ember; Set-Px 8 9 $ember; Set-Px 9 9 $emberHot

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
