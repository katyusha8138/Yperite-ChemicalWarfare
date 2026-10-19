"""Shared parts of worn models: the GeckoLib armour skeleton and the player's body boxes.

Bone names and joint positions follow GeckoLib's GeoArmorRenderer: it finds the bones by these names and copies the
pose of the player's body parts onto them, so the pivots must sit on the vanilla joints.
"""
import numpy as np

from geo import Model, Voxels

HEAD = ((-4, 24, -4), (4, 32, 4))
BODY = ((-4, 12, -2), (4, 24, 2))
RIGHT_ARM = ((-8, 12, -2), (-4, 24, 2))
LEFT_ARM = ((4, 12, -2), (8, 24, 2))
RIGHT_LEG = ((-3.9, 0, -2), (0.1, 12, 2))
LEFT_LEG = ((-0.1, 0, -2), (3.9, 12, 2))

BONES = {
    "armorHead": (0, 24, 0),
    "armorBody": (0, 24, 0),
    "armorRightArm": (-5, 22, 0),
    "armorLeftArm": (5, 22, 0),
    "armorRightLeg": (-1.9, 12, 0),
    "armorLeftLeg": (1.9, 12, 0),
    "armorRightBoot": (-1.9, 12, 0),
    "armorLeftBoot": (1.9, 12, 0),
}


def armor_model(identifier):
    m = Model(identifier)
    for name, pivot in BONES.items():
        m.bone(name, pivot=pivot)
    return m


def box_mask(vox, lo, hi):
    """Cells whose centres lie inside the box."""
    X, Y, Z = vox.centers()
    return (X > lo[0]) & (X < hi[0]) & (Y > lo[1]) & (Y < hi[1]) & (Z > lo[2]) & (Z < hi[2])


def superellipse(dx, dz, ax, az, p=2.5):
    with np.errstate(divide="ignore", invalid="ignore"):
        v = (np.abs(dx) / np.maximum(ax, 1e-6)) ** p + (np.abs(dz) / np.maximum(az, 1e-6)) ** p
    return v ** (1.0 / p)


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)
