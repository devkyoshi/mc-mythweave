# Generates the Ashenox model and texture straight from its design sheet
# (docs/reference/ashenox/Ashenox_ Ember Fox Mount Design Sheet.png), so the in-game creature
# matches the sheet instead of a hand-guessed approximation.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools\models\ashenox_gen.ps1
# Options (to try variants without touching the project, add -OutDir <folder>):
#   -Detail <n>   model units per pixel (default 2.5; Cindercub uses 1.5)
#   -Shaded       copy every shade from the sheet instead of its flat palette
#
# How it works:
#   1. Crops the sheet's front, side and back views, keys out the grey background, and scales
#      each view so the ear tips are EAR_TIP units above the ground (1 unit = 1/DETAIL pixel).
#   2. Carves the body, head, ruff and legs as a voxel hull: a voxel is solid when it lies
#      inside both the side silhouette and the (mirrored, so symmetric) front silhouette. The
#      tails are left out of the hull; they overlap in every view.
#   3. Splits the voxels into animatable parts (body, chest ruff, head, ears, four two-part
#      legs) and greedily merges each part's voxels into as few boxes as possible.
#   4. Adds the three tails as chains of tapering segments shaped like the sheet's plumes.
#   5. Packs every box into one texture atlas and paints each face texel with the colour the
#      sheet shows at that point (side faces from the side view, front faces from the front
#      view, the rest from the side view), snapped to the sheet's ~10-colour palette and
#      smoothed (the majority of five nearby samples wins), so it reads as clean flat pixel art.
#      Bright warm texels (eyes, spirals, ear tips) go into the glow map too. The tails are
#      painted in clean bands along their length, charcoal to yellow.
#   6. Writes:
#        src/client/java/com/mythweave/client/model/entity/AshenoxModelGeometry.java
#        src/main/resources/assets/mythweave/textures/entity/ashenox/ashenox.png
#        src/main/resources/assets/mythweave/textures/entity/ashenox/ashenox_glow.png
#        tools/models/ashenox_parts.json   (the part tree, for tools/previews/mythlings.ps1)
#
# Deterministic: running it twice gives identical files. Commit all four outputs together.

