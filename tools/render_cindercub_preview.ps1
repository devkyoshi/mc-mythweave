# Renders README preview images of Cindercub from the real texture and the same cube layout
# as CindercubModel.java (no game needed). Outputs to docs/images/.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\render_cindercub_preview.ps1
#
# The cube list below duplicates CindercubModel#createBodyLayer and #setSittingPose. If you
# change the model, update it here too. In-game screenshots (F2) are always the real truth.

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$texturePath = Join-Path $root 'src\main\resources\assets\mythweave\textures\entity\cindercub.png'
$glowPath = Join-Path $root 'src\main\resources\assets\mythweave\textures\entity\cindercub_glow.png'
$outDir = Join-Path $root 'docs\images'
New-Item -ItemType Directory -Force $outDir | Out-Null

Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public class Mat {
	// 3x4 affine transform (rotation + translation), applied as p' = R p + t.
	public double[] m = { 1,0,0,0, 0,1,0,0, 0,0,1,0 };
	public static Mat Pose(double px, double py, double pz, double xr, double yr, double zr) {
		// Minecraft ModelPart: translate to pivot, then rotate Z * Y * X.
		double cx = Math.Cos(xr), sx = Math.Sin(xr), cy = Math.Cos(yr), sy = Math.Sin(yr), cz = Math.Cos(zr), sz = Math.Sin(zr);
		double[] rx = { 1,0,0, 0,cx,-sx, 0,sx,cx };
		double[] ry = { cy,0,sy, 0,1,0, -sy,0,cy };
		double[] rz = { cz,-sz,0, sz,cz,0, 0,0,1 };
		double[] r = Mul3(Mul3(rz, ry), rx);
		Mat t = new Mat();
		t.m = new double[] { r[0],r[1],r[2],px, r[3],r[4],r[5],py, r[6],r[7],r[8],pz };
		return t;
	}
	static double[] Mul3(double[] a, double[] b) {
		double[] c = new double[9];
		for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) c[i*3+j] += a[i*3+k] * b[k*3+j];
		return c;
	}
	public Mat Then(Mat child) {
		Mat o = new Mat();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 4; j++) {
				double v = 0;
				for (int k = 0; k < 3; k++) v += m[i*4+k] * child.m[k*4+j];
				if (j == 3) v += m[i*4+3];
				o.m[i*4+j] = v;
			}
		}
		return o;
	}
	public double[] Apply(double x, double y, double z) {
		return new double[] { m[0]*x+m[1]*y+m[2]*z+m[3], m[4]*x+m[5]*y+m[6]*z+m[7], m[8]*x+m[9]*y+m[10]*z+m[11] };
	}
	public double[] ApplyDir(double x, double y, double z) {
		return new double[] { m[0]*x+m[1]*y+m[2]*z, m[4]*x+m[5]*y+m[6]*z, m[8]*x+m[9]*y+m[10]*z };
	}
}

public class Preview {
	class Quad { public double[][] p; public Color c; public double bias; }
	List<Quad> quads = new List<Quad>();
	// Pushes the next boxes slightly back so they lose depth ties against flush surfaces.
	public double DepthBias = 0;
	Bitmap tex, glow;
	double yaw, pitch;

	public Preview(string texturePath, string glowPath, double yawDeg, double pitchDeg) {
		tex = new Bitmap(texturePath);
		glow = new Bitmap(glowPath);
		yaw = yawDeg * Math.PI / 180; pitch = pitchDeg * Math.PI / 180;
	}

	// Camera: rotate around the vertical axis (yaw), then tilt to look down (pitch).
	// Model space has y pointing down, so screen y = rotated y.
	double[] View(double[] p) {
		double x1 = p[0]*Math.Cos(yaw) + p[2]*Math.Sin(yaw);
		double z1 = -p[0]*Math.Sin(yaw) + p[2]*Math.Cos(yaw);
		double y2 = p[1]*Math.Cos(pitch) - z1*Math.Sin(pitch);
		double z2 = p[1]*Math.Sin(pitch) + z1*Math.Cos(pitch);
		return new double[] { x1, y2, z2 };
	}

