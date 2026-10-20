"""Held weapons drawn as 3D models: the stick grenade.

Item models are centred on the item: GeckoLib puts the model's origin at the middle of the item's block, so the
grenade runs along +Y from y = -7 (the end of the handle) to y = +7 (the top of the head).
"""
from geo import Model
from kit import octagon
from paint import Metal, Steel, hexramp

HEAD_GREY = hexramp("22272a", "2e3437", "3b4245", "4a5255", "5b6466", "6f787a", "889092")
BEECH = hexramp("4a3018", "5e3e20", "74502b", "8a6236", "a07643", "b68b55", "c9a06a")
CAP_STEEL = hexramp("2c2f31", "3e4245", "53585b", "6b7073", "868b8e", "a5aaad")
UP = (0, 1, 0)


def stick_grenade():
    """German stick grenade of 1917 (Stielhandgranate): a steel can of explosive on a long wooden handle, the
    steel collar where the handle screws in, the belt hook on the side of the can, and the steel cap over the
    pull cord at the end of the handle."""
    m = Model("stick_grenade")
    m.bone("grenade", pivot=(0, 0, 0))
    head = Steel(HEAD_GREY, seed=501, sheen=0.8, wear=0.05)
    wood = Steel(BEECH, seed=502, sheen=0.25, wear=0.0, grain=0.35, mottle=0.25)
    steel = Metal(CAP_STEEL, seed=503)
    # the handle, and the cap at its end
    octagon(m, "grenade", (0, -2.2, 0), UP, 0.55, 9.6, wood, up=(0, 0, 1))
    octagon(m, "grenade", (0, -6.7, 0), UP, 0.68, 0.6, steel, up=(0, 0, 1))
    # the collar and the head, with a rounded top
    octagon(m, "grenade", (0, 2.4, 0), UP, 0.8, 0.8, steel, up=(0, 0, 1))
    octagon(m, "grenade", (0, 4.75, 0), UP, 1.45, 3.9, head, up=(0, 0, 1))
    octagon(m, "grenade", (0, 6.8, 0), UP, 1.15, 0.3, head, up=(0, 0, 1))
    # the belt hook: a flat steel strip along the side of the can
    m.box("grenade", (1.38, 3.3, -0.25), (0.22, 2.8, 0.5), steel)
    m.pack()
    return m
