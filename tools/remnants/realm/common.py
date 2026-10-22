"""Shared paths, ramps and pixel helpers for the Eclipsed Realm asset generators.

Everything here is deterministic: generators seed their own RNGs and write the same bytes on every
run. Files go into BOTH resource trees (1.20.1 and 1.21.1 share the asset format) through
`save_png` / `save_json` / `save_bytes`, which also record what was written for the build report.
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
TREES = [ROOT / v / 'common/src/main/resources/assets/remnant_bosses' for v in ('1.20.1', '1.21.1')]
DOCS = ROOT / 'docs/models/remnants/realm'
NS = 'remnant_bosses'

WRITTEN = []          # asset-relative paths written this run (for the report)
TEXTURES = {}         # asset-relative texture path -> PIL image (for the contact sheet)


def hexc(value, alpha=255):
    value = value.lstrip('#')
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def ramp(*values):
    return [hexc(v) for v in values]


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


def with_alpha(c, a):
    return (c[0], c[1], c[2], int(a))


# Material ramps: 3-6 tones, hue-shifted (cool shadows, warm lights).
STONE = ramp('#27252F', '#34323F', '#42404E', '#524F5F', '#646072', '#7A7588')      # eclipse stone: slate, a breath of violet
STONE_LIGHT = ramp('#3C3A46', '#4C4957', '#5E5B69', '#716E7D', '#878392', '#9E9AA8')  # weathered/chalky variant
MORTAR = ramp('#17141E', '#211D2A')
GOLD = ramp('#3E2F16', '#5A4520', '#8A6A2E', '#B8913F', '#D9B860', '#F2DA8C')         # tarnished gold (Vael)
GRACE = ramp('#7A4A12', '#B8781E', '#E0A838', '#F6D068', '#FFF0B0', '#FFFCE8')        # living grace light
SOUL = ramp('#2E8C8C', '#4FC9C1', '#9CF2E6', '#E0FFFA')                                # Remnant light
VIOLET = ramp('#0B0812', '#1B1230', '#2E1F52', '#4A3484', '#7158B8', '#A890E8')       # eclipse violet
ECLIPSE_BLACK = hexc('#07050C')
BARK = ramp('#454550', '#626069', '#827E7E', '#A39D94', '#C4BDAE')                    # pale ash bark
WOOD = ramp('#6E5838', '#8F7650', '#AE9468', '#C8AF80', '#DEC89C')                    # warm pale wood
LEAF = ramp('#6B4612', '#9A6A1C', '#C79428', '#E8BC42', '#F8DC74', '#FFF2B8')         # gilded leaves
STEEL = ramp('#262A34', '#424955', '#68707E', '#98A0AC', '#CDD4DE', '#F0F4F8')
ASH = ramp('#29241F', '#3A332C', '#4C443A', '#605648', '#766A58')                     # ash earth
LICHEN = ramp('#56603A', '#76804A', '#98A060')
GRAVE = ramp('#423E4B', '#585362', '#6F6A79', '#8A8593', '#A6A1AD', '#C2BDC7')       # gravestone stone


class Canvas:
    """RGBA pixel canvas over a numpy array. Coordinates x right, y down."""

    def __init__(self, w, h, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), dtype=np.uint8)
        self.a[:, :] = fill

    def put(self, x, y, c, wrap=False):
        x, y = int(x), int(y)
        if wrap:
            x %= self.w
            y %= self.h
        if 0 <= x < self.w and 0 <= y < self.h:
            if len(c) == 3:
                c = (*c, 255)
            self.a[y, x] = c

    def get(self, x, y, wrap=False):
        x, y = int(x), int(y)
        if wrap:
            x %= self.w
            y %= self.h
        return tuple(int(v) for v in self.a[y, x])

    def rect(self, x0, y0, x1, y1, c):
        """Fill [x0, x1) x [y0, y1)."""
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, c)

    def blend(self, x, y, c, t, wrap=False):
        x, y = int(x), int(y)
        if wrap:
            x %= self.w
            y %= self.h
        if 0 <= x < self.w and 0 <= y < self.h:
            old = self.get(x, y)
            self.a[y, x] = (*mix(old[:3], c[:3], t), old[3])

    def image(self):
        return Image.fromarray(self.a, 'RGBA')

    def copy(self):
        c = Canvas(self.w, self.h)
        c.a = self.a.copy()
        return c


def grid_image(rows, ink, outline=None):
    """Character grid -> image. `ink` maps characters to RGBA; '.' and ' ' are transparent."""
    h, w = len(rows), max(len(r) for r in rows)
    c = Canvas(w, h)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ink:
                c.put(x, y, ink[ch])
    im = c.image()
    if outline:
        im = outlined(im, outline)
    return im


def outlined(im, colour=(12, 10, 18, 255)):
    """Selective dark outline: transparent pixels touching the sprite get the outline colour."""
    src = np.array(im)
    out = src.copy()
    h, w = src.shape[:2]
    for y in range(h):
        for x in range(w):
            if src[y, x, 3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[ny, nx, 3] > 100:
                    out[y, x] = colour
                    break
    return Image.fromarray(out, 'RGBA')


def _record(rel):
    if rel not in WRITTEN:
        WRITTEN.append(rel)


def save_png(im, rel, sheet=True):
    for tree in TREES:
        out = tree / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        im.save(out, optimize=False)
    _record(rel)
    if sheet:
        TEXTURES[rel] = im
    return rel


def save_json(obj, rel):
    text = json.dumps(obj, indent=2) + '\n'
    for tree in TREES:
        out = tree / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(text, encoding='utf-8', newline='\n')
    _record(rel)
    return rel


def save_bytes(data, rel):
    for tree in TREES:
        out = tree / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(data)
    _record(rel)
    return rel


def rl(path):
    """Resource location in this mod's namespace."""
    return f'{NS}:{path}'
