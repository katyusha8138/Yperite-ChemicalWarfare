"""Caps and other headgear that is not a steel helmet: each army's peaked cap, the Pickelhaube (bare and in its
field cover) and the Austro-Hungarian shako.

They are worn in the head slot like a helmet, over a gas mask in the Curios face slot (fit.py checks every mask
under every one): the band sits on the head above the mask's brow and wide enough to go over a mask's straps and
the rubber helmet of the Zelinsky mask, and the crown closes over the top of the head.
"""
import numpy as np

from armor import armor_model
from helmets import _chin_strap, _inside_shell
from kit import chamfered_prism, hinge_plate, octagon
from paint import Features, Leather, Metal, Steel, Wool, hexramp
from uniforms import AUSTRIAN_FELDGRAU, BRITISH_KHAKI, FRENCH_HORIZON, GERMAN_FELDGRAU, RUSSIAN_KHAKI

BRASS = hexramp("4a3a18", "6a5524", "8c7234", "ad8f47", "c9aa62", "e0c584")
GOLD = hexramp("5a4410", "7e6018", "a37f26", "c79f3a", "e0bc55", "f2d885")
SILVER = hexramp("4c4f52", "6b6f73", "8c9095", "aeb2b6", "cfd2d5", "eceef0")
PATENT = hexramp("0b0b0c", "141416", "1e1f22", "2a2c30", "3a3d42", "555a60")
BROWN_LEATHER = hexramp("2a180d", "3b2414", "4f321d", "644128", "7a5235", "936644")
BLACK_FELT = hexramp("101012", "18181b", "212125", "2b2b30", "37373d", "45454c")
SCARLET = hexramp("3e0c0c", "5a1212", "781a19", "962423", "b0302d", "c84340")
WHITE = hexramp("8f8f8a", "a9a9a3", "c3c3bc", "d9d9d2", "ebebe5")
BLACK = hexramp("0e0e0f", "17171a", "212124", "2c2c30")
ORANGE = hexramp("6a2d08", "8e3d0c", "b05212", "cc6a1c", "e0852c")
YELLOW = hexramp("6a5410", "8e7218", "b09124", "ccad36", "e0c54c")

# The band of every cap: on the head above a mask's brow, outside its straps and rubber (fit.py).
BAND_Y = 29.4
BAND = (4.7, 4.85, 0.8)       # half-widths in x and z, corner cut


def _prism(m, size, y0, y1, material, zc=0.0, top=True, bottom=True, **info):
    a, b, c = size
    chamfered_prism(m, "armorHead", (0.0, zc), a, b, c, y0, y1, material, top=top, bottom=bottom, **info)


def _peak(m, size, y, drop, out, material, zc=0.0, t=0.3, back=False):
    """The peak in front (or a neck guard behind): a plate hanging forward from the bottom edge of the band."""
    a, b, c = size
    sz = 1 if back else -1
    p = (-(a - c), y, zc + sz * b)
    q = ((a - c), y, zc + sz * b)
    hinge_plate(m, "armorHead", p, q, (0, 0, sz), drop, out, t, material)


def _cockade(m, center, rings):
    """A round cockade facing forward: rings = [(radius, material)] from the outside in, each standing a little
    proud of the one under it."""
    x, y, z = center
    for k, (radius, material) in enumerate(rings):
        octagon(m, "armorHead", (x, y, z - 0.1 - 0.06 * k), (0, 0, -1), radius, 0.2, material)


def _piping(m, size, y, material, zc=0.0, h=0.25, proud=0.06):
    """A thin coloured cord round an edge of the cap (the top of the crown, the edges of the band)."""
    a, b, c = size
    _prism(m, (a + proud, b + proud, c), y, y + h, material, zc=zc, top=False)


def _chin_cord(m, size, y, strap, buttons, zc=0.0):
    """The chin strap of a peaked cap, worn up across the front of the band, with a small button at each end."""
    a, b, c = size
    half = a - c - 0.15
    m.box("armorHead", (-half, y, zc - b - 0.12), (2 * half, 0.4, 0.12), strap)
    for sx in (-1, 1):
        octagon(m, "armorHead", (sx * (half + 0.05), y + 0.2, zc - b - 0.12), (0, 0, -1), 0.3, 0.2, buttons)


def _cloth(ramp, seed):
    return Wool(ramp, seed=seed, noise=0.8)


def _peaked_cap(name, crown, band, peak, *, crown_tiers, band_top=30.8, peak_shape=(0.8, 2.3)):
    """A peaked cap: the band round the head, a crown flaring out over it to a flat top, a peak in front."""
    m = armor_model(name)
    _prism(m, BAND, BAND_Y, band_top, band)
    y = band_top
    for size, y1 in crown_tiers:
        _prism(m, size, y, y1, crown)
        y = y1
    _peak(m, BAND, BAND_Y, *peak_shape, peak)
    return m


