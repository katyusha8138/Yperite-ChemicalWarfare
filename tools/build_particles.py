#!/usr/bin/env python3
"""Writes the gas particle sprites and their particle definitions.

A cloud puff is a few overlapping round blobs, lit from the upper left, nearly opaque inside and fading at the edge
through two translucent rings. A mist puff is flatter and thinner: a ragged patch with holes, drawn lying on the
ground. Both are white and grey: the game tints them with the gas's colour.

Usage: python3 tools/build_particles.py [--preview FILE]
"""
import argparse
import json
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "yperite")
TONES = [158, 178, 198, 218, 236, 250]


def _field(blobs, size=16):
    """How far inside the shape each pixel is (0 outside), and the shape's 'surface' slope for lighting."""
    inside = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            best = 0.0
            for (cx, cy, r) in blobs:
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                best = max(best, 1.0 - d / r)
            inside[y][x] = best
    return inside


def _puff(seed, mist=False):
    rnd = random.Random(seed)
    blobs = []
    if mist:
        blobs.append((8.0, 8.0, rnd.uniform(4.5, 5.5)))
        for _ in range(6):
            a = rnd.uniform(0, 2 * math.pi)
            d = rnd.uniform(2.5, 4.5)
            blobs.append((8 + d * math.cos(a), 8 + d * math.sin(a), rnd.uniform(2.8, 4.0)))
    else:
        blobs.append((rnd.uniform(7, 9), rnd.uniform(7, 9), rnd.uniform(4.6, 5.6)))
        for _ in range(rnd.randint(2, 4)):
            blobs.append((rnd.uniform(4.5, 11.5), rnd.uniform(4.5, 11.5), rnd.uniform(2.6, 4.0)))
    f = _field(blobs)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = f[y][x]
            if v <= 0.0:
                continue
            # the edge fades in two steps; mist also has holes in it
            if mist and v < 0.35 and rnd.random() < 0.15:
                continue
            if v < 0.12:
                a = 0.28 if (x + y) % 2 == 0 else 0.0
            elif v < 0.3:
                a = 0.55
            else:
                a = 0.85 if not mist else 0.5
            if a <= 0.0:
                continue
            # light from the upper left: compare with the neighbour towards the light
            ul = f[y - 1][x - 1] if x > 0 and y > 0 else 0.0
            dr = f[y + 1][x + 1] if x < 15 and y < 15 else 0.0
            level = 3 + (1 if ul < v - 0.04 else 0) - (1 if dr < v - 0.04 else 0) + (1 if v > 0.55 else 0)
            level = max(0, min(len(TONES) - 1, level))
            g = TONES[level]
            img.putpixel((x, y), (g, g, g, int(255 * a)))
    return img


CLOUDS = 8
MISTS = 4


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    args = ap.parse_args()
    tex = os.path.join(ASSETS, "textures", "particle")
    os.makedirs(tex, exist_ok=True)
    defs = os.path.join(ASSETS, "particles")
    os.makedirs(defs, exist_ok=True)
    images = []
    for kind, count, mist in (("gas_cloud", CLOUDS, False), ("gas_mist", MISTS, True)):
        names = []
        for k in range(count):
            img = _puff(1000 * (2 if mist else 1) + k, mist)
            img.save(os.path.join(tex, f"{kind}_{k}.png"))
            names.append(f"yperite:{kind}_{k}")
            images.append(img)
        with open(os.path.join(defs, kind + ".json"), "w", encoding="utf-8") as f:
            json.dump({"textures": names}, f, indent=2)
            f.write("\n")
    print(f"{CLOUDS} cloud and {MISTS} mist sprites written")
    if args.preview:
        scale = 10
        out = Image.new("RGBA", (len(images) * (16 * scale + 6), 16 * scale), (60, 70, 50, 255))
        for k, img in enumerate(images):
            tinted = Image.new("RGBA", img.size)
            for y in range(16):
                for x in range(16):
                    r, g, b, a = img.getpixel((x, y))
                    tinted.putpixel((x, y), (r * 0xB9 // 255, g * 0xD9 // 255, b * 0x4A // 255, a))
            out.alpha_composite(tinted.resize((16 * scale, 16 * scale), Image.NEAREST), (k * (16 * scale + 6), 0))
        out.save(args.preview)


if __name__ == "__main__":
    main()