param(
	# Model units per pixel: higher = finer voxels.
	[double]$Detail = 2.5,
	# Copy every shade from the sheet instead of its small palette (smoothed, the default).
	[switch]$Shaded,
	# Sculpt the head as a fox-head shape instead of carving it from the sheet's outlines.
	[switch]$SculptHead,
	# Write everything to this folder instead of the project (for trying out variants).
	[string]$OutDir = ''
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

public class AshenoxGen {
	// ---- Tunables ---------------------------------------------------------------------------
	// Every size below was tuned at 2.5x detail and scales with S = DETAIL / 2.5 (Configure).
	static double DETAIL = 2.5;         // model units per pixel (the mesh is scaled down by this)
	static double S = 1.0;
	static bool FLAT;                   // paint in the sheet's palette, smoothed (no fine shading)
	static double EAR_TIP;              // ear tips, in units above the ground
	const int GROUND = 24;              // model y of the ground (unchanged by MeshTransformer.scaling)
	static int BELLY;                   // voxels at or below this height (units) belong to the legs
	static int KNEE;                    // ...and at or below this, to the lower legs
	static int EAR_BASE;                // head voxels at or above this height are ears
	static int HEAD_Z;                  // voxels in front of this (and above NECK_H) are the head
	static int NECK_H;
	static int CHEST_Z;                 // the ruff: in front of this, between the legs and the neck
	static int TAIL_Z;                  // tails start here; the hull stops behind it above BACK_H
	static int BACK_H;
	static int Z_OFFSET;                // side-view column (units) that becomes z = 0
	const int TAIL_SEGMENTS = 8;

	static int Si(double v) { return (int)Math.Round(v * S, MidpointRounding.AwayFromZero); }

	static void Configure(double detail, bool flat, bool sculpt) {
		DETAIL = detail; S = detail / 2.5; FLAT = flat; SCULPT = sculpt;
		EAR_TIP = 63 * S;
		BELLY = Si(19); KNEE = Si(9); EAR_BASE = Si(55); HEAD_Z = Si(-19); NECK_H = Si(36);
		CHEST_Z = Si(-13); TAIL_Z = Si(8); BACK_H = Si(40); Z_OFFSET = Si(36);
		XMIN = Si(-16); XMAX = Si(16); HMAX = Si(66); ZMIN = Si(-40); ZMAX = Si(24);
		BELLY_HALF = 8.5 * S; HEAD_HALF = 7.5 * S; SNOUT_HALF = Math.Max(1.0, 2.0 * S); BODY_HALF = 11.0 * S;
	}

	// Sheet crops (pixels): the "Additional Views" row.
	static readonly Rectangle FRONT = new Rectangle(920, 118, 126, 255);
	static readonly Rectangle SIDE = new Rectangle(1055, 118, 311, 255);
	static readonly Rectangle BACK = new Rectangle(1372, 118, 149, 255);

	class View {
		public Bitmap Bmp; public Rectangle R; public bool[,] Fg;
		public int Ground, Top, Left, Right; public double Scale; public double Center;
		public bool Inside(int px, int py) {
			return px >= R.Left && px < R.Right && py >= R.Top && py < R.Bottom && Fg[px - R.Left, py - R.Top];
		}
		// Height (units above ground) and horizontal position (units from Left) -> pixel.
		public int Px(double u) { return (int)Math.Floor(Left + u / Scale); }
		public int Py(double h) { return (int)Math.Floor(Ground + 1 - h / Scale); }
	}

	class Box { public double X, Y, Z; public int W, H, D; public int U, V; public string Paint; public Part Part; public int Index; }
	class Part {
		public string Name; public Part Parent; public double PX, PY, PZ; public double RX, RY, RZ;
		public List<Box> Boxes = new List<Box>(); public List<Part> Children = new List<Part>();
		public Part Add(Part c) { c.Parent = this; Children.Add(c); return c; }
	}

	static Bitmap sheet;
	static View front, side, back;
	static Random noise = new Random(7);

	static bool IsBg(Color c) {
		int lum = (c.R + c.G + c.B) / 3;
		return Math.Abs(c.R - c.G) < 10 && (c.B - c.G) >= -2 && (c.B - c.G) < 16 && lum > 44 && lum < 95;
	}

	static View Load(Rectangle r, bool sideView) {
		View v = new View { Bmp = sheet, R = r, Fg = new bool[r.Width, r.Height] };
		v.Ground = r.Top; v.Top = r.Bottom; v.Left = r.Right; v.Right = r.Left;
		for (int x = r.Left; x < r.Right; x++) for (int y = r.Top; y < r.Bottom; y++) {
			if (IsBg(sheet.GetPixel(x, y))) continue;
			v.Fg[x - r.Left, y - r.Top] = true;
			v.Ground = Math.Max(v.Ground, y); v.Left = Math.Min(v.Left, x); v.Right = Math.Max(v.Right, x);
		}
		// Ear tips: the highest foreground pixel (in the side view, only over the head end).
		int limit = sideView ? v.Left + (v.Right - v.Left) / 4 : v.Right;
		for (int y = r.Top; y < r.Bottom && v.Top == r.Bottom; y++)
			for (int x = v.Left; x <= limit; x++) if (v.Fg[x - r.Left, y - r.Top]) { v.Top = y; break; }
		v.Scale = EAR_TIP / (v.Ground - v.Top + 1);
		// Centre line: midway between the outermost foot pixels (stray flames at the edges of
		// the view would skew the bounding box).
		int fl = int.MaxValue, fr = int.MinValue;
		for (int y = v.Ground - 3; y <= v.Ground; y++) for (int x = r.Left; x < r.Right; x++)
			if (v.Fg[x - r.Left, y - r.Top]) { fl = Math.Min(fl, x); fr = Math.Max(fr, x); }
		v.Center = ((fl + fr + 1) / 2.0 - v.Left) * v.Scale;
		return v;
	}

	// ---- Sampling the sheet ----------------------------------------------------------------
	static bool SideFg(double z, double h) { return side.Inside(side.Px(z + Z_OFFSET), side.Py(h)); }
	static bool FrontFg(double x, double h) { return front.Inside(front.Px(x + front.Center), front.Py(h)); }
	static Color SideColor(double z, double h) { return Nearest(side, side.Px(z + Z_OFFSET), side.Py(h)); }
	static Color FrontColor(double x, double h) {
		int px = front.Px(x + front.Center), py = front.Py(h);
		return front.Inside(px, py) ? sheet.GetPixel(px, py) : Color.Empty;
	}
	// The sheet colour at a pixel, or at the nearest foreground pixel (for faces just outside
	// the silhouette, e.g. a leg's inner side).
	static Color Nearest(View v, int px, int py) {
		for (int rad = 0; rad < 12; rad++)
			for (int dy = -rad; dy <= rad; dy++) for (int dx = -rad; dx <= rad; dx++) {
				if (Math.Max(Math.Abs(dx), Math.Abs(dy)) != rad) continue;
				if (v.Inside(px + dx, py + dy)) return sheet.GetPixel(px + dx, py + dy);
			}
		return Color.FromArgb(41, 33, 45);
	}

	// ---- Hull -------------------------------------------------------------------------------
	static int XMIN, XMAX, HMAX, ZMIN, ZMAX;
	static bool[,,] solid;   // [x - XMIN, h, z - ZMIN], voxel x..x+1, height h-1..h (h is 1-based), z..z+1

	static bool InHull(int x, int h, int z) {
		double xc = x + 0.5, hc = h - 0.5, zc = z + 0.5;
		if (zc >= TAIL_Z && hc >= BACK_H) return false;      // tails are added separately
		// The head's 3D shape can't be carved from two flat outlines, so it's sculpted instead.
		if (SCULPT && z < HEAD_Z && h >= NECK_H) return InHead(xc / S, hc / S, zc / S);
		if (!SideFg(zc, hc)) return false;
		if (!(FrontFg(xc, hc) && FrontFg(-xc, hc))) return false;   // symmetric
		// Ears are thin: keep them to a slab around their centre line.
		if (h >= EAR_BASE && Math.Abs(zc - earZ) > 2.5 * S) return false;
		// The front silhouette is widened by cheek fur and the tails peeking out at the sides,
		// so cap the width: the head tapers from the skull to a narrow snout, the body and ruff
		// stay within the chest and haunches.
		if (z < HEAD_Z && h >= NECK_H && h < EAR_BASE)
			return Math.Abs(xc) <= Math.Min(HEAD_HALF, SNOUT_HALF + (zc - noseZ) * HEAD_TAPER);
		if (h > BELLY) return Math.Abs(xc) <= (h < BELLY + Si(10) ? BELLY_HALF : BODY_HALF);
		return Math.Abs(xc) <= BELLY_HALF;   // legs
	}
	static bool SCULPT;

	static bool InEllipsoid(double x, double h, double z, double cx, double ch, double cz, double rx, double rh, double rz) {
		double a = (x - cx) / rx, b = (h - ch) / rh, c = (z - cz) / rz;
		return a * a + b * b + c * c <= 1.0;
	}

	// A fox head, in units of the 2.5x grid (scaled by S), measured from the sheet's side and
	// front views and anchored at the traced nose tip (n) and ear line: a rounded skull, a snout
	// tapering to the nose, a cream jaw, cheek fluff flaring out, and tall triangular ears.
	static bool InHead(double x, double h, double z) {
		double n = noseZ / S, ez = earZ / S;
		if (InEllipsoid(x, h, z, 0, 47.5, n + 12.5, 6.8, 6.0, 6.2)) return true;         // skull
		if (z >= n - 0.5 && z <= n + 9) {                                                    // snout
			double t = (n + 9 - z) / 9.5;
			double ch = 45.8 - 3.6 * t, rx = 3.6 - 2.0 * t, rh = 2.8 - 1.3 * t;
			if ((x / rx) * (x / rx) + ((h - ch) / rh) * ((h - ch) / rh) <= 1.0) return true;
		}
		if (InEllipsoid(x, h, z, 0, 42.0, n + 7.0, 2.2, 1.3, 4.5)) return true;            // jaw
		foreach (double s in new[] { -1.0, 1.0 }) {
			if (InEllipsoid(x, h, z, 5.4 * s, 44.0, n + 13, 2.2, 3.0, 2.8)) return true;     // cheek fluff
			if (h >= 51 && h <= 62) {                                                        // ear
				double t = (h - 51) / 11.0;
				double cx = s * (4.2 + 1.6 * t), w = 3.3 * (1 - t) + 0.5, d = 1.5 * (1 - t) + 0.5;
				if (Math.Abs(x - cx) <= w && Math.Abs(z - ez) <= d) return true;
			}
		}
		return false;
	}

	// The sculpted head's face, painted by region (2.5x units) instead of projected from the
	// sheet's flat views, which smear on a rounded head. Returns the colour and whether it glows.
	static Color HeadColor(string face, double x, double h, double z, out bool g) {
		g = false;
		double n = noseZ / S, ax = Math.Abs(x);
		Color red = Color.FromArgb(165, 47, 39), dark = Color.FromArgb(22, 16, 22), cream = Color.FromArgb(232, 214, 185),
			creamDim = Color.FromArgb(198, 172, 146), eye = Color.FromArgb(255, 181, 46), hot = Color.FromArgb(255, 236, 150);
		if (h >= 51) {                                                          // ears
			if (h >= 59.5) { g = true; return h >= 61 ? hot : Color.FromArgb(255, 195, 77); }
			double t = (h - 51) / 11.0, cx = 4.2 + 1.6 * t;
			if (face == "front" && Math.Abs(ax - cx) < 1.8 * (1 - t) + 0.4) return h > 56.5 ? Color.FromArgb(255, 131, 31) : VOLCANIC;
			return CHARCOAL;
		}
		if (face == "top" || face == "back") return Math.Abs(x) < 1.1 && z < n + 12 ? red : CHARCOAL;
		if (face == "bottom") return cream;
		bool front = face == "front";
		// Eyes: slanted amber almonds on the face above the snout, a dark line beneath.
		double eyeMid = 48.4 + (ax - 2.0) * 0.3;
		if (z < n + 10.5 && ax >= 1.8 && ax <= 4.9) {
			if (Math.Abs(h - eyeMid) <= 0.75) { g = true; return ax < 3.0 ? hot : eye; }
			if (h < eyeMid - 0.75 && h >= eyeMid - 1.4) return dark;
		}
		if (z < n + 1.2 && h > 43.5) return dark;                               // nose
		if (h < 43.5 && z < n + 12) return ax > 3.8 || h < 42.3 ? cream : creamDim;   // jaw and lower muzzle
		if (ax > 4.6 && h < 46.5) return h < 44.5 ? cream : creamDim;           // cheek fluff
		if (h > 50.5) return front && ax < 1.1 ? red : CHARCOAL;                // forehead, blaze down the middle
		if (!front && z > n + 15) return CHARCOAL;                              // back of the head
		return red;
	}

	static double BELLY_HALF;
	static double HEAD_HALF, SNOUT_HALF, BODY_HALF;
	const double HEAD_TAPER = 0.55;
	static double earZ, noseZ;

	static void BuildHull() {
		// Ear centre line from the side view.
		double sum = 0; int n = 0;
		for (int z = ZMIN; z < HEAD_Z + Si(12); z++) for (int h = EAR_BASE; h <= HMAX; h++)
			if (SideFg(z + 0.5, h - 0.5)) { sum += z + 0.5; n++; }
		earZ = n > 0 ? sum / n : HEAD_Z;
		// Nose tip: the front-most side-view pixel at head height.
		noseZ = HEAD_Z;
		for (int z = ZMIN; z < HEAD_Z && noseZ == HEAD_Z; z++) for (int h = NECK_H; h < EAR_BASE; h++)
			if (SideFg(z + 0.5, h - 0.5)) { noseZ = z; break; }
		solid = new bool[XMAX - XMIN, HMAX + 1, ZMAX - ZMIN];
		for (int x = XMIN; x < XMAX; x++) for (int h = 1; h <= HMAX; h++) for (int z = ZMIN; z < ZMAX; z++)
			solid[x - XMIN, h, z - ZMIN] = InHull(x, h, z);
		// Trim thin protrusions (voxels with at most one solid neighbour), twice.
		for (int pass = 0; pass < 2; pass++) {
			var drop = new List<int[]>();
			for (int x = 0; x < XMAX - XMIN; x++) for (int h = 1; h <= HMAX; h++) for (int z = 0; z < ZMAX - ZMIN; z++) {
				if (!solid[x, h, z]) continue;
				int nb = 0;
				if (x > 0 && solid[x - 1, h, z]) nb++;
				if (x < XMAX - XMIN - 1 && solid[x + 1, h, z]) nb++;
				if (h > 1 && solid[x, h - 1, z]) nb++;
				if (h < HMAX && solid[x, h + 1, z]) nb++;
				if (z > 0 && solid[x, h, z - 1]) nb++;
				if (z < ZMAX - ZMIN - 1 && solid[x, h, z + 1]) nb++;
				if (nb <= 1) drop.Add(new[] { x, h, z });
			}
			foreach (var d in drop) solid[d[0], d[1], d[2]] = false;
		}
		// Keep only the biggest connected piece (drops stray ember specks).
		int[,,] label = new int[XMAX - XMIN, HMAX + 1, ZMAX - ZMIN];
		int best = 0, bestSize = 0, next = 0;
		for (int x = 0; x < XMAX - XMIN; x++) for (int h = 1; h <= HMAX; h++) for (int z = 0; z < ZMAX - ZMIN; z++) {
			if (!solid[x, h, z] || label[x, h, z] != 0) continue;
			next++; int size = 0;
			var q = new Queue<int[]>(); q.Enqueue(new[] { x, h, z }); label[x, h, z] = next;
			while (q.Count > 0) {
				var p = q.Dequeue(); size++;
				foreach (var d in new[] { new[] { 1, 0, 0 }, new[] { -1, 0, 0 }, new[] { 0, 1, 0 }, new[] { 0, -1, 0 }, new[] { 0, 0, 1 }, new[] { 0, 0, -1 } }) {
					int a = p[0] + d[0], b = p[1] + d[1], c = p[2] + d[2];
					if (a < 0 || a >= XMAX - XMIN || b < 1 || b > HMAX || c < 0 || c >= ZMAX - ZMIN) continue;
					if (!solid[a, b, c] || label[a, b, c] != 0) continue;
					label[a, b, c] = next; q.Enqueue(new[] { a, b, c });
				}
			}
			if (size > bestSize) { bestSize = size; best = next; }
		}
		for (int x = 0; x < XMAX - XMIN; x++) for (int h = 1; h <= HMAX; h++) for (int z = 0; z < ZMAX - ZMIN; z++)
			if (solid[x, h, z] && label[x, h, z] != best) solid[x, h, z] = false;
	}

	static string PartOf(int x, int h, int z) {
		if (h <= BELLY) {
			string leg = (z < Si(-4) ? "front" : "hind") + "_leg";
			string sideName = x < 0 ? "right_" : "left_";
			return sideName + leg + (h <= KNEE ? "/lower" : "");
		}
		if (z < HEAD_Z && h >= NECK_H) {
			if (h >= EAR_BASE) return x < 0 ? "head/right_ear" : "head/left_ear";
			return "head";
		}
		if (z < CHEST_Z && h < NECK_H) return "chest";
		return "body";
	}

	// Greedy merge of one part's voxels into boxes (x first, then z, then height).
	static List<int[]> Merge(Func<int, int, int, bool> has) {
		var used = new HashSet<long>();
		var boxes = new List<int[]>();
		Func<int, int, int, long> key = (x, h, z) => ((long)(x + 100) * 1000 + h) * 1000 + (z + 500);
		for (int h = 1; h <= HMAX; h++) for (int z = ZMIN; z < ZMAX; z++) for (int x = XMIN; x < XMAX; x++) {
			if (!has(x, h, z) || used.Contains(key(x, h, z))) continue;
			int w = 1; while (x + w < XMAX && has(x + w, h, z) && !used.Contains(key(x + w, h, z))) w++;
			int d = 1;
			while (z + d < ZMAX) {
				bool ok = true;
				for (int i = 0; i < w && ok; i++) ok = has(x + i, h, z + d) && !used.Contains(key(x + i, h, z + d));
				if (!ok) break; d++;
			}
			int t = 1;
			while (h + t <= HMAX) {
				bool ok = true;
				for (int i = 0; i < w && ok; i++) for (int k = 0; k < d && ok; k++) ok = has(x + i, h + t, z + k) && !used.Contains(key(x + i, h + t, z + k));
				if (!ok) break; t++;
			}
			for (int i = 0; i < w; i++) for (int k = 0; k < d; k++) for (int j = 0; j < t; j++) used.Add(key(x + i, h + j, z + k));
			boxes.Add(new[] { x, h, z, w, t, d });
		}
		return boxes;
	}

	// ---- Build ------------------------------------------------------------------------------
	static Dictionary<string, Part> parts = new Dictionary<string, Part>();
	static Part rootPart = new Part { Name = "root" };
	static List<Box> allBoxes = new List<Box>();

	static Part P(string name, Part parent, double px, double py, double pz, double rx = 0, double ry = 0, double rz = 0) {
		var p = new Part { Name = name, PX = px, PY = py, PZ = pz, RX = rx, RY = ry, RZ = rz };
		parent.Add(p); parts[name] = p; return p;
	}

	// Absolute pivot (rest pose, no rotations above voxel parts).
	static double[] Abs(Part p) {
		double x = 0, y = 0, z = 0;
		for (var q = p; q != null && q != rootPart; q = q.Parent) { x += q.PX; y += q.PY; z += q.PZ; }
		return new[] { x, y, z };
	}

	static double Y(double h) { return GROUND - h; }   // height above ground (units) -> model y

	static void BuildParts() {
		// Pivots (absolute, model units), chosen at the joints.
		Part body = P("body", rootPart, 0, Y(Si(30)), 0);
		P("chest", body, 0, Y(NECK_H) - Y(Si(30)), CHEST_Z - Si(4));
		Part head = P("head", body, 0, Y(Si(44)) - Y(Si(30)), HEAD_Z + 1);
		double earX = 5.5 * S;
		P("right_ear", head, -earX, Y(EAR_BASE) - Y(Si(44)), earZ - (HEAD_Z + 1));
		P("left_ear", head, earX, Y(EAR_BASE) - Y(Si(44)), earZ - (HEAD_Z + 1));
		foreach (string s in new[] { "right", "left" }) foreach (string fh in new[] { "front", "hind" }) {
			string name = s + "_" + fh + "_leg";
			// Leg pivot: the top centre of its voxels; knee at KNEE.
			double sx = 0, sz = 0; int n = 0;
			for (int x = XMIN; x < XMAX; x++) for (int z = ZMIN; z < ZMAX; z++) for (int h = KNEE + 1; h <= BELLY; h++)
				if (solid[x - XMIN, h, z - ZMIN] && PartOf(x, h, z) == name) { sx += x + 0.5; sz += z + 0.5; n++; }
			double cx = Math.Round(sx / n * 2) / 2, cz = Math.Round(sz / n * 2) / 2;
			Part leg = P(name, rootPart, cx, Y(BELLY + Si(2)), cz);
			var lower = new Part { Name = "lower", PX = 0, PY = Y(KNEE) - Y(BELLY + Si(2)), PZ = 0 };
			leg.Add(lower); parts[name + "/lower"] = lower;
		}
		// Voxel boxes: each PartOf key and the part its voxels go to.
		var voxelParts = new Dictionary<string, Part> {
			{ "body", body }, { "chest", parts["chest"] }, { "head", head },
			{ "head/right_ear", parts["right_ear"] }, { "head/left_ear", parts["left_ear"] } };
		foreach (string s in new[] { "right", "left" }) foreach (string fh in new[] { "front", "hind" }) {
			string name = s + "_" + fh + "_leg";
			voxelParts[name] = parts[name];
			voxelParts[name + "/lower"] = parts[name + "/lower"];
		}
		foreach (var kv in voxelParts) {
			string key = kv.Key;
			var part = kv.Value;
			double[] a = Abs(part);
			foreach (var b in Merge((x, h, z) => x >= XMIN && x < XMAX && h >= 1 && h <= HMAX && z >= ZMIN && z < ZMAX
					&& solid[x - XMIN, h, z - ZMIN] && PartOf(x, h, z) == key)) {
				// Box spans x..x+w, heights h-1..h-1+t (model y from Y(h-1+t) to Y(h-1)), z..z+d.
				var box = new Box { X = b[0] - a[0], Y = Y(b[1] - 1 + b[4]) - a[1], Z = b[2] - a[2], W = b[3], H = b[4], D = b[5], Paint = "sheet", Part = part };
				part.Boxes.Add(box);
			}
		}

		// Eyelids: a flat plate over the eyes, shown only while asleep.
		// The eyes are the warm (glowing) pixels on the front view's face.
		var eyelids = P("eyelids", head, 0, 0, 0);
		double[] ha = Abs(head);
		int eyeLo = int.MaxValue, eyeHi = int.MinValue, eyeX = 0;
		for (int h = NECK_H; h < EAR_BASE; h++) for (int x = -Si(8); x < Si(8); x++)
			if (Warm(FrontColor(x + 0.5, h - 0.5))) { eyeLo = Math.Min(eyeLo, h); eyeHi = Math.Max(eyeHi, h); eyeX = Math.Max(eyeX, Math.Abs(x) + 1); }
		if (eyeLo > eyeHi) { eyeLo = Si(46); eyeHi = Si(48); eyeX = Si(6); }
		int eyeMid = (eyeLo + eyeHi) / 2;
		eyelids.Boxes.Add(new Box { X = -eyeX, Y = Y(eyeHi) - ha[1], Z = FrontZAt(head, eyeMid) - ha[2] - 0.1,
			W = 2 * eyeX, H = eyeHi - eyeLo + 1, D = 0, Paint = "eyelids", Part = eyelids });

		// Tails: from the rump, the centre one sweeping high, the side ones back and out.
		double tz = TAIL_Z - Si(2), ty = Y(BACK_H - Si(2)) - Y(Si(30));
		Tail("tail_center", body, 0, ty, tz, 0.8, 0);
		Tail("tail_left", body, 4 * S, ty + 2 * S, tz, 0.3, 0.35);
		Tail("tail_right", body, -4 * S, ty + 2 * S, tz, 0.3, -0.35);
	}

	// The front-most z of the head at a height (for placing the eyelids on the face).
	static double FrontZAt(Part head, double h) {
		for (int z = ZMIN; z < ZMAX; z++) for (int x = -Math.Max(1, Si(3)); x < Math.Max(1, Si(3)); x++)
			if (solid[x - XMIN, (int)Math.Ceiling(h), z - ZMIN] && PartOf(x, (int)Math.Ceiling(h), z) == "head") return z;
		return HEAD_Z;
	}

	static readonly int[] TAIL_SIZE = { 7, 9, 11, 12, 13, 12, 10, 7 };
	static readonly double[] TAIL_LEN = { 4, 4, 4, 4.5, 4.5, 4.5, 4, 3.5 };
	const double TAIL_CURL = 0.06;

	static void Tail(string name, Part body, double x, double y, double z, double lift, double yaw) {
		Part seg = P(name, body, x, y, z, lift, yaw, 0);
		for (int i = 0; i < TAIL_SEGMENTS; i++) {
			int s = Math.Max(2, Si(TAIL_SIZE[i]));
			seg.Boxes.Add(new Box { X = -s / 2.0, Y = -s / 2.0, Z = 0, W = s, H = s, D = (int)Math.Ceiling(TAIL_LEN[i] * S) + 1, Paint = "tail" + i, Part = seg });
			if (i == TAIL_SEGMENTS - 1) break;
			var next = new Part { Name = "seg" + (i + 1), PZ = TAIL_LEN[i] * S, RX = TAIL_CURL };
			seg.Add(next); seg = next;
		}
		// Flame tufts at the end: a big one and two small licks.
		var flame = new Part { Name = "flame", PZ = TAIL_LEN[TAIL_SEGMENTS - 1] * S, RX = 0.15 };
		seg.Add(flame);
		int f5 = Math.Max(2, Si(5)), f3 = Math.Max(1, Si(3)), f2 = Math.Max(1, Si(2));
		flame.Boxes.Add(new Box { X = -f5 / 2.0, Y = -f5 / 2.0, Z = 0, W = f5, H = f5, D = f5, Paint = "flame", Part = flame });
		flame.Boxes.Add(new Box { X = -f2 / 2.0, Y = -f5 / 2.0 - f2 / 2.0, Z = 3 * S, W = f2, H = f3, D = f3, Paint = "flame", Part = flame });
		flame.Boxes.Add(new Box { X = -f2 / 2.0, Y = -f2 / 2.0, Z = 4.5 * S, W = f2, H = f2, D = f3, Paint = "flame", Part = flame });
	}

	// ---- UV packing -------------------------------------------------------------------------
	static int texW = 256, texH = 256;
	static void Pack() {
		foreach (var p in AllParts(rootPart)) allBoxes.AddRange(p.Boxes);
		for (int i = 0; i < allBoxes.Count; i++) allBoxes[i].Index = i;
		var order = allBoxes.OrderByDescending(b => b.D + b.H).ThenByDescending(b => 2 * (b.W + b.D)).ThenBy(b => b.Index).ToList();
		foreach (int size in new[] { 256, 512, 1024 }) {
			texW = size; texH = size;
			int x = 0, y = 0, rowH = 0; bool ok = true;
			foreach (var b in order) {
				int w = 2 * (b.W + b.D), h = b.D + b.H;
				if (x + w > texW) { x = 0; y += rowH; rowH = 0; }
				if (y + h > texH) { ok = false; break; }
				b.U = x; b.V = y; x += w; rowH = Math.Max(rowH, h);
			}
			if (ok) return;
		}
		throw new Exception("Texture atlas overflow");
	}

	static List<Part> AllParts(Part p) {
		var list = new List<Part> { p };
		foreach (var c in p.Children) list.AddRange(AllParts(c));
		return list;
	}

	// ---- Painting ---------------------------------------------------------------------------
	static Bitmap tex, glow;
	static readonly Color CHARCOAL = Color.FromArgb(41, 33, 45), MAROON = Color.FromArgb(73, 35, 43),
		DEEP = Color.FromArgb(110, 36, 36), VOLCANIC = Color.FromArgb(165, 47, 39), FLAME = Color.FromArgb(232, 75, 32),
		EMBER = Color.FromArgb(255, 131, 31), YELLOW = Color.FromArgb(255, 195, 77), HOT = Color.FromArgb(255, 236, 150);

	static void Put(int u, int v, Color c, bool glowing) {
		tex.SetPixel(u, v, c);
		if (glowing) glow.SetPixel(u, v, c);
	}

	static Color Shade(Color c, double f) {
		return Color.FromArgb(Math.Min(255, (int)(c.R * f)), Math.Min(255, (int)(c.G * f)), Math.Min(255, (int)(c.B * f)));
	}

	static bool Warm(Color c) { return c.R > 225 && c.G > 120 && c.B < 140; }

	static void Paint() {
		tex = new Bitmap(texW, texH, PixelFormat.Format32bppArgb);
		glow = new Bitmap(texW, texH, PixelFormat.Format32bppArgb);
		foreach (var b in allBoxes) {
			double[] a = Abs(b.Part);
			// Face texels: (face, i, j) -> texture (u, v) and the point on the box surface.
			PaintFace(b, a, "front", b.U + b.D, b.V + b.D, b.W, b.H);
			PaintFace(b, a, "back", b.U + 2 * b.D + b.W, b.V + b.D, b.W, b.H);
			PaintFace(b, a, "minx", b.U, b.V + b.D, b.D, b.H);
			PaintFace(b, a, "maxx", b.U + b.D + b.W, b.V + b.D, b.D, b.H);
			PaintFace(b, a, "top", b.U + b.D, b.V, b.W, b.D);
			PaintFace(b, a, "bottom", b.U + b.D + b.W, b.V, b.W, b.D);
		}
	}

	static void PaintFace(Box b, double[] a, string face, int u0, int v0, int fw, int fh) {
		for (int i = 0; i < fw; i++) for (int j = 0; j < fh; j++) {
			double x0 = a[0] + b.X, y0 = a[1] + b.Y, z0 = a[2] + b.Z;
			double x, y, z;
			switch (face) {
				case "front": x = x0 + i + 0.5; y = y0 + j + 0.5; z = z0; break;
				case "back": x = x0 + b.W - i - 0.5; y = y0 + j + 0.5; z = z0 + b.D; break;
				case "minx": x = x0; y = y0 + j + 0.5; z = z0 + b.D - i - 0.5; break;
				case "maxx": x = x0 + b.W; y = y0 + j + 0.5; z = z0 + i + 0.5; break;
				case "top": x = x0 + i + 0.5; y = y0; z = z0 + b.D - j - 0.5; break;
				default: x = x0 + i + 0.5; y = y0 + b.H; z = z0 + b.D - j - 0.5; break;
			}
			double h = GROUND - y;
			Color c; bool g = false;
			string pname = b.Part.Name;
			if (b.Paint == "sheet" && SCULPT && (pname == "head" || pname.EndsWith("_ear"))) {
				c = HeadColor(face, x / S, h / S, z / S, out g);
			} else if (b.Paint == "sheet") {
				if (FLAT) {
					// Five samples (the texel and one unit either way along the face), each
					// snapped to the palette; the most common one wins, so specks vanish.
					var votes = new Dictionary<int, int>();
					double[][] offsets = { new[] { 0.0, 0.0 }, new[] { 1.0, 0.0 }, new[] { -1.0, 0.0 }, new[] { 0.0, 1.0 }, new[] { 0.0, -1.0 } };
					foreach (var o in offsets) {
						double sx = x, sz = z, sh = h;
						if (face == "front" || face == "back") { sx += o[0]; sh += o[1]; }
						else if (face == "top" || face == "bottom") { sx += o[0]; sz += o[1]; }
						else { sz += o[0]; sh += o[1]; }
						int key = Quantize(SheetColor(face, sx, sz, sh, u0 + i, v0 + j)).ToArgb();
						votes[key] = (votes.ContainsKey(key) ? votes[key] : 0) + (o[0] == 0 && o[1] == 0 ? 2 : 1);
					}
					c = Color.FromArgb(votes.OrderByDescending(kv => kv.Value).ThenBy(kv => kv.Key).First().Key);
					// The sheet never shows the tops or the rump: plain fur there, so they don't
					// repeat one edge row as stripes (the cream ruff stays cream).
					if (face == "top" && b.Part.Name != "chest") c = CHARCOAL;
					else if (face == "back" && b.Part.Name == "body") c = MAROON;
				} else {
					c = SheetColor(face, x, z, h, u0 + i, v0 + j);
				}
				string pn = b.Part.Name;
				// Glow: eyes and spirals; on the ears only the inner strip (front) and the tips.
				g = Warm(c) && face != "top" && (pn == "head" || pn == "body"
					|| (pn.EndsWith("_ear") && (face == "front" || h >= EAR_TIP - 3 * S)));
			} else if (b.Paint == "eyelids") {
				c = j == fh - 1 && (i < fw / 2 - 1 || i > fw / 2) ? Color.FromArgb(22, 16, 22) : VOLCANIC;
			} else if (b.Paint == "flame") {
				c = FLAT ? (face == "back" || face == "top" ? HOT : YELLOW)
					: ((u0 + i) * 7 + (v0 + j) * 13) % 3 == 0 ? HOT : YELLOW;
				g = true;
			} else {
				// Tail segment k of 8: charcoal at the root to yellow at the tip, brighter on top.
				int k = int.Parse(b.Paint.Substring(4));
				double t = (k + (face == "back" ? 1.0 : face == "front" ? 0.0 : (face == "top" || face == "bottom" ? (b.D - j - 0.5) / b.D : (face == "minx" ? (b.D - i - 0.5) / b.D : (i + 0.5) / b.D)))) / TAIL_SEGMENTS;
				t += (FLAT ? 0 : (Hash(u0 + i, v0 + j) - 0.5) * 0.14) - (face == "top" ? 0.04 : face == "bottom" ? -0.06 : 0);
				c = TailColor(t, out g);
			}
			Put(u0 + i, v0 + j, c, g);
		}
	}

	static Color SheetColor(string face, double x, double z, double h, int tu, int tv) {
		Color c;
		if (face == "front") {
			c = FrontColor(x, h);
			if (c.IsEmpty) c = SideColor(z, h);
		} else if (face == "top") {
			// The sheet never shows the top: borrow side colours just below the edge (jittered
			// unless FLAT, so the back doesn't streak), and never glowing.
			c = FLAT ? SideColor(z, h - 1.5 * S)
				: SideColor(z + (Hash(tu, tv) - 0.5) * 3 * S, h - (1.5 + Hash(tv, tu) * 3) * S);
			if (Warm(c)) c = VOLCANIC;
		} else if (face == "bottom") {
			c = Shade(SideColor(z, h + 0.6 * S), 0.8);
		} else if (face == "back") {
			// Hidden behind the tails on the sheet: borrow side colours just in from the rump.
			c = FLAT ? SideColor(z - 1.5 * S, h)
				: Shade(SideColor(z - (0.6 + Hash(tu, tv) * 4) * S, h + (Hash(tv, tu) - 0.5) * 2 * S), 0.9);
		} else {
			c = SideColor(z, h);
		}
		return c;
	}

	// The sheet's palette (its swatches plus the spec's colours), for FLAT painting.
	static readonly Color[] PALETTE = {
		Color.FromArgb(41, 33, 45), Color.FromArgb(73, 35, 43), Color.FromArgb(110, 36, 36),
		Color.FromArgb(165, 47, 39), Color.FromArgb(232, 75, 32), Color.FromArgb(255, 131, 31),
		Color.FromArgb(255, 195, 77), Color.FromArgb(232, 214, 185), Color.FromArgb(198, 172, 146),
		Color.FromArgb(22, 16, 22) };

	static Color Quantize(Color c) {
		Color best = PALETTE[0]; double bestD = double.MaxValue;
		foreach (var p in PALETTE) {
			// Weighted RGB distance (green matters most to the eye).
			double dr = c.R - p.R, dg = c.G - p.G, db = c.B - p.B;
			double d = 2 * dr * dr + 4 * dg * dg + 3 * db * db;
			if (d < bestD) { bestD = d; best = p; }
		}
		return best;
	}

	static double Hash(int x, int y) {
		unchecked {
			uint h = (uint)(x * 374761393 + y * 668265263);
			h = (h ^ (h >> 13)) * 1274126177u;
			return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0;
		}
	}

	static Color TailColor(double t, out bool g) {
		g = false;
		if (t < 0.14) return CHARCOAL;
		if (t < 0.26) return MAROON;
		if (t < 0.36) return DEEP;
		if (t < 0.48) return VOLCANIC;
		if (t < 0.6) return FLAME;
		if (t < 0.76) return EMBER;
		g = true;
		return t < 0.9 ? YELLOW : HOT;
	}

	// ---- Output -----------------------------------------------------------------------------
	static string F(double v) {
		string s = Math.Round(v, 3).ToString("0.###", CultureInfo.InvariantCulture);
		return (s.Contains(".") ? s : s + ".0") + "F";
	}

	static string JavaName(string name) {
		var sb = new StringBuilder(); bool up = false;
		foreach (char ch in name) { if (ch == '_') { up = true; continue; } sb.Append(up ? char.ToUpper(ch) : ch); up = false; }
		return sb.ToString();
	}

	static void WriteJava(string path) {
		var sb = new StringBuilder();
		sb.AppendLine("package com.mythweave.client.model.entity;");
		sb.AppendLine();
		sb.AppendLine("import net.minecraft.client.model.geom.PartPose;");
		sb.AppendLine("import net.minecraft.client.model.geom.builders.CubeListBuilder;");
		sb.AppendLine("import net.minecraft.client.model.geom.builders.LayerDefinition;");
		sb.AppendLine("import net.minecraft.client.model.geom.builders.MeshDefinition;");
		sb.AppendLine("import net.minecraft.client.model.geom.builders.MeshTransformer;");
		sb.AppendLine("import net.minecraft.client.model.geom.builders.PartDefinition;");
		sb.AppendLine();
		sb.AppendLine("/**");
		sb.AppendLine(" * GENERATED by tools/models/ashenox_gen.ps1 from the Ashenox design sheet. Do not edit by hand:");
		sb.AppendLine(" * change the generator and run it again. The animation lives in {@link AshenoxModel}.");
		sb.AppendLine(" *");
		sb.AppendLine(" * <p>Built at " + DETAIL.ToString(CultureInfo.InvariantCulture) + "x detail (" + allBoxes.Count + " boxes) and scaled back down by");
		sb.AppendLine(" * {@link MeshTransformer#scaling}, so part positions in the animation are in model units");
		sb.AppendLine(" * ({@link #DETAIL} per pixel). Texture: " + texW + " x " + texH + ".");
		sb.AppendLine(" */");
		sb.AppendLine("final class AshenoxModelGeometry {");
		sb.AppendLine("\t/** Model units per pixel. */");
		sb.AppendLine("\tstatic final float DETAIL = " + F(DETAIL) + ";");
		sb.AppendLine("\t/** Segments per tail (seg0 is the tail part itself, then seg1.., then flame). */");
		sb.AppendLine("\tstatic final int TAIL_SEGMENTS = " + TAIL_SEGMENTS + ";");
		sb.AppendLine();
		sb.AppendLine("\tprivate AshenoxModelGeometry() {");
		sb.AppendLine("\t}");
		sb.AppendLine();
		sb.AppendLine("\tstatic LayerDefinition createBodyLayer() {");
		sb.AppendLine("\t\tMeshDefinition mesh = new MeshDefinition();");
		sb.AppendLine("\t\tPartDefinition root = mesh.getRoot();");
		int counter = 0;
		foreach (var c in rootPart.Children) WritePart(sb, c, "root", ref counter);
		sb.AppendLine("\t\treturn LayerDefinition.create(mesh, " + texW + ", " + texH + ").apply(MeshTransformer.scaling(1.0F / DETAIL));");
		sb.AppendLine("\t}");
		sb.AppendLine("}");
		File.WriteAllText(path, sb.ToString().Replace("\r\n", "\n"));
	}

	static void WritePart(StringBuilder sb, Part p, string parentVar, ref int counter) {
		string v = "p" + (counter++);
		sb.Append("\t\tPartDefinition " + v + " = " + parentVar + ".addOrReplaceChild(\"" + p.Name + "\", CubeListBuilder.create()");
		foreach (var b in p.Boxes)
			sb.Append("\n\t\t\t\t.texOffs(" + b.U + ", " + b.V + ").addBox(" + F(b.X) + ", " + F(b.Y) + ", " + F(b.Z) + ", " + F(b.W) + ", " + F(b.H) + ", " + F(b.D) + ")");
		if (p.RX == 0 && p.RY == 0 && p.RZ == 0)
			sb.AppendLine(",\n\t\t\t\tPartPose.offset(" + F(p.PX) + ", " + F(p.PY) + ", " + F(p.PZ) + "));");
		else
			sb.AppendLine(",\n\t\t\t\tPartPose.offsetAndRotation(" + F(p.PX) + ", " + F(p.PY) + ", " + F(p.PZ) + ", " + F(p.RX) + ", " + F(p.RY) + ", " + F(p.RZ) + "));");
		foreach (var c in p.Children) WritePart(sb, c, v, ref counter);
	}

	static void WriteJson(string path) {
		var sb = new StringBuilder();
		sb.Append("{\"detail\":" + DETAIL.ToString(CultureInfo.InvariantCulture) + ",\"parts\":[");
		bool first = true;
		foreach (var c in rootPart.Children) WriteJsonPart(sb, c, ref first);
		sb.Append("]}");
		File.WriteAllText(path, sb.ToString());
	}

	static Dictionary<Part, int> ids = new Dictionary<Part, int>();
	static int Id(Part p) { int id; if (!ids.TryGetValue(p, out id)) { id = ids.Count; ids[p] = id; } return id; }

	static void WriteJsonPart(StringBuilder sb, Part p, ref bool first) {
		if (!first) sb.Append(",");
		first = false;
		Func<double, string> n = d => Math.Round(d, 4).ToString(CultureInfo.InvariantCulture);
		sb.Append("{\"name\":\"" + p.Name + "\",\"parent\":\"" + (p.Parent == rootPart ? "" : p.Parent.Name + "#" + Id(p.Parent)) + "\",\"id\":\"" + p.Name + "#" + Id(p) + "\"");
		sb.Append(",\"pivot\":[" + n(p.PX) + "," + n(p.PY) + "," + n(p.PZ) + "],\"rot\":[" + n(p.RX) + "," + n(p.RY) + "," + n(p.RZ) + "],\"boxes\":[");
		for (int i = 0; i < p.Boxes.Count; i++) {
			var b = p.Boxes[i];
			if (i > 0) sb.Append(",");
			sb.Append("[" + b.U + "," + b.V + "," + n(b.X) + "," + n(b.Y) + "," + n(b.Z) + "," + b.W + "," + b.H + "," + b.D + "]");
		}
		sb.Append("]}");
		foreach (var c in p.Children) WriteJsonPart(sb, c, ref first);
	}

	public static string Run(string root, double detail, bool flat, bool sculpt, string outDir) {
		Configure(detail, flat, sculpt);
		sheet = new Bitmap(Path.Combine(root, "docs", "reference", "ashenox", "Ashenox_ Ember Fox Mount Design Sheet.png"));
		front = Load(FRONT, false); side = Load(SIDE, true); back = Load(BACK, false);
		BuildHull();
		BuildParts();
		Pack();
		Paint();
		bool trial = !string.IsNullOrEmpty(outDir);
		string texDir = trial ? outDir : Path.Combine(root, "src", "main", "resources", "assets", "mythweave", "textures", "entity", "ashenox");
		Directory.CreateDirectory(texDir);
		tex.Save(Path.Combine(texDir, "ashenox.png"), ImageFormat.Png);
		glow.Save(Path.Combine(texDir, "ashenox_glow.png"), ImageFormat.Png);
		WriteJava(trial ? Path.Combine(outDir, "AshenoxModelGeometry.java")
			: Path.Combine(root, "src", "client", "java", "com", "mythweave", "client", "model", "entity", "AshenoxModelGeometry.java"));
		WriteJson(trial ? Path.Combine(outDir, "ashenox_parts.json") : Path.Combine(root, "tools", "models", "ashenox_parts.json"));
		int voxels = 0; foreach (bool s in solid) if (s) voxels++;
		return "boxes " + allBoxes.Count + ", voxels " + voxels + ", texture " + texW + "x" + texH
			+ ", scales front " + Math.Round(front.Scale, 3) + " side " + Math.Round(side.Scale, 3) + ", ear z " + Math.Round(earZ, 1);
	}
}
'@

[AshenoxGen]::Run($root, $Detail, -not $Shaded, [bool]$SculptHead, $OutDir)