def british_cap():
    """British officer's Service Dress cap: khaki crown and band, a brown leather peak and chin strap with two
    small brass buttons, the regimental badge in brass on the front."""
    cloth = _cloth(BRITISH_KHAKI, 401)
    m = _peaked_cap("british_cap", cloth, cloth, Steel(BROWN_LEATHER, seed=402, sheen=0.8, wear=0.0),
                    crown_tiers=[((4.95, 5.1, 1.4), 32.1), ((5.2, 5.35, 1.6), 32.8)])
    _chin_cord(m, BAND, 29.65, Leather(BROWN_LEATHER, seed=403), Metal(BRASS))
    brass = Metal(BRASS, seed=404)
    m.box("armorHead", (-0.6, 30.1, -BAND[1] - 0.22), (1.2, 1.5, 0.22), brass)
    m.box("armorHead", (-0.35, 31.6, -BAND[1] - 0.18), (0.7, 0.6, 0.18), brass)
    m.pack()
    return m


def french_kepi():
    """French kepi in horizon blue (1915): a low crown sloping back to a small flat top, a flat black peak and a
    black chin strap on the front with a brass button at each end."""
    m = armor_model("french_kepi")
    cloth = _cloth(FRENCH_HORIZON, 411)
    top = Wool(FRENCH_HORIZON, seed=412, noise=0.8, shift=0.3)
    # the front stays upright; the back slopes in towards the top
    _prism(m, BAND, BAND_Y, 30.7, cloth)
    _prism(m, (4.6, 4.6, 0.8), 30.7, 32.5, cloth, zc=-0.25)
    _prism(m, (4.35, 4.25, 0.8), 32.5, 34.3, top, zc=-0.6)
    _peak(m, BAND, BAND_Y, 0.45, 2.0, Steel(PATENT, seed=413, sheen=1.0, wear=0.0))
    _chin_cord(m, BAND, 29.6, Leather(PATENT, seed=414), Metal(BRASS))
    m.pack()
    return m


def german_cap():
    """German officer's peaked cap (Schirmmütze) in field grey: a wide crown with red piping round its top, a red
    band, a glossy black peak, the black-white-red cockade of the Reich on the crown and the state's cockade on
    the band."""
    cloth = _cloth(GERMAN_FELDGRAU, 421)
    red = Wool(SCARLET, seed=422, noise=0.5)
    m = _peaked_cap("german_cap", cloth, red, Steel(PATENT, seed=423, sheen=1.0, wear=0.0),
                    crown_tiers=[((5.0, 5.15, 1.4), 32.2), ((5.4, 5.55, 1.7), 32.9)])
    _piping(m, (5.4, 5.55, 1.7), 32.65, red)
    _piping(m, BAND, 30.6, red, h=0.2, proud=0.04)
    _cockade(m, (0, 31.6, -5.15), [(0.65, Wool(SCARLET, seed=424, noise=0.3)), (0.45, Wool(WHITE, seed=425, noise=0.3)),
                                   (0.25, Wool(BLACK, seed=426, noise=0.3))])
    _cockade(m, (0, 30.1, -BAND[1]), [(0.55, Wool(BLACK, seed=427, noise=0.3)), (0.3, Wool(WHITE, seed=428, noise=0.3))])
    m.pack()
    return m


def austrian_cap():
    """Austro-Hungarian officer's cap (Kappe) in field grey: a soft upright crown with a flat top, the folding
    neck flap buttoned in front with two small buttons, a short cloth peak, and the round gilt badge with the
    emperor's cypher at the top of the front."""
    m = armor_model("austrian_cap")
    cloth = _cloth(AUSTRIAN_FELDGRAU, 431)
    flap = Wool(AUSTRIAN_FELDGRAU, seed=432, noise=0.8, shift=-0.35)
    _prism(m, (4.78, 4.93, 0.8), BAND_Y, 31.0, flap)
    _prism(m, (4.6, 4.75, 0.9), 31.0, 33.3, cloth)
    _peak(m, (4.78, 4.93, 0.8), BAND_Y, 0.55, 1.7, Wool(AUSTRIAN_FELDGRAU, seed=433, noise=0.6, shift=-0.2))
    gilt = Metal(GOLD, seed=434)
    for sx in (-1, 1):
        octagon(m, "armorHead", (sx * 0.65, 30.45, -4.93 - 0.08), (0, 0, -1), 0.3, 0.16, gilt)
    _cockade(m, (0, 32.35, -4.75), [(0.65, gilt), (0.35, Metal(YELLOW, seed=435))])
    m.pack()
    return m


