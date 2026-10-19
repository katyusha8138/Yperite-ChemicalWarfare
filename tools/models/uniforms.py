"""First World War field uniforms of four armies: tunic, trousers, and puttees with ankle boots.

The pieces are layered so that they can all be worn together, and under the body armour and a gas mask's
haversack (fit.py checks this): from the skin outwards the trousers lie 0.15-0.25 off the legs, the puttees 0.45
and the boots 0.5; the tunic lies 0.5 off the body and arms, its pockets, buttons and belt reach 0.85 at the front.
Anything worn over the tunic moves out by STAND_OFF (see the bones named layer_front and layer_back).

Coordinates are Bedrock's: the front is -Z and the wearer's right is -X. One unit is one texel.
"""
import numpy as np

from armor import LEFT_ARM, LEFT_LEG, RIGHT_ARM, RIGHT_LEG, armor_model
from kit import chamfered_block
from paint import Features, Steel, Wool, hexramp

# How far the tunic stands off the chest and the back: what goes over it moves out by this much.
STAND_OFF = {"front": 1.0, "back": 0.8}

BRITISH_KHAKI = hexramp("2a2517", "3a3321", "4c442d", "5f5639", "736946", "897d56", "a09468")
FRENCH_HORIZON = hexramp("2c3541", "3b4655", "4c5869", "5f6c7e", "748294", "8c99aa", "a7b2bf")
GERMAN_FELDGRAU = hexramp("252a23", "333a30", "434a3e", "555c4e", "697060", "7e8574", "969c8a")
GERMAN_STEINGRAU = hexramp("29292a", "373839", "474849", "595a5b", "6d6e6f", "838485", "9b9c9c")
GERMAN_COLLAR = hexramp("18211d", "212c27", "2b3832", "36463e", "42554b", "506457")
AUSTRIAN_FELDGRAU = hexramp("2a2921", "39372d", "4a473a", "5c5949", "706c5a", "86816d", "9e9884")
WEB_KHAKI = hexramp("4a432c", "5a5236", "6b6242", "7d734f", "8f855e", "a2986f")
BOOT_BROWN = hexramp("1d130c", "2a1c12", "392719", "4a3321", "5d412a", "735235", "8c6644")
BLACK_LEATHER = hexramp("101011", "19191b", "232426", "2f3033", "3d3f42", "505357")
SOLE = hexramp("120d09", "1c140e", "271c14")
BRASS = hexramp("4a3a18", "6a5524", "8c7234", "ad8f47", "c9aa62", "e0c584")
STEEL_GREY = hexramp("2a2c2e", "3c3f42", "53575b", "6c7176", "878c91", "a4a9ad")
FRENCH_PATCH = hexramp("1c2238", "263052", "33406a", "c9a94a")
AUSTRIAN_PATCH = hexramp("3d1212", "5a1a1a", "7a2525", "9a3333")
PIPING_RED = (150, 36, 32, 255)


def _box(m, bone, lo, hi, material, **info):
    lo = np.asarray(lo, float)
    hi = np.asarray(hi, float)
    return m.box(bone, tuple(lo), tuple(hi - lo), material, **info)


def _around(lo, hi, off, y0=None, y1=None):
    """The corners of a body part grown by `off` on every side (and cut to y0..y1 if given)."""
    lo = np.asarray(lo, float) - off
    hi = np.asarray(hi, float) + off
    if y0 is not None:
        lo[1] = y0
    if y1 is not None:
        hi[1] = y1
    return lo, hi


def _cloth(ramp, seams=(), flat=False, folds=None, shift=0.0):
    return Wool(ramp, seams=seams, flat=flat, stripes=folds, shift=shift)


def _folds(*creases):
    """Creases in the cloth: each is (y, xs, height) - short darker strokes at height y, at the x positions
    given (None: along the whole width, broken every third texel), on the faces that stand upright."""
    def f(t):
        n = t.normal
        if abs(n[1]) > 0.5:
            return 0.0
        for y, xs, h in creases:
            if abs(t.pos[1] - y) > h / 2:
                continue
            if xs is None:
                if t.i % 3 != 1:
                    return -0.8
                continue
            across = t.pos[0] if abs(n[2]) > 0.5 else t.pos[2]
            if any(abs(across - x) < 0.5 for x in xs):
                return -0.8
        return 0.0
    return f


# ---------------------------------------------------------------------------------------------- tunics

def _flap(m, cx, top, width, height, material):
    """A pocket flap standing 0.35 off the tunic, hanging down from `top`."""
    _box(m, "armorBody", (cx - width / 2, top - height, -2.85), (cx + width / 2, top, -2.5), material)


