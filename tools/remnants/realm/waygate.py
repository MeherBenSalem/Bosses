"""The Eclipse Waygate: GeckoLib block geometry, pixel-art atlas + glowmask, and its three clips.

Geometry is authored in render ("display") space - the space GeckoLib draws in after it mirrors
bedrock X - and exported to bedrock JSON with x' = -x, so what the preview shows is what the game
shows. Model origin = the core block's bottom centre; x in [-24, 24], y in [0, 64], front at -Z.
`pillar_left` is on the left of a player standing in front of the gate (display +X).

Doorway: x in [-10, 10] between the shafts, y from the 3 px threshold to the corbels (y 38-45).
Layers inside it, front to back: ring_outer (z -2.5..-1.5), ring_inner (z -1..0), vortex_c (z 0.5),
vortex_b (z 1.5), vortex_a (z 2.5). The vortex discs are r = 22 circles centred on (0, 23.5);
whatever lies outside the doorway is buried inside the pillars, threshold and lintel.
"""
import math
import random

import numpy as np

from common import (GOLD, GRACE, SOUL, STONE, VIOLET, ECLIPSE_BLACK, Canvas, save_json,
                    save_png, with_alpha)

ATLAS_W = 256
DOOR_C = (0.0, 23.5)
VORTEX_R = 22
DISC_C = (0.0, 53.0)

GLYPHS = {
    'p0': ['..##..', '.#..#.', '#.##.#', '#.##.#', '.#..#.', '..##..', '..##..'],
    'p1': ['..##..', '.####.', '#.##.#', '..##..', '.#..#.', '#....#', '#....#'],
    'p2': ['.####.', '#....#', '#.##.#', '#....#', '.####.', '..##..', '.####.'],
    'p3': ['#.##.#', '#.##.#', '######', '#....#', '#.##.#', '#.##.#', '##..##'],
    'l0': ['.###.', '#...#', '#.#.#', '#...#', '.###.'],
    'l1': ['#.#.#', '.###.', '##.##', '.###.', '#.#.#'],
}
PILLAR_GLYPH_Y = (8, 16, 24, 32)          # bottom of each 7-tall glyph on the shafts
LINTEL_GLYPHS = ((18.5, 'l0'), (12.5, 'l1'))  # |x| centre, glyph (outer pair lights first)
LINTEL_GLYPH_Y = 48


class Model:
    def __init__(self):
        self.bones = []
        self.cubes = []
        self.regions = {}

    def bone(self, name, pivot, parent='root'):
        self.bones.append({'name': name, 'pivot': tuple(pivot), 'parent': parent if name != 'root' else None})

    def region(self, key, w, h, painter):
        if key not in self.regions:
            self.regions[key] = {'w': int(math.ceil(w - 1e-6)), 'h': int(math.ceil(h - 1e-6)), 'painter': painter}
        return key

    def cube(self, bone, lo, hi, faces, rot=None, pivot=None):
        """faces: face -> region key (already registered)."""
        self.cubes.append({'bone': bone, 'lo': tuple(lo), 'hi': tuple(hi), 'faces': faces,
                           'rot': rot, 'pivot': pivot})

    def box(self, bone, name, lo, hi, painters, rot=None, pivot=None, skip=()):
        """A full cube; painters: face -> painter(w, h) or a default under key '*'."""
        sx, sy, sz = (b - a for a, b in zip(lo, hi))
        dims = {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy), 'up': (sx, sz), 'down': (sx, sz)}
        faces = {}
        for f, (w, h) in dims.items():
            if f in skip or w <= 0 or h <= 0:
                continue
            p = painters.get(f, painters.get('*'))
            faces[f] = self.region(f'{name}.{f}', w, h, p)
        self.cube(bone, lo, hi, faces, rot, pivot)

    def pack(self):
        order = sorted(self.regions.items(), key=lambda kv: (-kv[1]['h'], -kv[1]['w'], kv[0]))
        x = y = shelf_h = 0
        for key, r in order:
            if x + r['w'] > ATLAS_W:
                x, y = 0, y + shelf_h
                shelf_h = 0
            r['u'], r['v'] = x, y
            x += r['w']
            shelf_h = max(shelf_h, r['h'])
        used = y + shelf_h
        self.atlas_h = next(h for h in (64, 128, 256, 512) if h >= used)
        return used

    def paint(self):
        base = Canvas(ATLAS_W, self.atlas_h)
        glow = Canvas(ATLAS_W, self.atlas_h)
        for key, r in self.regions.items():
            b, g = r['painter'](r['w'], r['h'])
            base.a[r['v']:r['v'] + r['h'], r['u']:r['u'] + r['w']] = b.a
            if g is not None:
                glow.a[r['v']:r['v'] + r['h'], r['u']:r['u'] + r['w']] = g.a
        # emissive pixels only; transparent elsewhere with zero RGB so additive layers add nothing
        glow.a[glow.a[..., 3] == 0] = 0
        return base.image(), glow.image()

    def geo_json(self):
        bones = []
        for b in self.bones:
            px, py, pz = b['pivot']
            jb = {'name': b['name']}
            if b['parent']:
                jb['parent'] = b['parent']
            jb['pivot'] = [r4(-px), r4(py), r4(pz)]
            cubes = []
            for c in self.cubes:
                if c['bone'] != b['name']:
                    continue
                lo, hi = c['lo'], c['hi']
                jc = {'origin': [r4(-hi[0]), r4(lo[1]), r4(lo[2])],
                      'size': [r4(hi[0] - lo[0]), r4(hi[1] - lo[1]), r4(hi[2] - lo[2])]}
                if c['rot']:
                    cpx, cpy, cpz = c['pivot']
                    rx, ry, rz = c['rot']
                    jc['pivot'] = [r4(-cpx), r4(cpy), r4(cpz)]
                    jc['rotation'] = [r4(-rx), r4(-ry), r4(rz)]
                uv = {}
                sx, sy, sz = (h - l for l, h in zip(lo, hi))
                dims = {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy), 'up': (sx, sz), 'down': (sx, sz)}
                for f in ('north', 'east', 'south', 'west', 'up', 'down'):
                    if f not in c['faces']:
                        continue
                    r = self.regions[c['faces'][f]]
                    w, h = dims[f]
                    if f in ('up', 'down'):
                        uv[f] = {'uv': [r4(r['u'] + w), r4(r['v'] + h)], 'uv_size': [r4(-w), r4(-h)]}
                    else:
                        uv[f] = {'uv': [r4(r['u']), r4(r['v'])], 'uv_size': [r4(w), r4(h)]}
                jc['uv'] = uv
                cubes.append(jc)
            if cubes:
                jb['cubes'] = cubes
            bones.append(jb)
        return {
            'format_version': '1.12.0',
            'minecraft:geometry': [{
                'description': {
                    'identifier': 'geometry.eclipse_waygate',
                    'texture_width': ATLAS_W,
                    'texture_height': self.atlas_h,
                    'visible_bounds_width': 4,
                    'visible_bounds_height': 4.5,
                    'visible_bounds_offset': [0, 2, 0],
                },
                'bones': bones,
            }],
        }


