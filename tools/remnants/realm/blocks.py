"""16x16 block textures for the Eclipsed Realm.

Hand-built pixel art from the shared ramps in common.py. Large shapes come from low-frequency
periodic fields (so every cube texture tiles seamlessly) quantised into 3-5 tones; details
(flecks, chips, veins, cracks) are placed individually. No per-pixel noise fills.
"""
import math
import random

import numpy as np

from common import (ASH, BARK, GOLD, GRACE, GRAVE, LEAF, LICHEN, MORTAR, SOUL, STEEL, STONE, STONE_LIGHT,
                    VIOLET, WOOD, ECLIPSE_BLACK, Canvas, hexc, save_json, save_png, with_alpha)

S = 16


def periodic_field(seed, terms, size=S):
    """Sum of cosines with integer wave numbers: tiles perfectly. Returned as ranks in [0, 1)."""
    rng = np.random.default_rng(seed)
    ys, xs = np.mgrid[0:size, 0:size]
    f = np.zeros((size, size))
    for kx, ky, amp in terms:
        f += amp * np.cos(2 * np.pi * (kx * xs + ky * ys) / size + rng.uniform(0, 2 * np.pi))
    order = f.flatten().argsort(kind='stable')
    ranks = np.empty(size * size)
    ranks[order] = np.arange(size * size) / (size * size)
    return ranks.reshape(size, size), f


LOW = ((1, 0, 1.0), (0, 1, 1.0), (1, 1, 0.8), (1, -1, 0.8), (2, 1, 0.55), (1, 2, 0.55), (2, -1, 0.5),
       (-1, 2, 0.5), (3, 1, 0.3), (1, 3, 0.3), (3, -2, 0.25), (2, 3, 0.2))
MID = ((2, 1, 1.0), (1, 2, 1.0), (2, -1, 0.9), (-1, 2, 0.9), (3, 1, 0.8), (1, 3, 0.8), (3, -2, 0.7),
       (2, 3, 0.7), (4, 1, 0.6), (1, -4, 0.6), (4, 3, 0.4), (3, 4, 0.4), (1, 1, 0.5), (1, -1, 0.5))


def eclipse_stone(mirror_safe=True):
    ranks, f = periodic_field(31, LOW)
    c = Canvas(S, S)
    for y in range(S):
        for x in range(S):
            r = ranks[y, x]
            tone = 2 if r < 0.24 else (4 if r > 0.83 else 3)
            c.put(x, y, STONE[tone])
    # pits: darkest few pixels of each dark blotch sink one more tone, lit lip above them
    flat = sorted(((f[y, x], x, y) for y in range(S) for x in range(S)))
    for _, x, y in flat[:9]:
        c.put(x, y, STONE[1])
        if c.get(x, y - 1, True)[:3] == STONE[3][:3]:
            c.put(x, y - 1, STONE[4], True)
    for _, x, y in flat[-4:]:
        c.put(x, y, STONE[5])
    # two short hairline cracks (dark run with a lit pixel on the upper-left side)
    for (sx, sy, steps) in ((3, 9, ((1, 1), (1, 0), (1, 1))), (11, 2, ((0, 1), (1, 1), (0, 1)))):
        x, y = sx, sy
        for dx, dy in steps:
            c.put(x, y, STONE[1], True)
            c.put(x - 1, y, STONE[4], True) if dx else None
            x, y = x + dx, y + dy
        c.put(x, y, STONE[1], True)
    # faint gold flecks: a bright grain with a dull shadow pixel
    for x, y, bright in ((6, 4, True), (13, 11, False), (1, 13, False)):
        c.put(x, y, GOLD[3] if bright else GOLD[2], True)
        c.put(x + 1, y + 1, GOLD[1] if bright else STONE[2], True)
    return c.image()


