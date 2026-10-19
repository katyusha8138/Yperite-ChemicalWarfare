#!/usr/bin/env python3
"""Builds the GeckoLib models and textures of worn equipment, and preview sheets to judge them.

Usage:
  python3 tools/build_models.py                 # build everything into the mod's assets
  python3 tools/build_models.py --preview DIR   # also write a preview sheet per model into DIR
  python3 tools/build_models.py --only NAME     # build only one model

Pieces worn together are checked against each other (fit.py), in the positions the game puts them in: a mask
under every helmet, every tunic under the body armour, trousers under the tunics and into the puttees, and the
Small Box Respirator's haversack over tunics and armour. The build fails if one piece shows through another.
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "models"))

import body_armor  # noqa: E402
import fit  # noqa: E402
import helmets  # noqa: E402
import masks  # noqa: E402
import uniforms  # noqa: E402
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
    "sappenpanzer": (body_armor.sappenpanzer, None),
    "dayfield": (body_armor.dayfield, None),
}
NATIONS = ("british", "french", "german", "austrian")
for _nation in NATIONS:
    for _piece in ("tunic", "trousers", "puttees"):
        MODELS[f"{_nation}_{_piece}"] = (getattr(uniforms, f"{_nation}_{_piece}"), None)

# How far each piece stands off the chest, front and back, for what is worn over it (the game has the same
# numbers in the items): tunics, then body armour.
STAND_OFF = {f"{n}_tunic": (uniforms.STAND_OFF["front"], uniforms.STAND_OFF["back"]) for n in NATIONS}
STAND_OFF.update(body_armor.STAND_OFF)


MASKS = {"gas_mask", "small_box_respirator", "m2_mask", "lederschutzmaske", "austrian_gas_mask"}
HELMETS = {"stahlhelm_m1916", "brodie_mk1", "adrian_m15", "berndorfer"}


def _worn(model, front, back):
    """The model with its layer bones moved out as the game moves them over what is worn underneath."""
    return fit.shifted(model, {"layer_front": (0, 0, -front), "layer_back": (0, 0, back)})


def check_fit(built):
    """Every pair of pieces that can be worn together, in their worn positions: report where the inner one comes
    out through the outer one."""
    pairs = []  # (outer name, outer model, inner name, inner model, tuck)
    up = (0.0, 1.0, 0.0)
    for h in HELMETS:
        for k in MASKS:
            pairs.append((h, built[h], k, built[k], None))
    armours = [a for a in body_armor.STAND_OFF]
    for n in NATIONS:
        tunic, trousers, puttees = built[f"{n}_tunic"], built[f"{n}_trousers"], built[f"{n}_puttees"]
        tf, tb = STAND_OFF[f"{n}_tunic"]
        pairs.append((f"{n}_tunic", tunic, f"{n}_trousers", trousers, None))
        pairs.append((f"{n}_puttees", puttees, f"{n}_trousers", trousers, up))
        sbr = built["small_box_respirator"]
        pairs.append(("small_box_respirator", _worn(sbr, tf, tb), f"{n}_tunic", tunic, None))
        for a in armours:
            af, ab = STAND_OFF[a]
            armour = _worn(built[a], tf, tb)
            pairs.append((a, armour, f"{n}_tunic", tunic, None))
            pairs.append(("small_box_respirator", _worn(sbr, tf + af, tb + ab), a, armour, None))
    for a in armours:
        pairs.append(("small_box_respirator", _worn(built["small_box_respirator"], *STAND_OFF[a]), a, built[a], None))
    bad = 0
    for outer_name, outer, inner_name, inner, tuck in pairs:
        crossings = fit.piercings(outer, inner, tuck=tuck or (0.0, -1.0, 0.0))
        if crossings:
            bad += 1
            _, _, point, _ = crossings[0]
            print(f"FIT: {inner_name} shows through {outer_name} in {len(crossings)} places, "
                  f"e.g. at {tuple(round(float(c), 2) for c in point)}")
    return bad


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
    built = {}
    for name, (build, icon_view) in MODELS.items():
        if args.only and name != args.only:
            continue
        model = build()
        built[name] = model
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
    if not args.only:
        if check_fit(built):
            sys.exit("some pieces show through others")
        print("fit: every piece fits under what is worn over it")


if __name__ == "__main__":
    main()
