import math
"""First World War steel helmets."""
import numpy as np

from armor import HEAD, armor_model, box_mask, smoothstep, superellipse
from geo import Voxels, loft, plate, quad_plate, rounded_ring, surface
from kit import chamfered_prism, hinge_plate
from paint import Features, Leather, Metal, Steel, hexramp

FELDGRAU_HELMET = hexramp("232a27", "2f3731", "3c453b", "4b5546", "5d6753", "727b62", "8c9273")
LEATHER_BROWN = hexramp("3a2416", "52331f", "6b4429", "855634")
BRASS = hexramp("5a4a1e", "8a7330", "b39a48", "d9c27a")
NICKEL = hexramp("4a4c4f", "76797d", "a3a7ab", "cfd3d6")


def _inside_shell(axis_z):
    """Faces turned towards the helmet's vertical axis, or the underside of the crown, are the inside."""

    def inside(p, n):
        r = np.array([p[0], 0.0, p[2] - axis_z])
        norm = np.linalg.norm(r)
        if norm > 1e-6 and float(n @ r) / norm < -0.35:
            return True
        return n[1] < -0.5 and p[1] > 31.0

    return inside


def _chin_strap(m, strap, buckle, top=28.5):
    # down both cheeks from under the shell, and across under the chin
    h = top - 23.5
    m.box("armorHead", (-4.5, 23.5, -1.5), (0.5, h, 1.0), strap)
    m.box("armorHead", (4.0, 23.5, -1.5), (0.5, h, 1.0), strap)
    m.box("armorHead", (-4.0, 23.5, -1.5), (8.0, 0.5, 1.0), strap, faces=("north", "south", "down"))
    m.box("armorHead", (-4.75, 25.0, -1.75), (0.75, 1.0, 1.5), buckle)


def _profile(Y, steps, below):
    """Half-width of a helmet at each height: `steps` are (y, width) points of the crown, linear between them;
    under the lowest point the width follows `below(y)`."""
    ys = np.array([p[0] for p in steps])
    ws = np.array([p[1] for p in steps])
    crown = np.interp(Y, ys, ws, left=np.nan, right=0.0)
    return np.where(Y < ys[0], below(np.minimum(Y, ys[0])), crown)


def _superellipse_dir(phi, p):
    """Direction on a rounded square: phi = 0 is the front (-Z), growing towards the wearer's left (+X)."""
    sn, cs = math.sin(phi), math.cos(phi)
    return (math.copysign(abs(sn) ** (2.0 / p), sn), -math.copysign(abs(cs) ** (2.0 / p), cs))


def _from_front(phi):
    """Angle from the front, 0..pi."""
    phi = phi % (2 * math.pi)
    return min(phi, 2 * math.pi - phi)