def russian_cap():
    """Russian furazhka in khaki: a broad flat crown over a band piped in red, a black peak and chin strap, and
    the oval cockade (orange, black and white) on the band."""
    cloth = _cloth(RUSSIAN_KHAKI, 441)
    band = Wool(RUSSIAN_KHAKI, seed=442, noise=0.8, shift=-0.5)
    m = _peaked_cap("russian_cap", cloth, band, Steel(PATENT, seed=443, sheen=1.0, wear=0.0),
                    crown_tiers=[((4.95, 5.1, 1.4), 32.1), ((5.3, 5.45, 1.7), 32.75)])
    red = Wool(SCARLET, seed=444, noise=0.5)
    _piping(m, (5.3, 5.45, 1.7), 32.5, red)
    _piping(m, BAND, 30.6, red, h=0.2, proud=0.04)
    _chin_cord(m, BAND, 29.6, Leather(PATENT, seed=445), Metal(BRASS))
    _cockade(m, (0, 30.35, -BAND[1]), [(0.5, Metal(ORANGE, seed=446)), (0.33, Wool(BLACK, seed=447, noise=0.3)),
                                       (0.17, Metal(SILVER, seed=448))])
    m.pack()
    return m


# ---------------------------------------------------------------------- Pickelhaube

# the dome: (half-widths, corner cut) from the brim up, front and back peaks hang from the lowest tier
PICKEL_TIERS = [((4.85, 5.2, 1.6), BAND_Y, 32.25), ((4.45, 4.75, 1.5), 32.25, 33.15), ((3.7, 3.9, 1.3), 33.15, 33.8),
                ((2.5, 2.6, 0.9), 33.8, 34.15)]
PICKEL_ZC = 0.25


def _pickel_shell(m, shell, peaks):
    for k, (size, y0, y1) in enumerate(PICKEL_TIERS):
        _prism(m, size, y0, y1, shell, zc=PICKEL_ZC, bottom=(k == 0))
    size = PICKEL_TIERS[0][0]
    _peak(m, size, BAND_Y, 0.85, 2.1, peaks, zc=PICKEL_ZC)
    _peak(m, size, BAND_Y, 1.7, 1.5, peaks, zc=PICKEL_ZC, back=True)


def _spike(m, metal, zc=PICKEL_ZC):
    """The spike on its base, with the four arms of the cross-shaped plate lying over the top of the dome."""
    top = PICKEL_TIERS[-1][2]
    up = (0, 1, 0)
    octagon(m, "armorHead", (0, top + 0.15, zc), up, 1.25, 0.3, metal, up=(0, 0, 1))
    for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        m.box("armorHead", (-0.4 + sx * 1.6 if sx else -0.4, top - 0.55, zc - 0.4 + sz * 1.6 if sz else zc - 0.4),
              (0.8, 0.6, 0.8), metal)
    octagon(m, "armorHead", (0, top + 0.6, zc), up, 0.6, 0.6, metal, up=(0, 0, 1))
    octagon(m, "armorHead", (0, top + 1.45, zc), up, 0.42, 1.1, metal, up=(0, 0, 1))
    octagon(m, "armorHead", (0, top + 2.4, zc), up, 0.28, 0.8, metal, up=(0, 0, 1))
    octagon(m, "armorHead", (0, top + 3.05, zc), up, 0.14, 0.5, metal, up=(0, 0, 1))


def pickelhaube():
    """Prussian Pickelhaube M1895: a shell of black lacquered leather with a front peak and a deeper neck guard,
    brass trim on the peak, the brass spike on its cross-shaped base, the brass eagle on the front and brass
    chin scales."""
    m = armor_model("pickelhaube")
    leather = Steel(PATENT, seed=451, sheen=0.9, wear=0.0, inside=_inside_shell(PICKEL_ZC))
    _pickel_shell(m, leather, leather)
    brass = Metal(BRASS, seed=452)
    _spike(m, brass)
    # brass trim along the edge of the front peak
    size = PICKEL_TIERS[0][0]
    front = PICKEL_ZC - size[1]
    # the eagle: the body and tail low on the front, the wings spread wide above it, the crowned head on the tier
    # above
    m.box("armorHead", (-1.0, 29.75, front - 0.25), (2.0, 1.35, 0.25), brass)
    m.box("armorHead", (-1.85, 30.7, front - 0.2), (3.7, 1.45, 0.2), brass)
    m.box("armorHead", (-0.45, 29.55, front - 0.18), (0.9, 0.3, 0.18), brass)
    upper = PICKEL_ZC - PICKEL_TIERS[1][0][1]
    m.box("armorHead", (-0.55, 32.25, upper - 0.2), (1.1, 0.75, 0.2), brass)
    _chin_strap(m, Metal(BRASS, seed=453), Metal(BRASS, seed=454), top=29.6)
    m.pack()
    return m


