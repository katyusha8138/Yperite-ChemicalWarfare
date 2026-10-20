"""Building GeckoLib (Bedrock) geometry, packing its texture and painting it.

Coordinates are Bedrock model coordinates in pixels (1/16 block): +Y is up, the front of a worn model is -Z and the
wearer's right is -X. GeckoLib turns these into its own space by flipping X; the texel-to-surface mapping below is a
port of GeckoLib's BakedModelFactory/GeoQuad (per-face UV, no mirroring), so a texture painted here lands exactly
where GeckoLib draws it.

A model is made of bones holding cubes. Cubes come from two sources:
  * explicit boxes (any size, inflate and rotation), and
  * voxel sculpting: a 1-pixel grid filled by shape functions and merged greedily into boxes, keeping only the faces
    that touch air. One voxel is one texel, so sculpted parts have exactly the pixel size of a vanilla skin.
Every face gets its own rectangle in the texture. Painting is done per texel from the 3D point it lands on, so a
material looks continuous across faces and does not care about UV orientation.
"""
import json
import math
from dataclasses import dataclass, field

import numpy as np

FACES = ("north", "south", "east", "west", "up", "down")
# Outward normals of the faces in Bedrock coordinates. GeckoLib's "east" quad sits on its +X side, which is -X here.
NORMAL = {
    "north": (0.0, 0.0, -1.0),
    "south": (0.0, 0.0, 1.0),
    "east": (-1.0, 0.0, 0.0),
    "west": (1.0, 0.0, 0.0),
    "up": (0.0, 1.0, 0.0),
    "down": (0.0, -1.0, 0.0),
}


@dataclass
class Cube:
    origin: tuple
    size: tuple
    material: object
    inflate: float = 0.0
    pivot: tuple = None
    rotation: tuple = None
    faces: tuple = FACES
    info: dict = field(default_factory=dict)
    uv: dict = field(default_factory=dict)  # face -> (u, v, w, h) once packed

    def face_texels(self, face):
        """Width and height in texels of a face's rectangle (at least one). A cube marked density=k gets k texels
        per pixel, for a small detail that needs finer drawing (a stencilled number)."""
        k = self.info.get("density", 1)
        w, h, d = (s * k for s in self.size)
        dims = {
            "north": (w, h), "south": (w, h),
            "east": (d, h), "west": (d, h),
            "up": (w, d), "down": (w, d),
        }[face]
        return max(1, int(math.ceil(dims[0] - 1e-6))), max(1, int(math.ceil(dims[1] - 1e-6)))


@dataclass
class Bone:
    name: str
    parent: str = None
    pivot: tuple = (0.0, 0.0, 0.0)
    rotation: tuple = None
    cubes: list = field(default_factory=list)


class Model:
    def __init__(self, identifier):
        self.identifier = identifier
        self.bones = {}
        self.order = []
        self.tex_w = 0
        self.tex_h = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        if name not in self.bones:
            self.bones[name] = Bone(name, parent, tuple(float(p) for p in pivot), rotation)
            self.order.append(name)
        return self.bones[name]

    def box(self, bone, origin, size, material, inflate=0.0, pivot=None, rotation=None, faces=FACES, **info):
        cube = Cube(tuple(float(o) for o in origin), tuple(float(s) for s in size), material, inflate,
                    tuple(float(p) for p in pivot) if pivot is not None else None,
                    tuple(float(r) for r in rotation) if rotation is not None else None, tuple(faces), info)
        self.bones[bone].cubes.append(cube)
        return cube

    # ------------------------------------------------------------------ packing

    def pack(self, min_w=16, min_h=16, max_size=1024):
        rects = []
        for name in self.order:
            for ci, cube in enumerate(self.bones[name].cubes):
                for face in cube.faces:
                    w, h = cube.face_texels(face)
                    rects.append((h, w, name, ci, face))
        rects.sort(key=lambda r: (-r[0], -r[1]))
        area = sum(r[0] * r[1] for r in rects)
        tw = min_w
        while tw * tw < area * 1.3 and tw < max_size:
            tw *= 2
        tw = max(tw, max((r[1] for r in rects), default=1))
        while True:
            placed = _shelf_pack(rects, tw)
            if placed is not None:
                break
            tw *= 2
        positions, used_h = placed
        th = min_h
        while th < used_h:
            th *= 2
        # Square-ish is not required; GeckoLib takes any power-of-two size from the file header.
        self.tex_w, self.tex_h = tw, th
        for (h, w, name, ci, face), (u, v) in zip(rects, positions):
            self.bones[name].cubes[ci].uv[face] = (u, v, w, h)

    # ------------------------------------------------------------------ JSON

    def to_json(self):
        bones = []
        for name in self.order:
            b = self.bones[name]
            out = {"name": b.name, "pivot": list(b.pivot)}
            if b.parent:
                out["parent"] = b.parent
            if b.rotation:
                out["rotation"] = list(b.rotation)
            cubes = []
            for c in b.cubes:
                cj = {"origin": _r(c.origin), "size": _r(c.size)}
                if c.inflate:
                    cj["inflate"] = round(c.inflate, 4)
                if c.rotation:
                    cj["pivot"] = _r(c.pivot or c.origin)
                    cj["rotation"] = _r(c.rotation)
                cj["uv"] = {f: {"uv": [u, v], "uv_size": [w, h]} for f, (u, v, w, h) in c.uv.items()}
                cubes.append(cj)
            if cubes:
                out["cubes"] = cubes
            bones.append(out)
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": "geometry." + self.identifier,
                    "texture_width": self.tex_w,
                    "texture_height": self.tex_h,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1.5, 0],
                },
                "bones": bones,
            }],
        }

    def write(self, geo_path, png_path, image):
        with open(geo_path, "w", encoding="utf-8") as f:
            json.dump(self.to_json(), f, indent="\t")
            f.write("\n")
        image.save(png_path)