	// Adds one textured box using Minecraft's box-UV layout at texOffs (u, v).
	public void Box(Mat pose, int u, int v, double x0, double y0, double z0, int w, int h, int d) {
		// Each face: normal, then a function from texel (i, j) to its 4 model-space corners.
		AddFace(pose, 0, 0, -1, u+d, v+d, w, h, (i, j) => R(x0+i, y0+j, z0, 1, 1, 0, 'z'));             // front (-z)
		AddFace(pose, 0, 0, 1, u+2*d+w, v+d, w, h, (i, j) => R(x0+w-i-1, y0+j, z0+d, 1, 1, 0, 'z'));   // back
		AddFace(pose, -1, 0, 0, u, v+d, d, h, (i, j) => R(x0, y0+j, z0+d-i-1, 0, 1, 1, 'x'));           // side at min x
		AddFace(pose, 1, 0, 0, u+d+w, v+d, d, h, (i, j) => R(x0+w, y0+j, z0+i, 0, 1, 1, 'x'));          // side at max x
		AddFace(pose, 0, -1, 0, u+d, v, w, d, (i, j) => R(x0+i, y0, z0+d-j-1, 1, 0, 1, 'y'));           // top
		AddFace(pose, 0, 1, 0, u+d+w, v, w, d, (i, j) => R(x0+i, y0+h, z0+d-j-1, 1, 0, 1, 'y'));        // bottom
	}

	static double[][] R(double x, double y, double z, double dx, double dy, double dz, char flat) {
		if (flat == 'z') return new[] { new[]{x,y,z}, new[]{x+dx,y,z}, new[]{x+dx,y+dy,z}, new[]{x,y+dy,z} };
		if (flat == 'x') return new[] { new[]{x,y,z}, new[]{x,y,z+dz}, new[]{x,y+dy,z+dz}, new[]{x,y+dy,z} };
		return new[] { new[]{x,y,z}, new[]{x+dx,y,z}, new[]{x+dx,y,z+dz}, new[]{x,y,z+dz} };
	}