# A 3 x 5 pixel font for the regiment's number on the cover.
DIGITS = {
    "2": ["###", "..#", "###", "#..", "###"],
    "5": ["###", "#..", "###", "..#", "###"],
}


def _number(text, colour, x0, y1, density=2):
    """Paints `text` in `colour` on the front face of a patch whose left edge (as one looks at it) is at x = x0
    and top at y = y1, drawn at `density` texels per pixel."""
    glyphs = [DIGITS[c] for c in text]
    width = 4 * len(glyphs) - 1

    def f(t):
        if t.face != "north":
            return None
        cols = int(round(t.w))
        i = int(np.floor((t.pos[0] - x0) * density)) - (cols - width) // 2
        j = int(np.floor((y1 - t.pos[1]) * density)) - (t.h - 5) // 2
        if not (0 <= j < 5 and 0 <= i < width) or i % 4 == 3:
            return None
        return colour if glyphs[i // 4][j][i % 4] == "#" else None

    return f


def pickelhaube_covered():
    """The Pickelhaube in its field-grey cloth cover (Überzug), as worn at the front in 1914-1915: the cover goes
    over the shell and both peaks, the spike stands out of it, the regiment's number is sewn on the front in red."""
    m = armor_model("pickelhaube_covered")
    cover = Wool(GERMAN_FELDGRAU, seed=461, noise=0.8)
    _pickel_shell(m, cover, Wool(GERMAN_FELDGRAU, seed=462, noise=0.6, shift=-0.2))
    _spike(m, Steel(GERMAN_FELDGRAU, seed=463, sheen=0.6, wear=0.15))
    size = PICKEL_TIERS[0][0]
    front = PICKEL_ZC - size[1]
    red = (150, 34, 32, 255)
    m.box("armorHead", (-1.75, 29.55, front - 0.05), (3.5, 2.45, 0.05),
          Features(Wool(GERMAN_FELDGRAU, seed=461, noise=0.8), _number("52", red, -1.75, 32.0)), density=2)
    _chin_strap(m, Leather(BROWN_LEATHER, seed=464), Metal(BRASS, seed=465), top=29.6)
    m.pack()
    return m


def austrian_shako():
    """Austro-Hungarian shako (Tschako): a tall body of black felt narrowing a little to the top, a black leather
    top and peak, a gold braid round the top, the brass double-headed eagle on the front, and the black and gold
    cockade with its gold loop above it."""
    m = armor_model("austrian_shako")
    felt = Wool(BLACK_FELT, seed=471, noise=0.7)
    leather = Steel(PATENT, seed=472, sheen=0.9, wear=0.0)
    _prism(m, (4.75, 4.9, 0.9), BAND_Y, 31.4, felt)
    _prism(m, (4.65, 4.8, 0.9), 31.4, 33.1, felt)
    _prism(m, (4.55, 4.7, 0.9), 33.1, 34.4, felt, top=False)
    _prism(m, (4.6, 4.75, 0.9), 34.4, 34.65, leather)
    gold = Metal(GOLD, seed=473)
    _piping(m, (4.55, 4.7, 0.9), 33.85, gold, h=0.45, proud=0.07)
    _piping(m, (4.75, 4.9, 0.9), BAND_Y, leather, h=0.35, proud=0.06)
    _peak(m, (4.75, 4.9, 0.9), BAND_Y, 0.8, 2.0, leather)
    brass = Metal(BRASS, seed=474)
    front = -4.9
    m.box("armorHead", (-0.8, 29.85, front - 0.22), (1.6, 1.3, 0.22), brass)         # the body and the shield
    m.box("armorHead", (-1.75, 30.55, front - 0.18), (3.5, 1.2, 0.18), brass)        # the wings
    for sx in (-1, 1):
        m.box("armorHead", (sx * 0.6 - 0.3, 31.4, -4.8 - 0.18), (0.6, 0.6, 0.18), brass)    # the two heads
    m.box("armorHead", (-0.35, 32.0, -4.8 - 0.16), (0.7, 0.4, 0.16), brass)                 # the crown
    m.box("armorHead", (-0.25, 32.45, -4.8 - 0.12), (0.5, 0.75, 0.12), gold)                # the loop
    _cockade(m, (0, 33.45, -4.7), [(0.6, Wool(BLACK, seed=475, noise=0.3)), (0.35, Metal(YELLOW, seed=476))])
    m.pack()
    return m