def _r(values):
    return [round(float(v), 4) for v in values]


def _shelf_pack(rects, width):
    """Shelf packing with a little gap between rectangles so that texture filtering never bleeds."""
    positions = []
    x = y = shelf_h = 0
    for h, w, *_ in rects:
        if w > width:
            return None
        if x + w > width:
            y += shelf_h
            x = shelf_h = 0
        positions.append((x, y))
        x += w
        shelf_h = max(shelf_h, h)
    return positions, y + shelf_h


# ---------------------------------------------------------------------- geometry of faces (port of GeckoLib)

def _rot_matrix(rotation):
    """GeckoLib applies Z, then Y, then X (pose stack order), with X and Y angles negated, in its flipped-X space.
    Returned matrix works on Bedrock coordinates."""
    if not rotation:
        return np.eye(3)
    rx, ry, rz = (math.radians(a) for a in rotation)
    # In GeckoLib space: angles (-rx, -ry, rz). Converting the whole rotation back to Bedrock space (flip X on both
    # sides) turns that into (rx, ry, -rz)... flip F = diag(-1,1,1): F * R_g * F.
    gx, gy, gz = -rx, -ry, rz
    Rx = np.array([[1, 0, 0], [0, math.cos(gx), -math.sin(gx)], [0, math.sin(gx), math.cos(gx)]])
    Ry = np.array([[math.cos(gy), 0, math.sin(gy)], [0, 1, 0], [-math.sin(gy), 0, math.cos(gy)]])
    Rz = np.array([[math.cos(gz), -math.sin(gz), 0], [math.sin(gz), math.cos(gz), 0], [0, 0, 1]])
    Rg = Rz @ Ry @ Rx
    F = np.diag([-1.0, 1.0, 1.0])
    return F @ Rg @ F


def face_corners(cube, face):
    """The four corners of a face in Bedrock coordinates, in the order of the texture's
    (left-top, right-top, right-bottom, left-bottom) as GeckoLib maps them."""
    ox, oy, oz = cube.origin
    sx, sy, sz = cube.size
    i = cube.inflate
    # GeckoLib space: x' = -(x + size) .. -x
    x0, x1 = -(ox + sx) - i, -ox + i
    y0, y1 = oy - i, oy + sy + i
    z0, z1 = oz - i, oz + sz + i
    BLB, BRB = (x0, y0, z0), (x0, y0, z1)
    TLB, TRB = (x0, y1, z0), (x0, y1, z1)
    TLF, TRF = (x1, y1, z0), (x1, y1, z1)
    BLF, BRF = (x1, y0, z0), (x1, y0, z1)
    quads = {
        "west": (TRB, TLB, BLB, BRB),
        "east": (TLF, TRF, BRF, BLF),
        "north": (TLB, TLF, BLF, BLB),
        "south": (TRF, TRB, BRB, BRF),
        "up": (TRB, TRF, TLF, TLB),
        "down": (BLB, BLF, BRF, BRB),
    }
    v = quads[face]
    # GeoQuad.build without mirroring: vertex0 <- (u+w, v), vertex1 <- (u, v), vertex2 <- (u, v+h), vertex3 <- (u+w, v+h)
    left_top, right_top, left_bottom, right_bottom = v[1], v[0], v[2], v[3]
    corners = [left_top, right_top, right_bottom, left_bottom]
    out = []
    for c in corners:
        out.append(np.array([-c[0], c[1], c[2]]))  # back to Bedrock coordinates
    return out


