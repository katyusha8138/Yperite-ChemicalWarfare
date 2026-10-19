"""Painting textures for models built with geo.py, pixel-art style.

Every texel is painted from the 3D point it lands on (see geo.face_geometry), so materials are continuous across
faces. A material picks a tone on a short colour ramp (dark to light, like hand-made pixel art) and the result is
quantised to that ramp, with ordered dithering where two tones meet. Lighting is left to the game: only small,
local cues are baked in (edge highlights, grime in low places, wear on exposed edges).
"""
import math

import numpy as np
from PIL import Image

from geo import FACES, Transforms, face_geometry

BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 - 0.5


# ---------------------------------------------------------------------- noise

def _hash(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144269504) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def hash3(p, seed=0):
    """A random value 0..1 per pixel cell of a 3D point."""
    return _hash(int(math.floor(p[0])), int(math.floor(p[1])), int(math.floor(p[2])), seed)


def vnoise(p, scale, seed=0):
    """Smooth value noise 0..1 with features of the given size in pixels."""
    x, y, z = p[0] / scale, p[1] / scale, p[2] / scale
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    v = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                v += w * _hash(ix + dx, iy + dy, iz + dz, seed)
    return v


# ---------------------------------------------------------------------- texel context

class Texel:
    """What a material knows about the texel it paints."""
    __slots__ = ("pos", "normal", "face", "cube", "s", "t", "w", "h", "i", "j", "u", "v", "fy", "fx", "fid")

    def edge(self):
        """Distance in texels to the nearest border of the face (0 on the border row)."""
        return min(self.i, self.j, self.w - 1 - self.i, self.h - 1 - self.j)

    def edge_side(self):
        """Which border is nearest: 'top', 'bottom', 'left' or 'right' (top/bottom follow the texture rows)."""
        d = {"left": self.i, "right": self.w - 1 - self.i, "top": self.j, "bottom": self.h - 1 - self.j}
        return min(d, key=d.get)

    def dither(self):
        return BAYER4[self.v % 4, self.u % 4]

    def corners(self):
        """For sculpted (voxel) parts: the in-plane directions in which the surface turns away (a convex edge),
        as unit vectors. Empty for explicit boxes, which use the face border instead."""
        solid = self.cube.info.get("solid")
        if solid is None:
            return None
        n = np.asarray(self.normal)
        back = self.pos - n * 0.5
        out = []
        for axis in range(3):
            if abs(n[axis]) > 0.5:
                continue
            for sgn in (-1.0, 1.0):
                d = np.zeros(3)
                d[axis] = sgn
                q = back + d
                # the cell beside is air and so is the one in front of it: the surface ends here
                if not solid(q) and not solid(q + n):
                    out.append(d)
        return out

    def rim(self):
        """'top' when the texel sits on an upper convex edge, 'bottom' on a lower one, 'side' on a side edge,
        'edge' on the free edge of a surface made of plates, None inside a face."""
        pl = self.cube.info.get("plate")
        if pl is not None:
            if not self.cube.info.get("last_row"):
                return None
            rel = self.pos - pl["center"]
            along = float(rel @ pl["e2"]) * pl["v_sign"]
            big = abs(float(np.asarray(self.normal) @ pl["n"])) > 0.5
            if big and along > pl["hh"] - 1.1:
                return "edge"
            if not big and float(np.asarray(self.normal) @ pl["e2"]) * pl["v_sign"] > 0.5:
                return "edge"
            return None
        c = self.corners()
        if c is None:
            if self.edge() != 0 or self.w <= 1 or self.h <= 1:
                return None
            side = self.edge_side()
            return "top" if side == "top" else ("bottom" if side == "bottom" else "side")
        if not c:
            return None
        if abs(self.normal[1]) > 0.5:      # on a top or bottom face every edge is an outline
            return "top" if self.normal[1] > 0 else "bottom"
        ys = [d[1] for d in c]
        if any(y > 0.5 for y in ys):
            return "top"
        if any(y < -0.5 for y in ys):
            return "bottom"
        return "side"


def pick(ramp, level, texel=None, dither=0.9):
    """The ramp colour for a fractional level, dithered between neighbours."""
    if texel is not None:
        level += texel.dither() * dither
    idx = int(round(level))
    idx = max(0, min(len(ramp) - 1, idx))
    c = ramp[idx]
    return (c[0], c[1], c[2], c[3] if len(c) > 3 else 255)


def hexramp(*codes):
    return [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) for c in codes]


# ---------------------------------------------------------------------- painting a model