	void AddFace(Mat pose, double nx, double ny, double nz, int u, int v, int w, int h, Func<int, int, double[][]> texel) {
		double[] n = View(pose.ApplyDir(nx, ny, nz));
		if (n[2] >= -1e-6) return; // facing away from the camera
		// Simple lighting: light from above-front-left, plus ambient.
		double[] l = { -0.35, -0.8, -0.5 };
		double len = Math.Sqrt(l[0]*l[0] + l[1]*l[1] + l[2]*l[2]);
		double diffuse = Math.Max(0, -(n[0]*l[0] + n[1]*l[1] + n[2]*l[2]) / len);
		double shade = 0.68 + 0.32 * diffuse;
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				Color t = tex.GetPixel(u + i, v + j);
				if (t.A == 0) continue;
				// Glowing texels (runes, eyes, crystals, flames) ignore lighting, like in game.
				double s = glow.GetPixel(u + i, v + j).A > 0 ? 1.0 : shade;
				double[][] c = texel(i, j);
				Quad q = new Quad();
				q.p = new double[4][];
				for (int k = 0; k < 4; k++) q.p[k] = View(pose.Apply(c[k][0], c[k][1], c[k][2]));
				q.c = Color.FromArgb(255, (int)(t.R * s), (int)(t.G * s), (int)(t.B * s));
				q.bias = DepthBias;
				quads.Add(q);
			}
		}
	}

	// Rasterises every texel quad with a z-buffer at 3x size, then downsamples for smooth edges.
	public Bitmap Render(int size, bool shadow) {
		int ss = 3, S = size * ss;
		double minX = double.MaxValue, maxX = double.MinValue, minY = double.MaxValue, maxY = double.MinValue;
		foreach (Quad q in quads) foreach (double[] p in q.p) {
			minX = Math.Min(minX, p[0]); maxX = Math.Max(maxX, p[0]); minY = Math.Min(minY, p[1]); maxY = Math.Max(maxY, p[1]);
		}
		double scale = 0.78 * S / Math.Max(maxX - minX, maxY - minY);
		double ox = S / 2.0 - (minX + maxX) / 2 * scale, oy = S / 2.0 - (minY + maxY) / 2 * scale;

		Bitmap big = new Bitmap(S, S, PixelFormat.Format32bppArgb);
		using (Graphics g = Graphics.FromImage(big)) {
			g.Clear(Color.Transparent);
			if (shadow) {
				// Soft ground shadow under the feet (model y = 24 is the ground).
				double[] c = View(new double[] { 0, 24, 0 });
				float cx = (float)(c[0]*scale + ox), cy = (float)(c[1]*scale + oy);
				float rx = (float)(9 * scale), ry = (float)(9 * scale * Math.Sin(pitch) + 2);
				using (GraphicsPath path = new GraphicsPath()) {
					path.AddEllipse(cx - rx, cy - ry, 2 * rx, 2 * ry);
					using (PathGradientBrush b = new PathGradientBrush(path)) {
						b.CenterColor = Color.FromArgb(110, 0, 0, 0);
						b.SurroundColors = new[] { Color.FromArgb(0, 0, 0, 0) };
						g.FillEllipse(b, cx - rx, cy - ry, 2 * rx, 2 * ry);
					}
				}
			}
		}

		double[] zbuf = new double[S * S];
		for (int i = 0; i < zbuf.Length; i++) zbuf[i] = double.MaxValue;
		BitmapData data = big.LockBits(new Rectangle(0, 0, S, S), ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
		int[] px = new int[S * S];
		System.Runtime.InteropServices.Marshal.Copy(data.Scan0, px, 0, px.Length);
		foreach (Quad q in quads) {
			double[][] s = new double[4][];
			for (int k = 0; k < 4; k++) s[k] = new[] { q.p[k][0]*scale + ox, q.p[k][1]*scale + oy, q.p[k][2] + q.bias };
			Tri(s[0], s[1], s[2], q.c.ToArgb(), px, zbuf, S);
			Tri(s[0], s[2], s[3], q.c.ToArgb(), px, zbuf, S);
		}
		System.Runtime.InteropServices.Marshal.Copy(px, 0, data.Scan0, px.Length);
		big.UnlockBits(data);

		Bitmap small = new Bitmap(size, size, PixelFormat.Format32bppArgb);
		using (Graphics g = Graphics.FromImage(small)) {
			g.InterpolationMode = InterpolationMode.HighQualityBicubic;
			g.PixelOffsetMode = PixelOffsetMode.HighQuality;
			g.DrawImage(big, 0, 0, size, size);
		}
		big.Dispose();
		return small;
	}

	static void Tri(double[] a, double[] b, double[] c, int color, int[] px, double[] zbuf, int S) {
		int x0 = Math.Max(0, (int)Math.Floor(Math.Min(a[0], Math.Min(b[0], c[0]))));
		int x1 = Math.Min(S - 1, (int)Math.Ceiling(Math.Max(a[0], Math.Max(b[0], c[0]))));
		int y0 = Math.Max(0, (int)Math.Floor(Math.Min(a[1], Math.Min(b[1], c[1]))));
		int y1 = Math.Min(S - 1, (int)Math.Ceiling(Math.Max(a[1], Math.Max(b[1], c[1]))));
		double area = (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0]);
		if (Math.Abs(area) < 1e-9) return;
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				double pxc = x + 0.5, pyc = y + 0.5;
				double w0 = ((b[0]-pxc)*(c[1]-pyc) - (b[1]-pyc)*(c[0]-pxc)) / area;
				double w1 = ((c[0]-pxc)*(a[1]-pyc) - (c[1]-pyc)*(a[0]-pxc)) / area;
				double w2 = 1 - w0 - w1;
				// Small tolerance closes hairline gaps between neighbouring texel quads.
				if (w0 < -0.02 || w1 < -0.02 || w2 < -0.02) continue;
				double z = w0*a[2] + w1*b[2] + w2*c[2];
				int idx = y * S + x;
				if (z < zbuf[idx]) { zbuf[idx] = z; px[idx] = color; }
			}
		}
	}
}
'@