class Transforms:
    """Rest-pose transforms of bones and cubes, in Bedrock coordinates."""

    def __init__(self, model):
        self.model = model
        self.cache = {}

    def bone_apply(self, name, p):
        b = self.model.bones[name]
        if b.rotation:
            piv = np.array(b.pivot)
            p = _rot_matrix(b.rotation) @ (p - piv) + piv
        if b.parent:
            p = self.bone_apply(b.parent, p)
        return p

    def bone_normal(self, name, n):
        b = self.model.bones[name]
        if b.rotation:
            n = _rot_matrix(b.rotation) @ n
        if b.parent:
            n = self.bone_normal(b.parent, n)
        return n


def face_geometry(model, bone_name, cube, face, transforms=None):
    """Corners (left-top, right-top, right-bottom, left-bottom) and outward normal of a face after cube and bone
    rotations."""
    transforms = transforms or Transforms(model)
    corners = face_corners(cube, face)
    normal = np.array(NORMAL[face])
    if cube.rotation:
        R = _rot_matrix(cube.rotation)
        piv = np.array(cube.pivot or cube.origin)
        corners = [R @ (c - piv) + piv for c in corners]
        normal = R @ normal
    corners = [transforms.bone_apply(bone_name, c) for c in corners]
    normal = transforms.bone_normal(bone_name, normal)
    return corners, normal


# ---------------------------------------------------------------------- voxels

class Voxels:
    """A 1-pixel grid in Bedrock coordinates. Cells hold a material key (or None)."""

    def __init__(self, lo, hi):
        self.lo = np.array(lo, dtype=int)
        self.hi = np.array(hi, dtype=int)
        shape = tuple(self.hi - self.lo)
        self.grid = np.empty(shape, dtype=object)

    def centers(self):
        xs = np.arange(self.lo[0], self.hi[0]) + 0.5
        ys = np.arange(self.lo[1], self.hi[1]) + 0.5
        zs = np.arange(self.lo[2], self.hi[2]) + 0.5
        return np.meshgrid(xs, ys, zs, indexing="ij")

    def shell(self, outer, thickness=1, open_bottom=True):
        """The skin of a solid: cells of `outer` that have air next to them, `thickness` cells deep. With
        `open_bottom` the solid's floor is left out, which leaves a hollow open from below (a helmet, a hood)."""
        inner = outer.copy()
        for _ in range(thickness):
            nxt = inner.copy()
            for axis in (0, 2):
                for shift in (1, -1):
                    rolled = np.roll(inner, shift, axis=axis)
                    edge = [slice(None)] * 3
                    edge[axis] = 0 if shift == 1 else -1
                    rolled[tuple(edge)] = False
                    nxt &= rolled
            above = np.roll(inner, -1, axis=1)
            above[:, -1, :] = False
            nxt &= above
            if not open_bottom:
                below = np.roll(inner, 1, axis=1)
                below[:, 0, :] = False
                nxt &= below
            inner = nxt
        return outer & ~inner

    def fill(self, mask, material):
        self.grid[mask] = material

    def clear(self, mask):
        self.grid[mask] = None

    def filled(self):
        return np.vectorize(lambda m: m is not None, otypes=[bool])(self.grid)

    def to_cubes(self, model, bone, inflate=0.0, occluder=None):
        """Greedy merge into boxes of one material; only faces touching air are kept. `occluder` marks cells that
        are taken by something else that hides faces (the wearer's head, say) without being part of the model."""
        g = self.grid
        nx, ny, nz = g.shape
        solid = self.filled()
        blocked = solid if occluder is None else (solid | occluder)
        lookup = _lookup(solid, self.lo)
        used = np.zeros(g.shape, dtype=bool)
        cubes = []
        for y in range(ny):
            for z in range(nz):
                for x in range(nx):
                    if used[x, y, z] or not solid[x, y, z]:
                        continue
                    mat = g[x, y, z]

                    def ok(xx, yy, zz):
                        return solid[xx, yy, zz] and not used[xx, yy, zz] and g[xx, yy, zz] == mat

                    x2 = x
                    while x2 + 1 < nx and ok(x2 + 1, y, z):
                        x2 += 1
                    z2 = z
                    while z2 + 1 < nz and all(ok(xx, y, z2 + 1) for xx in range(x, x2 + 1)):
                        z2 += 1
                    y2 = y
                    while y2 + 1 < ny and all(ok(xx, y2 + 1, zz) for xx in range(x, x2 + 1) for zz in range(z, z2 + 1)):
                        y2 += 1
                    used[x:x2 + 1, y:y2 + 1, z:z2 + 1] = True
                    faces = []
                    # A face is kept when at least one cell on its other side is air (or outside the grid).
                    rng = ((x, x2), (y, y2), (z, z2))
                    if _exposed(blocked, *rng, 2, -1): faces.append("north")
                    if _exposed(blocked, *rng, 2, +1): faces.append("south")
                    if _exposed(blocked, *rng, 0, -1): faces.append("east")
                    if _exposed(blocked, *rng, 0, +1): faces.append("west")
                    if _exposed(blocked, *rng, 1, +1): faces.append("up")
                    if _exposed(blocked, *rng, 1, -1): faces.append("down")
                    if not faces:
                        continue
                    origin = (self.lo[0] + x, self.lo[1] + y, self.lo[2] + z)
                    size = (x2 - x + 1, y2 - y + 1, z2 - z + 1)
                    cubes.append(model.box(bone, origin, size, mat, inflate=inflate, faces=faces, solid=lookup))
        return cubes


