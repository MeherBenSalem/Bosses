"""The Grace Ember, redesigned: a weathered stone cairn inside a ring of standing stones, an ember bed of
glowing ash, and (drawn by the block-entity renderer) a floating golden flame, a pool of light on the ground and
a faint beam that marks the grace from afar.

Block model + textures go to textures/block/, the renderer's additive textures to textures/effect/ (premultiplied:
black is transparent under additive blending), the item icon to textures/item/.
"""
import math

import numpy as np
from PIL import Image

from block_models import blockstate, element, face, model
from common import (ASH, GOLD, GRACE, LICHEN, STONE_LIGHT, Canvas, grid_image, hexc, rl, save_png)

S = 16
GLOW_DATA = {'block_light': 15, 'sky_light': 15}

# Standing stones around the cairn: (centre x, centre z, width, depth, height, y-rotation, x-tilt)
RING = [
    (13.4, 8.6, 2.0, 2.5, 4.5, 22.5, 0),
    (11.2, 13.1, 2.5, 2.0, 3.0, -22.5, 0),
    (5.3, 13.4, 2.0, 2.0, 5.0, 45, 0),
    (2.6, 9.5, 2.0, 2.5, 2.5, 0, 0),
    (3.6, 4.0, 2.5, 2.0, 4.0, -45, 0),
    (8.6, 2.5, 2.0, 2.0, 3.5, 22.5, 0),
    (12.6, 3.9, 1.5, 1.5, 2.0, 0, 0),
]


def stone_texture():
    """Rows 0-9: standing-stone faces (chalky, gold-dusted carving, lichen). Rows 10-15: flagstone tops."""
    c = Canvas(S, S)
    for y in range(10):
        for x in range(S):
            tone = 3 if y else 4
            if y >= 7:
                tone = 2
            if x % 5 == 0 and y:
                tone -= 1
            c.put(x, y, STONE_LIGHT[max(0, tone)])
    for x, y in ((2, 3), (7, 5), (11, 2), (14, 6), (4, 7), (9, 8)):
        c.put(x, y, STONE_LIGHT[1])
        c.put(x + 1, y - 1, STONE_LIGHT[4])
    for x, y in ((1, 2), (6, 1), (12, 4), (13, 1), (3, 5)):      # gold dust in the carving
        c.put(x, y, GOLD[3])
    for x, y in ((5, 8), (10, 7), (15, 8), (0, 6)):
        c.put(x, y, LICHEN[1])
        c.put(x, y + 1, LICHEN[0])
    for y in range(10, 16):
        for x in range(S):
            tone = 3 if (x + y * 3) % 7 else 2
            if (x, y) in ((4, 12), (11, 14), (8, 11)):
                tone = 1
            c.put(x, y, STONE_LIGHT[tone])
    for x in range(2, 14):                                       # carved ring rune on the flagstone
        if x % 3:
            c.put(x, 12, GOLD[2])
    return c.image()


def ember_bed_texture():
    """Ash with glowing cracks: the light pooled under the floating flame."""
    c = Canvas(S, S)
    rng = np.random.default_rng(1731)
    for y in range(S):
        for x in range(S):
            c.put(x, y, ASH[1 if (x * 3 + y * 5) % 7 else 2])
    cracks = [((2, 8), (5, 7), (8, 8), (11, 6), (14, 7)), ((8, 2), (7, 5), (8, 8), (9, 11), (8, 14)),
              ((4, 3), (6, 5)), ((12, 12), (10, 10))]
    for path in cracks:
        for (x0, y0), (x1, y1) in zip(path, path[1:]):
            steps = max(abs(x1 - x0), abs(y1 - y0))
            for i in range(steps + 1):
                x = round(x0 + (x1 - x0) * i / max(1, steps))
                y = round(y0 + (y1 - y0) * i / max(1, steps))
                d = math.hypot(x - 8, y - 8)
                c.put(x, y, GRACE[5] if d < 2.5 else GRACE[4] if d < 5 else GRACE[3])
    for x, y in ((3, 12), (13, 3), (5, 10), (11, 4)):
        c.put(x, y, GRACE[2])
    for _ in range(6):
        x, y = rng.integers(0, S, 2)
        c.put(int(x), int(y), GRACE[1])
    return c.image()