def _ss(e0, e1, x):
    t = min(1.0, max(0.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


# The crown of a helmet: (t, y, scale) from the top (t = 0) down to the band where the skirt starts (t = 1).
# Flat and wide enough near the top to hide the corners of the square head.
CROWN = [(0.0, 34.0, 0.0), (0.12, 33.9, 0.40), (0.26, 33.6, 0.63), (0.42, 33.05, 0.80), (0.58, 32.3, 0.91),
         (0.74, 31.3, 0.97), (0.88, 30.1, 1.0), (1.0, 29.0, 1.0)]


def _crown(t):
    for (t0, y0, s0), (t1, y1, s1) in zip(CROWN, CROWN[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0)
            return y0 + (y1 - y0) * f, s0 + (s1 - s0) * f
    return CROWN[-1][1], CROWN[-1][2]


def _skirt(m, bone, band, depth, flare, thickness, material, zc):
    """The skirt under a crown band: facet k hangs down to depth[k]; every corner moves outwards by the mean
    flare of the two facets that share it, so neighbouring facets stay joined."""
    n = len(band)
    corner_flare = [(flare[(k - 1) % n] + flare[k]) / 2 for k in range(n)]
    for k in range(n):
        k2 = (k + 1) % n

        def low(i, y):
            p = band[i]
            d = np.array([p[0], 0.0, p[2] - zc])
            d = d / np.linalg.norm(d)
            return np.array([p[0], y, p[2]]) + d * corner_flare[i]

        overlap = 0.7 if k % 2 == 1 else 0.25
        quad_plate(m, bone, band[k], band[k2], low(k2, depth[k]), low(k, depth[k]), thickness, material,
                   overlap=overlap, edge="bottom", facet=k)


def _crown_tiers(m, tiers, material, zc):
    """Stacked chamfered prisms: (a, b, c, y0, y1) from the bottom up."""
    for k, (a, b, c, y0, y1) in enumerate(tiers):
        chamfered_prism(m, "armorHead", (0.0, zc), a, b, c, y0, y1, material, bottom=(k == 0))


def _skirt_plates(m, a, b, c, y, zc, parts, t, material):
    """Plates hanging from the bottom edges of a crown tier. parts maps a side ('front', 'back', 'left', 'right')
    to (drop, out); corners are left open, which keeps every plate's edges clean."""
    edges = {
        "front": ((-(a - c), y, zc - b), ((a - c), y, zc - b), (0, 0, -1)),
        "back": (((a - c), y, zc + b), (-(a - c), y, zc + b), (0, 0, 1)),
        "left": ((a, y, zc - (b - c)), (a, y, zc + (b - c)), (1, 0, 0)),
        "right": ((-a, y, zc + (b - c)), (-a, y, zc - (b - c)), (-1, 0, 0)),
    }
    for side, (drop, out) in parts.items():
        p, q, o = edges[side]
        hinge_plate(m, "armorHead", p, q, o, drop, out, t, material)


def _corner_plates(m, a, b, c, y, zc, corners, t, material):
    """Plates hanging from the cut corners: corners maps ('front'|'back', 'left'|'right') to (drop, out)."""
    for (fb, lr), (drop, out) in corners.items():
        sz = -1 if fb == "front" else 1
        sx = 1 if lr == "left" else -1
        p = (sx * a, y, zc + sz * (b - c))
        q = (sx * (a - c), y, zc + sz * b)
        hinge_plate(m, "armorHead", p, q, (sx, 0, sz), drop, out, t, material)


def stahlhelm_m1916():
    """German M1916, simplified: a crown of three cut-corner tiers, a short visor in front, deep side pieces over
    the ears and a flared neck guard behind, each hanging from an edge of the crown. Two lugs and a chin strap."""
    m = armor_model("stahlhelm_m1916")
    zc = 0.4
    t = 0.6
    steel = Steel(FELDGRAU_HELMET, seed=16, inside=_inside_shell(zc), wear=0.02)
    a, b, c = 5.0, 5.4, 1.7
    _crown_tiers(m, [(a, b, c, 29.0, 32.6), (4.2, 4.6, 1.5, 32.6, 33.5), (3.0, 3.4, 1.1, 33.5, 34.1)], steel, zc)
    _skirt_plates(m, a, b, c, 29.0, zc, {
        "front": (0.5, 1.5),           # the visor: short and almost level
        "left": (4.0, 0.7), "right": (4.0, 0.7),
        "back": (4.5, 1.2),            # the neck guard
    }, t, steel)
    _corner_plates(m, a, b, c, 29.0, zc, {("back", "left"): (4.2, 0.95), ("back", "right"): (4.2, 0.95)}, t, steel)

    lug = Steel(FELDGRAU_HELMET, seed=17, wear=0.3)
    for side in (-1, 1):
        m.box("armorHead", (side * 5.55 - 0.5, 29.6, -1.0), (1.0, 1.2, 1.2), lug)
    _chin_strap(m, Leather(LEATHER_BROWN), Metal(NICKEL))
    m.pack()
    return m

BRODIE_KHAKI = hexramp("2a2d1f", "3a3d28", "4c4f31", "5f613b", "737447", "8a8955", "a3a06a")
HORIZON_BLUE = hexramp("2b3440", "3a4653", "4b5867", "5e6c7b", "73828f", "8b99a3", "a8b3b6")
AUSTRIAN_GREY = hexramp("29261f", "38342a", "484335", "5a5442", "6e6751", "857c62", "a09678")
BRONZE = hexramp("4a3a1c", "6e5628", "93743a", "b8944f")


def _crown(m, rings, top_box, material, t):
    """A crown made of a flat top box and plates between successive rings."""
    (x, z, half_x, half_z, y) = top_box
    m.box("armorHead", (x - half_x, y - t, z - half_z), (2 * half_x, t, 2 * half_z), material)
    for upper, lower in zip(rings, rings[1:]):
        loft(m, "armorHead", upper, lower, t, material)


def brodie_mk1():
    """British Brodie Mk I, simplified: a shallow two-tier bowl on a wide flat brim with cut corners."""
    m = armor_model("brodie_mk1")
    zc = 0.2
    paint = Steel(BRODIE_KHAKI, seed=26, inside=_inside_shell(zc), wear=0.02)
    rim = Steel(BRODIE_KHAKI, seed=27, inside=_inside_shell(zc), wear=0.06)
    _crown_tiers(m, [(5.0, 5.25, 1.7, 30.9, 33.0), (4.1, 4.35, 1.5, 33.0, 33.9), (2.8, 3.05, 1.0, 33.9, 34.4)], paint, zc)
    chamfered_prism(m, "armorHead", (0.0, zc), 7.3, 7.6, 2.8, 30.4, 30.9, rim, rim=True)
    _chin_strap(m, Leather(LEATHER_BROWN), Metal(BRASS), top=30.6)
    m.pack()
    return m


def adrian_m15():
    """French Adrian M15, simplified: a rounded three-tier bowl, a crest along the top, a brim with a long peak in
    front and a shorter one behind, and the bronze grenade badge."""
    m = armor_model("adrian_m15")
    zc = 0.3
    t = 0.5
    paint = Steel(HORIZON_BLUE, seed=36, inside=_inside_shell(zc), wear=0.02)
    a, b, c = 5.0, 5.35, 1.8
    _crown_tiers(m, [(a, b, c, 29.6, 32.2), (4.3, 4.65, 1.6, 32.2, 33.2), (3.1, 3.45, 1.2, 33.2, 33.8)], paint, zc)
    # brim: a thin ring slab, then the two peaks hanging from it
    chamfered_prism(m, "armorHead", (0.0, zc), 5.8, 6.15, 2.1, 29.1, 29.6, paint)
    _skirt_plates(m, 5.8, 6.15, 2.1, 29.1, zc, {"front": (0.7, 1.6), "back": (0.9, 1.2)}, t, paint)
    crest = Steel(HORIZON_BLUE, seed=37, wear=0.06)
    m.box("armorHead", (-0.4, 33.8, zc - 3.0), (0.8, 1.0, 6.0), crest)
    for sign in (-1, 1):
        cz = zc + sign * 3.75
        m.box("armorHead", (-0.4, 33.0, cz - 0.75), (0.8, 1.0, 1.5), crest)
    badge = Metal(BRONZE)
    m.box("armorHead", (-0.9, 30.1, zc - b - 0.3), (1.8, 1.8, 0.3), badge)
    m.box("armorHead", (-0.4, 31.9, zc - b - 0.25), (0.8, 0.8, 0.25), badge)
    _chin_strap(m, Leather(LEATHER_BROWN), Metal(BRASS), top=29.5)
    m.pack()
    return m


def berndorfer():
    """Austro-Hungarian Berndorfer, simplified like the M1916 but rounder and with an all-round bell-shaped skirt
    and large lugs, in a browner field grey."""
    m = armor_model("berndorfer")
    zc = 0.4
    t = 0.6
    paint = Steel(AUSTRIAN_GREY, seed=46, inside=_inside_shell(zc), wear=0.025)
    a, b, c = 5.05, 5.4, 1.9
    _crown_tiers(m, [(a, b, c, 29.2, 32.4), (4.3, 4.65, 1.7, 32.4, 33.3), (3.1, 3.45, 1.25, 33.3, 33.9)], paint, zc)
    _skirt_plates(m, a, b, c, 29.2, zc, {
        "front": (0.9, 1.0), "left": (3.8, 0.9), "right": (3.8, 0.9), "back": (4.2, 1.0)}, t, paint)
    _corner_plates(m, a, b, c, 29.2, zc, {("back", "left"): (4.0, 0.95), ("back", "right"): (4.0, 0.95)}, t, paint)
    lug = Steel(AUSTRIAN_GREY, seed=47, wear=0.3)
    for side in (-1, 1):
        m.box("armorHead", (side * 5.7 - 0.65, 29.8, -1.1), (1.3, 1.5, 1.5), lug)
    _chin_strap(m, Leather(LEATHER_BROWN), Metal(NICKEL))
    m.pack()
    return m
