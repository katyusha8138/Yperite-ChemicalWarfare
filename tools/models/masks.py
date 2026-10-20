"""Gas masks: the mod's own mask and four First World War patterns.

All share the same layout on the head (front is -Z, the face is the plane z = -4): a face piece in front of the
face, cheek pieces back to the ears, eyepieces, a mouth part and a head harness. The filter, where there is one,
sits in its own bone named "filter" so that the game can hide it while no filter is loaded.
"""
import numpy as np

from armor import armor_model
from kit import chamfered_block, octagon, oriented_box
from paint import Glass, Steel, hexramp

RUBBER = hexramp("16181a", "1f2224", "292d2e", "343a39", "414845", "515953", "656d64")
OD_PAINT = hexramp("252a1e", "323927", "414933", "51593f", "626c4b", "77805b", "8f9770")
KHAKI_FABRIC = hexramp("332d20", "433b2a", "554b35", "675c42", "7a6e50", "8f8260", "a49674")
FRENCH_CLOTH = hexramp("3b3f3d", "4b504d", "5d625e", "6f7570", "838983", "989d96", "aeb2aa")
LEATHER_DARK = hexramp("1a130e", "251b14", "32241a", "402e22", "50392a", "634735", "7a5842")
GERMAN_GREY = hexramp("252b27", "323a34", "414a42", "515b50", "636d60", "788271", "8f9886")
AUSTRIAN_FABRIC = hexramp("2b2b25", "393931", "48483e", "59594d", "6b6b5d", "807f6f", "989683")
BRASS = hexramp("4a3a18", "6a5524", "8c7234", "ad8f47", "c9aa62", "e0c584")
NICKEL = hexramp("3c3f42", "54585c", "6e7378", "8a8f94", "a8adb1", "c8ccce")
GLASS = hexramp("0d1a1c", "15282b", "1f3a3e", "2c5258", "8fb8bc")
WEBBING = hexramp("4a4532", "5a543e", "6c654b", "7e7759", "918a69")
TAPE = hexramp("7d776a", "8f897b", "a29c8d", "b5af9f")

FWD = np.array([0.0, 0.0, -1.0])
X = np.array([1.0, 0.0, 0.0])
Y = np.array([0.0, 1.0, 0.0])


# How every mask leaves room for a helmet worn over it (fit.py checks this against each helmet): nothing in front
# of the face rises above the eyepieces, which sit just under the visors, and above BROW the face piece narrows and
# thins into a brow band that goes up under the brim into the crown. Straps are thin and close to the head, so that
# they pass under the helmets' skirts and chin straps.
BROW = 28.3
BROW_TOP = 29.9
BROW_DEPTH = 0.6
EYE_Y = 27.25
EYE_R = 1.0
STRAP = 0.3


def _face_piece(m, material, top=BROW_TOP, bottom=23.8, half_width=4.4, chamfer=1.5, depth=0.9, cheek_back=-1.8):
    # the full-width part, from the chin up to the brow
    chamfered_block(m, "armorHead", (0, (BROW + bottom) / 2, -4.0 - depth / 2), X, Y, half_width, (BROW - bottom) / 2,
                    chamfer, depth, material)
    # the brow band: as wide as the top edge below it; its lower corners are buried in the part below
    brow_half = half_width - chamfer
    low = BROW - 0.8
    hh = (top - low) / 2
    chamfered_block(m, "armorHead", (0, low + hh, -4.0 - BROW_DEPTH / 2), X, Y, brow_half, hh, 0.8, BROW_DEPTH, material)
    # cheek pieces back towards the ears (above the top of a tunic's collar)
    y0, y1 = max(bottom + 0.6, 24.7), BROW - 0.1
    for side in (-1, 1):
        oriented_box(m, "armorHead", (side * (4.0 + STRAP / 2 + 0.03), (y0 + y1) / 2, (-4.0 + cheek_back) / 2), X, Y,
                     STRAP + 0.06, y1 - y0, -4.0 - cheek_back + 0.0001, material)


