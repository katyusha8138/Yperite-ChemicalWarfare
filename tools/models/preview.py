"""A small software renderer for models built with geo.py, to judge shapes and textures without the game.

It draws textured faces with a z-buffer in orthographic projection and shades them roughly like Minecraft does
for entities (lighter on top, darker below and on the sides). Front view: the viewer faces the model's front
(-Z), so the wearer's right (-X) is on the left of the picture.
"""
import math

import numpy as np
from PIL import Image

from geo import Transforms, face_geometry

LIGHT = np.array([0.35, 1.0, -0.6])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def _rot_y(deg):
    a = math.radians(deg)
    return np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])


def _rot_x(deg):
    a = math.radians(deg)
    return np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])


def render(models_and_textures, yaw=0.0, pitch=0.0, scale=12, size=(360, 420), center=(0, 20, 0), bg=(46, 48, 52)):
    """models_and_textures: list of (model, PIL image). yaw turns the model (degrees), pitch tilts the view."""
    W, H = size
    color = np.zeros((H, W, 3), dtype=float)
    color[:] = bg
    depth = np.full((H, W), np.inf)
    R = _rot_x(pitch) @ _rot_y(yaw)
    c = np.array(center, dtype=float)
    for model, tex in models_and_textures:
        texarr = np.asarray(tex.convert("RGBA")).astype(float)
        transforms = Transforms(model)
        for name in model.order:
            for cube in model.bones[name].cubes:
                for face, (u0, v0, w, h) in cube.uv.items():
                    corners, normal = face_geometry(model, name, cube, face, transforms)
                    pts = [R @ (p - c) for p in corners]
                    n = R @ normal
                    if n[2] >= 0:          # faces turned away from the viewer (viewer looks along +Z)
                        continue
                    shade = 0.55 + 0.45 * max(0.0, float(normal @ LIGHT)) + 0.1 * normal[1]
                    _raster(color, depth, pts, texarr, (u0, v0, w, h), shade, scale, W, H)
    return Image.fromarray(np.clip(color, 0, 255).astype(np.uint8), "RGB")


def _raster(color, depth, pts, tex, uv, shade, scale, W, H):
    lt, rt, rb, lb = pts
    # screen: x to the right is +X (the model's left in front view), y down is -Y
    def scr(p):
        return np.array([W / 2 + p[0] * scale, H / 2 - p[1] * scale])
    a, b, d = scr(lt), scr(rt), scr(lb)
    ex, ey = b - a, d - a
    det = ex[0] * ey[1] - ex[1] * ey[0]
    if abs(det) < 1e-9:
        return
    quad = [scr(p) for p in pts]
    xs = [q[0] for q in quad]
    ys = [q[1] for q in quad]
    x0, x1 = max(0, int(math.floor(min(xs)))), min(W - 1, int(math.ceil(max(xs))))
    y0, y1 = max(0, int(math.floor(min(ys)))), min(H - 1, int(math.ceil(max(ys))))
    if x0 > x1 or y0 > y1:
        return
    gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
    rx, ry = gx - a[0], gy - a[1]
    s = (rx * ey[1] - ry * ey[0]) / det
    t = (ex[0] * ry - ex[1] * rx) / det
    inside = (s >= 0) & (s <= 1) & (t >= 0) & (t <= 1)
    if not inside.any():
        return
    z = lt[2] + (rt[2] - lt[2]) * s + (lb[2] - lt[2]) * t
    u0, v0, w, h = uv
    tu = np.clip((u0 + s * w).astype(int), u0, u0 + w - 1)
    tv = np.clip((v0 + t * h).astype(int), v0, v0 + h - 1)
    texel = tex[tv, tu]
    opaque = texel[..., 3] > 8
    sub_depth = depth[y0:y1 + 1, x0:x1 + 1]
    closer = inside & opaque & (z < sub_depth - 1e-6)
    sub_depth[closer] = z[closer]
    sub_color = color[y0:y1 + 1, x0:x1 + 1]
    sub_color[closer] = texel[..., :3][closer] * shade


def sheet(models_and_textures, views=((0, 0), (35, 8), (90, 0), (160, 8), (-35, -25)), scale=12, size=(300, 360),
          center=(0, 22, 0)):
    tiles = [render(models_and_textures, yaw, pitch, scale, size, center) for yaw, pitch in views]
    out = Image.new("RGB", (size[0] * len(tiles), size[1]))
    for k, tile in enumerate(tiles):
        out.paste(tile, (k * size[0], 0))
    return out
