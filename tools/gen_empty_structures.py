#!/usr/bin/env python3
"""Generates the empty GameTest structure templates (stone floor, air above).

Usage: python3 tools/gen_empty_structures.py
Output: src/gametest/resources/data/yperite/structures/empty_<X>x<Y>x<Z>.nbt
"""
import gzip
import os
import struct

DATA_VERSION = 3465  # Minecraft 1.20.1
SIZES = [(20, 10, 20), (32, 16, 32)]
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "gametest", "resources",
                   "data", "yperite", "structures")

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def int_tag(key, value):
    return bytes([TAG_INT]) + name(key) + struct.pack(">i", value)


def string_tag(key, value):
    return bytes([TAG_STRING]) + name(key) + name(value)


def int_list(key, values):
    return (bytes([TAG_LIST]) + name(key) + bytes([TAG_INT]) + struct.pack(">i", len(values))
            + b"".join(struct.pack(">i", v) for v in values))


def compound_list(key, items):
    body = bytes([TAG_LIST]) + name(key)
    if not items:
        return body + bytes([TAG_END]) + struct.pack(">i", 0)
    return body + bytes([TAG_COMPOUND]) + struct.pack(">i", len(items)) + b"".join(items)


def structure(size_x, size_y, size_z):
    palette = [string_tag("Name", "minecraft:stone") + bytes([TAG_END]),
               string_tag("Name", "minecraft:air") + bytes([TAG_END])]
    blocks = []
    for x in range(size_x):
        for z in range(size_z):
            blocks.append(int_list("pos", [x, 0, z]) + int_tag("state", 0) + bytes([TAG_END]))
    root = (int_tag("DataVersion", DATA_VERSION)
            + int_list("size", [size_x, size_y, size_z])
            + compound_list("palette", palette)
            + compound_list("blocks", blocks)
            + compound_list("entities", []))
    return bytes([TAG_COMPOUND]) + name("") + root + bytes([TAG_END])


def main():
    os.makedirs(OUT, exist_ok=True)
    for sx, sy, sz in SIZES:
        path = os.path.join(OUT, f"empty_{sx}x{sy}x{sz}.nbt")
        with gzip.open(path, "wb") as f:
            f.write(structure(sx, sy, sz))
        print("wrote", os.path.normpath(path))


if __name__ == "__main__":
    main()