def paint(model):
    """Paints every packed face of the model and returns the texture."""
    img = np.zeros((model.tex_h, model.tex_w, 4), dtype=np.uint8)
    transforms = Transforms(model)
    for name in model.order:
        for cube in model.bones[name].cubes:
            for face, (u0, v0, w, h) in cube.uv.items():
                corners, normal = face_geometry(model, name, cube, face, transforms)
                lt, rt, rb, lb = corners
                ex = rt - lt
                ey = lb - lt
                ys = [c[1] for c in corners]
                y_lo, y_hi = min(ys), max(ys)
                # a horizontal direction along the face, to place highlights consistently
                along = ex if abs(ex[1]) <= abs(ey[1]) else ey
                along = along / (np.linalg.norm(along) or 1.0)
                fid = (u0 * 73856093 + v0 * 19349663) & 0xFFFF
                for j in range(h):
                    for i in range(w):
                        t = Texel()
                        t.s = (i + 0.5) / w
                        t.t = (j + 0.5) / h
                        t.pos = lt + ex * t.s + ey * t.t
                        t.normal = normal
                        t.face = face
                        t.cube = cube
                        t.w, t.h, t.i, t.j = w, h, i, j
                        t.u, t.v = u0 + i, v0 + j
                        t.fy = (t.pos[1] - y_lo) / (y_hi - y_lo) if y_hi - y_lo > 1e-6 else 0.5
                        t.fx = float((t.pos - lt) @ along)
                        t.fid = fid
                        img[v0 + j, u0 + i] = cube.material.color(t)
    return Image.fromarray(img, "RGBA")


# ---------------------------------------------------------------------- materials

class Material:
    def color(self, t):
        raise NotImplementedError


class Flat(Material):
    def __init__(self, rgba):
        self.rgba = rgba if len(rgba) == 4 else (*rgba, 255)

    def color(self, t):
        return self.rgba


LIGHT_DIR = np.array([0.45, 0.8, -0.4])
LIGHT_DIR = LIGHT_DIR / np.linalg.norm(LIGHT_DIR)


def _rows_from_top(t):
    return int(round((1.0 - t.fy) * t.h - 0.5))


def _glint(t):
    """A single short highlight on a large face: one texel high, on the second row from the top, towards the
    start of the face. Returns the extra level for the texel."""
    w, h = max(t.w, 1), max(t.h, 1)
    if w < 5 or h < 3:
        return 0.0
    row = _rows_from_top(t)
    x0 = int(0.2 * w) + (t.fid % 2)
    length = max(2, int(0.35 * w))
    col = int(t.fx)
    if row == 1 and x0 <= col < x0 + length:
        return 2.4
    if row == 2 and col == x0 + length:
        return 1.2
    return 0.0


class Steel(Material):
    """Painted steel with a soft sheen. Vertical faces run from a lighter top to a darker foot; faces that look
    towards the light carry a thin diagonal glint; upward faces are brighter, with a short streak of shine. Real
    rims (the free edge of a skirt, visor or brim, or boxes marked rim=True) get a highlight and a few chips.
    `inside(p, n)` tells whether a face looks into the shell's cavity, which is painted darker.

    The ramp should run dark to light with a slight hue shift (cool shadows, warm lights) and have 6 or more tones."""

    def __init__(self, ramp, bare=None, seed=1, mottle=0.0, wear=0.04, inside=None, inner_ramp=None, grain=0.0,
                 sheen=1.0, rust=0.0):
        self.ramp = ramp
        self.bare = bare or hexramp("3b3b3d", "5d5c59", "8a8780")
        self.seed = seed
        self.mottle = mottle
        self.wear = wear
        self.inside = inside
        self.inner_ramp = inner_ramp
        self.grain = grain
        self.sheen = sheen

    def color(self, t):
        p, n = t.pos, np.asarray(t.normal)
        if self.inside is not None and self.inside(p, n):
            ramp = self.inner_ramp or [tuple(int(c * 0.5) for c in col) for col in self.ramp]
            return pick(ramp, 1.0 + 0.6 * t.fy)
        top = len(self.ramp) - 1
        lit = float(n @ LIGHT_DIR)
        level = top * 0.46 + 0.9 * lit
        level += (vnoise(p, 6.0, self.seed) - 0.5) * 2.0 * self.mottle
        level += (hash3(p, self.seed + 1) - 0.5) * 2.0 * self.grain
        if abs(n[1]) < 0.6:
            # vertical faces: a touch lighter at the top, a crisp light line where the face turns over
            level += (t.fy - 0.5) * 0.7
            if _rows_from_top(t) == 0 and t.h > 2:
                level += 1.4
            if lit > 0.2:
                level += self.sheen * _glint(t)
        elif n[1] > 0:
            level += 0.7
            if lit > 0.2:
                level += self.sheen * _glint(t)
        else:
            level -= 0.8
        marked = t.cube.info.get("rim")
        rim = t.rim() if (marked or t.cube.info.get("plate") is not None) else None
        if rim is not None:
            r = hash3(p + 0.11, self.seed + 2)
            if r < self.wear * 4:
                return pick(self.bare, 1 if r < self.wear * 2 else 2)
            level += 0.9
        return pick(self.ramp, level)


