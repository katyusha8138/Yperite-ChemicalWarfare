#!/usr/bin/env python3
"""Generates the placeholder textures and models for the protection items.

These are stand-ins so that everything can be seen in the game. Replace the PNG files in
src/main/resources/assets/yperite/textures/ with real art at any time; names and sizes stay the same:
  item icons 16x16, block textures 16x16, curios slot icon 16x16, armour layers 64x32.

Usage: python3 tools/gen_placeholder_assets.py
"""
import json
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "yperite")

OUT = (30, 30, 30, 255)          # outline
RUBBER = (170, 180, 60, 255)     # yellow-green protective material
RUBBER_D = (120, 130, 40, 255)
GREY = (150, 150, 155, 255)
GREY_D = (90, 90, 95, 255)
GLASS = (110, 200, 235, 255)
WHITE = (235, 235, 235, 255)
RED = (205, 60, 50, 255)
ORANGE = (235, 150, 40, 255)
GREEN = (80, 190, 90, 255)
BLUE = (60, 90, 170, 255)
YELLOW = (240, 210, 70, 255)
CLEAR = (0, 0, 0, 0)


def canvas(size=16):
    return Image.new("RGBA", (size, size), CLEAR)


def save(img, *parts):
    path = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def box(d, x0, y0, x1, y1, fill, outline=OUT):
    d.rectangle([x0, y0, x1, y1], fill=fill, outline=outline)


# ---------------------------------------------------------------- item icons
def gas_mask():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 3, 2, 12, 12, RUBBER)
    d.rectangle([4, 3, 11, 4], fill=RUBBER_D)
    for x in (5, 9):
        box(d, x, 5, x + 2, 7, GLASS)
    box(d, 6, 9, 9, 13, GREY)
    d.line([7, 10, 8, 10], fill=GREY_D)
    d.line([7, 12, 8, 12], fill=GREY_D)
    d.point([(2, 6), (13, 6)], fill=OUT)
    return img


def filter_can(body, stripe):
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 5, 3, 10, 13, body)
    box(d, 5, 2, 10, 3, GREY)
    d.rectangle([6, 7, 9, 8], fill=stripe)
    d.line([6, 11, 9, 11], fill=GREY_D)
    return img


def sealed_fabric():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 2, 3, 13, 12, RUBBER)
    for x in range(3, 13, 3):
        d.line([x, 4, x, 11], fill=RUBBER_D)
    for y in range(5, 12, 3):
        d.line([3, y, 12, y], fill=RUBBER_D)
    return img


def hood():
    img = canvas(); d = ImageDraw.Draw(img)
    d.rounded_rectangle([3, 2, 12, 13], radius=3, fill=RUBBER, outline=OUT)
    box(d, 5, 5, 10, 8, GLASS)
    d.line([4, 11, 11, 11], fill=RUBBER_D)
    return img


def jacket():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 4, 2, 11, 14, RUBBER)
    box(d, 1, 3, 4, 9, RUBBER)
    box(d, 11, 3, 14, 9, RUBBER)
    d.line([7, 3, 7, 13], fill=RUBBER_D)
    d.line([8, 3, 8, 13], fill=RUBBER_D)
    return img


def trousers():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 3, 2, 12, 5, RUBBER)
    box(d, 3, 5, 7, 14, RUBBER)
    box(d, 8, 5, 12, 14, RUBBER)
    d.line([3, 3, 12, 3], fill=RUBBER_D)
    return img


def boots():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 2, 4, 6, 12, RUBBER)
    box(d, 9, 4, 13, 12, RUBBER)
    box(d, 1, 11, 6, 13, RUBBER_D)
    box(d, 8, 11, 13, 13, RUBBER_D)
    return img


def detector():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 4, 3, 11, 14, GREY)
    box(d, 5, 4, 10, 8, GREEN)
    d.line([6, 7, 7, 5], fill=OUT); d.line([7, 5, 9, 6], fill=OUT)
    d.point([(6, 11), (9, 11)], fill=RED)
    d.line([10, 3, 12, 0], fill=OUT)
    return img


def wind_vane():
    img = canvas(); d = ImageDraw.Draw(img)
    d.line([7, 5, 7, 14], fill=GREY_D, width=2)
    d.polygon([(2, 4), (10, 4), (13, 2), (10, 0), (2, 0)], fill=RED, outline=OUT)
    d.rectangle([5, 13, 10, 14], fill=GREY_D)
    return img


def bottle(liquid, label=None):
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 6, 1, 9, 3, GREY)
    box(d, 5, 4, 10, 14, (200, 225, 235, 255))
    d.rectangle([6, 8, 9, 13], fill=liquid)
    if label:
        d.rectangle([6, 5, 9, 6], fill=label)
    return img


def powder():
    img = canvas(); d = ImageDraw.Draw(img)
    d.polygon([(8, 2), (13, 13), (3, 13)], fill=WHITE, outline=OUT)
    d.line([5, 11, 11, 11], fill=GREY)
    box(d, 6, 1, 9, 3, GREY_D)
    return img


def antidote():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 3, 6, 11, 9, (210, 230, 240, 255))
    d.rectangle([4, 7, 9, 8], fill=(80, 200, 220, 255))
    d.line([11, 7, 14, 7], fill=OUT)
    d.line([1, 7, 3, 7], fill=OUT)
    box(d, 2, 5, 3, 10, GREY_D)
    return img


