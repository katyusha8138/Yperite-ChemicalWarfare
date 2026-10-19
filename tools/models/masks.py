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


def _face_piece(m, material, top=30.6, bottom=23.8, half_width=4.4, chamfer=1.5, depth=0.9, cheek_back=-1.8):
    cy = (top + bottom) / 2
    chamfered_block(m, "armorHead", (0, cy, -4.0 - depth / 2), X, Y, half_width, (top - bottom) / 2, chamfer, depth, material)
    for side in (-1, 1):
        oriented_box(m, "armorHead", (side * (4.0 + 0.25), cy - 0.2, (-4.0 + cheek_back) / 2), X, Y,
                     0.5, top - bottom - 1.2, -4.0 - cheek_back + 0.0001, material)


def _eyepieces(m, rim, glass, y=28.1, dx=1.95, radius=1.3, z=-4.9):
    for side in (-1, 1):
        octagon(m, "armorHead", (side * dx, y, z - 0.25), FWD, radius, 0.5, rim)
        octagon(m, "armorHead", (side * dx, y, z - 0.5), FWD, radius * 0.68, 0.06, glass)


def _harness(m, strap, ys=(29.6, 26.4), pad=None):
    """Straps from the face piece round the back of the head; the side straps reach the back corners and the back
    straps fit between them, so that nothing overlaps. A pad, if any, sits on the back straps where they cross."""
    for y in ys:
        for side in (-1, 1):
            oriented_box(m, "armorHead", (side * 4.18, y, 1.28), X, Y, 0.36, 0.55, 6.16, strap)
        oriented_box(m, "armorHead", (0, y, 4.18), X, Y, 8.0, 0.55, 0.36, strap)
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
    octagon(m, "armorHead", (0, 25.4, -5.15), FWD, 1.0, 0.6, rim)
    _filter_canister(m, (0, 25.2, -5.4), (0, -0.42, -1), 1.6, 2.7, Steel(OD_PAINT, seed=53, sheen=1.0, wear=0.03),
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
    _eyepieces(m, brass, Glass(GLASS), radius=1.2)
    octagon(m, "armorHead", (0, 25.3, -5.1), FWD, 0.75, 0.5, brass)
    # the hose: down from the mouth (moves with the head), then on into the bag (moves with the body)
    for k in range(3):
        octagon(m, "armorHead", (0, 24.6 - k * 0.75, -5.3 + k * 0.1), (0, -1, -0.15), 0.62, 0.7, hose)
    m.bone("filter", parent="armorBody", pivot=(0, 20.5, -2.0))
    for k in range(3):
        octagon(m, "filter", (0, 22.3 - k * 0.75, -4.7 + k * 0.35), (0, -1, 0.4), 0.62, 0.7, hose)
    # the haversack with the box inside, on the chest
    bag = Steel(KHAKI_FABRIC, seed=64, sheen=0.1, wear=0.0)
    oriented_box(m, "armorBody", (0, 17.4, -2.9), X, Y, 6.6, 6.0, 1.8, bag)
    oriented_box(m, "armorBody", (0, 19.6, -3.85), X, Y, 6.8, 2.2, 0.25, bag)       # the flap
    for side in (-1, 1):
        oriented_box(m, "armorBody", (side * 2.0, 18.6, -4.0), X, Y, 0.6, 0.6, 0.15, brass)
        oriented_box(m, "armorBody", (side * 2.6, 22.3, -1.0), X, Y, 0.8, 3.6, 2.4, Steel(WEBBING, seed=65, sheen=0.0))
    _harness(m, Steel(WEBBING, seed=66, sheen=0.0), ys=(29.4, 26.6))
    m.pack()
    return m


def m2_mask():
    """French M2: an impregnated cloth mask in one piece, a rounded snout over nose and mouth, two eye windows,
    tied on with tapes. No filter to change."""
    m = armor_model("m2_mask")
    cloth = Steel(FRENCH_CLOTH, seed=71, sheen=0.0, wear=0.0)
    _face_piece(m, cloth, top=30.8, bottom=23.4, chamfer=2.0, depth=0.8)
    # the snout: two steps forward over nose and mouth
    chamfered_block(m, "armorHead", (0, 25.6, -5.25), X, Y, 2.9, 2.1, 1.0, 0.9, cloth)
    chamfered_block(m, "armorHead", (0, 25.2, -6.05), X, Y, 2.1, 1.5, 0.75, 0.7, cloth)
    _eyepieces(m, Steel(FRENCH_CLOTH, seed=72, sheen=0.0, wear=0.0), Glass(GLASS), y=28.3, radius=1.25, z=-4.8)
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
    _eyepieces(m, Steel(NICKEL, seed=83, sheen=1.0, wear=0.0), Glass(GLASS), radius=1.35)
    octagon(m, "armorHead", (0, 25.3, -5.05), FWD, 1.15, 0.4, steel)
    _filter_canister(m, (0, 25.3, -5.25), (0, -0.15, -1), 1.75, 2.0, steel, ring=Steel(GERMAN_GREY, seed=84, sheen=1.0))
    _harness(m, Steel(hexramp("3a3a36", "4a4a45", "5c5c56", "6e6e67"), seed=85, sheen=0.0), ys=(29.5, 26.5))
    m.pack()
    return m


def austrian_gas_mask():
    """Austro-Hungarian mask: rubberised grey-brown fabric, round eyepieces, a long filter drum at the mouth."""
    m = armor_model("austrian_gas_mask")
    fabric = Steel(AUSTRIAN_FABRIC, seed=91, sheen=0.5, wear=0.0)
    metal = Steel(GERMAN_GREY, seed=92, sheen=1.0, wear=0.04)
    _face_piece(m, fabric, chamfer=1.6)
    _eyepieces(m, Steel(BRASS, seed=93, sheen=1.0, wear=0.0), Glass(GLASS), radius=1.25)
    octagon(m, "armorHead", (0, 25.2, -5.05), FWD, 1.0, 0.4, metal)
    _filter_canister(m, (0, 25.2, -5.25), (0, -0.3, -1), 1.5, 2.8, metal, ring=Steel(BRASS, seed=94, sheen=1.0))
    _harness(m, Steel(hexramp("3d3a30", "4d493d", "5f5a4b", "716b5a"), seed=95, sheen=0.0), ys=(29.5, 26.5))
    m.pack()
    return m