def r4(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def _c(w, h, fill=(0, 0, 0, 0)):
    return Canvas(w, h, fill)


def stone_block(seed, gold_row=None, chips=2, light=0, ramp=STONE):
    """Bevelled block face: lit top/left edge, shadowed bottom/right edge, a few chips."""
    def p(w, h):
        rng = random.Random(seed * 131 + w * 7 + h)
        m = 3 + light
        c = _c(w, h, ramp[m])
        for x in range(w):
            c.put(x, 0, ramp[min(5, m + 1)])
            if h > 2:
                c.put(x, h - 1, ramp[m - 2])
        for y in range(1, h - 1):
            c.put(0, y, ramp[min(5, m + 1)] if h > 3 else ramp[m])
            if w > 3:
                c.put(w - 1, y, ramp[m - 1])
        for _ in range(chips if w > 3 and h > 2 else 0):
            x, y = rng.randrange(w), rng.choice((0, h - 1))
            c.put(x, y, ramp[m - 2])
            c.put(x + (1 if x < w - 1 else -1), y, ramp[m - 1])
        if gold_row is not None and h > 3:
            gy = gold_row if gold_row >= 0 else h + gold_row
            for x in range(w):
                c.put(x, gy, GOLD[4] if x % 4 == 1 else GOLD[3])
                if gy + 1 < h - 1:
                    c.put(x, gy + 1, GOLD[1] if x % 2 else ramp[m - 2])
        return c, None
    return p


def top_face(seed, ramp=STONE, worn=False):
    def p(w, h):
        rng = random.Random(seed)
        c = _c(w, h, ramp[3])
        for x in range(w):
            for y in range(h):
                if x in (0, w - 1) or y in (0, h - 1):
                    c.put(x, y, ramp[4])
        for _ in range(max(1, w * h // 40)):
            x, y = rng.randrange(1, max(2, w - 1)), rng.randrange(1, max(2, h - 1))
            c.put(x, y, ramp[2])
        if worn:
            for x in range(2, w - 2):
                for y in range(2, h - 2):
                    if abs(x - (w - 1) / 2) < w / 5:
                        c.put(x, y, ramp[4])
            for x in range(w):
                c.put(x, 1, GOLD[3] if x % 3 else GOLD[4])
        return c, None
    return p


def engrave(c, glyph, u0, v0, deep=STONE[1], lip=STONE[4], skip=None):
    bitmap = GLYPHS[glyph]
    for y, row in enumerate(bitmap):
        for x, ch in enumerate(row):
            if ch == '#':
                c.put(u0 + x, v0 + y, deep)
    for y, row in enumerate(bitmap):
        for x, ch in enumerate(row):
            if ch == '#':
                bx, by = x + 1, y + 1
                below = bitmap[by][x] if by < len(bitmap) else '.'
                if below != '#':
                    c.put(u0 + x, v0 + by, lip)


def monolith(seed, grooves=(), panel=2, crack=True, gold_rows=()):
    """Carved monolith face: bevel, recessed panel, engraved runes, chips, a hairline crack."""
    def p(w, h):
        rng = random.Random(seed)
        c = _c(w, h, STONE[3])
        # recessed panel: shadow on its top/left inner edge, light on its bottom/right inner edge
        x0, y0, x1, y1 = panel, panel, w - 1 - panel, h - 1 - panel
        for x in range(x0, x1 + 1):
            c.put(x, y0, STONE[2])
            c.put(x, y1, STONE[4])
        for y in range(y0, y1 + 1):
            c.put(x0, y, STONE[2])
            c.put(x1, y, STONE[4])
        c.put(x0, y1, STONE[3])
        c.put(x1, y0, STONE[3])
        # outer bevel
        for x in range(w):
            c.put(x, 0, STONE[4])
            c.put(x, h - 1, STONE[1])
        for y in range(1, h - 1):
            c.put(0, y, STONE[4])
            c.put(w - 1, y, STONE[2])
        c.put(0, 0, STONE[5])
        for gy in gold_rows:
            gy = gy if gy >= 0 else h + gy
            for x in range(1, w - 1):
                c.put(x, gy, GOLD[3] if x % 3 else GOLD[4])
        for glyph, u0, v0 in grooves:
            engrave(c, glyph, u0, v0)
        # chips along the arrises
        for _ in range(3):
            x = rng.randrange(1, w - 1)
            y = rng.choice((0, h - 1))
            c.put(x, y, STONE[2] if y == 0 else STONE[0])
        for _ in range(2):
            y = rng.randrange(2, h - 2)
            x = rng.choice((0, w - 1))
            c.put(x, y, STONE[1])
            c.put(x, y + 1, STONE[2])
        if crack and h > 12:
            x, y = rng.randrange(2, w - 2), rng.randrange(h // 2, h - 4)
            for _ in range(rng.randrange(4, 7)):
                c.put(x, y, STONE[1])
                y += 1
                x += rng.choice((-1, 0, 1))
                x = max(1, min(w - 2, x))
        # weathering: a darker band near the foot
        for x in range(1, w - 1):
            if (x * 5) % 7 < 3:
                c.put(x, h - 2, STONE[2])
        return c, None
    return p


def dressed_side(seed):
    def p(w, h):
        rng = random.Random(seed)
        c = _c(w, h, STONE[3])
        for x in range(w):
            c.put(x, 0, STONE[4])
            c.put(x, h - 1, STONE[1])
        for y in range(1, h - 1):
            c.put(0, y, STONE[2])
            c.put(w - 1, y, STONE[2])
        # tooling: short vertical drag marks in a staggered rhythm
        for x in range(2, w - 2, 3):
            for y in range(3 + (x % 2) * 4, h - 3, 9):
                c.put(x, y, STONE[2])
                c.put(x, y + 1, STONE[2])
                c.put(x + 1, y, STONE[4])
        for _ in range(2):
            x, y = rng.randrange(1, w - 1), rng.randrange(2, h - 2)
            c.put(x, y, STONE[1])
            c.put(x + 1, y + 1, STONE[4])
        for x in range(1, w - 1):
            if (x * 3) % 5 < 2:
                c.put(x, h - 2, STONE[2])
        return c, None
    return p


def keystone_front(seed):
    """Keystone face with the round recess the eclipse disc sits in, carved rays and a gold collar."""
    def p(w, h):
        base, _ = monolith(seed, panel=1, crack=False)(w, h)
        c = base
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for y in range(h):
            for x in range(w):
                r = math.hypot(x - cx, y - cy)
                if r < 7.2:
                    c.put(x, y, STONE[0] if r < 6.5 else STONE[1])
                elif r < 8.0:
                    a = math.atan2(y - cy, x - cx)
                    c.put(x, y, GOLD[4] if -2.4 < a < -0.6 else (GOLD[2] if 0.6 < a < 2.4 else GOLD[3]))
        return c, None
    return p


def glyph_plate(glyph, colour='soul'):
    def p(w, h):
        bitmap = GLYPHS[glyph]
        c, g = _c(w, h), _c(w, h)
        for y, row in enumerate(bitmap):
            for x, ch in enumerate(row):
                if ch != '#':
                    continue
                above = y > 0 and bitmap[y - 1][x] == '#'
                if colour == 'soul':
                    col = SOUL[3] if not above else (SOUL[2] if y < len(bitmap) - 1 else SOUL[1])
                else:
                    col = GRACE[5] if not above else (GRACE[4] if y < len(bitmap) - 1 else GRACE[3])
                c.put(x, y, col)
                g.put(x, y, col)
        return c, g
    return p


def disc_plate(w, h):
    """The dormant eclipse: a black disc in a tarnished gold ring (r <= 6.9), transparent corners."""
    c = _c(w, h)
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            r = math.hypot(x - cx, y - cy)
            a = math.atan2(y - cy, x - cx)
            if r < 4.6:
                c.put(x, y, ECLIPSE_BLACK)
            elif r < 5.6:
                c.put(x, y, VIOLET[1] if -2.6 < a < -0.4 else ECLIPSE_BLACK)
            elif r < 6.9:
                c.put(x, y, GOLD[4] if -2.4 < a < -0.7 else (GOLD[1] if 0.7 < a < 2.4 else GOLD[2]))
    return c, None


def corona_plate(w, h):
    """The lit eclipse: bright rim, rays and light cracking across the black disc. All emissive."""
    c = _c(w, h)
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            r = math.hypot(x - cx, y - cy)
            a = math.atan2(y - cy, x - cx)
            if 5.6 <= r < 6.7:
                c.put(x, y, GRACE[5] if -2.6 < a < -0.2 else GRACE[4])
            elif 6.7 <= r < 7.5:
                c.put(x, y, GRACE[2])
            elif 7.5 <= r < 9.9:
                for k in range(8):
                    ang = k * math.pi / 4 + math.pi / 8
                    d = abs((a - ang + math.pi) % (2 * math.pi) - math.pi) * r
                    if d < (0.75 if r < 8.7 else 0.5):
                        c.put(x, y, GRACE[3] if r < 8.7 else GRACE[2])
                if r < 8.3:
                    for k in range(8):
                        ang = k * math.pi / 4
                        d = abs((a - ang + math.pi) % (2 * math.pi) - math.pi) * r
                        if d < 0.5:
                            c.put(x, y, GRACE[2])
    # cracks of light running from the rim into the black
    cracks = [[(0, -1), (1, -2), (1, -3), (2, -4), (2, -5)],
              [(1, 1), (2, 1), (3, 2), (4, 2), (5, 3)],
              [(-1, 0), (-2, 1), (-3, 1), (-4, 2), (-5, 2)],
              [(0, 1), (-1, 2), (-1, 3), (-2, 4)]]
    ix, iy = int(round(cx)), int(round(cy))
    for crack in cracks:
        for k, (dx, dy) in enumerate(crack):
            c.put(ix + dx, iy + dy, GRACE[4] if k > 1 else GRACE[5])
    c.put(ix, iy, GRACE[5])
    return c, c.copy()


def _spiral(w, h, arms, twist):
    cx, cy = w / 2, h / 2
    ys, xs = np.mgrid[0:h, 0:w]
    x, y = xs + 0.5 - cx, ys + 0.5 - cy
    r = np.hypot(x, y)
    th = np.arctan2(y, x)
    phi = th - twist * np.log(r + 1.0)
    return r, np.cos(arms * phi)


def vortex_a(w, h):
    """Back layer: deep violet-black with four slow arms, nearly opaque. 4-fold symmetric."""
    r, arm = _spiral(w, h, 4, 1.5)
    c, g = _c(w, h), _c(w, h)
    for y in range(h):
        for x in range(w):
            rr, a = r[y, x], arm[y, x]
            if rr > 21.3:
                continue
            col = VIOLET[1]
            if a > 0.5:
                col = VIOLET[2]
            if a > 0.86 and rr > 5:
                col = VIOLET[3]
            if rr < 5.0:
                col = ECLIPSE_BLACK
            alpha = 235 if rr < 17.5 else (200 if rr < 19.3 else (150 if rr < 20.4 else 90))
            c.put(x, y, with_alpha(col, alpha))
            if a > 0.95 and 7 < rr < 17:
                c.put(x, y, with_alpha(VIOLET[4], alpha))
                g.put(x, y, VIOLET[3])
    return c, g


def vortex_b(w, h):
    """Middle layer: two arms shading from violet (rim) to gold (centre). 2-fold symmetric."""
    r, arm = _spiral(w, h, 2, 2.1)
    c, g = _c(w, h), _c(w, h)
    for y in range(h):
        for x in range(w):
            rr, a = r[y, x], arm[y, x]
            if rr > 20.4 or rr < 4.5 or a < 0.42:
                continue
            strong = a > 0.78
            if rr > 15:
                col = VIOLET[4] if strong else VIOLET[3]
            elif rr > 10:
                col = GRACE[1] if strong else VIOLET[4]
            elif rr > 7:
                col = GRACE[2] if strong else GRACE[1]
            else:
                col = GRACE[4] if strong else GRACE[3]
            alpha = 215 if strong else 165
            if rr > 18.6:
                alpha = 120
            c.put(x, y, with_alpha(col, alpha))
            if strong and rr <= 15:
                g.put(x, y, col)
    return c, g


def vortex_c(w, h):
    """Front layer: three thin gold streaks with cyan sparks, around the black eclipse centre."""
    r, arm = _spiral(w, h, 3, 2.8)
    c, g = _c(w, h), _c(w, h)
    for y in range(h):
        for x in range(w):
            rr, a = r[y, x], arm[y, x]
            if rr < 3.2:
                c.put(x, y, ECLIPSE_BLACK)
                continue
            if rr < 4.3:
                col = GRACE[5]
                c.put(x, y, col)
                g.put(x, y, col)
                continue
            if rr < 5.3:
                c.put(x, y, with_alpha(GRACE[3], 170))
                g.put(x, y, GRACE[2])
                continue
            if rr > 18.8 or a < 0.9:
                continue
            col = GRACE[4] if rr < 9 else (GRACE[3] if rr < 14 else SOUL[2])
            c.put(x, y, with_alpha(col, 225))
            g.put(x, y, col)
    # cyan sparks (placed with 4-fold symmetry about the pixel-corner centre)
    for sx, sy in ((7, -12), (-14, -4)):
        for k in range(4):
            px, py = sx, sy
            for _ in range(k):
                px, py = -py - 1, px
            c.put(w // 2 + px, h // 2 + py, SOUL[3])
            g.put(w // 2 + px, h // 2 + py, SOUL[3])
    return c, g


def ring_outer_face(w, h):
    """Segment front: stone lip, tarnished gold inlay (lit by the glow plates once kindled), stone foot."""
    c = _c(w, h, STONE[3])
    for x in range(w):
        c.put(x, 0, STONE[4])
        c.put(x, h - 1, STONE[2])
        if h >= 3:
            c.put(x, 1, GOLD[3] if x % 2 == 0 else GOLD[2])
    return c, None


def ring_outer_edge(w, h):
    c = _c(w, h, STONE[3])
    for x in range(w):
        c.put(x, 0, STONE[4] if x % 2 else STONE[3])
    for x in range(0, w, 2):
        c.put(x, h - 1, GOLD[2])
    return c, None


def ring_inner_face(w, h):
    c = _c(w, h, GOLD[3])
    for x in range(w):
        c.put(x, 0, GOLD[4] if x % 2 == 0 else GOLD[3])
    return c, None


def ring_inner_edge(w, h):
    c = _c(w, h, GOLD[2])
    for x in range(0, w, 2):
        c.put(x, 0, GOLD[3])
    return c, None


def ring_glow_strip(bright):
    """Emissive inlay strip laid over a ring segment (ring_*_glow bones)."""
    def p(w, h):
        c = _c(w, h)
        for x in range(w):
            for y in range(h):
                if bright:
                    col = GRACE[5] if x % 2 == 0 else GRACE[4]
                else:
                    col = GRACE[4] if x % 2 == 0 else GRACE[3]
                c.put(x, y, col)
        return c, c.copy()
    return p


def shard_face(seed):
    def p(w, h):
        rng = random.Random(seed)
        c = _c(w, h, STONE[3])
        for x in range(w):
            c.put(x, 0, STONE[4])
            c.put(x, h - 1, STONE[1])
        c.put(rng.randrange(w), rng.randrange(h), GOLD[3])
        return c, None
    return p


def build_model():
    M = Model()
    M.bone('root', (0, 0, 0))
    # threshold: the core block's 3 px plinth
    M.box('root', 'threshold', (-8, 0, -7), (8, 3, 7), {'*': stone_block(1, chips=1), 'up': top_face(2, worn=True)})

    for side, s in (('left', 1), ('right', -1)):
        bone = f'pillar_{side}'
        M.bone(bone, (16 * s, 0, 0))

        def X(a, b):
            return (min(a * s, b * s), max(a * s, b * s))
        seed = 10 if s > 0 else 40
        x0, x1 = X(8, 24)
        M.box(bone, f'{bone}.base0', (x0, 0, -8), (x1, 3, 8), {'*': stone_block(seed, chips=3), 'up': top_face(seed)})
        x0, x1 = X(9, 23)
        M.box(bone, f'{bone}.base1', (x0, 3, -7), (x1, 6, 7), {'*': stone_block(seed + 1, chips=2, light=0), 'up': top_face(seed + 1)})
        x0, x1 = X(10, 22)
        grooves_front = [(f'p{i}', 3, 41 - (y + 7)) for i, y in enumerate(PILLAR_GLYPH_Y)]
        M.box(bone, f'{bone}.shaft', (x0, 6, -6), (x1, 41, 6), {
            'north': monolith(seed + 2, grooves_front),
            'south': monolith(seed + 3, grooves_front),
            'east': dressed_side(seed + 4), 'west': dressed_side(seed + 5),
            'up': top_face(seed + 6), 'down': top_face(seed + 7)})
        # capital: the right one lost its back corner
        x0, x1 = X(9, 23)
        zb = 7 if s > 0 else 5
        M.box(bone, f'{bone}.capital', (x0, 41, -7), (x1, 45, zb), {'*': stone_block(seed + 8, gold_row=1, chips=2, light=1),
                                                                     'up': top_face(seed + 8)})
        if s < 0:
            # a fallen chunk of that corner resting on the base step
            M.box(bone, f'{bone}.rubble', (-22, 3, 4), (-19, 5, 7), {'*': stone_block(seed + 9, chips=0)}, rot=(0, 22.5, 0), pivot=(-20.5, 3, 5.5))

    # lintel: beam (right end broken), keystone carrying the eclipse disc, crown cap and corbels
    M.bone('lintel', (0, 45, 0))
    # left beam spans x 8..24; a glyph centred at +xc covers x xc-2.5..xc+2.5 -> north u = 24 - (xc + 2.5)
    lg_left = [(g, int(round(24 - (xc + 2.5))), int(round(57 - (LINTEL_GLYPH_Y + 5)))) for xc, g in LINTEL_GLYPHS]
    lg_left_s = [(g, int(round((xc - 2.5) - 8)), int(round(57 - (LINTEL_GLYPH_Y + 5)))) for xc, g in LINTEL_GLYPHS]
    M.box('lintel', 'beam_left', (8, 45, -7), (24, 57, 7), {
        'north': monolith(61, lg_left, panel=1, crack=False, gold_rows=(-3,)),
        'south': monolith(62, lg_left_s, panel=1, crack=False, gold_rows=(-3,)),
        '*': stone_block(63, gold_row=-3, chips=2), 'up': top_face(64)})
    # right beam spans x -24..-8 and is 9 tall (top broken at y 54)
    lg_right = [(g, int(round(-8 - (-xc + 2.5))), int(round(54 - (LINTEL_GLYPH_Y + 5)))) for xc, g in LINTEL_GLYPHS]
    lg_right_s = [(g, int(round((-xc - 2.5) + 24)), int(round(54 - (LINTEL_GLYPH_Y + 5)))) for xc, g in LINTEL_GLYPHS]
    M.box('lintel', 'beam_right', (-24, 45, -7), (-8, 54, 7), {
        'north': monolith(65, lg_right, panel=1, crack=False, gold_rows=(-3,)),
        'south': monolith(66, lg_right_s, panel=1, crack=False, gold_rows=(-3,)),
        '*': stone_block(67, gold_row=-3, chips=3), 'up': top_face(68)})
    M.box('lintel', 'beam_right_stub', (-14, 54, -6), (-8, 56, 6), {'*': stone_block(69, chips=2), 'up': top_face(69)})
    M.box('lintel', 'keystone', (-8, 45, -8), (8, 61, 8), {
        'north': keystone_front(70), 'south': keystone_front(71),
        '*': stone_block(72, gold_row=2, chips=2, light=1), 'up': top_face(73), 'down': top_face(74)})
    M.box('lintel', 'crown', (-5, 61, -5), (5, 63, 5), {'*': stone_block(75, gold_row=None, chips=1, light=1), 'up': top_face(76)})
    for s in (1, -1):
        def X(a, b):
            return (min(a * s, b * s), max(a * s, b * s))
        x0, x1 = X(7, 10)
        M.box('lintel', f'corbel_low_{s}', (x0, 38, -5), (x1, 42, 5), {'*': stone_block(80 + s, chips=1)})
        x0, x1 = X(4, 10)
        M.box('lintel', f'corbel_high_{s}', (x0, 42, -5), (x1, 45, 5), {'*': stone_block(82 + s, gold_row=None, chips=1, light=1)})

    # eclipse disc set into the keystone (front and back), with the lit corona as its child
    M.bone('eclipse_disc', (DISC_C[0], DISC_C[1], 0), 'lintel')
    disc = M.region('eclipse_disc.plate', 14, 14, disc_plate)
    M.cube('eclipse_disc', (-7, 46, -8.2), (7, 60, -8.2), {'north': disc})
    M.cube('eclipse_disc', (-7, 46, 8.2), (7, 60, 8.2), {'south': disc})
    M.bone('eclipse_glow', (DISC_C[0], DISC_C[1], 0), 'eclipse_disc')
    corona = M.region('eclipse_glow.plate', 20, 20, corona_plate)
    M.cube('eclipse_glow', (-10, 43, -8.4), (10, 63, -8.4), {'north': corona})
    M.cube('eclipse_glow', (-10, 43, 8.4), (10, 63, 8.4), {'south': corona})

    # rings, centred on the doorway, in front of the vortex. Each has a child *_glow bone holding thin
    # emissive inlay strips (front and back), so a dormant gate shows plain tarnished gold.
    cx, cy = DOOR_C
    for name, n, r_out, thick, z0, z1, face_p, edge_p, band in (
            ('ring_outer', 16, 9.5, 2.5, -2.5, -1.5, ring_outer_face, ring_outer_edge, (0.25, -0.75)),
            ('ring_inner', 12, 5.6, 1.0, -1.0, 0.0, ring_inner_face, ring_inner_edge, (0.5, -0.5))):
        M.bone(name, (cx, cy, (z0 + z1) / 2))
        M.bone(f'{name}_glow', (cx, cy, (z0 + z1) / 2), name)
        seg = 2 * r_out * math.tan(math.pi / n)
        r_mid = r_out - thick / 2
        fr = M.region(f'{name}.face', seg, thick, face_p)
        er = M.region(f'{name}.edge', seg, z1 - z0, edge_p)
        sr = M.region(f'{name}.side', z1 - z0, thick, edge_p)
        gr = M.region(f'{name}.glow', seg, band[0] - band[1], ring_glow_strip(name == 'ring_inner'))
        for k in range(n):
            th = 2 * math.pi * k / n
            px, py = cx + r_mid * math.cos(th), cy + r_mid * math.sin(th)
            rot = (0, 0, math.degrees(th) + 90)
            lo = (px - seg / 2, py - thick / 2, z0)
            hi = (px + seg / 2, py + thick / 2, z1)
            M.cube(name, lo, hi, {'north': fr, 'south': fr, 'up': er, 'down': er, 'east': sr, 'west': sr},
                   rot=rot, pivot=(px, py, (z0 + z1) / 2))
            for z, f in ((z0 - 0.06, 'north'), (z1 + 0.06, 'south')):
                M.cube(f'{name}_glow', (px - seg / 2, py + band[1], z), (px + seg / 2, py + band[0], z), {f: gr},
                       rot=rot, pivot=(px, py, (z0 + z1) / 2))

    # runes: glyph plates over the engraved grooves, each its own bone so they can light one by one
    for side, s in (('left', 1), ('right', -1)):
        parent = f'runes_{side}'
        M.bone(parent, (16 * s, 6, 0))
        for i, y in enumerate(PILLAR_GLYPH_Y):
            name = f'{parent}_{i}'
            M.bone(name, (16 * s, y + 3.5, 0), parent)
            reg = M.region(f'glyph.p{i}', 6, 7, glyph_plate(f'p{i}'))
            xs = (16 * s - 3, 16 * s + 3)
            M.cube(name, (xs[0], y, -6.15), (xs[1], y + 7, -6.15), {'north': reg})
            M.cube(name, (xs[0], y, 6.15), (xs[1], y + 7, 6.15), {'south': reg})
    M.bone('runes_lintel', (0, LINTEL_GLYPH_Y + 2.5, 0), 'lintel')
    for j, (xc, g) in enumerate(LINTEL_GLYPHS):
        for s in (1, -1):
            name = f'runes_lintel_{j}{"l" if s > 0 else "r"}'
            M.bone(name, (xc * s, LINTEL_GLYPH_Y + 2.5, 0), 'runes_lintel')
            reg = M.region(f'glyph.{g}', 5, 5, glyph_plate(g, 'gold'))
            lo_x, hi_x = xc * s - 2.5, xc * s + 2.5
            M.cube(name, (lo_x, LINTEL_GLYPH_Y, -7.15), (hi_x, LINTEL_GLYPH_Y + 5, -7.15), {'north': reg})
            M.cube(name, (lo_x, LINTEL_GLYPH_Y, 7.15), (hi_x, LINTEL_GLYPH_Y + 5, 7.15), {'south': reg})

    # floating shards of the broken lintel and pillars
    M.bone('shards', (0, 0, 0))
    for i, (centre, size, rot) in enumerate(SHARDS):
        name = f'shard_{i}'
        M.bone(name, centre, 'shards')
        lo = tuple(c - d / 2 for c, d in zip(centre, size))
        hi = tuple(c + d / 2 for c, d in zip(centre, size))
        M.box(name, name, lo, hi, {'*': shard_face(90 + i)}, rot=rot, pivot=centre)

    # the vortex: three spinning discs filling the doorway (drawn last: translucent)
    M.bone('vortex', (cx, cy, 0))
    for name, z, painter in (('vortex_a', 2.5, vortex_a), ('vortex_b', 1.5, vortex_b), ('vortex_c', 0.5, vortex_c)):
        M.bone(name, (cx, cy, z), 'vortex')
        reg = M.region(f'{name}.disc', 2 * VORTEX_R, 2 * VORTEX_R, painter)
        M.cube(name, (cx - VORTEX_R, cy - VORTEX_R, z), (cx + VORTEX_R, cy + VORTEX_R, z), {'north': reg})
    return M


SHARDS = [
    ((-19.0, 59.0, -1.0), (3.0, 2.5, 3.0), (15, 30, 20)),
    ((-12.5, 62.0, 2.0), (2.0, 2.0, 2.0), (-20, 45, 10)),
    ((17.0, 60.5, 1.0), (2.5, 2.0, 2.0), (10, -25, -15)),
    ((15.0, 9.0, -10.0), (2.0, 2.0, 2.0), (25, 10, 35)),
    ((-20.5, 7.0, -9.5), (2.5, 1.5, 2.0), (-10, -35, 20)),
]
SHARD_RISE = (2.5, 3.0, 2.0, 2.0, 1.5)


def key(v, easing='linear'):
    return {'vector': [r4(x) for x in v], 'easing': easing}


def track(points):
    """points: [(time, vector, easing)] -> {"t": {...}}"""
    out = {}
    for p in points:
        t, v = p[0], p[1]
        e = p[2] if len(p) > 2 else 'linear'
        out[f'{t:.4f}'.rstrip('0').rstrip('.') if t % 1 else f'{t:.1f}'] = key(v, e)
    return out


def glyph_bones():
    names = [f'runes_{side}_{i}' for side in ('left', 'right') for i in range(4)]
    names += [f'runes_lintel_{j}{s}' for j in range(len(LINTEL_GLYPHS)) for s in 'lr']
    return names


def spin(t0, t1, total, axis=2, samples=12, ease=lambda f: f * f):
    """Rotation keys that accelerate from rest (default quadratic) to reach `total` degrees at t1."""
    pts = []
    for i in range(samples + 1):
        f = i / samples
        t = t0 + (t1 - t0) * f
        v = [0, 0, 0]
        v[axis] = total * ease(f)
        pts.append((round(t, 4), v, 'linear'))
    return pts


def clips():
    G = glyph_bones()
    hidden = [0, 0, 1]
    dormant = {'loop': True, 'animation_length': 4.0, 'bones': {}}
    db = dormant['bones']
    for g in G:
        db[g] = {'scale': track([(0.0, hidden)])}
    db['eclipse_glow'] = {'scale': track([(0.0, hidden)])}
    db['ring_outer_glow'] = {'scale': track([(0.0, hidden)])}
    db['ring_inner_glow'] = {'scale': track([(0.0, hidden)])}
    db['vortex'] = {'scale': track([(0.0, hidden)])}
    for v in ('vortex_a', 'vortex_b', 'vortex_c'):
        db[v] = {'rotation': track([(0.0, [0, 0, 0])])}
    db['ring_outer'] = {'rotation': track([(0.0, [0, 0, 0]), (2.0, [0, 0, 1.5], 'easeInOutSine'), (4.0, [0, 0, 0], 'easeInOutSine')])}
    db['ring_inner'] = {'rotation': track([(0.0, [0, 0, 0]), (2.0, [0, 0, -2.0], 'easeInOutSine'), (4.0, [0, 0, 0], 'easeInOutSine')])}
    for i in range(len(SHARDS)):
        ph = (i * 0.8) % 4.0
        bob = 0.4 + 0.1 * i
        pts = [(0.0, [0, 0, 0])]
        pts.append((round(min(3.6, 1.0 + ph * 0.25), 3), [0, bob, 0], 'easeInOutSine'))
        pts.append((4.0, [0, 0, 0], 'easeInOutSine'))
        db[f'shard_{i}'] = {'position': track(pts), 'rotation': track([(0.0, [0, 0, 0])])}

    # kindle: 6.0 s, holds on the last frame
    kindle = {'loop': 'hold_on_last_frame', 'animation_length': 6.0, 'bones': {}}
    kb = kindle['bones']
    times = {}
    for i in range(4):
        times[f'runes_left_{i}'] = times[f'runes_right_{i}'] = 0.35 + i * 0.65
    for j in range(len(LINTEL_GLYPHS)):
        times[f'runes_lintel_{j}l'] = times[f'runes_lintel_{j}r'] = 2.75 + j * 0.4
    for g in G:
        t0 = times[g]
        kb[g] = {'scale': track([(0.0, hidden), (t0, hidden), (round(t0 + 0.18, 3), [1.3, 1.3, 1], 'easeOutQuad'),
                                 (round(t0 + 0.34, 3), [1, 1, 1], 'easeInOutSine'), (6.0, [1, 1, 1])])}
    kb['ring_outer'] = {'rotation': track([(0.0, [0, 0, 0])] + spin(0.6, 6.0, 135.0))}
    kb['ring_inner'] = {'rotation': track([(0.0, [0, 0, 0])] + spin(0.6, 6.0, -240.0))}
    kb['eclipse_glow'] = {'scale': track([(0.0, hidden), (1.8, hidden), (1.95, [0.4, 0.4, 1], 'easeOutQuad'),
                                          (2.1, [0.15, 0.15, 1]), (2.3, [0.6, 0.6, 1], 'easeOutQuad'),
                                          (2.45, [0.4, 0.4, 1]), (3.0, [0.85, 0.85, 1], 'easeInOutSine'),
                                          (3.5, [1, 1, 1], 'easeInOutSine'), (4.4, [1, 1, 1]),
                                          (4.6, [1.3, 1.3, 1], 'easeOutQuad'), (5.3, [1, 1, 1], 'easeInOutSine'),
                                          (6.0, [1, 1, 1])])}
    kb['eclipse_disc'] = {'position': track([(0.0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.1, [0.25, 0, 0]), (2.2, [-0.25, 0.1, 0]),
                                             (2.3, [0.2, -0.1, 0]), (2.4, [0, 0, 0]), (4.5, [0, 0, 0]), (4.55, [0.3, 0.1, 0]),
                                             (4.65, [-0.3, 0, 0]), (4.75, [0.15, -0.1, 0]), (4.85, [0, 0, 0]), (6.0, [0, 0, 0])])}
    kb['ring_outer_glow'] = {'scale': track([(0.0, hidden), (0.6, hidden), (1.3, [1, 1, 1], 'easeOutQuad'), (6.0, [1, 1, 1])])}
    kb['ring_inner_glow'] = {'scale': track([(0.0, hidden), (1.2, hidden), (1.9, [1, 1, 1], 'easeOutQuad'), (6.0, [1, 1, 1])])}
    # the parent never exceeds 1 (the back disc already fills the doorway); the bright layers overshoot
    kb['vortex'] = {'scale': track([(0.0, hidden), (4.5, hidden), (5.4, [1, 1, 1], 'easeOutCubic'), (6.0, [1, 1, 1])])}
    for v, total, over in (('vortex_a', -180.0, 1.0), ('vortex_b', 360.0, 1.07), ('vortex_c', -360.0, 1.15)):
        kb[v] = {'rotation': track([(0.0, [0, 0, 0]), (4.5, [0, 0, 0])] +
                                   spin(4.5, 6.0, total, samples=6, ease=lambda f: 1 - (1 - f) ** 2)[1:])}
        if over > 1:
            kb[v]['scale'] = track([(0.0, [1, 1, 1]), (5.0, [1, 1, 1]), (5.55, [over, over, 1], 'easeOutQuad'),
                                    (6.0, [1, 1, 1], 'easeInOutSine')])
    for i in range(len(SHARDS)):
        rise = SHARD_RISE[i]
        kb[f'shard_{i}'] = {
            'position': track([(0.0, [0, 0, 0]), (1.0 + 0.15 * i, [0, 0, 0]), (2.5, [0, rise * 0.35, 0], 'easeInOutSine'),
                               (2.6, [0.2, rise * 0.35, 0]), (2.7, [-0.2, rise * 0.4, 0]), (2.8, [0, rise * 0.4, 0]),
                               (5.5, [0, rise, 0], 'easeInOutSine'), (6.0, [0, rise, 0])]),
            'rotation': track([(0.0, [0, 0, 0]), (1.0, [0, 0, 0]), (6.0, [0, 360, 0], 'easeInQuad')]),
        }

    # open: 4.0 s seamless loop
    opn = {'loop': True, 'animation_length': 4.0, 'bones': {}}
    ob = opn['bones']
    for g in G:
        ob[g] = {'scale': track([(0.0, [1, 1, 1])])}
    ob['ring_outer'] = {'rotation': track([(0.0, [0, 0, 0]), (4.0, [0, 0, 180])])}
    ob['ring_outer_glow'] = {'scale': track([(0.0, [1, 1, 1])])}
    ob['ring_inner_glow'] = {'scale': track([(0.0, [1, 1, 1])])}
    ob['ring_inner'] = {'rotation': track([(0.0, [0, 0, 0]), (4.0, [0, 0, -360])])}
    ob['eclipse_glow'] = {'scale': track([(0.0, [1, 1, 1]), (1.0, [1.06, 1.06, 1], 'easeInOutSine'), (2.0, [1, 1, 1], 'easeInOutSine'),
                                          (3.0, [1.06, 1.06, 1], 'easeInOutSine'), (4.0, [1, 1, 1], 'easeInOutSine')])}
    ob['vortex'] = {'scale': track([(0.0, [1, 1, 1]), (1.0, [1.03, 1.03, 1], 'easeInOutSine'), (2.0, [1, 1, 1], 'easeInOutSine'),
                                    (3.0, [1.03, 1.03, 1], 'easeInOutSine'), (4.0, [1, 1, 1], 'easeInOutSine')])}
    ob['vortex_a'] = {'rotation': track([(0.0, [0, 0, 0]), (4.0, [0, 0, -90])])}
    ob['vortex_b'] = {'rotation': track([(0.0, [0, 0, 0]), (4.0, [0, 0, 180])]),
                      'scale': track([(0.0, [1, 1, 1]), (2.0, [0.96, 0.96, 1], 'easeInOutSine'), (4.0, [1, 1, 1], 'easeInOutSine')])}
    ob['vortex_c'] = {'rotation': track([(0.0, [0, 0, 0]), (4.0, [0, 0, -360])]),
                      'scale': track([(0.0, [1, 1, 1]), (0.5, [1.06, 1.06, 1], 'easeInOutSine'), (1.5, [0.97, 0.97, 1], 'easeInOutSine'),
                                      (2.5, [1.06, 1.06, 1], 'easeInOutSine'), (3.5, [0.97, 0.97, 1], 'easeInOutSine'),
                                      (4.0, [1, 1, 1], 'easeInOutSine')])}
    for i in range(len(SHARDS)):
        rise = SHARD_RISE[i]
        a = 0.6 + 0.15 * i
        sx = 0.5 if i % 2 else -0.5
        ob[f'shard_{i}'] = {
            'position': track([(0.0, [0, rise, 0]), (1.0, [sx, rise + a, 0], 'easeInOutSine'), (2.0, [0, rise, 0], 'easeInOutSine'),
                               (3.0, [-sx, rise - a * 0.6, 0], 'easeInOutSine'), (4.0, [0, rise, 0], 'easeInOutSine')]),
            'rotation': track([(0.0, [0, 0, 0]), (4.0, [0, 360 if i % 2 else -360, 0])]),
        }
    return {'dormant': dormant, 'kindle': kindle, 'open': opn}


def animation_json():
    anims = {}
    for name, clip in clips().items():
        anims[f'animation.eclipse_waygate.{name}'] = clip
    for name, clip in clips().items():      # short aliases: `RawAnimation.begin().thenLoop("open")` also works
        anims[name] = clip
    return {'format_version': '1.8.0', 'animations': anims}


def build():
    M = build_model()
    M.pack()
    base, glow = M.paint()
    geo = M.geo_json()
    save_json(geo, 'geo/block/eclipse_waygate.geo.json')
    save_json(animation_json(), 'animations/block/eclipse_waygate.animation.json')
    save_png(base, 'textures/block/eclipse_waygate.png', sheet=False)
    save_png(glow, 'textures/block/eclipse_waygate_glowmask.png', sheet=False)
    return M, geo, base, glow