def ointment():
    img = canvas(); d = ImageDraw.Draw(img)
    box(d, 3, 7, 12, 13, ORANGE)
    box(d, 3, 5, 12, 7, GREY)
    d.line([4, 10, 11, 10], fill=YELLOW)
    return img


ICONS = {
    "gas_mask": gas_mask,
    "gas_filter_general": lambda: filter_can((130, 150, 70, 255), WHITE),
    "gas_filter_special": lambda: filter_can(BLUE, YELLOW),
    "sealed_fabric": sealed_fabric,
    "protective_hood": hood,
    "protective_jacket": jacket,
    "protective_trousers": trousers,
    "protective_boots": boots,
    "gas_detector": detector,
    "wind_vane": wind_vane,
    "decon_solution": lambda: bottle((120, 200, 255, 255), WHITE),
    "decon_powder": powder,
    "medicine_general": lambda: bottle(GREEN, WHITE),
    "antidote_nerve": antidote,
    "ointment_blister": ointment,
}
HANDHELD = {"gas_detector", "wind_vane"}


# ---------------------------------------------------------------- block, slot icon
def alarm_block(lit):
    img = canvas(); d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], fill=GREY, outline=GREY_D)
    d.rectangle([1, 1, 14, 14], outline=(185, 185, 190, 255))
    for p in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.point(p, fill=GREY_D)
    lamp = (255, 200, 90, 255) if lit else (110, 40, 40, 255)
    d.ellipse([4, 4, 11, 11], fill=lamp, outline=OUT)
    if lit:
        d.ellipse([6, 6, 9, 9], fill=(255, 245, 190, 255))
    d.rectangle([5, 12, 10, 13], fill=GREY_D)
    return img


def face_slot():
    img = canvas(); d = ImageDraw.Draw(img)
    c = (139, 139, 139, 255)
    d.rounded_rectangle([3, 2, 12, 13], radius=3, outline=c, fill=(198, 198, 198, 255))
    d.rectangle([5, 5, 6, 7], fill=c)
    d.rectangle([9, 5, 10, 7], fill=c)
    d.rectangle([6, 9, 9, 11], fill=c)
    return img


# ---------------------------------------------------------------- armour layers (64x32)
def armor_layers():
    # Head: x 0..32, y 0..16 (front face at 8..16 x 8..16). Body: 16..40 x 16..32. Arms: 40..56 x 16..32.
    # Legs: 0..16 x 16..32.
    mask = Image.new("RGBA", (64, 32), CLEAR); d = ImageDraw.Draw(mask)
    d.rectangle([8, 8, 15, 15], fill=RUBBER, outline=OUT)
    d.rectangle([9, 10, 10, 11], fill=GLASS)
    d.rectangle([13, 10, 14, 11], fill=GLASS)
    d.rectangle([11, 13, 12, 15], fill=GREY)
    for x0 in (0, 16, 24):
        d.rectangle([x0, 10, x0 + 7, 11], fill=RUBBER_D)
    d.rectangle([8, 8, 15, 9], fill=RUBBER_D)
    save(mask, "textures", "models", "armor", "gas_mask_layer_1.png")

    suit1 = Image.new("RGBA", (64, 32), CLEAR); d = ImageDraw.Draw(suit1)
    d.rectangle([0, 0, 31, 15], fill=RUBBER)                       # hood
    d.rectangle([9, 9, 14, 12], fill=GLASS)                         # visor
    d.rectangle([16, 16, 39, 31], fill=RUBBER)                      # jacket body
    d.rectangle([40, 16, 55, 31], fill=RUBBER)                      # sleeves
    d.rectangle([20, 20, 35, 21], fill=RUBBER_D)
    d.rectangle([0, 26, 15, 31], fill=RUBBER_D)                     # boots
    save(suit1, "textures", "models", "armor", "protective_suit_layer_1.png")

    suit2 = Image.new("RGBA", (64, 32), CLEAR); d = ImageDraw.Draw(suit2)
    d.rectangle([0, 16, 15, 31], fill=RUBBER)                       # trousers
    d.rectangle([16, 26, 39, 31], fill=RUBBER_D)                    # waist
    d.rectangle([0, 17, 15, 17], fill=RUBBER_D)
    save(suit2, "textures", "models", "armor", "protective_suit_layer_2.png")


# ---------------------------------------------------------------- models
def write_json(obj, *parts):
    path = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    for name, make in ICONS.items():
        save(make(), "textures", "item", f"{name}.png")
        parent = "minecraft:item/handheld" if name in HANDHELD else "minecraft:item/generated"
        write_json({"parent": parent, "textures": {"layer0": f"yperite:item/{name}"}}, "models", "item", f"{name}.json")

    save(alarm_block(False), "textures", "block", "gas_alarm.png")
    save(alarm_block(True), "textures", "block", "gas_alarm_on.png")
    write_json({"parent": "minecraft:block/cube_all", "textures": {"all": "yperite:block/gas_alarm"}}, "models", "block", "gas_alarm.json")
    write_json({"parent": "minecraft:block/cube_all", "textures": {"all": "yperite:block/gas_alarm_on"}}, "models", "block", "gas_alarm_on.json")
    write_json({"parent": "yperite:block/gas_alarm"}, "models", "item", "gas_alarm.json")
    variants = {f"power={p}": {"model": "yperite:block/gas_alarm" if p == 0 else "yperite:block/gas_alarm_on"} for p in range(16)}
    write_json({"variants": variants}, "blockstates", "gas_alarm.json")

    save(face_slot(), "textures", "slot", "face.png")
    armor_layers()
    print("wrote placeholder assets to", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
