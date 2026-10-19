"""Clean low-poly parts: few pieces that meet edge to edge, so nothing pokes out of the surface.

* chamfered_prism: an upright block with cut corners. Three boxes side by side (no overlap) and four corner
  pieces turned 45 degrees about the vertical axis, which sit wholly inside the cut and are set back by a hair
  at top and bottom so that they never fight with the faces next to them.
* hinge_plate: a flat plate hanging from an edge, turned about that edge only (a skirt, a visor, a brim). Its
  top edge is exactly the given edge, so it joins the part above without a gap or an overhang.
"""
import math

import numpy as np

from geo import plate, quad_plate

EPS = 0.02


def chamfered_prism(m, bone, center, a, b, c, y0, y1, material, top=True, bottom=True, **info):
    # pass rim=True in info to have painters treat the outline of every face as a rim (a brim's rolled edge)
    """Half-widths a (x) and b (z) around center = (x, z), corners cut by c, from y0 to y1."""
    cx, cz = center
    h = y1 - y0
    faces_mid = [f for f in ("north", "south", "up", "down") if (f != "up" or top) and (f != "down" or bottom)]
    # centre: full depth, between the cut corners in x
    m.box(bone, (cx - (a - c), y0, cz - b), (2 * (a - c), h, 2 * b), material, faces=tuple(faces_mid), **info)
    side_faces = [f for f in ("north", "south", "up", "down") if (f != "up" or top) and (f != "down" or bottom)]
    # the two sides: full width in x beyond the centre box, shorter in z
    m.box(bone, (cx + (a - c), y0, cz - (b - c)), (c, h, 2 * (b - c)), material,
          faces=tuple(side_faces + ["west"]), **info)
    m.box(bone, (cx - a, y0, cz - (b - c)), (c, h, 2 * (b - c)), material,
          faces=tuple(side_faces + ["east"]), **info)
    # corners: the outer face lies on the cut, the rest is inside
    depth = c / math.sqrt(2)
    for sx in (-1, 1):
        for sz in (-1, 1):
            p1 = np.array([cx + sx * a, 0.0, cz + sz * (b - c)])
            p2 = np.array([cx + sx * (a - c), 0.0, cz + sz * b])
            mid = (p1 + p2) / 2
            n = np.array([sx, 0.0, sz]) / math.sqrt(2)
            e1 = p2 - p1
            centre = mid - n * depth / 2
            centre[1] = (y0 + y1) / 2
            faces = ["up", "down", "north", "south", "east", "west"]
            plate(m, bone, centre, e1, n, c * math.sqrt(2), h - 2 * EPS, depth, material, faces=tuple(faces), **info)


def hinge_plate(m, bone, p, q, outward, drop, out, thickness, material, edge="bottom", **info):
    """A plate hanging from the edge p-q: its lower edge is the top edge moved `drop` down and `out` outwards
    (outward is the horizontal direction the plate leans to). The outer face is on the plate's outside."""
    p, q = np.asarray(p, float), np.asarray(q, float)
    o = np.asarray(outward, float)
    o = o / np.linalg.norm(o)
    shift = np.array([0.0, -drop, 0.0]) + o * out
    A, B = p, q
    D, C = p + shift, q + shift
    # make sure A-B-C-D runs so that the normal points outwards
    n = np.cross((B - A) + (C - D), (D - A) + (C - B))
    if n @ (o + np.array([0, 0.0, 0])) < 0 and n[1] * -1 <= 0:
        A, B, C, D = B, A, D, C
    n = np.cross((B - A) + (C - D), (D - A) + (C - B))
    if n @ o < 0:
        A, B, C, D = B, A, D, C
    return quad_plate(m, bone, A, B, C, D, thickness, material, overlap=0.0, edge=edge, fit="mean", **info)


def _unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def oriented_box(m, bone, center, ex, ey, sx, sy, sz, material, faces=None, **info):
    """A box of size (sx, sy, sz) along the axes ex, ey and ez = ex x ey, centred on `center`."""
    ex, ey = _unit(ex), _unit(ey)
    return plate(m, bone, center, ex, ey, sx, sz, sy, material, faces=faces, **info)


def chamfered_block(m, bone, center, u, v, a, b, c, depth, material, back=True, front=True, **info):
    """A block with cut corners in any orientation: half-widths a along u and b along v, corners cut by c,
    `depth` along w = u x v (centred). front/back say whether the faces at +w/2 and -w/2 are drawn."""
    u, v = _unit(u), _unit(v)
    w = np.cross(u, v)
    centre = np.asarray(center, float)
    # plate(): e1 = width axis, normal = thickness axis, e2 = e1 x normal is the height axis.
    # We want the depth along w drawn as "height" so that front/back are the e2 ends: use e1 = u, normal = v
    # gives e2 = u x v = w. Sizes: width (u), height (w), thickness (v).
    def piece(cu, cv, half_u, half_v):
        p = centre + u * cu + v * cv
        faces = ["east", "west", "up", "down"]
        if front:
            faces.append("south")
        if back:
            faces.append("north")
        return plate(m, bone, p, u, v, 2 * half_u, depth, 2 * half_v, material, faces=tuple(faces), **info)

    piece(0.0, 0.0, a - c, b)
    piece(a - c / 2, 0.0, c / 2, b - c)
    piece(-(a - c / 2), 0.0, c / 2, b - c)
    d = c / math.sqrt(2)
    for su in (-1, 1):
        for sv in (-1, 1):
            p1 = centre + u * su * a + v * sv * (b - c)
            p2 = centre + u * su * (a - c) + v * sv * b
            n = _unit(u * su + v * sv)
            mid = (p1 + p2) / 2 - n * d / 2
            e1 = p2 - p1
            faces = ["up", "down", "east", "west"]
            if front:
                faces.append("south")
            if back:
                faces.append("north")
            # width along the cut, thickness inwards (n), height along w
            plate(m, bone, mid, e1, n, c * math.sqrt(2), depth - 2 * EPS, d, material, faces=tuple(faces), **info)


def octagon(m, bone, center, axis, radius, length, material, up=(0, 1, 0), **info):
    """A short octagonal cylinder (a lens rim, a filter drum) along `axis`."""
    w = _unit(axis)
    upv = np.asarray(up, float)
    if abs(float(upv @ w)) > 0.95:
        upv = np.array([1.0, 0.0, 0.0])
    u = _unit(np.cross(upv, w))
    v = np.cross(w, u)
    c = radius * (2 - math.sqrt(2))
    chamfered_block(m, bone, center, u, v, radius, radius, c, length, material, **info)