def _eyepieces(m, rim, glass, y=EYE_Y, dx=1.95, radius=EYE_R, z=-4.9, length=0.35):
    for side in (-1, 1):
        octagon(m, "armorHead", (side * dx, y, z - length / 2), FWD, radius, length, rim)
        octagon(m, "armorHead", (side * dx, y, z - length), FWD, radius * 0.68, 0.06, glass)


def _harness(m, strap, ys=(29.5, 26.4), pad=None):
    """Straps from the face piece round the back of the head; the side straps reach the back corners and the back
    straps fit between them, so that nothing overlaps. A pad, if any, sits on the back straps where they cross."""
    h = STRAP
    for y in ys:
        for side in (-1, 1):
            oriented_box(m, "armorHead", (side * (4.0 + h / 2), y, (-1.8 + 4.0 + h) / 2), X, Y, h, 0.55, 5.8 + h, strap,
                         strap=True)
        oriented_box(m, "armorHead", (0, y, 4.0 + h / 2), X, Y, 8.0, 0.55, h, strap, strap=True)
    if pad is not None:
        oriented_box(m, "armorHead", (0, 28.0, 4.3), X, Y, 3.0, 3.6, 0.6, pad)


def _filter_canister(m, start, direction, radius, length, paint, ring=None):
    """The filter in its own bone, so that it can be hidden while none is loaded."""
    m.bone("filter", parent="armorHead", pivot=tuple(start))
    d = np.asarray(direction, float)
    d = d / np.linalg.norm(d)
    centre = np.asarray(start, float) + d * (length / 2)
    octagon(m, "filter", centre, d, radius, length, paint)
    if ring is not None:
        octagon(m, "filter", np.asarray(start, float) + d * 0.35, d, radius * 1.08, 0.5, ring)


def gas_mask():
    """The mod's own mask: black rubber face piece, nickel-rimmed eyepieces, a threaded mouthpiece and an olive
    drum filter screwed on below the chin, rubber harness."""
    m = armor_model("gas_mask")
    rubber = Steel(RUBBER, seed=51, sheen=0.9, wear=0.0)
    rim = Steel(NICKEL, seed=52, sheen=1.0, wear=0.0)
    _face_piece(m, rubber)
    oriented_box(m, "armorHead", (0, 23.85, -3.6), X, Y, 7.0, 0.5, 2.4, rubber)
    _eyepieces(m, rim, Glass(GLASS))
    octagon(m, "armorHead", (0, 25.2, -5.15), FWD, 1.0, 0.6, rim)
    _filter_canister(m, (0, 25.0, -5.4), (0, -0.42, -1), 1.6, 2.7, Steel(OD_PAINT, seed=53, sheen=1.0, wear=0.03),
                     ring=rim)
    _harness(m, rubber, pad=rubber)
    m.pack()
    return m


def small_box_respirator():
    """British Small Box Respirator: a khaki rubberised face piece with brass-rimmed eyepieces, a corrugated hose
    from the mouthpiece down to the filter box carried in a haversack on the chest."""
    m = armor_model("small_box_respirator")
    fabric = Steel(KHAKI_FABRIC, seed=61, sheen=0.25, wear=0.0)
    brass = Steel(BRASS, seed=62, sheen=1.0, wear=0.0)
    hose = Steel(RUBBER, seed=63, sheen=0.8, wear=0.0)
    _face_piece(m, fabric, chamfer=1.8)
    _eyepieces(m, brass, Glass(GLASS))
    octagon(m, "armorHead", (0, 25.3, -5.1), FWD, 0.75, 0.5, brass)
    # the hose: down from the mouth and forward (moves with the head), then on into the bag (moves with the bag,
    # which stands further out the more is worn under it; the two ends meet over a tunic or body armour)
    for k in range(3):
        octagon(m, "armorHead", (0, 24.6 - k * 0.75, -5.3 - k * 0.15), (0, -1, -0.25), 0.62, 0.7, hose)
    # the haversack with the box inside, on the chest, and its sling up to the neck. They hang in front of whatever
    # is worn on the chest: the game moves the bone layer_front out over a tunic or body armour.
    m.bone("layer_front", parent="armorBody", pivot=(0, 24, 0))
    bag = Steel(KHAKI_FABRIC, seed=64, sheen=0.1, wear=0.0)
    oriented_box(m, "layer_front", (0, 17.4, -2.95), X, Y, 6.6, 6.0, 1.8, bag)
    oriented_box(m, "layer_front", (0, 19.6, -3.9), X, Y, 6.8, 2.2, 0.25, bag)       # the flap
    sling = Steel(WEBBING, seed=65, sheen=0.0)
    for side in (-1, 1):
        oriented_box(m, "layer_front", (side * 2.0, 18.6, -4.05), X, Y, 0.6, 0.6, 0.15, brass)
        oriented_box(m, "layer_front", (side * 2.6, 22.0, -2.175), X, Y, 0.8, 3.2, 0.25, sling, strap=True)
    m.bone("filter", parent="layer_front", pivot=(0, 20.5, -3.0))
    for k in range(2):
        octagon(m, "filter", (0, 21.95 - k * 0.75, -3.75 + k * 0.2), (0, -1, 0.25), 0.62, 0.75, hose)
    _harness(m, Steel(WEBBING, seed=66, sheen=0.0), ys=(29.4, 26.6))
    m.pack()
    return m


