#!/usr/bin/env python3
"""Paints the cavalry horse armour: a texture laid out on the vanilla horse model, which the game draws over the
horse a little larger than it (as it draws every horse armour).

The vanilla model is a set of boxes, each with its faces packed into the texture from a corner (u, v): for a box of
w x h x d (x, y, z) the upper row holds the top (d+w .. ) and the bottom, the row below it the four sides, each as
tall as the box: a side (d wide), the front (w), the other side (d) and the back (w). Rows run from the top of the
box down. Parts left transparent are not drawn (the mane, the tail, the ears, the saddle).

The armour: a quilted olive caparison over the body and the neck with a red border, a dark leather breast strap
with brass studs across the chest, a light steel chanfron over the face with leather cheek straps, and leather
boots round the lower legs.

Usage: python3 tools/build_horse_armor.py [--preview FILE]
"""
import argparse
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "yperite", "textures", "entity", "horse",
                   "armor", "cavalry_horse_armor.png")


def hexramp(*codes):
    return [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) for c in codes]


OLIVE = hexramp("2e3220", "3b4029", "4a5033", "5a613e", "6c744b", "808958")
RED = hexramp("5a1414", "7a1c1c", "9a2624", "b8322e")
LEATHER = hexramp("1f140c", "2d1d12", "3d2819", "4f3421", "62432b")
BRASS = hexramp("6a5524", "8c7234", "b8964c", "d9bc72")
STEEL = hexramp("3c4247", "525a60", "6b747a", "889196", "a8b0b4", "c8ced1")

# (u, v, w, h, d) of the boxes of the vanilla horse model (HorseModel.createBodyMesh)
BODY = (0, 32, 10, 10, 22)
NECK = (0, 35, 4, 12, 7)
HEAD = (0, 13, 6, 5, 7)
MOUTH = (0, 25, 4, 5, 5)
LEG = (48, 21, 4, 11, 4)


def faces(box):
    """The rectangles of a box's faces: name -> (u0, v0, width, height)."""
    u, v, w, h, d = box
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "side1": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "side2": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


class Painter:
    def __init__(self, seed=7):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def tone(self, ramp, level):
        level = max(0, min(len(ramp) - 1, int(round(level))))
        return (*ramp[level], 255)

    def fill(self, rect, colour):
        """colour(i, j, w, h) -> RGBA or None for each texel (i across, j down from the top of the face)."""
        u0, v0, w, h = rect
        for j in range(h):
            for i in range(w):
                c = colour(i, j, w, h)
                if c is not None:
                    self.px[u0 + i, v0 + j] = c

    # ------------------------------------------------------------------ materials

    def quilt(self, i, j, w, h, top_row=0, border_rows=1, light=0.0):
        """The quilted caparison: olive cloth with darker stitched diamonds, a red border along its lower edge."""
        if h - j <= border_rows:
            return self.tone(RED, 2 if h - j == border_rows else 1)
        level = 3 + light + (self.rnd.random() - 0.5) * 0.9
        if (i + j) % 4 == 0 or (i - j) % 4 == 0:
            level -= 1.1
        if j == top_row:
            level += 0.6
        return self.tone(OLIVE, level)

    def leather(self, i, j, level=2.4):
        return self.tone(LEATHER, level + (self.rnd.random() - 0.5) * 0.8)

    def steel(self, i, j, w, h):
        level = 3.2 + (0.8 if j == 0 else 0) - (0.6 if j == h - 1 else 0) + (self.rnd.random() - 0.5) * 0.4
        if i == 0 or i == w - 1:
            level -= 0.7
        return self.tone(STEEL, level)


def paint():
    p = Painter()
    # the body: the caparison over the top, the sides and the rump; across the chest the leather breast strap
    body = faces(BODY)
    p.fill(body["top"], lambda i, j, w, h: p.quilt(i, j, w, h, top_row=-1, border_rows=0, light=0.5))
    for side in ("side1", "side2"):
        p.fill(body[side], lambda i, j, w, h: p.quilt(i, j, w, h, top_row=0, border_rows=2))
    p.fill(body["back"], lambda i, j, w, h: p.quilt(i, j, w, h, top_row=0, border_rows=2))

    def chest(i, j, w, h):
        if j in (2, 3):
            # the breast strap, studded with brass
            return p.tone(BRASS, 3) if i % 3 == 1 and j == 2 else p.leather(i, j)
        if j == h - 1:
            return p.tone(RED, 2)
        return p.quilt(i, j, w, h, top_row=0, border_rows=0)
    p.fill(body["front"], chest)
    # the neck: the caparison continues up it (the crinet), red-edged at the throat
    neck = faces(NECK)
    for side in ("side1", "side2"):
        p.fill(neck[side], lambda i, j, w, h: p.tone(RED, 2) if i == 0 else p.quilt(i, j, w, h, top_row=-1, border_rows=0))
    p.fill(neck["front"], lambda i, j, w, h: p.quilt(i, j, w, h, top_row=-1, border_rows=0, light=-0.4))
    p.fill(neck["back"], lambda i, j, w, h: p.quilt(i, j, w, h, top_row=-1, border_rows=0))
    p.fill(neck["top"], lambda i, j, w, h: p.quilt(i, j, w, h, top_row=-1, border_rows=0, light=0.4))
    # the head: a steel chanfron down the face, the headstall's leather straps on the cheeks with a brass boss
    head = faces(HEAD)
    p.fill(head["front"], p.steel)
    p.fill(head["top"], lambda i, j, w, h: p.steel(i, j, w, h) if j < 3 else (p.leather(i, j) if j == 4 else None))
    for side in ("side1", "side2"):
        def cheek(i, j, w, h):
            if j == 1:
                return p.tone(BRASS, 3) if i == w - 3 else p.leather(i, j)     # the browband
            if i == w - 3:
                return p.leather(i, j)                                          # the cheek strap
            return None
        p.fill(head[side], cheek)
    mouth = faces(MOUTH)
    p.fill(mouth["front"], lambda i, j, w, h: p.steel(i, j, w, h) if j < 3 else None)
    p.fill(mouth["top"], lambda i, j, w, h: p.steel(i, j, w, h))
    for side in ("side1", "side2"):
        p.fill(mouth[side], lambda i, j, w, h: p.leather(i, j) if j == 1 else None)          # the noseband
    # the legs: leather boots round the lower legs, buckled
    leg = faces(LEG)
    for side in ("side1", "front", "side2", "back"):
        p.fill(leg[side], lambda i, j, w, h: (p.tone(BRASS, 2) if (j == 8 and i == 1) else p.leather(i, j, 2.0))
               if j >= 7 else None)
    return p.img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    args = ap.parse_args()
    img = paint()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("cavalry_horse_armor.png written")
    if args.preview:
        big = Image.new("RGBA", (64 * 8, 64 * 8), (60, 60, 60, 255))
        big.alpha_composite(img.resize((64 * 8, 64 * 8), Image.NEAREST))
        big.save(args.preview)


if __name__ == "__main__":
    main()