def tunic(name, ramp, *, collar=None, buttons=None, belt, buckle, breast_pockets=False, collar_patch=None,
          flap_buttons=True):
    """A single-breasted field tunic down to the hips, with a stand collar, lower pockets (and breast pockets),
    shoulder straps, cuffs and a belt. `buttons` is the material of a visible front row, None for a fly front."""
    m = armor_model(name)
    # a fly front shows as one sewn line down the middle; the back has its centre seam either way
    lines = [(0, 0.0, (0, 0, 1))] + ([] if buttons is not None else [(0, 0.0, (0, 0, -1))])
    # the cloth gathers in short folds above the belt; the sleeves crease at the elbow
    cloth = _cloth(ramp, seams=lines, folds=_folds((13.9, (-3.4, -1.4, 1.6, 3.6), 0.8)))
    plain = _cloth(ramp)
    sleeve = _cloth(ramp, folds=_folds((17.6, None, 0.5)))
    pocket_mat = _cloth(ramp, flat=True, shift=-0.4)
    flap_mat = _cloth(ramp, flat=True, shift=0.35)
    collar_mat = _cloth(collar) if collar is not None else plain

    # body down to the belt, the skirt below it, the belt
    _box(m, "armorBody", (-4.5, 12.8, -2.5), (4.5, 24.5, 2.5), cloth)
    _box(m, "armorBody", (-4.7, 11.0, -2.7), (4.7, 12.9, 2.7), plain)
    _box(m, "armorBody", (-4.8, 12.3, -2.8), (4.8, 13.5, 2.8), belt, rim=True)
    _box(m, "armorBody", (-0.8, 12.15, -3.0), (0.8, 13.65, -2.8), buckle, rim=True)
    # the collar shows as a band under the chin; the rest of it is inside the head
    _box(m, "armorBody", (-3.3, 23.4, -2.85), (3.3, 24.7, 2.85), collar_mat)
    if collar_patch is not None:
        for side in (-1, 1):
            _box(m, "armorBody", (side * 2.6 - 0.55, 23.45, -2.95), (side * 2.6 + 0.55, 24.0, -2.85), collar_patch)

    if buttons is not None:
        for y in (22.3, 20.2, 18.1, 16.0, 13.9):
            _box(m, "armorBody", (-0.3, y, -2.72), (0.3, y + 0.6, -2.5), buttons)

    def pocket(cx, y0, y1, width, flap_h):
        _box(m, "armorBody", (cx - width / 2, y0, -2.72), (cx + width / 2, y1, -2.5), pocket_mat)
        _flap(m, cx, y1 + 0.5, width + 0.2, flap_h, flap_mat)
        if flap_buttons:
            b = buttons or buckle
            _box(m, "armorBody", (cx - 0.25, y1 - 0.3, -2.98), (cx + 0.25, y1 + 0.2, -2.85), b)

    for side in (-1, 1):
        pocket(side * 2.55, 13.7, 16.5, 2.9, 1.1)
        if breast_pockets:
            pocket(side * 2.3, 18.0, 20.6, 2.4, 1.2)

    # sleeves with turned-back cuffs, and shoulder straps on top
    for bone, (lo, hi), sx in (("armorRightArm", RIGHT_ARM, -1), ("armorLeftArm", LEFT_ARM, 1)):
        lo, hi = np.asarray(lo, float), np.asarray(hi, float)
        a, b = _around(lo, hi, 0.45, 13.0, 24.4)
        _box(m, bone, a, b, sleeve)
        a, b = _around(lo, hi, 0.6, 13.0, 14.4)
        _box(m, bone, a, b, plain, rim=True)
        x_in, x_out = (lo[0] + 0.6, hi[0] - 0.4) if sx > 0 else (lo[0] + 0.4, hi[0] - 0.6)
        _box(m, bone, (x_in, 24.4, -0.6), (x_out, 24.6, 0.6), collar_mat if collar is not None else plain)
    m.pack()
    return m


# ---------------------------------------------------------------------------------------------- trousers

def trousers(name, ramp, piping=False):
    """Straight trousers, narrower below the knee where the puttees go over them, with a waistband under the
    tunic. German trousers carry a red piping down the outer seam."""
    m = armor_model(name)
    cloth = _cloth(ramp, folds=_folds((8.6, None, 0.5)))
    red = Features(cloth, _piping) if piping else cloth
    for bone, (lo, hi) in (("armorRightLeg", RIGHT_LEG), ("armorLeftLeg", LEFT_LEG)):
        a, b = _around(lo, hi, 0.25, 6.5, 12.25)
        _box(m, bone, a, b, red)
        a, b = _around(lo, hi, 0.15, 1.6, 6.5)
        _box(m, bone, a, b, red)
    _box(m, "armorBody", (-4.3, 11.6, -2.3), (4.3, 13.0, 2.3), cloth)
    m.pack()
    return m