def _voronoi(seeds, squash=1.0, size=S):
    d1 = np.full((size, size), 1e9)
    d2 = np.full((size, size), 1e9)
    owner = np.zeros((size, size), dtype=int)
    off = np.zeros((size, size, 2))
    for i, (sx, sy) in enumerate(seeds):
        for y in range(size):
            for x in range(size):
                dx = (x - sx + size / 2) % size - size / 2
                dy = (y - sy + size / 2) % size - size / 2
                d = math.hypot(dx, dy * squash)
                if d < d1[y, x]:
                    d2[y, x] = d1[y, x]
                    d1[y, x] = d
                    owner[y, x] = i
                    off[y, x] = (dx, dy)
                elif d < d2[y, x]:
                    d2[y, x] = d
    return owner, d1, d2, off


def cobbled_eclipse_stone():
    seeds = [(2.5, 2.0), (9.0, 1.5), (14.0, 5.0), (5.5, 7.0), (11.0, 9.5), (1.0, 11.5), (7.0, 13.5), (13.5, 14.0)]
    base = [3, 2, 3, 3, 2, 4, 3, 2]
    owner, d1, d2, off = _voronoi(seeds, squash=1.15)
    c = Canvas(S, S)
    for y in range(S):
        for x in range(S):
            if d2[y, x] - d1[y, x] < 1.05:
                c.put(x, y, MORTAR[0] if (x + y) % 3 else MORTAR[1])
                continue
            i = owner[y, x]
            dx, dy = off[y, x]
            tone = base[i]
            lean = dx + dy * 1.2
            if lean < -2.2:
                tone += 1
            elif lean > 2.4:
                tone -= 1
            c.put(x, y, STONE[max(1, min(5, tone))])
    # rim light on the upper-left inside edge of each stone, shadow on the lower-right
    src = c.copy()
    for y in range(S):
        for x in range(S):
            if src.get(x, y)[:3] in (MORTAR[0][:3], MORTAR[1][:3]):
                continue
            up = src.get(x, y - 1, True)[:3] in (MORTAR[0][:3], MORTAR[1][:3])
            left = src.get(x - 1, y, True)[:3] in (MORTAR[0][:3], MORTAR[1][:3])
            down = src.get(x, y + 1, True)[:3] in (MORTAR[0][:3], MORTAR[1][:3])
            i = owner[y, x]
            if up or left:
                c.put(x, y, STONE[min(5, base[i] + 1)])
            elif down:
                c.put(x, y, STONE[max(1, base[i] - 1)])
    for x, y in ((4, 3), (12, 10)):
        c.put(x, y, GOLD[3])
    c.put(8, 6, STONE[1])
    return c.image()


def _brick_layout():
    """(x0, y0, length, height) bricks with 1 px mortar on their right and bottom; staggered rows."""
    return [(0, 0, 15, 7), (8, 8, 15, 7)]


