# Paints the ember particle sprite (8 x 8): a white square with a soft edge. EmberParticle
# tints it (gold, orange, red) and draws it full-bright, so the edge reads as a faint glow.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\textures\ember_particle.ps1
#
# Output: src/main/resources/assets/mythweave/textures/particle/ember.png

param(
	[string]$Out = (Join-Path $PSScriptRoot '..\..\src\main\resources\assets\mythweave\textures\particle\ember.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap 8, 8, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($x = 0; $x -lt 8; $x++) {
	for ($y = 0; $y -lt 8; $y++) {
		# Rings from the centre: solid 4x4 core, a half-transparent ring, a faint outer ring.
		$ring = [Math]::Max([Math]::Max(3 - $x, $x - 4), [Math]::Max(3 - $y, $y - 4))
		$alpha = switch ($ring) { { $_ -le 0 } { 255 } 1 { 170 } default { 60 } }
		$bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, 255, 255, 255))
	}
}

New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Wrote $((Resolve-Path $Out).Path)"
