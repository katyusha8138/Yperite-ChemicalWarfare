"""Trench body armour: the German Sappenpanzer and the British Dayfield body shield.

Both are drawn to fit straight onto the body. Their front parts sit in a bone named layer_front and their back
parts in layer_back; the game moves those bones out when a tunic is worn under the armour (by the tunic's stand-off,
see uniforms.STAND_OFF), so the armour lies on the tunic without cutting into it.
"""
import math

import numpy as np

from armor import armor_model
from helmets import FELDGRAU_HELMET
from kit import chamfered_block, oriented_box
from paint import Steel, Wool, hexramp
from uniforms import BRASS, BOOT_BROWN, WEB_KHAKI

X = np.array([1.0, 0.0, 0.0])
Y = np.array([0.0, 1.0, 0.0])
BODY_FRONT = -2.05     # just off the chest
BODY_BACK = 2.05
DAYFIELD_KHAKI = hexramp("3a3424", "4b4430", "5e563d", "72694b", "877d5b", "9d926d", "b4a881")


def _layer_bones(m):
    m.bone("layer_front", parent="armorBody", pivot=(0, 24, 0))
    m.bone("layer_back", parent="armorBody", pivot=(0, 24, 0))


def _scale(m, bone, top, height, half_width, chamfer, depth, back_z, tilt, material):
    """A plate hanging with its top edge against the body at back_z and its lower edge tilted out by `tilt`
    degrees, like a scale: the next plate down tucks its top edge in behind this one's lower edge.
    Returns the centre of its outer face and its up and outward directions, to place rivets."""
    th = math.radians(tilt)
    v = np.array([0.0, math.cos(th), math.sin(th)])      # up the plate, leaning in towards the body
    w = np.cross(X, v)                                    # through the plate, towards the body
    top_back = np.array([0.0, top, back_z])
    centre = top_back - v * (height / 2) - w * (depth / 2)
    chamfered_block(m, bone, centre, X, v, half_width, height / 2, chamfer, depth, material)
    return centre - w * (depth / 2), v, -w


def sappenpanzer():
    """German Sappenpanzer (1916/17): a breastplate and three plates below it, each hung slightly out over the
    next, held by leather straps over the shoulders and across the back. Painted field grey, riveted."""
    m = armor_model("sappenpanzer")
    _layer_bones(m)
    steel = Steel(FELDGRAU_HELMET, seed=201, wear=0.05)
    heads = Steel(FELDGRAU_HELMET, seed=204, sheen=0.0, wear=0.3)
    plates = [  # top, height, half width, chamfer
        (24.0, 5.5, 3.9, 1.1),
        (18.75, 2.3, 3.75, 0.55),
        (16.75, 2.3, 3.6, 0.55),
        (14.75, 2.3, 3.45, 0.55),
    ]
    for top, h, hw, c in plates:
        face, up, out = _scale(m, "layer_front", top, h, hw, c, 0.45, BODY_FRONT, 8.0, steel)
        # a rivet near each top corner, where the straps are fixed
        for sx in (-1, 1):
            at = face + X * sx * (hw - 0.9) + up * (h / 2 - 0.6) + out * 0.075
            oriented_box(m, "layer_front", at, X, up, 0.5, 0.5, 0.15, heads)
    # the straps: two down the back and one round the waist behind
    leather = Steel(BOOT_BROWN, seed=202, sheen=0.7, wear=0.0)
    for sx in (-1, 1):
        oriented_box(m, "layer_back", (sx * 2.4, 19.0, BODY_BACK + 0.125), X, Y, 0.9, 9.6, 0.25, leather, strap=True)
    oriented_box(m, "layer_back", (0, 15.6, BODY_BACK + 0.3), X, Y, 7.8, 0.9, 0.1, leather, strap=True)
    for sx in (-1, 1):
        oriented_box(m, "layer_back", (sx * 2.4, 15.6, BODY_BACK + 0.38), X, Y, 0.7, 0.7, 0.06, Steel(BRASS, seed=203))
    m.pack()
    return m


def dayfield():
    """British Dayfield body shield (1916): steel plates sewn into a khaki cloth waistcoat, front and back, each
    panel quilted into an upper and a lower plate, with straps over the shoulders."""
    m = armor_model("dayfield")
    _layer_bones(m)
    cloth = Wool(DAYFIELD_KHAKI, seams=[(1, 18.7, (0, 0, -1)), (1, 18.7, (0, 0, 1))])
    chamfered_block(m, "layer_front", (0, 18.65, BODY_FRONT - 0.3), X, Y, 3.9, 5.25, 1.2, 0.6, cloth)
    chamfered_block(m, "layer_back", (0, 18.65, BODY_BACK + 0.3), X, Y, 3.9, 5.25, 1.2, 0.6, cloth)
    # tapes round the edges, and the buckles of the side straps
    web = Steel(WEB_KHAKI, seed=211, sheen=0.2, wear=0.0)
    oriented_box(m, "layer_front", (0, 23.6, BODY_FRONT - 0.65), X, Y, 5.4, 0.5, 0.1, web)
    oriented_box(m, "layer_back", (0, 23.6, BODY_BACK + 0.65), X, Y, 5.4, 0.5, 0.1, web)
    brass = Steel(BRASS, seed=212, sheen=1.0, wear=0.0)
    for sx in (-1, 1):
        oriented_box(m, "layer_front", (sx * 3.2, 21.3, BODY_FRONT - 0.65), X, Y, 0.6, 0.8, 0.1, brass)
        oriented_box(m, "layer_front", (sx * 3.2, 15.9, BODY_FRONT - 0.65), X, Y, 0.6, 0.8, 0.1, brass)
    m.pack()
    return m


# How far each armour stands off the chest at the front and the back: a gas mask's haversack goes over it.
STAND_OFF = {"sappenpanzer": (1.35, 0.5), "dayfield": (0.75, 0.75)}
