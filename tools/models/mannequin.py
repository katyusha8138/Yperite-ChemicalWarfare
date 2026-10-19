"""A plain player figure for previews, so that worn models can be judged on a body."""
from geo import Model
from paint import Material, hexramp, pick, paint

SKIN = hexramp("8f6650", "b38466", "c99878")
HAIR = hexramp("2b1d14", "3b281b")
SHIRT = hexramp("2f4f6b", "3d6587")
PANTS = hexramp("2d2f45", "3a3c57")
SHOE = hexramp("29292b", "3a3a3d")


class Skin(Material):
    def color(self, t):
        x, y, z = t.pos
        n = t.normal
        part = t.cube.info.get("part")
        if part == "head":
            if y > 30.2 or (n[2] > 0.5 and y > 26) or (abs(n[0]) > 0.5 and y > 29 and z > -2):
                return pick(HAIR, 0.6 + 0.4 * n[1], t)
            if n[2] < -0.5 and 27 <= y < 28:
                if -3 <= x < -1 or 1 <= x < 3:
                    inner = (-2 <= x < -1) or (1 <= x < 2)
                    return (40, 50, 110, 255) if inner else (230, 230, 230, 255)
            if n[2] < -0.5 and 25 <= y < 26 and -2 <= x < 2:
                return pick(SKIN, 0, None)
            return pick(SKIN, 1.2 + 0.4 * n[1], t)
        if part == "arm":
            return pick(SKIN, 1.1 + 0.3 * n[1], t) if y < 21 else pick(SHIRT, 1, t)
        if part == "body":
            return pick(SHIRT, 0.8 + 0.4 * n[1], t)
        if part == "leg":
            return pick(SHOE, 1, t) if y < 2 else pick(PANTS, 0.8, t)
        return (255, 0, 255, 255)


def mannequin():
    m = Model("mannequin")
    skin = Skin()
    m.bone("head", pivot=(0, 24, 0))
    m.box("head", (-4, 24, -4), (8, 8, 8), skin, part="head")
    m.bone("body", pivot=(0, 24, 0))
    m.box("body", (-4, 12, -2), (8, 12, 4), skin, part="body")
    m.bone("right_arm", pivot=(-5, 22, 0))
    m.box("right_arm", (-8, 12, -2), (4, 12, 4), skin, part="arm")
    m.bone("left_arm", pivot=(5, 22, 0))
    m.box("left_arm", (4, 12, -2), (4, 12, 4), skin, part="arm")
    m.bone("right_leg", pivot=(-1.9, 12, 0))
    m.box("right_leg", (-3.9, 0, -2), (4, 12, 4), skin, part="leg")
    m.bone("left_leg", pivot=(1.9, 12, 0))
    m.box("left_leg", (-0.1, 0, -2), (4, 12, 4), skin, part="leg")
    m.pack()
    return m, paint(m)
