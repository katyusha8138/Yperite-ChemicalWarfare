#!/usr/bin/env python3
"""Writes every item icon (textures/item/<name>.png) from the pixel art in tools/icons, and the icon of the
empty Curios face slot (textures/slot/face.png).

Usage:
  python3 tools/build_icons.py                  # write the icons into the mod's assets
  python3 tools/build_icons.py --preview FILE   # also write one enlarged sheet of all of them
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "icons"))

import gear  # noqa: E402
import pixel  # noqa: E402
import wear  # noqa: E402

TEXTURES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "yperite", "textures")
ITEMS = os.path.join(TEXTURES, "item")
GASES = ("tear_gas", "chlorine", "blood_gas", "yperite", "nerve_gas", "blue_cross")


def icons():
    out = {
        "stahlhelm_m1916": wear.stahlhelm, "brodie_mk1": wear.brodie, "adrian_m15": wear.adrian,
        "berndorfer": wear.berndorfer, "adrian_m16_russian": wear.adrian_russian,
        "british_cap": wear.british_cap, "french_kepi": wear.french_kepi, "german_cap": wear.german_cap,
        "austrian_cap": wear.austrian_cap, "russian_cap": wear.russian_cap, "pickelhaube": wear.pickelhaube,
        "pickelhaube_covered": wear.pickelhaube_covered, "austrian_shako": wear.austrian_shako,
        "gas_mask": wear.gas_mask, "small_box_respirator": wear.small_box_respirator, "m2_mask": wear.m2_mask,
        "lederschutzmaske": wear.lederschutzmaske, "austrian_gas_mask": wear.austrian_gas_mask,
        "zelinsky_kummant_mask": wear.zelinsky_kummant_mask,
        "sappenpanzer": wear.sappenpanzer, "dayfield": wear.dayfield,
        "gas_filter_general": gear.gas_filter_general, "gas_filter_special": gear.gas_filter_special,
        "protective_hood": gear.protective_hood, "protective_jacket": gear.protective_jacket,
        "protective_trousers": gear.protective_trousers, "protective_boots": gear.protective_boots,
        "sealed_fabric": gear.sealed_fabric, "gas_detector": gear.gas_detector, "wind_vane": gear.wind_vane,
        "decon_solution": gear.decon_solution, "decon_powder": gear.decon_powder,
        "medicine_general": gear.medicine_general, "antidote_nerve": gear.antidote_nerve,
        "ointment_blister": gear.ointment_blister, "sabre": gear.sabre, "officer_whistle": gear.officer_whistle,
    }
    for army, feet in (("british", "puttees"), ("french", "puttees"), ("german", "puttees"),
                       ("austrian", "puttees"), ("russian", "boots")):
        for piece in ("tunic", "trousers", feet):
            out[f"{army}_{piece}"] = getattr(wear, f"{army}_{piece}")
    for gas in GASES:
        out[f"{gas}_grenade"] = lambda g=gas: gear.grenade(g)
        out[f"grenade_40mm_{gas}"] = lambda g=gas: gear.grenade_40mm(g)
        out[f"mortar_shell_{gas}"] = lambda g=gas: gear.mortar_shell(g)
        out[f"large_shell_{gas}"] = lambda g=gas: gear.large_shell(g)
    return out


def face_slot():
    """The empty Curios face slot, in the manner of vanilla's empty armour slots: a gas mask in light grey."""
    from PIL import Image
    rows = [
        "................",
        "................",
        ".....aaaaaa.....",
        "....aaaaaaaa....",
        "...aaaaaaaaaa...",
        "..aaabbaabbaaa..",
        "..aabccaabccaa..",
        "..aabccaabccaa..",
        "..aaabbaabbaaa..",
        "...aaaaddaaaa...",
        "....adddddda....",
        "....dddddddd....",
        ".....dddddd.....",
        "......dddd......",
        "................",
        "................",
    ]
    colours = {"a": (198, 198, 198, 255), "b": (139, 139, 139, 255), "c": (110, 110, 110, 255), "d": (160, 160, 160, 255)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), colours[ch])
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    args = ap.parse_args()
    drawn = {name: make() for name, make in icons().items()}
    for name, image in drawn.items():
        image.save(os.path.join(ITEMS, name + ".png"))
    face_slot().save(os.path.join(TEXTURES, "slot", "face.png"))
    missing = sorted(f[:-4] for f in os.listdir(ITEMS) if f.endswith(".png") and f[:-4] not in drawn)
    print(f"{len(drawn)} icons written" + (f"; not drawn here: {', '.join(missing)}" if missing else ""))
    if args.preview:
        pixel.sheet(list(drawn.values()), scale=6, columns=12).save(args.preview)


if __name__ == "__main__":
    main()
