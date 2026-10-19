#!/usr/bin/env python3
"""Writes the block models and textures of the gas cylinder and the gas alarm.

The cylinder is an eight-sided bottle built from block-model elements (a centre slab, two side slabs and four
corner pieces turned by 45 degrees), with a shoulder, neck, brass valve, outlet and handwheel. Its band is drawn
with tint 0, which the game colours after the gas inside. The alarm is a steel cabinet with a square lamp that
glows (full brightness) while it is sounding.

Usage: python3 tools/build_blocks.py
"""
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "icons"))

from pixel import draw, ramp, tones  # noqa: E402

ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "yperite")
NS = "yperite:block/"

PAINT = ramp("1f2620", "2b342c", "3a463b", "4c5a4c", "5f6e5e", "778674", "93a08e")
BAND = ramp("8e8e8e", "b4b4b4", "d2d2d2", "eaeaea", "fafafa")
BRASS = ramp("4a3a18", "6e5628", "93743a", "b8944f", "d6b86c", "efd795")
RED = ramp("3d0c0c", "6a1414", "962020", "c02e2e", "e04a4a", "f27a6a")
GREEN = ramp("0f2d12", "1a4a1f", "286b2e", "3a8f41", "56b25c", "86d48a")
CABINET = ramp("23282a", "323a3d", "434d51", "566267", "6c787d", "879398", "a6b0b4")
LAMP_OFF = ramp("1a0807", "2e0d0b", "461412", "5e1d19", "7a2a24")
LAMP_ON = ramp("8a1a10", "c4301c", "f0582a", "ff8a3c", "ffbf6a", "fff0c0")


# ---------------------------------------------------------------------- textures

def _cylinder_side():
    # one vertical profile across the face strip (columns 6-9 are used); light at the top, a sheen down column 7
    rows = []
    for r in range(16):
        base = 5 if r < 5 else (4 if r < 9 else (3 if r < 13 else 2))
        row = ""
        for c in range(16):
            level = base + (1 if c == 7 else 0) - (1 if c >= 9 else 0)
            if r == 15:
                level = 1
            row += str(max(0, min(6, level)))
        rows.append(row)
    return draw(rows, tones(PAINT))


def _band():
    rows = []
    for r in range(16):
        level = 3 if r % 3 == 1 else 2
        rows.append("".join(str(level + (1 if c == 7 else 0) - (1 if c >= 9 else 0)) for c in range(16)))
    return draw(rows, tones(BAND))


def _cylinder_top():
    # seen from above: lighter towards the upper left (north-west), darker to the south-east
    rows = []
    for z in range(16):
        row = ""
        for x in range(16):
            d = (x - 7.5) + (z - 7.5)
            level = 4 - int(round(d / 5.0))
            if abs(x - 7.5) < 1.6 and abs(z - 7.5) < 1.6:
                level = 2
            row += str(max(1, min(6, level)))
        rows.append(row)
    return draw(rows, tones(PAINT))