def elements():
    T, B = '#stone', '#bed'
    els = [
        # flagstone disc
        element((3, 0, 2), (13, 1, 14), {'north': face((2, 10, 12, 11), T), 'south': face((3, 11, 13, 12), T),
                                         'east': face((1, 12, 13, 13), T), 'west': face((2, 13, 14, 14), T),
                                         'up': face((3, 10, 13, 16), T), 'down': face((3, 10, 13, 16), T, 'down')}),
        element((2, 0, 3), (14, 1, 13), {'north': face((1, 14, 13, 15), T), 'south': face((2, 15, 14, 16), T),
                                         'east': face((2, 10, 12, 11), T), 'west': face((3, 11, 13, 12), T),
                                         'up': face((2, 10, 14, 16), T)}),
        # ember bed (fullbright on Forge/NeoForge; the renderer's glow covers Fabric)
        element((5, 1, 5), (11, 1.25, 11), {'up': face((5, 5, 11, 11), B), 'north': face((5, 0, 11, 0.25), B),
                                            'south': face((5, 0, 11, 0.25), B), 'east': face((5, 0, 11, 0.25), B),
                                            'west': face((5, 0, 11, 0.25), B)}, shade=False, extra={
            'forge_data': GLOW_DATA, 'neoforge_data': GLOW_DATA}),
        # the cairn: three stones stacked a quarter-turn apart
        element((6.5, 1, 6.5), (9.5, 2.5, 9.5), {
            'north': face((0, 1, 3, 2.5), T), 'south': face((4, 1, 7, 2.5), T), 'east': face((8, 1, 11, 2.5), T),
            'west': face((12, 1, 15, 2.5), T), 'up': face((5, 10, 8, 13), T)},
            rotation={'origin': [8, 1, 8], 'axis': 'y', 'angle': 22.5}),
        element((7, 2.5, 7), (9, 3.75, 9), {
            'north': face((1, 3, 3, 4.25), T), 'south': face((5, 3, 7, 4.25), T), 'east': face((9, 3, 11, 4.25), T),
            'west': face((13, 3, 15, 4.25), T), 'up': face((9, 11, 11, 13), T)},
            rotation={'origin': [8, 2.5, 8], 'axis': 'y', 'angle': -22.5}),
        element((7.5, 3.75, 7.5), (8.5, 4.5, 8.5), {
            'north': face((2, 0, 3, 0.75), T), 'south': face((6, 0, 7, 0.75), T), 'east': face((10, 0, 11, 0.75), T),
            'west': face((14, 0, 15, 0.75), T), 'up': face((11, 12, 12, 13), T)},
            rotation={'origin': [8, 3.75, 8], 'axis': 'y', 'angle': 45}),
    ]
    for i, (cx, cz, w, d, h, ry, _) in enumerate(RING):
        frm = (cx - w / 2, 0, cz - d / 2)
        to = (cx + w / 2, h, cz + d / 2)
        u = (i * 3) % 12
        faces = {
            'north': face((u, 10 - h if h < 10 else 0, u + w, 10), T),
            'south': face((u + 1, 10 - h if h < 10 else 0, u + 1 + w, 10), T),
            'east': face((u + 2, 10 - h if h < 10 else 0, u + 2 + d, 10), T),
            'west': face((u + 3, 10 - h if h < 10 else 0, u + 3 + d, 10), T),
            'up': face((u, 0, u + w, d), T),
        }
        rot = {'origin': [cx, 0, cz], 'axis': 'y', 'angle': ry} if ry else None
        els.append(element(frm, to, faces, rotation=rot))
    # one fallen stone lying in the grass
    els.append(element((9.5, 0, 11.6), (13.5, 1.5, 13.1), {
        'north': face((0, 2, 4, 3.5), T), 'south': face((4, 2, 8, 3.5), T), 'east': face((8, 2, 9.5, 3.5), T),
        'west': face((10, 2, 11.5, 3.5), T), 'up': face((0, 0, 4, 1.5), T)},
        rotation={'origin': [11.5, 0, 12.35], 'axis': 'y', 'angle': -22.5}))
    return els