function New-Cindercub([double]$yaw, [double]$pitch, [bool]$sitting, [bool]$tame) {
	$p = New-Object Preview $texturePath, $glowPath, $yaw, $pitch
	$id = [Mat]::Pose(0, 0, 0, 0, 0, 0)

	# Rest pose, from CindercubModel#createBodyLayer (flames at their resting size).
	$bodyY = 19.0; $bodyXRot = 0.0; $headY = 18.0; $headZ = -3.0; $tailXRot = 0.6
	$hindY = 21.0; $hindXRot = 0.0; $hindSpread = 0.0
	if ($sitting) {
		# From CindercubModel#setSittingPose.
		$bodyY += 2.0; $bodyXRot = -0.5; $headY += 0.8; $headZ += 1.3; $tailXRot = 0.3
		$hindY += 2.0; $hindXRot = -1.4; $hindSpread = 0.5
	}

	$body = $id.Then([Mat]::Pose(0, $bodyY, 0, $bodyXRot, 0, 0))
	$p.Box($body, 0, 11, -2, -2, -3, 4, 4, 6)
	foreach ($z in -0.5, 1.5) { $p.Box($body.Then([Mat]::Pose(0, -2, $z, -0.4, 0, 0)), 54, 0, -0.5, -2, -0.5, 1, 2, 1) }
	$tail = $body.Then([Mat]::Pose(0, -1, 3, $tailXRot, 0.15, 0))
	if (-not $tame) {
		$p.Box($tail, 22, 11, -1.5, -1.5, 0, 3, 3, 5)                          # wild: fur tail
	} else {
		# Bonded: fire tail at rest (segment curl from CindercubModel#animateFireTail).
		$p.Box($tail, 22, 20, -1.5, -1.5, 0, 3, 3, 3)
		$p.Box($tail.Then([Mat]::Pose(0, -1.5, 1.5, -($bodyXRot + $tailXRot), 0, 0)), 48, 11, -0.5, -2, -0.5, 1, 2, 1)
		$mid = $tail.Then([Mat]::Pose(0, 0, 3, 0.2, 0, 0))
		$p.Box($mid, 36, 20, -1, -1, 0, 2, 2, 3)
		$p.Box($mid.Then([Mat]::Pose(0, -1, 1.5, -($bodyXRot + $tailXRot + 0.2), 0, 0)), 48, 11, -0.5, -2, -0.5, 1, 2, 1)
		$tip = $mid.Then([Mat]::Pose(0, 0, 3, 0.3, 0, 0))
		$p.Box($tip, 40, 11, -1, -1, 0, 2, 2, 2)
		$p.Box($tip.Then([Mat]::Pose(0, -1, 1, -($bodyXRot + $tailXRot + 0.5), 0, 0)), 58, 0, -0.5, -2, -0.5, 1, 2, 1)
	}

	$head = $id.Then([Mat]::Pose(0, $headY, $headZ, 0, 0, 0))
	$p.Box($head, 0, 0, -3.5, -5, -4, 7, 5, 5)
	$p.Box($head, 24, 0, -1.5, -2, -5, 3, 2, 1)
	foreach ($ear in @(@(-2.5, -0.2, 34), @(2.5, 0.2, 40))) {
		$p.Box($head.Then([Mat]::Pose($ear[0], -5, -2, 0, 0, $ear[1])), $ear[2], 0, -1, -4, 0, 2, 4, 1)
	}
	foreach ($horn in @(@(-1.0, -0.35), @(1.0, 0.35))) {
		$p.Box($head.Then([Mat]::Pose($horn[0], -5, -3, -0.6, 0, $horn[1])), 50, 0, -0.5, -2, -0.5, 1, 2, 1)
	}

	# Legs touch the body where they join; a small bias lets the body win those edges.
	$p.DepthBias = 0.05
	foreach ($leg in @(@((-1 - $hindSpread), $hindY, 2, $hindXRot), @((1 + $hindSpread), $hindY, 2, $hindXRot), @(-1, 21, -2, 0), @(1, 21, -2, 0))) {
		$p.Box($id.Then([Mat]::Pose($leg[0], $leg[1], $leg[2], $leg[3], 0, 0)), 0, 23, -1, 0, -1, 2, 3, 2)
	}
	return $p
}