def _exposed(solid, xr, yr, zr, axis, sign):
    ranges = [list(range(xr[0], xr[1] + 1)), list(range(yr[0], yr[1] + 1)), list(range(zr[0], zr[1] + 1))]
    edge = (xr, yr, zr)[axis][1 if sign > 0 else 0] + sign
    if edge < 0 or edge >= solid.shape[axis]:
        return True
    ranges[axis] = [edge]
    sub = solid[np.ix_(*ranges)]
    return not sub.all()


def _lookup(solid, lo):
    lo = np.asarray(lo)

    def inside(p):
        idx = np.floor(np.asarray(p)).astype(int) - lo
        if np.any(idx < 0) or np.any(idx >= solid.shape):
            return False
        return bool(solid[tuple(idx)])

    return inside


def solid_lookup(voxels):
    """A function telling whether a Bedrock point lies inside a filled voxel (for painters that look around)."""
    solid = voxels.filled()

    def inside(p):
        idx = np.floor(np.asarray(p)).astype(int) - voxels.lo
        if np.any(idx < 0) or np.any(idx >= solid.shape):
            return False
        return bool(solid[tuple(idx)])

    return inside


# ---------------------------------------------------------------------- smooth surfaces from plates

def euler_for(M):
    """Bedrock rotation angles (degrees) whose GeckoLib rotation equals the 3x3 matrix M (columns: where the cube's
    local X, Y and Z axes end up). Inverse of _rot_matrix."""
    F = np.diag([-1.0, 1.0, 1.0])
    G = F @ M @ F                     # in GeckoLib space: G = Rz(c) Ry(b) Rx(a)
    b = math.asin(max(-1.0, min(1.0, -G[2, 0])))
    if abs(math.cos(b)) > 1e-6:
        a = math.atan2(G[2, 1], G[2, 2])
        c = math.atan2(G[1, 0], G[0, 0])
    else:                              # gimbal lock: put everything in a
        a = math.atan2(-G[1, 2], G[1, 1])
        c = 0.0
    # GeckoLib uses (-rx, -ry, rz) for (a, b, c)
    return (-math.degrees(a), -math.degrees(b), math.degrees(c))


def plate(model, bone, center, e1, normal, width, height, thickness, material, faces=None, **info):
    """A thin box centred on `center`, spanning `width` along e1 and `height` along e2 = e1 x normal, `thickness`
    along the normal. Returns the cube."""
    n = np.asarray(normal, dtype=float)
    n = n / np.linalg.norm(n)
    a = np.asarray(e1, dtype=float)
    a = a - n * (a @ n)
    a = a / np.linalg.norm(a)
    b = np.cross(a, n)
    M = np.column_stack([a, n, b])
    rot = euler_for(M)
    c = np.asarray(center, dtype=float)
    origin = c - np.array([width / 2, thickness / 2, height / 2])
    info.setdefault("plate", {"center": c, "e1": a, "e2": b, "n": n, "hw": width / 2, "hh": height / 2})
    return model.box(bone, origin, (width, thickness, height), material, pivot=c, rotation=rot,
                     faces=faces or ("north", "south", "east", "west", "up", "down"), **info)


