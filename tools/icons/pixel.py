"""Pixel-art item icons: drawn by hand at 16x16, shaded and outlined by rule.

An icon is a grid of 16 strings of 16 characters; '.' is empty. A legend maps every other character to a
(part, offset) pair: the part is a colour ramp (dark to light) with a name, the offset raises or lowers the tone of
that pixel by whole steps (a highlight, a crease). Pixels of the same part form one surface.

Light comes from the upper left, as on vanilla items: a pixel whose upper or left neighbour is not of its surface is
a lit edge and goes one tone up; one whose lower or right neighbour is not, a shaded edge, one tone down. A dark
outline - the darkest tone of the part beside it, darker still - runs round the whole shape on the four sides.
"""
from PIL import Image


def ramp(*codes):
    return [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) for c in codes]


class Part:
    def __init__(self, name, colours, base=None, bevel=True):
        self.name = name
        self.colours = colours
        self.base = (len(colours) - 1) // 2 if base is None else base
        self.bevel = bevel

    def tone(self, level):
        return self.colours[max(0, min(len(self.colours) - 1, level))]

    def outline(self):
        r, g, b = self.colours[0]
        return int(r * 0.55), int(g * 0.55), int(b * 0.55)


def tones(colours, digits="0123456789", name="paint"):
    """A legend for hand-shaded pixels: each digit is that tone of the ramp, as painted (no bevel)."""
    part = Part(name, colours, base=0, bevel=False)
    return {d: (part, i) for i, d in enumerate(digits[:len(colours)])}


def draw(rows, legend):
    """rows: 16 strings of 16 characters; legend: char -> Part or (Part, offset)."""
    assert len(rows) == 16, f"{len(rows)} rows"
    for r in rows:
        assert len(r) == 16, f"row {r!r} is {len(r)} wide"
    parts = {}
    for ch, v in legend.items():
        parts[ch] = v if isinstance(v, tuple) else (v, 0)

    def part_at(x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            ch = rows[y][x]
            if ch != ".":
                return parts[ch][0]
        return None

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            ch = rows[y][x]
            if ch == ".":
                continue
            part, offset = parts[ch]
            level = part.base + offset
            if part.bevel:
                up, left = part_at(x, y - 1), part_at(x - 1, y)
                down, right = part_at(x, y + 1), part_at(x + 1, y)
                lit = (up is not part) or (left is not part)
                shaded = (down is not part) or (right is not part)
                if lit and not shaded:
                    level += 1
                elif shaded and not lit:
                    level -= 1
            px[x, y] = (*part.tone(level), 255)
    # the outline: empty pixels next to the shape, in the darkest tone of the part beside them
    for y in range(16):
        for x in range(16):
            if rows[y][x] != ".":
                continue
            for dx, dy in ((0, 1), (1, 0), (0, -1), (-1, 0)):
                p = part_at(x + dx, y + dy)
                if p is not None:
                    px[x, y] = (*p.outline(), 255)
                    break
    return img


def sheet(icons, scale=8, columns=8, bg=(58, 60, 66)):
    """A preview: the icons enlarged, in a grid, with their names left out."""
    n = len(icons)
    rows = (n + columns - 1) // columns
    gap = 4
    out = Image.new("RGBA", (columns * (16 * scale + gap) + gap, rows * (16 * scale + gap) + gap), (*bg, 255))
    for k, im in enumerate(icons):
        x = gap + (k % columns) * (16 * scale + gap)
        y = gap + (k // columns) * (16 * scale + gap)
        out.alpha_composite(im.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y))
    return out