def _brass():
    rows = []
    for r in range(16):
        rows.append("".join(str(max(1, min(5, 4 - (r // 6) - (1 if c >= 9 else 0) + (1 if c == 7 else 0)))) for c in range(16)))
    return draw(rows, tones(BRASS))


def _wheel(colours):
    # a handwheel in the square 6..10 (as the model maps it): rim, four spokes and a hub
    rows = []
    for z in range(16):
        row = ""
        for x in range(16):
            if 6 <= x <= 9 and 6 <= z <= 9:
                rim = x in (6, 9) or z in (6, 9)
                spoke = (x in (7, 8)) or (z in (7, 8))
                level = 4 if rim else (3 if spoke else 1)
                if (x, z) in ((6, 6), (7, 6), (6, 7)):
                    level = 5
                if (x, z) in ((9, 9), (8, 9), (9, 8)):
                    level = 2
            else:
                level = 2
            row += str(level)
        rows.append(row)
    return draw(rows, tones(colours))


def _alarm_side(on):
    """The cabinet's face: a bevelled frame, a square lamp in a bright bezel (columns 6-9, rows 3-6) and a vent
    grille below it."""
    lamp = LAMP_ON if on else LAMP_OFF
    grid = [[4] * 16 for _ in range(16)]
    for i in range(16):
        grid[0][i], grid[1][i] = 6, 5
        grid[15][i], grid[14][i] = 2, 3
        grid[i][0], grid[i][1] = 6, 5
        grid[i][15], grid[i][14] = 2, 3
    grid[0][15], grid[15][0] = 5, 5
    for x in range(5, 11):                       # the bezel
        grid[2][x], grid[7][x] = 6, 2
    for y in range(2, 8):
        grid[y][5], grid[y][10] = 6, 2
    for y in range(9, 14):                       # the grille
        for x in range(3, 13):
            grid[y][x] = 1 if y % 2 == 1 else 4
    rows = ["".join(str(v) for v in r) for r in grid]
    lens = "abcdef"
    for y in range(3, 7):                        # the lens: brightest in the upper left
        line = list(rows[y])
        for x in range(6, 10):
            level = (len(lamp) - 1) - ((x - 6) + (y - 3)) // 2
            if on and (x, y) == (6, 3):
                level = len(lamp) - 1
            line[x] = lens[max(0, min(len(lamp) - 1, level))]
        rows[y] = "".join(line)
    legend = tones(CABINET)
    lamp_part = tones(lamp)["0"][0]
    for i, d in enumerate(lens[:len(lamp)]):
        legend[d] = (lamp_part, i)
    return draw(rows, legend)


def _alarm_top():
    rows = ["6666666666666665", "6555555555555543"]
    for r in range(2, 14):
        rows.append("654" + ("1111111111" if r % 2 == 0 else "4444444444") + "432")
    rows += ["6533333333333322", "5322222222222221"]
    return draw(rows, tones(CABINET))


def _alarm_bottom():
    rows = ["4444444444444443"] + ["4333333333333332"] * 14 + ["3222222222222221"]
    return draw(rows, tones(CABINET))


# ---------------------------------------------------------------------- the cylinder's elements

def _face(texture, uv, tint=None, light=False):
    f = {"texture": "#" + texture, "uv": [round(v, 3) for v in uv]}
    if tint is not None:
        f["tintindex"] = tint
    if light:
        f["forge_data"] = {"block_light": 15, "sky_light": 15}
    return f


def _el(frm, to, faces, rotation=None):
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if rotation is not None:
        e["rotation"] = rotation
    return e


def octagon(x0, x1, y0, y1, side, top, tint=None, top_faces=True):
    """An eight-sided prism over the square x0..x1 (in x and z): flat faces all of the same width."""
    w = x1 - x0
    c = w / (2 + math.sqrt(2))
    s = w - 2 * c                       # width of every face
    z0, z1 = x0, x1
    u0, u1 = 8 - s / 2, 8 + s / 2       # the face strip of the side texture
    v0, v1 = 16 - y1, 16 - y0
    strip = [u0, v0, u1, v1]
    els = []

    def caps(a, b, c_, d):
        if not top_faces:
            return {}
        return {"up": _face(top, [a, c_, b, d]), "down": _face(top, [a, c_, b, d])}

    els.append(_el([x0 + c, y0, z0], [x1 - c, y1, z1],
                   {"north": _face(side, strip, tint), "south": _face(side, strip, tint), **caps(x0 + c, x1 - c, z0, z1)}))
    els.append(_el([x0, y0, z0 + c], [x0 + c, y1, z1 - c],
                   {"west": _face(side, strip, tint), **caps(x0, x0 + c, z0 + c, z1 - c)}))
    els.append(_el([x1 - c, y0, z0 + c], [x1, y1, z1 - c],
                   {"east": _face(side, strip, tint), **caps(x1 - c, x1, z0 + c, z1 - c)}))
    # the corners: a slab on each cut, turned by 45 degrees about its own centre
    t = c / math.sqrt(2)
    for (cx, cz, angle, face) in ((x0, z0, 45, "north"), (x1, z0, -45, "north"),
                                  (x0, z1, -45, "south"), (x1, z1, 45, "south")):
        # midpoint of the cut, then half the thickness inwards
        mx = cx + (c / 2 if cx == x0 else -c / 2)
        mz = cz + (c / 2 if cz == z0 else -c / 2)
        nx = 1 if cx == x0 else -1
        nz = 1 if cz == z0 else -1
        px = mx + nx * t / 2 / math.sqrt(2)
        pz = mz + nz * t / 2 / math.sqrt(2)
        frm = [px - s / 2, y0, pz - t / 2]
        to = [px + s / 2, y1, pz + t / 2]
        faces = {face: _face(side, strip, tint)}
        faces.update(caps(frm[0], to[0], frm[2], to[2]))
        els.append(_el(frm, to, faces, {"origin": [round(px, 3), 8, round(pz, 3)], "axis": "y", "angle": angle}))
    return els


def box(frm, to, texture, top=None, uv_side=None):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    side_x = uv_side or [x0, 16 - y1, x1, 16 - y1 + (y1 - y0)]
    side_z = uv_side or [z0, 16 - y1, z1, 16 - y1 + (y1 - y0)]
    t = top or texture
    return _el(frm, to, {
        "north": _face(texture, side_x), "south": _face(texture, side_x),
        "east": _face(texture, side_z), "west": _face(texture, side_z),
        "up": _face(t, [x0, z0, x1, z1]), "down": _face(t, [x0, z0, x1, z1]),
    })


def cylinder_model(open_):
    els = []
    els += octagon(4.2, 11.8, 0.0, 11.5, "side", "top")
    els += octagon(4.1, 11.9, 8.5, 10.0, "band", "band", tint=0, top_faces=True)
    els += octagon(4.9, 11.1, 11.5, 12.5, "side", "top")
    els.append(box([7, 12.5, 7], [9, 13.5, 9], "side", top="top"))
    els.append(box([6.75, 13.5, 6.75], [9.25, 15, 9.25], "valve"))
    els.append(box([9.25, 14, 7.6], [10.75, 14.75, 8.4], "valve"))
    els.append(box([6, 15, 6], [10, 15.5, 10], "wheel"))
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {
            "particle": NS + "gas_cylinder_side",
            "side": NS + "gas_cylinder_side",
            "top": NS + "gas_cylinder_top",
            "band": NS + "gas_cylinder_band",
            "valve": NS + "gas_cylinder_valve",
            "wheel": NS + ("gas_cylinder_wheel_open" if open_ else "gas_cylinder_wheel"),
        },
        "elements": els,
    }


def alarm_model(on):
    model = {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "side": NS + ("gas_alarm_on" if on else "gas_alarm"),
            "top": NS + "gas_alarm_top",
            "bottom": NS + "gas_alarm_bottom",
        },
    }
    if on:
        # the lamp, lit: the cabinet, then the lens over it again at full brightness
        lens = [6, 3, 10, 7]
        lit = {"north", "south", "east", "west"}
        model = {
            "parent": "minecraft:block/block",
            "textures": {"particle": NS + "gas_alarm_on", "side": NS + "gas_alarm_on",
                         "top": NS + "gas_alarm_top", "bottom": NS + "gas_alarm_bottom"},
            "elements": [
                _el([0, 0, 0], [16, 16, 16], {
                    **{d: {"texture": "#side", "cullface": d} for d in lit},
                    "up": {"texture": "#top", "cullface": "up"}, "down": {"texture": "#bottom", "cullface": "down"}}),
                # the lens again, a hair outside each face, at full brightness and without shading
                {**_el([6, 9, -0.02], [10, 13, 16.02], {"north": _face("side", lens, light=True),
                                                         "south": _face("side", lens, light=True)}), "shade": False},
                {**_el([-0.02, 9, 6], [16.02, 13, 10], {"west": _face("side", lens, light=True),
                                                         "east": _face("side", lens, light=True)}), "shade": False},
            ],
        }
    return model


def main():
    tex = os.path.join(ASSETS, "textures", "block")
    images = {
        "gas_cylinder_side": _cylinder_side(), "gas_cylinder_band": _band(), "gas_cylinder_top": _cylinder_top(),
        "gas_cylinder_valve": _brass(), "gas_cylinder_wheel": _wheel(RED), "gas_cylinder_wheel_open": _wheel(GREEN),
        "gas_alarm": _alarm_side(False), "gas_alarm_on": _alarm_side(True), "gas_alarm_top": _alarm_top(),
        "gas_alarm_bottom": _alarm_bottom(),
    }
    for name, image in images.items():
        image.convert("RGBA").save(os.path.join(tex, name + ".png"))
    models = os.path.join(ASSETS, "models", "block")
    for name, model in (("gas_cylinder", cylinder_model(False)), ("gas_cylinder_open", cylinder_model(True)),
                        ("gas_alarm", alarm_model(False)), ("gas_alarm_on", alarm_model(True))):
        with open(os.path.join(models, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(model, f, indent=2)
            f.write("\n")
    print(f"{len(images)} textures and 4 models written")


if __name__ == "__main__":
    main()