def surface(model, bone, point, nu, nv, thickness, material, closed_u=True, overlap=0.35, rows=None, **info):
    """Covers a parametric surface point(u, v) (u, v in 0..1) with nu x nv plates. The outward side is the one the
    normal (du x dv) points to. Each plate knows its row and column, and whether it lies on the first or last row
    (painters use that for rims)."""
    grid = [[np.asarray(point(i / nu, j / nv), dtype=float) for j in range(nv + 1)] for i in range(nu + 1)]
    cubes = []
    for j in range(nv):
        if rows is not None and j not in rows:
            continue
        for i in range(nu):
            A, B = grid[i][j], grid[i + 1][j]
            D, C = grid[i][j + 1], grid[i + 1][j + 1]
            du = (B - A) + (C - D)
            dv = (D - A) + (C - B)
            n = np.cross(du, dv)
            if np.linalg.norm(n) < 1e-9 or np.linalg.norm(du) < 1e-9:
                continue
            n = n / np.linalg.norm(n)
            center = (A + B + C + D) / 4 - n * thickness / 2   # outer face on the surface
            width = max(np.linalg.norm(B - A), np.linalg.norm(C - D)) + overlap
            height = max(np.linalg.norm(D - A), np.linalg.norm(C - B)) + overlap
            cube = plate(model, bone, center, du, n, width, height, thickness, material,
                         row=j, col=i, first_row=(j == 0), last_row=(j == nv - 1), **info)
            # e2 = e1 x n points along -v or +v; remember which way v grows to find the rows' edges
            cube.info["plate"]["v_sign"] = float(np.sign(cube.info["plate"]["e2"] @ dv)) or 1.0
            cubes.append(cube)
    return cubes


def quad_plate(model, bone, A, B, C, D, thickness, material, overlap=0.25, edge=None, fit="mean", **info):
    """A plate over the quad A-B-C-D (A, B along the top edge, D below A, C below B, as seen from outside).
    The outer face lies on the quad; the plate grows inwards by `thickness`. `edge` names the free edge that
    painters should treat as a rim ('bottom' for the lower edge D-C, 'top' for A-B)."""
    A, B, C, D = (np.asarray(p, dtype=float) for p in (A, B, C, D))
    du = (B - A) + (C - D)
    dv = (D - A) + (C - B)
    n = np.cross(du, dv)
    n = n / np.linalg.norm(n)
    center = (A + B + C + D) / 4 - n * thickness / 2
    # a trapezoid is covered by a rectangle of its mean width: the corners of the wider end are left to the
    # neighbouring plates, which avoids spikes sticking out of the shell
    pick = (lambda a, b: (a + b) / 2) if fit == "mean" else max
    width = pick(np.linalg.norm(B - A), np.linalg.norm(C - D)) + overlap
    height = pick(np.linalg.norm(D - A), np.linalg.norm(C - B)) + overlap
    cube = plate(model, bone, center, du, n, width, height, thickness, material,
                 last_row=edge is not None, **info)
    v_sign = float(np.sign(cube.info["plate"]["e2"] @ dv)) or 1.0
    # the rim test in paint.Texel.rim looks for the edge in the +v direction; for a top edge that is -v
    cube.info["plate"]["v_sign"] = v_sign if edge != "top" else -v_sign
    return cube


def rounded_ring(y, half_x, half_z, chamfer, zc=0.0):
    """Eight points of a rectangle with cut corners at height y, going round from the front-left of the wearer's
    right... in order: front edge (right to left), left side, back edge, right side. Index pairs (0,1) front,
    (1,2) front-left chamfer, (2,3) left, (3,4) back-left chamfer, (4,5) back, (5,6) back-right, (6,7) right,
    (7,0) front-right chamfer."""
    a, b, c = half_x, half_z, chamfer
    return [
        np.array([-(a - c), y, zc - b]), np.array([(a - c), y, zc - b]),
        np.array([a, y, zc - (b - c)]), np.array([a, y, zc + (b - c)]),
        np.array([(a - c), y, zc + b]), np.array([-(a - c), y, zc + b]),
        np.array([-a, y, zc + (b - c)]), np.array([-a, y, zc - (b - c)]),
    ]


def loft(model, bone, top, bottom, thickness, material, edge=None, skip=(), corner_overlap=0.7, **info):
    """Plates between two closed rings of the same length (see rounded_ring). Facet k joins points k and k+1.
    Odd facets are the cut corners: they get a little more overlap to close the seams of their neighbours."""
    cubes = []
    n = len(top)
    for k in range(n):
        if k in skip:
            continue
        k2 = (k + 1) % n
        overlap = corner_overlap if k % 2 == 1 else 0.25
        cubes.append(quad_plate(model, bone, top[k], top[k2], bottom[k2], bottom[k], thickness, material,
                                overlap=overlap, edge=edge, facet=k, **info))
    return cubes