def icon():
    ink = {'1': GRACE[1], '2': GRACE[2], '3': GRACE[3], 'J': GRACE[4], 'K': GRACE[5],
           'L': STONE_LIGHT[5], 'M': STONE_LIGHT[4], 'N': STONE_LIGHT[3], 'D': STONE_LIGHT[2], 'E': STONE_LIGHT[1],
           'g': GOLD[3]}
    rows = [
        '.......2........',
        '......232.......',
        '.....23K32......',
        '.....3KJK3......',
        '....2JKKKJ2.....',
        '....3JKKKJ3.....',
        '.....3JKJ3......',
        '......212.......',
        '.......1........',
        '...M...NN...M...',
        '..MN..NLLN..NM..',
        '..MND.NMMN.DNM..',
        '.MNDDNNNNNNDDNM.',
        '..DEgEDDDDEgED..',
        '...EEEEEEEEEE...',
        '................',
    ]
    return grid_image(rows, ink, (14, 10, 22, 255))


# ------------------------------------------------------------------------------------------- effect textures
def _premult(rgb, a):
    return tuple(int(round(c * a)) for c in rgb)


def _image(w, h, fn):
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    for y in range(h):
        for x in range(w):
            r, g, b = fn(x, y)
            arr[y, x] = (min(255, r), min(255, g), min(255, b), 255)
    return Image.fromarray(arr, 'RGBA')


def halo():
    """Pool of light on the ground: soft gold falloff with a faint ring."""
    def fn(x, y):
        r = math.hypot(x + 0.5 - 32, y + 0.5 - 32) / 32
        a = max(0.0, 1 - r) ** 2.2
        a += 0.18 * math.exp(-((r - 0.62) / 0.06) ** 2)
        return _premult((255, 196, 96), min(1.0, a))
    return _image(64, 64, fn)


def core():
    def fn(x, y):
        r = math.hypot(x + 0.5 - 16, y + 0.5 - 16) / 16
        a = max(0.0, 1 - r) ** 1.6
        hot = max(0.0, 1 - r * 2.6)
        return _premult((255, 196 + int(44 * hot), 96 + int(110 * hot)), a)
    return _image(32, 32, fn)


def beam():
    """A faint shaft: brightest at the base, gone by the top; soft sides."""
    def fn(x, y):
        side = math.exp(-((x + 0.5 - 8) / 3.2) ** 2)
        v = (127 - y) / 127.0      # 1 at the bottom row, 0 at the top
        a = side * (v ** 1.35)
        return _premult((255, 205, 120), a)
    return _image(16, 128, fn)


def flame_strip(frames=8, w=32, h=64):
    """Eight frames of a wispy golden flame, pale gold at the base and fraying into orange tips."""
    sheet = Image.new('RGBA', (w * frames, h), (0, 0, 0, 255))
    for f in range(frames):
        ph = f / frames * 2 * math.pi

        def fn(x, y):
            t = 1 - (y + 0.5) / h                 # 0 at the bottom, 1 at the top
            sway = math.sin(ph + t * 5.0) * 2.2 * t + math.sin(ph * 2 + t * 9) * 0.8 * t
            cx = w / 2 + sway
            half = (w * 0.30) * math.sin(math.pi * min(1.0, (t + 0.08) / 1.02)) ** 0.8 * (1 - 0.55 * t)
            if half <= 0.1:
                return (0, 0, 0)
            dx = abs(x + 0.5 - cx) / half
            body = max(0.0, 1 - dx ** 2)
            flick = 0.85 + 0.15 * math.sin(ph * 3 + y * 0.6 + x * 0.3)
            a = body * (1 - t ** 1.4) * flick
            hot = max(0.0, 1 - t * 2.6) * (1 - dx)
            r = 255
            g = int(138 + 62 * (1 - t) + 30 * hot)
            b = int(34 + 34 * (1 - t) + 80 * hot)
            return _premult((r, g, b), min(1.0, a * 1.25))
        sheet.paste(_image(w, h, fn), (f * w, 0))
    return sheet


def build():
    save_png(stone_texture(), 'textures/block/grace_ember_stone.png')
    save_png(ember_bed_texture(), 'textures/block/grace_ember_bed.png')
    model('grace_ember', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                          'textures': {'stone': rl('block/grace_ember_stone'), 'bed': rl('block/grace_ember_bed'),
                                       'particle': rl('block/grace_ember_stone')},
                          'elements': elements()})
    blockstate('grace_ember', {'': {'model': rl('block/grace_ember')}})
    save_png(icon(), 'textures/item/grace_ember.png')
    save_png(halo(), 'textures/effect/grace_halo.png', sheet=False)
    save_png(core(), 'textures/effect/grace_core.png', sheet=False)
    save_png(beam(), 'textures/effect/grace_beam.png', sheet=False)
    save_png(flame_strip(), 'textures/effect/grace_flame.png', sheet=False)
