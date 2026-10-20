"""Held weapons drawn as 3D models: the stick grenade, the officer's sabre and the cavalry lance.

Item models are centred on the item: GeckoLib puts the model's origin at the middle of the item's block. Each runs
along +Y. The grenade is centred on the origin (y = -7 to +7). The sabre and the lance lie as a vanilla sword's
picture does once the item is turned 45 degrees: the hand holds them at y = -7, where a sword's hilt is.
"""
import math

import numpy as np

from geo import Model
from kit import chamfered_block, octagon, oriented_box
from paint import Features, Leather, Metal, Steel, Wool, hexramp

HEAD_GREY = hexramp("22272a", "2e3437", "3b4245", "4a5255", "5b6466", "6f787a", "889092")
BEECH = hexramp("4a3018", "5e3e20", "74502b", "8a6236", "a07643", "b68b55", "c9a06a")
CAP_STEEL = hexramp("2c2f31", "3e4245", "53585b", "6b7073", "868b8e", "a5aaad")
UP = (0, 1, 0)
X = np.array([1.0, 0.0, 0.0])
Y = np.array([0.0, 1.0, 0.0])
Z = np.array([0.0, 0.0, 1.0])
BLADE = hexramp("3d454b", "56606a", "75808a", "98a2aa", "bcc4ca", "dde3e7", "f4f7f8")
BRASS = hexramp("4a3a18", "6a5524", "8c7234", "ad8f47", "c9aa62", "e0c584")
BLACK_LEATHER = hexramp("0f0f10", "18181a", "232326", "2f2f33", "3e3e44")
WIRE = (74, 76, 82, 255)
ASH = hexramp("5a4228", "6e5232", "84643e", "9a774b", "ae8a5a", "c19d6b", "d2b07e")
BROWN_LEATHER = hexramp("2a180d", "3b2414", "4f321d", "644128", "7a5235", "936644")
PENNON_RED = hexramp("5a1212", "7a1a1a", "9a2422", "b8302c", "cc4440")
PENNON_WHITE = hexramp("a8a69c", "bdbbb1", "d1cfc5", "e2e0d7", "f0eee6")


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


def _wire(t):
    """Wire wound round a grip: every other row of the black leather catches the light."""
    if abs(t.normal[1]) > 0.5:
        return None
    return WIRE if int(math.floor(t.pos[1] * 1.0)) % 2 == 0 else None


def sabre():
    """An officer's sabre (the pattern is no one army's): a slightly curved, polished blade with its tip swept back
    towards the spine, a brass guard with a knuckle bow down to the pommel on the edge side, and a black leather
    grip bound with silver wire. The edge faces +X, the flat of the blade faces the viewer, as a vanilla sword's."""
    m = Model("sabre")
    m.bone("sabre", pivot=(0, 0, 0))
    steel = Steel(BLADE, seed=511, sheen=1.0, wear=0.0)
    brass = Metal(BRASS, seed=512)
    grip = Features(Leather(BLACK_LEATHER, seed=513, shine=False), _wire)
    # the blade: segments along a curve from the guard (y = -4.3) to the tip (y = +10.3)
    y0, y1, sweep = -4.3, 10.3, 1.3
    n = 8
    length = y1 - y0

    def centre(f):
        return np.array([-sweep * f * f, y0 + length * f, 0.0])

    for k in range(n):
        f0, f1 = k / n, (k + 1) / n
        a, b = centre(f0), centre(f1)
        along = (b - a) / np.linalg.norm(b - a)
        across = np.cross(along, Z)
        width = 1.05 - 0.45 * (f0 + f1) / 2
        last = k == n - 1
        oriented_box(m, "sabre", (a + b) / 2 - (across * 0.18 if last else 0), across, along,
                     width * (0.6 if last else 1.0), np.linalg.norm(b - a) + 0.06, 0.22, steel)
    # the guard, the knuckle bow and the pommel
    chamfered_block(m, "sabre", (0.45, -4.5, 0), X, Z, 1.45, 0.7, 0.45, 0.3, brass)
    m.box("sabre", (1.5, -10.25, -0.22), (0.32, 5.6, 0.44), brass)
    m.box("sabre", (0.25, -10.6, -0.22), (1.57, 0.35, 0.44), brass)
    octagon(m, "sabre", (0, -10.6, 0), UP, 0.6, 0.45, brass, up=(0, 0, 1))
    octagon(m, "sabre", (0, -7.45, 0), UP, 0.55, 5.6, grip, up=(0, 0, 1))
    m.pack()
    return m


def lance():
    """A cavalry lance, 2.75 blocks long: an ash shaft with a steel shoe at the butt and a leather grip, a
    leaf-shaped steel head on a socket, and below it the swallow-tailed pennon, red over white. The hand holds it
    at the grip (y = -7), a third of the way from the butt."""
    m = Model("lance")
    m.bone("lance", pivot=(0, 0, 0))
    wood = Steel(ASH, seed=521, sheen=0.3, wear=0.0, grain=0.3, mottle=0.25)
    steel = Steel(BLADE, seed=522, sheen=0.9, wear=0.05)
    butt, grip, top = -19.0, -7.0, 25.0
    octagon(m, "lance", (0, (butt + 0.7 + 19.0) / 2, 0), UP, 0.45, 19.0 - butt - 0.7, wood, up=(0, 0, 1))
    octagon(m, "lance", (0, butt + 0.4, 0), UP, 0.52, 0.8, steel, up=(0, 0, 1))
    octagon(m, "lance", (0, grip, 0), UP, 0.53, 4.0, Leather(BROWN_LEATHER, seed=523), up=(0, 0, 1))
    # the socket and the langets running down the shaft, then the head
    octagon(m, "lance", (0, 19.9, 0), UP, 0.5, 1.8, steel, up=(0, 0, 1))
    for sx in (-1, 1):
        m.box("lance", (sx * 0.45 - 0.08, 17.0, -0.18), (0.16, 2.2, 0.36), steel)
    chamfered_block(m, "lance", (0, 22.9, 0), X, Y, 0.7, 2.1, 0.7, 0.28, steel)
    # the pennon, on the side of the shaft below the socket: red above, white below, a swallow tail
    cloth = Features(Wool(PENNON_WHITE, seed=524, noise=0.4), lambda t: None if t.pos[1] < 16.05 else
                     Wool(PENNON_RED, seed=525, noise=0.4).color(t))
    m.box("lance", (0.45, 14.4, -0.05), (3.5, 3.3, 0.1), cloth)
    m.box("lance", (3.95, 16.4, -0.05), (2.3, 1.3, 0.1), cloth)
    m.box("lance", (3.95, 14.4, -0.05), (2.3, 1.3, 0.1), cloth)
    m.pack()
    return m