def eclipse_stone_bricks(seed=5, cracked=False):
    rng = random.Random(seed)
    c = Canvas(S, S, MORTAR[0])
    for bx, by, bl, bh in _brick_layout():
        for j in range(bh):
            for i in range(bl):
                x, y = (bx + i) % S, by + j
                tone = 3
                if j == 0:
                    tone = 4
                elif j == bh - 1:
                    tone = 2
                if i == 0 and j < bh - 1:
                    tone = 4
                elif i == bl - 1 and j > 0:
                    tone = 2
                c.put(x, y, STONE[tone])
        c.put(bx, by, STONE[5], True)
        # surface: a couple of tooling marks and a worn chip on one corner
        for _ in range(3):
            i, j = rng.randrange(2, bl - 2), rng.randrange(2, bh - 2)
            c.put(bx + i, by + j, STONE[2], True)
            if rng.random() < 0.5:
                c.put(bx + i + 1, by + j, STONE[4] if rng.random() < 0.4 else STONE[2], True)
        corner = rng.choice(((bl - 1, 0), (0, bh - 1), (bl - 2, bh - 1)))
        c.put(bx + corner[0], by + corner[1], STONE[1], True)
    # mortar is not flat: lighter specks where it catches light
    for x, y in ((3, 7), (11, 7), (15, 2), (7, 12), (5, 15), (13, 15)):
        c.put(x, y, MORTAR[1])
    if cracked:
        cracks = [
            [(3, 0), (3, 1), (4, 2), (4, 3), (5, 3), (6, 4), (6, 5), (7, 6)],
            [(7, 8), (8, 9), (8, 10), (9, 11)],
            [(11, 9), (12, 10), (12, 11), (11, 12), (11, 13), (12, 14)],
            [(13, 1), (12, 2)],
        ]
        for crack in cracks:
            for k, (x, y) in enumerate(crack):
                c.put(x, y, MORTAR[0] if k % 3 else STONE[1], True)
                right = c.get(x + 1, y, True)[:3]
                if right not in (MORTAR[0][:3], MORTAR[1][:3], STONE[1][:3]):
                    c.put(x + 1, y, STONE[4], True)
        # a corner that broke away: dark hollow with a lit lower lip
        for x, y in ((14, 4), (14, 5), (13, 5), (13, 6), (14, 6)):
            c.put(x, y, STONE[1])
        c.put(12, 6, STONE[2])
        c.put(6, 13, STONE[1])
        c.put(7, 13, STONE[1])
        c.put(6, 14, STONE[2])
    return c.image()


def chiseled_eclipse_stone_bricks():
    k, K = ECLIPSE_BLACK, VIOLET[1]
    ink = {'L': STONE[4], 'H': STONE[5], 'M': STONE[3], 'D': STONE[2], 'd': STONE[1], 'm': MORTAR[0],
           '1': GOLD[1], '2': GOLD[2], '3': GOLD[3], '4': GOLD[4], '5': GOLD[5], 'k': k, 'v': K, 'c': SOUL[0]}
    rows = [
        'HLLLLLLLLLLLLLLm',
        'LMMMMMMMMMMMMMMD',
        'LMddddddddddddMD',
        'LMdD3DD45DDD3dMD',
        'LMdDD2344432DLMD',
        'LMdDD4kkkkv3DLMD',
        'LMd34kkkkkkv3LMD',
        'LMdD5kkkkkkv4LMD',
        'LMdD4kkkkkkv3LMD',
        'LMd23vkkkkvv2LMD',
        'LMdDD3vvvvv2DLMD',
        'LMdDD12333221LMD',
        'LMdD2DD32DDD2LMD',
        'LMdLLLLLLLLLLLMD',
        'LMMMMMMMMMMMMMMD',
        'mDDDDDDDDDDDDDDd',
    ]
    c = Canvas(S, S)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            c.put(x, y, ink[ch])
    return c.image()


FURROWS = (2, 7, 12)


def graceroot_log():
    """Pale ash bark: three wandering furrows (shadowed left wall, lit right wall), flat plates,
    a few lenticels and two thin gold sap veins. Tiles vertically for stacked logs."""
    c = Canvas(S, S, BARK[2])
    rng = random.Random(78)
    paths = []
    for fx in FURROWS:
        xs, x = [], fx
        for y in range(S):
            xs.append(x)
            if y % 5 == 4 and y < S - 5:
                x += rng.choice((-1, 1))
        xs[-1] = round((xs[-2] + fx) / 2) if abs(xs[-2] - fx) > 1 else xs[-1]
        paths.append(xs)
    for xs in paths:
        for y, x in enumerate(xs):
            c.put(x - 1, y, BARK[1], True)
            c.put(x, y, BARK[0], True)
            c.put(x + 1, y, BARK[3], True)
    # plate faces: a soft lit band right of each furrow wall, a shadow band before the next furrow
    for xs in paths:
        for y in range(S):
            if y % 7 in (2, 3, 4):
                c.put(xs[y] + 2, y, BARK[3], True)
    for x, y in ((4, 5), (10, 11), (14, 3), (5, 14)):        # lenticels
        c.put(x, y, BARK[1], True)
        c.put(x + 1, y, BARK[1], True)
        c.put(x, y - 1, BARK[4], True)
    # two thin gold veins seeping along furrows
    for k, start, length in ((0, 3, 5), (2, 10, 4)):
        xs = paths[k]
        for i in range(length):
            y = (start + i) % S
            c.put(xs[y], y, GOLD[3] if i in (0, length - 1) else GRACE[2], True)
    return c.image()


