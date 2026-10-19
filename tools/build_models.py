#!/usr/bin/env python3
"""Builds the GeckoLib models and textures of worn equipment, and preview sheets to judge them.

Usage:
  python3 tools/build_models.py                 # build everything into the mod's assets
  python3 tools/build_models.py --preview DIR   # also write a preview sheet per model into DIR
  python3 tools/build_models.py --only NAME     # build only one model
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "models"))

import helmets  # noqa: E402
import masks  # noqa: E402
from mannequin import mannequin  # noqa: E402
from paint import paint  # noqa: E402
from preview import icon, sheet  # noqa: E402

ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "yperite")

# name -> (builder, icon view: yaw, pitch, scale, centre)
MODELS = {
    "stahlhelm_m1916": (helmets.stahlhelm_m1916, None),
    "brodie_mk1": (helmets.brodie_mk1, None),
    "adrian_m15": (helmets.adrian_m15, None),
    "berndorfer": (helmets.berndorfer, None),
    "gas_mask": (masks.gas_mask, None),
    "small_box_respirator": (masks.small_box_respirator, None),
    "m2_mask": (masks.m2_mask, None),
    "lederschutzmaske": (masks.lederschutzmaske, None),
    "austrian_gas_mask": (masks.austrian_gas_mask, None),
}


MASKS = {"gas_mask", "small_box_respirator", "m2_mask", "lederschutzmaske", "austrian_gas_mask"}


def _write_item_model(name):
    import json
    path = os.path.join(ASSETS, "models", "item", name + ".json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "yperite:item/" + name}}, f, indent=2)
        f.write("\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    ap.add_argument("--only")
    args = ap.parse_args()
    body = mannequin() if args.preview else None
    for name, (build, icon_view) in MODELS.items():
        if args.only and name != args.only:
            continue
        model = build()
        texture = paint(model)
        geo_dir = os.path.join(ASSETS, "geo", "armor")
        tex_dir = os.path.join(ASSETS, "textures", "armor")
        os.makedirs(geo_dir, exist_ok=True)
        os.makedirs(tex_dir, exist_ok=True)
        model.write(os.path.join(geo_dir, name + ".geo.json"), os.path.join(tex_dir, name + ".png"), texture)
        if icon_view:
            yaw, pitch, scale, centre = icon_view
            item_dir = os.path.join(ASSETS, "textures", "item")
            icon([(model, texture)], yaw, pitch, scale, centre).save(os.path.join(item_dir, name + ".png"))
            _write_item_model(name)
        cubes = sum(len(b.cubes) for b in model.bones.values())
        print(f"{name}: {cubes} cubes, texture {model.tex_w}x{model.tex_h}")
        if args.preview:
            os.makedirs(args.preview, exist_ok=True)
            sheet([body, (model, texture)]).save(os.path.join(args.preview, name + ".png"))
            close_center = (0, 27, -2) if name in MASKS else (0, 29, 0)
            sheet([(model, texture)], scale=24, size=(360, 360), center=close_center).save(
                os.path.join(args.preview, name + "_close.png"))


if __name__ == "__main__":
    main()