class Cloth(Material):
    """Woollen cloth: fine twill, a little fuzz, darker folds low down and on the edges."""

    def __init__(self, ramp, seed=11, twill=True, fold=0.6):
        self.ramp = ramp
        self.seed = seed
        self.twill = twill
        self.fold = fold

    def color(self, t):
        p, n = t.pos, t.normal
        mid = (len(self.ramp) - 1) / 2.0
        level = mid + 0.35 * n[1]
        level += (vnoise(p, 4.0, self.seed) - 0.5) * 2.0 * self.fold
        level += (hash3(p, self.seed + 1) - 0.5) * 0.9
        if self.twill and (t.u + t.v) % 3 == 0:
            level -= 0.45
        if t.edge() == 0 and t.edge_side() == "bottom":
            level -= 0.6
        return pick(self.ramp, level, t)


class Leather(Material):
    def __init__(self, ramp, seed=21, shine=True):
        self.ramp = ramp
        self.seed = seed
        self.shine = shine

    def color(self, t):
        p, n = t.pos, t.normal
        mid = (len(self.ramp) - 1) / 2.0
        level = mid + 0.4 * n[1] + (vnoise(p, 2.5, self.seed) - 0.5) * 1.6
        level += (hash3(p, self.seed + 1) - 0.5) * 0.6
        if t.edge() == 0:
            level -= 0.7
        if self.shine and hash3(p, self.seed + 2) > 0.93:
            level += 1.2
        return pick(self.ramp, level, t)


class Rubber(Material):
    def __init__(self, ramp, seed=31):
        self.ramp = ramp
        self.seed = seed

    def color(self, t):
        p, n = t.pos, t.normal
        mid = (len(self.ramp) - 1) / 2.0
        level = mid + 0.6 * n[1] + (vnoise(p, 3.0, self.seed) - 0.5) * 0.8
        if t.edge() == 0 and t.edge_side() == "top":
            level += 0.8
        return pick(self.ramp, level, t)


class Metal(Material):
    """Bare or plated metal: brass, nickel, gunmetal. Strong highlights on top edges."""

    def __init__(self, ramp, seed=41):
        self.ramp = ramp
        self.seed = seed

    def color(self, t):
        p, n = t.pos, t.normal
        mid = (len(self.ramp) - 1) / 2.0
        level = mid + 0.8 * n[1] - 0.3 * abs(n[0]) + (hash3(p, self.seed) - 0.5) * 0.8
        if t.edge() == 0:
            level += 1.0 if t.edge_side() in ("top", "left") else -0.8
        return pick(self.ramp, level, t)


class Glass(Material):
    def __init__(self, ramp, frame=None):
        self.ramp = ramp
        self.frame = frame

    def color(self, t):
        if self.frame is not None and t.edge() == 0 and t.w > 2 and t.h > 2:
            return self.frame.color(t)
        # a dark lens with a small reflection in the upper corner
        if t.i == 1 and t.j == 1 and t.w > 2 and t.h > 2:
            return pick(self.ramp, len(self.ramp) - 1)
        return pick(self.ramp, 0.6 + 0.6 * t.t, t)


class Features(Material):
    """A base material with painted details on top (rivets, seams, stencils). Each feature is a function
    (texel) -> colour or None."""

    def __init__(self, base, *features):
        self.base = base
        self.features = features

    def color(self, t):
        for f in self.features:
            c = f(t)
            if c is not None:
                return c if len(c) == 4 else (*c, 255)
        return self.base.color(t)


def rivet(center, radius, ramp):
    center = np.array(center, dtype=float)

    def f(t):
        d = np.linalg.norm(t.pos - center)
        if d <= radius:
            # light on the top-left, dark below: a small domed head
            rel = t.pos - center
            return pick(ramp, 1.5 + rel[1] * 1.5 - abs(rel[0]) * 0.3, None)
        return None

    return f


def seam(axis, value, width, ramp_color):
    """A stitched seam: a dark line where the coordinate `axis` equals value, dashed for stitches."""

    def f(t):
        if abs(t.pos[axis] - value) < width / 2:
            return ramp_color if (t.u + t.v) % 2 == 0 else None
        return None

    return f