def graceroot_log_top():
    c = Canvas(S, S)
    for y in range(S):
        for x in range(S):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            d = 0.55 * max(dx, dy) + 0.45 * math.hypot(dx, dy) * 0.92
            if max(dx, dy) >= 7:
                tone = BARK[1] if (x + y) % 4 else BARK[0]
                if y == 0 or x == 0:
                    tone = BARK[2]
                c.put(x, y, tone)
                continue
            if max(dx, dy) >= 6:
                c.put(x, y, BARK[3] if (y < 2 or x < 2) else BARK[2])
                continue
            ring = int(d)
            tone = WOOD[3] if ring % 2 == 0 else WOOD[2]
            if ring in (2, 5) and (x + y) % 5 != 0:
                tone = WOOD[1]
            c.put(x, y, tone)
    for x, y in ((7, 7), (8, 8)):
        c.put(x, y, GRACE[3])
    for x, y in ((8, 7), (7, 8)):
        c.put(x, y, GRACE[2])
    for x, y in ((9, 4), (10, 10), (4, 9)):     # radial checks
        c.put(x, y, WOOD[1])
    return c.image()


def graceroot_planks():
    rng = random.Random(12)
    c = Canvas(S, S)
    seams = (3, 11, 6, 14)
    for row in range(4):
        y0 = row * 4
        for j in range(4):
            for x in range(S):
                tone = WOOD[3] if j == 0 else (WOOD[1] if j == 3 else WOOD[2])
                c.put(x, y0 + j, tone)
        sx = seams[row]
        for j in range(3):
            c.put(sx, y0 + j, WOOD[1])
            c.put(sx + 1, y0 + j, WOOD[4] if j == 0 else WOOD[3], True)
        # grain dashes
        for _ in range(2):
            gx = rng.randrange(0, S)
            gy = y0 + rng.choice((1, 2))
            for i in range(rng.choice((2, 3, 4))):
                if (gx + i) % S not in (sx, (sx + 1) % S):
                    c.put(gx + i, gy, WOOD[1] if rng.random() < 0.7 else WOOD[4], True)
        c.put((sx + 2) % S, y0 + 1, WOOD[0])     # peg hole beside the joint
    return c.image()


def gilded_leaves():
    """Golden leaf clumps with see-through gaps. RGB under the gaps stays dark gold for Fast graphics."""
    hole = (58, 38, 12, 0)
    ranks, f = periodic_field(8, MID)
    c = Canvas(S, S, hole)
    for y in range(S):
        for x in range(S):
            r = ranks[y, x]
            if r < 0.17:
                continue
            tone = 1 if r < 0.36 else (2 if r < 0.62 else (3 if r < 0.86 else 4))
            c.put(x, y, LEAF[tone])
    # leaf clumps are lit from above: the row under a gap brightens, the row above a gap darkens
    src = c.copy()
    for y in range(S):
        for x in range(S):
            if not src.a[y, x, 3]:
                continue
            t = _tone_of(src.get(x, y), LEAF)
            if not src.a[(y - 1) % S, x, 3]:
                c.put(x, y, LEAF[min(4, t + 1)])
            elif not src.a[(y + 1) % S, x, 3]:
                c.put(x, y, LEAF[max(0, t - 1)])
    flat = sorted(((f[y, x], x, y) for y in range(S) for x in range(S)), reverse=True)
    for _, x, y in flat[:5]:
        c.put(x, y, LEAF[5])
    return c.image()