def m2_mask():
    """French M2: an impregnated cloth mask in one piece, a rounded snout over nose and mouth, two eye windows,
    tied on with tapes. No filter to change."""
    m = armor_model("m2_mask")
    cloth = Steel(FRENCH_CLOTH, seed=71, sheen=0.0, wear=0.0)
    _face_piece(m, cloth, bottom=23.4, chamfer=2.0, depth=0.8)
    # the snout: two steps forward over nose and mouth
    # (below the eye windows: its top stays under EYE_Y - EYE_R)
    chamfered_block(m, "armorHead", (0, 24.8, -5.2), X, Y, 2.4, 1.4, 0.9, 0.8, cloth)
    chamfered_block(m, "armorHead", (0, 24.6, -5.9), X, Y, 1.7, 1.0, 0.6, 0.6, cloth)
    _eyepieces(m, Steel(FRENCH_CLOTH, seed=72, sheen=0.0, wear=0.0), Glass(GLASS), z=-4.8, length=0.3)
    _harness(m, Steel(TAPE, seed=73, sheen=0.0), ys=(29.0,))
    m.pack()
    return m


def lederschutzmaske():
    """German leather mask (1917): dark oiled leather face piece, steel-rimmed eyepieces and the filter drum
    screwed straight onto the front of the mouth, elastic straps."""
    m = armor_model("lederschutzmaske")
    leather = Steel(LEATHER_DARK, seed=81, sheen=0.7, wear=0.0)
    steel = Steel(GERMAN_GREY, seed=82, sheen=1.0, wear=0.04)
    _face_piece(m, leather, chamfer=1.7)
    _eyepieces(m, Steel(NICKEL, seed=83, sheen=1.0, wear=0.0), Glass(GLASS), radius=1.05)
    octagon(m, "armorHead", (0, 25.0, -5.05), FWD, 1.1, 0.4, steel)
    _filter_canister(m, (0, 24.9, -5.3), (0, -0.15, -1), 1.55, 2.0, steel, ring=Steel(GERMAN_GREY, seed=84, sheen=1.0))
    _harness(m, Steel(hexramp("3a3a36", "4a4a45", "5c5c56", "6e6e67"), seed=85, sheen=0.0), ys=(29.5, 26.5))
    m.pack()
    return m


def austrian_gas_mask():
    """Austro-Hungarian mask: rubberised grey-brown fabric, round eyepieces, a long filter drum at the mouth."""
    m = armor_model("austrian_gas_mask")
    fabric = Steel(AUSTRIAN_FABRIC, seed=91, sheen=0.5, wear=0.0)
    metal = Steel(GERMAN_GREY, seed=92, sheen=1.0, wear=0.04)
    _face_piece(m, fabric, chamfer=1.6)
    _eyepieces(m, Steel(BRASS, seed=93, sheen=1.0, wear=0.0), Glass(GLASS))
    octagon(m, "armorHead", (0, 25.2, -5.05), FWD, 1.0, 0.4, metal)
    _filter_canister(m, (0, 25.2, -5.25), (0, -0.3, -1), 1.5, 2.8, metal, ring=Steel(BRASS, seed=94, sheen=1.0))
    _harness(m, Steel(hexramp("3d3a30", "4d493d", "5f5a4b", "716b5a"), seed=95, sheen=0.0), ys=(29.5, 26.5))
    m.pack()
    return m


