"""The protective outfit: hood, jacket, trousers and overboots of rubberised olive cloth.

It is worn like the uniforms (fit.py checks the layers): the hood goes over a gas mask worn in the Curios face slot,
leaving the whole face open so that the mask's face piece shows; the trousers go down into the overboots and up
under the jacket; body armour and a respirator's haversack go over the jacket, which stands off the chest by
STAND_OFF. A helmet cannot be worn with the hood (both take the head slot).
"""
import numpy as np

from armor import LEFT_ARM, LEFT_LEG, RIGHT_ARM, RIGHT_LEG, armor_model
from kit import chamfered_block, chamfered_prism, oriented_box
from paint import Steel, hexramp

RUBBER_OD = hexramp("1d2313", "2a321c", "3a4426", "4d5a31", "62713e", "7b8a4f", "97a566")
TAPE = hexramp("1a1c18", "262923", "33372f", "42473d", "53594c")
STAND_OFF = {"front": 0.9, "back": 0.6}

X = np.array([1.0, 0.0, 0.0])
Y = np.array([0.0, 1.0, 0.0])
Z = np.array([0.0, 0.0, 1.0])


def _rubber(seed):
    return Steel(RUBBER_OD, seed=seed, sheen=0.3, wear=0.0)


def _box(m, bone, lo, hi, material, **info):
    lo = np.asarray(lo, float)
    hi = np.asarray(hi, float)
    return m.box(bone, tuple(lo), tuple(hi - lo), material, **info)


def _around(lo, hi, off, y0, y1):
    lo = np.asarray(lo, float) - off
    hi = np.asarray(hi, float) + off
    lo[1], hi[1] = y0, y1
    return lo, hi


def protective_hood():
    """A rounded rubber hood over the head and down to the neck, open over the whole face."""
    m = armor_model("protective_hood")
    rubber = _rubber(301)
    # the crown, closed above the brow, and a flatter cap on top
    chamfered_prism(m, "armorHead", (0.0, 0.0), 4.5, 4.5, 0.6, 30.0, 32.6, rubber)
    chamfered_prism(m, "armorHead", (0.0, 0.0), 3.9, 3.9, 1.0, 32.6, 33.0, rubber, bottom=False)
    # the sides and the back below it, open at the front; the back corners cut like the crown's
    for sx in (-1, 1):
        x0, x1 = (4.0, 4.5) if sx > 0 else (-4.5, -4.0)
        _box(m, "armorHead", (x0, 23.8, -4.0), (x1, 30.0, 3.9), rubber)
        corner = np.array([sx * 4.2, 26.9, 4.2])
        u = np.array([-sx, 0.0, 1.0]) / np.sqrt(2)
        oriented_box(m, "armorHead", corner, u, Y, 1.0, 6.2, 0.5, rubber)
    _box(m, "armorHead", (-3.9, 23.8, 4.0), (3.9, 30.0, 4.7), rubber)
    # the upper corners of the face opening, beside a mask's brow and above its eyepieces
    for sx in (-1, 1):
        x0, x1 = (3.0, 4.5) if sx > 0 else (-4.5, -3.0)
        _box(m, "armorHead", (x0, 28.4, -4.5), (x1, 30.0, -4.0), rubber)
    m.pack()
    return m


def protective_jacket():
    """A smock of rubberised cloth with a sealed front, a high collar, a hem band and taped cuffs."""
    m = armor_model("protective_jacket")
    rubber = _rubber(311)
    tape = Steel(TAPE, seed=312, sheen=0.4, wear=0.0)
    _box(m, "armorBody", (-4.55, 12.4, -2.55), (4.55, 24.5, 2.55), rubber)
    _box(m, "armorBody", (-4.75, 11.2, -2.75), (4.75, 12.6, 2.75), rubber, rim=True)
    _box(m, "armorBody", (-0.55, 12.6, -2.85), (0.55, 24.3, -2.55), rubber, rim=True)
    _box(m, "armorBody", (-3.4, 23.2, -2.9), (3.4, 24.7, 2.9), rubber, rim=True)
    for bone, (lo, hi) in (("armorRightArm", RIGHT_ARM), ("armorLeftArm", LEFT_ARM)):
        a, b = _around(lo, hi, 0.5, 12.6, 24.4)
        _box(m, bone, a, b, rubber)
        a, b = _around(lo, hi, 0.65, 12.6, 13.8)
        _box(m, bone, a, b, tape, rim=True)
    m.pack()
    return m


def protective_trousers():
    """Rubberised trousers, straight down into the overboots, with a waistband under the jacket."""
    m = armor_model("protective_trousers")
    rubber = _rubber(321)
    for bone, (lo, hi) in (("armorRightLeg", RIGHT_LEG), ("armorLeftLeg", LEFT_LEG)):
        a, b = _around(lo, hi, 0.3, 2.0, 12.3)
        _box(m, bone, a, b, rubber)
    _box(m, "armorBody", (-4.35, 11.4, -2.35), (4.35, 13.0, 2.35), rubber)
    m.pack()
    return m


def protective_boots():
    """Rubber overboots up to the calf, with a rolled top, a rounded toe and a heavy sole."""
    m = armor_model("protective_boots")
    rubber = Steel(RUBBER_OD, seed=331, sheen=0.9, wear=0.0)
    sole = Steel(TAPE, seed=332, sheen=0.0, wear=0.0)
    for bone, (lo, hi) in (("armorRightBoot", RIGHT_LEG), ("armorLeftBoot", LEFT_LEG)):
        lo, hi = np.asarray(lo, float), np.asarray(hi, float)
        cx = (lo[0] + hi[0]) / 2
        a, b = _around(lo, hi, 0.6, -0.3, 7.0)
        _box(m, bone, a, b, rubber)
        a, b = _around(lo, hi, 0.75, 6.2, 7.0)
        _box(m, bone, a, b, rubber, rim=True)
        chamfered_block(m, bone, (cx, 0.55, -2.85), X, Z, 2.45, 0.8, 0.8, 1.7, rubber)
        _box(m, bone, (lo[0] - 0.7, -0.5, -3.75), (hi[0] + 0.7, -0.3, hi[2] + 0.7), sole)
    m.pack()
    return m