def _tone_of(col, ramp_):
    best, bi = 1e9, 0
    for i, r in enumerate(ramp_):
        d = sum((a - b) ** 2 for a, b in zip(col[:3], r[:3]))
        if d < best:
            best, bi = d, i
    return bi


def weathered_gravestone():
    """UV layout (see block_models.gravestone_*):
    u0-8 v0-11  headstone front (step2 row 0, step1 row 1, body rows 2-10) with the engraving
    u8-16 v0-11 headstone back and generic weathered stone (sides and tops sample it)
    u0-10 v11-13 plinth sides, u0-16 v13-16 plinth top
    """
    rng = random.Random(9)
    G, L = GRAVE, LICHEN
    c = Canvas(S, S)
    # back / generic stone: banded, worn
    for y in range(0, 11):
        for x in range(8, 16):
            tone = 3 if y < 2 else (2 if y > 8 else 3)
            if (x * 3 + y * 5) % 11 == 0:
                tone -= 1
            c.put(x, y, G[tone])
    for x, y in ((9, 3), (13, 6), (11, 9), (14, 2)):
        c.put(x, y, G[4])
    for x, y in ((10, 7), (10, 8), (15, 4), (12, 1)):   # lichen
        c.put(x, y, L[1] if (x + y) % 2 else L[2])
    # front: silhouette rows, bevel, engraved eclipse ring + epitaph lines
    front = [
        '..4444..',
        '.433332.',
        '43333332',
        '43333332',
        '43333332',
        '43333332',
        '43333332',
        '43333332',
        '43333332',
        '42222221',
        '32222221',
    ]
    for y, row in enumerate(front):
        for x, ch in enumerate(row):
            if ch != '.':
                c.put(x, y, G[int(ch)])
            else:
                c.put(x, y, G[3])
    # engraved eclipse ring: dark cut ring, lit lower-right inner lip, a worn gold trace on top
    ring = [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5)]
    for x, y in ring:
        c.put(x, y, G[1])
    for x, y in ((3, 3), (4, 3), (3, 4)):
        c.put(x, y, G[2])
    c.put(4, 4, G[4])
    c.put(3, 2, GOLD[2])
    c.put(4, 2, GOLD[3])
    # epitaph: two worn lines of glyph cuts
    for x in (2, 3, 5):
        c.put(x, 7, G[1])
    for x in (2, 4, 5):
        c.put(x, 8, G[1])
    c.put(4, 7, G[2])
    c.put(1, 6, L[1])
    c.put(6, 9, L[2])
    c.put(6, 8, L[1])
    c.put(1, 9, G[1])                              # chip
    # plinth top (u0-10 v11-16; its last two rows double as the front/back faces) and end strips
    for y in range(11, 16):
        for x in range(0, 10):
            tone = 3 if y in (11, 14) else 2
            if (x + 2 * y) % 7 == 0:
                tone = 4 if y != 15 else 1
            if y == 15:
                tone = 1 if x % 3 else 2
            c.put(x, y, G[tone])
    for x in range(10, 16):
        c.put(x, 11, G[3] if x % 3 else G[4])
        c.put(x, 12, G[1] if x % 2 else G[2])
        for y in range(13, 16):
            c.put(x, y, G[2] if (x + y) % 3 else L[0])
    for x, y in ((2, 12), (3, 12), (8, 13), (6, 15)):
        c.put(x, y, L[(x + y) % 3])
    return c.image()


