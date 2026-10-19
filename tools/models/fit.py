"""Checks that pieces of equipment worn together do not cut through each other.

Every cube becomes an oriented box in Bedrock coordinates (after its own and its bones' rotations); two boxes
collide when no separating axis exists between them (the separating axis test). A small tolerance lets faces that
merely touch pass.
"""
import itertools

import numpy as np

from geo import Transforms, face_geometry


class Box:
    def __init__(self, bone, cube, centre, axes, half):
        self.bone = bone
        self.cube = cube
        self.centre = centre
        self.axes = axes  # 3 unit vectors (rows)
        self.half = half  # half extents along the axes

    def __repr__(self):
        c = np.round(self.centre, 2)
        h = np.round(self.half * 2, 2)
        return f"{self.bone}:{self.cube.info.get('part', '?')} at {tuple(c)} size {tuple(h)}"


def boxes(model, bones=None):
    """The model's cubes as oriented boxes; only those of the given bones if any are named."""
    t = Transforms(model)
    out = []
    for bone_name, bone in model.bones.items():
        cubes = bone.cubes
        if bones is not None and bone_name not in bones:
            continue
        for cube in cubes:
            north, _ = face_geometry(model, bone_name, cube, "north", t)
            south, _ = face_geometry(model, bone_name, cube, "south", t)
            pts = np.array(north + south)
            centre = pts.mean(axis=0)
            # north corners: left-top, right-top, right-bottom, left-bottom
            ex = north[1] - north[0]
            ey = north[0] - north[3]
            ez = south[0] - north[1]
            axes, half = [], []
            for e in (ex, ey, ez):
                n = np.linalg.norm(e)
                axes.append(e / n if n > 1e-9 else np.array([1.0, 0.0, 0.0]))
                half.append(n / 2)
            out.append(Box(bone_name, cube, centre, np.array(axes), np.array(half)))
    return out


def collide(a, b, tolerance=0.02):
    """True when the two boxes overlap by more than the tolerance along every axis."""
    axes = list(a.axes) + list(b.axes)
    for u, v in itertools.product(a.axes, b.axes):
        c = np.cross(u, v)
        n = np.linalg.norm(c)
        if n > 1e-6:
            axes.append(c / n)
    d = b.centre - a.centre
    for axis in axes:
        ra = np.sum(a.half * np.abs(a.axes @ axis))
        rb = np.sum(b.half * np.abs(b.axes @ axis))
        if abs(d @ axis) > ra + rb - tolerance:
            return False
    return True


def clashes(model_a, model_b, tolerance=0.02):
    """Pairs of cubes, one from each model, that cut into each other."""
    a_boxes = boxes(model_a)
    b_boxes = boxes(model_b)
    return [(a, b) for a in a_boxes for b in b_boxes if collide(a, b, tolerance)]


# ---------------------------------------------------------------------- visible piercing

def _inside(points, boxes_, margin):
    """For each point, the index of a box that strictly contains it, or -1."""
    which = np.full(len(points), -1)
    for i, b in enumerate(boxes_):
        local = (points - b.centre) @ b.axes.T
        hit = np.all(np.abs(local) < b.half - margin, axis=1) & (which < 0)
        which[hit] = i
    return which


def _surface_points(box, spacing):
    """A grid of points on each face of the box, with the index pairs of grid neighbours."""
    pts, pairs = [], []
    for axis in range(3):
        u, v = [a for a in range(3) if a != axis]
        nu = max(2, int(np.ceil(2 * box.half[u] / spacing)) + 1)
        nv = max(2, int(np.ceil(2 * box.half[v] / spacing)) + 1)
        su = np.linspace(-box.half[u], box.half[u], nu)
        sv = np.linspace(-box.half[v], box.half[v], nv)
        for sign in (-1, 1):
            base = len(pts)
            for i, a in enumerate(su):
                for j, b in enumerate(sv):
                    local = np.zeros(3)
                    local[axis] = sign * box.half[axis]
                    local[u] = a
                    local[v] = b
                    pts.append(box.centre + local @ box.axes)
                    k = base + i * nv + j
                    if i > 0:
                        pairs.append((k - nv, k))
                    if j > 0:
                        pairs.append((k - 1, k))
    return np.array(pts), pairs


# The wearer's head: a ray that runs into it from a helmet's face starts in the gap under the helmet.
HEAD = Box("head", None, np.array([0.0, 28.0, 0.0]), np.eye(3), np.array([4.0, 4.0, 4.0]))


def _exterior(point, normal, occluders, reach=2.0, step=0.1):
    """True when a ray from a point on a face, along its normal, gets away without entering another piece of the
    helmet or the head: the face is part of the helmet's outside."""
    for k in range(1, int(reach / step) + 1):
        p = (point + normal * (k * step + 0.03))[None, :]
        if _inside(p, occluders, 0.0)[0] >= 0:
            return False
    return True


def piercings(outer, inner, spacing=0.12, margin=0.02):
    """Where a cube of `inner` (worn under, like a mask) comes out through the outside of `outer` (worn over, like
    a helmet). Crossings through a face that looks down are fine: going up under a brim is how a mask sits beneath
    a helmet. So are crossings of faces inside the helmet (seams, the gap over the head): only a face that a ray
    can leave without running into the helmet or the head counts. Cubes tagged strap=True on both sides may
    cross: a strap passes under a strap. Returns (inner box, outer box, point, outer face normal) per crossing."""
    outer_boxes = boxes(outer)
    occluders = outer_boxes + [HEAD]
    found = []
    for box in boxes(inner):
        pts, pairs = _surface_points(box, spacing)
        inside = _inside(pts, outer_boxes, margin)
        near = _inside(pts, outer_boxes, -margin)
        for a, b in pairs:
            if inside[a] >= 0 and near[b] < 0:
                k_in, p_out = a, pts[b]
            elif inside[b] >= 0 and near[a] < 0:
                k_in, p_out = b, pts[a]
            else:
                continue
            ob = outer_boxes[inside[k_in]]
            if box.cube.info.get("strap") and ob.cube.info.get("strap"):
                continue
            local = (p_out - ob.centre) @ ob.axes.T
            axis = int(np.argmax(np.abs(local) / ob.half))
            normal = np.sign(local[axis]) * ob.axes[axis]
            if normal[1] < -0.7:
                continue
            # the point where the segment leaves the box, on its face
            p_in = pts[k_in]
            l_in = (p_in - ob.centre) @ ob.axes.T
            t = (ob.half[axis] * np.sign(local[axis]) - l_in[axis]) / (local[axis] - l_in[axis])
            crossing = p_in + (p_out - p_in) * np.clip(t, 0.0, 1.0)
            if not _exterior(crossing, normal, occluders):
                continue
            found.append((box, ob, crossing, normal))
    return found