TIN = hexramp("23272a", "343a3e", "4a5257", "636c71", "7f888d", "a0a8ac")
GREY_RUBBER = hexramp("1d201e", "2a2e2b", "393e3a", "4a504b", "5d645e", "747b74", "8d948c")


def zelinsky_kummant_mask():
    """Russian Zelinsky-Kummant mask: a helmet of grey rubber drawn over the whole head, two small glass
    eyepieces in thin tin rims, and a flat tin box of charcoal hanging straight from the mouth below the chin.
    The rubber over the head lies close enough to go under any helmet or the protective hood."""
    m = armor_model("zelinsky_kummant_mask")
    rubber = Steel(GREY_RUBBER, seed=101, sheen=0.7, wear=0.0)
    tin = Steel(TIN, seed=102, sheen=1.0, wear=0.05)
    _face_piece(m, rubber, chamfer=1.6)
    _eyepieces(m, Steel(TIN, seed=103, sheen=1.0, wear=0.0), Glass(GLASS), radius=0.9)
    # the rubber helmet: over the top, down the sides and the back, and over the forehead beside and above the brow
    # band. Its front corners are cut at 45 degrees, just outside the corners of the head and inside those of
    # every helmet's crown (fit.py).
    h = STRAP
    c = 0.5                                          # the cut, from the corner of the shell's outline
    y0, y1 = 24.1, 31.85                            # the top stays under every helmet's upper crown tiers
    for sx in (-1, 1):
        x0, x1 = (4.0, 4.0 + h) if sx > 0 else (-4.0 - h, -4.0)
        _box(m, (x0, y0, -4.0 - h + c), (x1, y1, 4.0), rubber)
        _box(m, (sx * 2.9 if sx > 0 else -4.0 - h + c, BROW, -4.0 - h),
             (4.0 + h - c if sx > 0 else -2.9, BROW_TOP, -4.0), rubber)
        corner = np.array([sx * (4.0 + h - c / 2), (y0 + y1 + h) / 2, -4.0 - h + c / 2])
        u = np.array([sx * 1.0, 0.0, 1.0]) / np.sqrt(2)
        inward = np.array([-sx * 1.0, 0.0, 1.0]) / np.sqrt(2)
        oriented_box(m, "armorHead", corner + inward * (h / 2), u, Y, c * np.sqrt(2) + 0.1, y1 + h - y0, h, rubber)
    _box(m, (-4.0 - h, y0, 4.0), (4.0 + h, y1, 4.0 + h), rubber)
    _box(m, (-4.0 - h, y1, -4.0 - h + c), (4.0 + h, y1 + h, 4.0 + h), rubber)
    _box(m, (-4.0 - h + c, y1, -4.0 - h), (4.0 + h - c, y1 + h, -4.0 - h + c), rubber)
    _box(m, (-4.0 - h + c, BROW_TOP, -4.0 - h), (4.0 + h - c, y1, -4.0), rubber)
    # the rubber over the face behind the face piece, showing at its cut corners (rather than the face)
    _box(m, (-4.0, y0, -4.2), (4.0, BROW - 0.1, -4.0), rubber)
    # the mouthpiece and the charcoal box below it
    octagon(m, "armorHead", (0, 25.0, -5.05), FWD, 0.9, 0.4, tin)
    m.bone("filter", parent="armorHead", pivot=(0, 24.0, -5.6))
    chamfered_block(m, "filter", (0, 23.0, -5.85), X, Y, 1.8, 1.7, 0.5, 1.5, tin)
    oriented_box(m, "filter", (0, 24.85, -5.55), X, Y, 1.0, 0.5, 0.9, tin)
    m.pack()
    return m


def _box(m, lo, hi, material, **info):
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    return m.box("armorHead", tuple(lo), tuple(hi - lo), material, **info)