def fog_gate_frames(frames=32):
    """Slowly swirling gold-white fog, alpha 35-60 %. Periodic in x, y and time; 4 tone bands."""
    ys, xs = np.mgrid[0:S, 0:S] / S
    out = []
    tones = [
        (hexc('#E4CC92'), 90),     # deep gold haze (~35 %)
        (hexc('#F0DFB2'), 110),
        (hexc('#F9EED4'), 130),
        (hexc('#FFFCF0'), 152),    # bright white-gold wisps (~60 %)
    ]
    motes = [(3, 0.0), (11, 0.37), (7, 0.71)]
    for i in range(frames):
        t = i / frames
        tau = 2 * np.pi * t
        # domain warp with integer wave numbers keeps the swirl tileable; the time terms loop
        wx = 0.16 * np.sin(2 * np.pi * ys + tau) + 0.07 * np.sin(2 * np.pi * (xs + ys) - tau)
        wy = 0.16 * np.cos(2 * np.pi * xs - tau) + 0.07 * np.cos(2 * np.pi * (xs - ys) + 2 * tau)
        u, v = xs + wx, ys + wy
        f = (np.sin(2 * np.pi * (u + v) + tau) + 0.8 * np.sin(2 * np.pi * (2 * u - v) - tau)
             + 0.6 * np.cos(2 * np.pi * (u - 2 * v) + 2 * tau) + 0.45 * np.sin(2 * np.pi * (3 * u + v) - 2 * tau))
        f = (f - f.min()) / (np.ptp(f) + 1e-9)
        c = Canvas(S, S)
        for y in range(S):
            for x in range(S):
                band = min(3, int(f[y, x] * 4.0))
                col, a = tones[band]
                c.put(x, y, with_alpha(col, a))
        # drifting motes rise one pixel every two frames, wrapping
        for mx, phase in motes:
            my = int(round((1 - (t + phase) % 1.0) * S)) % S
            c.put(mx, my, (255, 252, 236, 210))
            c.put(mx, (my + 1) % S, (255, 240, 200, 150))
        out.append(c.image())
    strip = Canvas(S, S * frames).image()
    for i, im in enumerate(out):
        strip.paste(im, (0, i * S))
    return strip, out


def eclipse_waygate_particle():
    """Break/landing particles for the waygate core, frame and veil: carved slate with gold and the vortex."""
    base = np.array(eclipse_stone_bricks(seed=21))
    c = Canvas(S, S)
    c.a = base.copy()
    for x, y in ((3, 3), (4, 3), (12, 10), (11, 11)):
        c.put(x, y, GOLD[3] if (x + y) % 2 else GOLD[4])
    for (x, y), col in {(6, 10): VIOLET[3], (7, 10): VIOLET[4], (7, 11): GRACE[3], (8, 11): VIOLET[2],
                        (6, 11): ECLIPSE_BLACK, (13, 3): SOUL[1], (2, 12): SOUL[0]}.items():
        c.put(x, y, col)
    return c.image()


def build():
    save_png(eclipse_stone(), 'textures/block/eclipse_stone.png')
    save_png(cobbled_eclipse_stone(), 'textures/block/cobbled_eclipse_stone.png')
    save_png(eclipse_stone_bricks(), 'textures/block/eclipse_stone_bricks.png')
    save_png(eclipse_stone_bricks(cracked=True), 'textures/block/cracked_eclipse_stone_bricks.png')
    save_png(chiseled_eclipse_stone_bricks(), 'textures/block/chiseled_eclipse_stone_bricks.png')
    save_png(graceroot_log(), 'textures/block/graceroot_log.png')
    save_png(graceroot_log_top(), 'textures/block/graceroot_log_top.png')
    save_png(graceroot_planks(), 'textures/block/graceroot_planks.png')
    save_png(gilded_leaves(), 'textures/block/gilded_leaves.png')
    save_png(weathered_gravestone(), 'textures/block/weathered_gravestone.png')
    strip, frames = fog_gate_frames()
    save_png(strip, 'textures/block/fog_gate.png')
    save_json({'animation': {'interpolate': True, 'frametime': 3}}, 'textures/block/fog_gate.png.mcmeta')
    save_png(eclipse_waygate_particle(), 'textures/block/eclipse_waygate_particle.png')
    return frames