function Save([System.Drawing.Bitmap]$bmp, [string]$name) {
	$path = Join-Path $outDir $name
	$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
	Write-Output "Wrote docs/images/$name"
}

# Yaw 0 looks at the face; 180 looks at the tail.
# Standing = wild (fur tail); sitting and back = bonded (fire tail).
$standing = (New-Cindercub 35 25 $false $false).Render(640, $true)
Save $standing 'cindercub_standing.png'
$sitting = (New-Cindercub 60 15 $true $true).Render(640, $true)
Save $sitting 'cindercub_sitting.png'
$back = (New-Cindercub 215 25 $false $true).Render(640, $true)
Save $back 'cindercub_back.png'

# Texture sheet, scaled 8x with nearest-neighbour on a dark checkerboard.
$tex = [System.Drawing.Image]::FromFile($texturePath)
$sheet = New-Object System.Drawing.Bitmap 512, 256
$g = [System.Drawing.Graphics]::FromImage($sheet)
for ($x = 0; $x -lt 64; $x++) { for ($y = 0; $y -lt 32; $y++) {
	$shade = if ((($x + $y) % 2) -eq 0) { 38 } else { 48 }
	$g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, $shade, $shade, $shade + 4))), $x * 8, $y * 8, 8, 8)
} }
$g.InterpolationMode = 'NearestNeighbor'; $g.PixelOffsetMode = 'Half'
$g.DrawImage($tex, 0, 0, 512, 256)
$g.Dispose(); $tex.Dispose()
Save $sheet 'cindercub_texture.png'

# Banner: warm ember gradient, title, tagline, and the hero render.
$banner = New-Object System.Drawing.Bitmap 1280, 400
$g = [System.Drawing.Graphics]::FromImage($banner)
$g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAliasGridFit'
$rect = New-Object System.Drawing.Rectangle 0, 0, 1280, 400
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.Color]::FromArgb(255, 34, 28, 30)), ([System.Drawing.Color]::FromArgb(255, 120, 40, 22)), 20.0
$g.FillRectangle($grad, $rect)
# A few soft ember dots for atmosphere.
$rng = New-Object System.Random 7
for ($i = 0; $i -lt 40; $i++) {
	$r = $rng.Next(2, 6); $a = $rng.Next(40, 140)
	$g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, 255, 180, 60))), $rng.Next(560, 1270), $rng.Next(10, 390), $r, $r)
}
$titleFont = New-Object System.Drawing.Font 'Segoe UI', 66, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$subFont = New-Object System.Drawing.Font 'Segoe UI', 26, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$smallFont = New-Object System.Drawing.Font 'Segoe UI', 20, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString('Mythweave', $titleFont, (New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 214, 140))), 64, 112)
$g.DrawString('Discover, bond with, and raise magical Mythlings', $subFont, (New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 243, 211, 163))), 70, 200)
$g.DrawString('A Fabric mod for Minecraft Java 26.3', $smallFont, (New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(200, 243, 211, 163))), 70, 246)
$hero = (New-Cindercub 35 25 $false $true).Render(400, $true)
$g.DrawImage($hero, 840, 0, 400, 400)
$g.Dispose()
Save $banner 'banner.png'