def _piping(t):
    """A red line down the middle of the outer side of each leg."""
    n = t.normal
    if abs(n[0]) < 0.5 or abs(t.pos[2]) > 0.25:
        return None
    outer = (n[0] < 0 and t.pos[0] < -1.9) or (n[0] > 0 and t.pos[0] > 1.9)
    return PIPING_RED if outer else None


# ---------------------------------------------------------------------------------------------- puttees and boots

def _spiral(t):
    """The turns of a puttee: a dark diagonal line where one turn lies over the next, a lighter one below it."""
    n = t.normal
    if abs(n[1]) > 0.5:
        return 0.0
    along = t.pos[0] if abs(n[2]) > 0.5 else t.pos[2]
    phase = (t.pos[1] + 0.25 * along * (1 if (n[2] > 0.5 or n[0] < -0.5) else -1)) % 1.5
    return -0.9 if phase < 0.5 else 0.0


def puttees(name, ramp, boot_ramp, tape_ramp=None):
    """Puttees wound from the ankle to below the knee and tied at the top, over laced ankle boots."""
    m = armor_model(name)
    wrap = Wool(ramp, stripes=_spiral, noise=0.6)
    tape = Wool(tape_ramp or ramp, flat=True)
    boot = Steel(boot_ramp, seed=101, sheen=0.9, wear=0.0)
    sole = Steel(SOLE, seed=102, sheen=0.0, wear=0.0)
    for bone, (lo, hi) in (("armorRightBoot", RIGHT_LEG), ("armorLeftBoot", LEFT_LEG)):
        lo, hi = np.asarray(lo, float), np.asarray(hi, float)
        cx = (lo[0] + hi[0]) / 2
        a, b = _around(lo, hi, 0.45, 2.1, 6.8)
        _box(m, bone, a, b, wrap)
        a, b = _around(lo, hi, 0.55, 6.15, 6.6)
        _box(m, bone, a, b, tape)
        a, b = _around(lo, hi, 0.5, -0.25, 2.35)
        _box(m, bone, a, b, boot, rim=True)
        # a rounded toe cap in front, and the sole under everything
        chamfered_block(m, bone, (cx, 0.475, -2.75), np.array([1.0, 0, 0]), np.array([0, 0, 1.0]), 2.35, 0.75,
                        0.8, 1.45, boot)
        _box(m, bone, (lo[0] - 0.6, -0.45, -3.6), (hi[0] + 0.6, -0.25, hi[2] + 0.6), sole)
    m.pack()
    return m


# ---------------------------------------------------------------------------------------------- the four armies

def british_tunic():
    return tunic("british_tunic", BRITISH_KHAKI, buttons=Steel(BRASS, seed=111, sheen=1.0, wear=0.0),
                 belt=Steel(WEB_KHAKI, seed=112, sheen=0.2, wear=0.0), buckle=Steel(BRASS, seed=113, sheen=1.0, wear=0.0),
                 breast_pockets=True)


def british_trousers():
    return trousers("british_trousers", BRITISH_KHAKI)


def british_puttees():
    return puttees("british_puttees", BRITISH_KHAKI, BOOT_BROWN)


def french_tunic():
    brass = Steel(BRASS, seed=121, sheen=1.0, wear=0.0)
    return tunic("french_tunic", FRENCH_HORIZON, buttons=brass,
                 belt=Steel(BOOT_BROWN, seed=122, sheen=0.8, wear=0.0), buckle=brass,
                 collar_patch=Steel(FRENCH_PATCH, seed=123, sheen=0.3, wear=0.0))


def french_trousers():
    return trousers("french_trousers", FRENCH_HORIZON)


def french_puttees():
    return puttees("french_puttees", FRENCH_HORIZON, BOOT_BROWN)


def german_tunic():
    steel = Steel(STEEL_GREY, seed=131, sheen=1.0, wear=0.0)
    return tunic("german_tunic", GERMAN_FELDGRAU, collar=GERMAN_COLLAR,
                 belt=Steel(BLACK_LEATHER, seed=132, sheen=1.0, wear=0.0), buckle=steel)


def german_trousers():
    return trousers("german_trousers", GERMAN_STEINGRAU, piping=True)


def german_puttees():
    return puttees("german_puttees", GERMAN_FELDGRAU, BOOT_BROWN)


def austrian_tunic():
    return tunic("austrian_tunic", AUSTRIAN_FELDGRAU, breast_pockets=True,
                 belt=Steel(BOOT_BROWN, seed=142, sheen=0.8, wear=0.0),
                 buckle=Steel(STEEL_GREY, seed=143, sheen=1.0, wear=0.0),
                 collar_patch=Steel(AUSTRIAN_PATCH, seed=144, sheen=0.3, wear=0.0))


def austrian_trousers():
    return trousers("austrian_trousers", AUSTRIAN_FELDGRAU)


def austrian_puttees():
    return puttees("austrian_puttees", AUSTRIAN_FELDGRAU, BOOT_BROWN)
