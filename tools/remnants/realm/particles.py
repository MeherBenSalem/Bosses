"""Particle sprites (textures/particle/, 8x8 frames) and their particles/*.json frame lists.

Vanilla SpriteSet.get(age, lifetime) walks the list from the first frame at birth to the last at
death, so frame 0 is the brightest and the last the faintest."""
import math

from common import GRACE, SOUL, VIOLET, Canvas, rl, save_json, save_png, with_alpha

N = 6


def grace_mote(i):
    """Soft golden ember: white-gold core, gold body, translucent halo; shrinks and dims with age."""
    c = Canvas(8, 8)
    t = i / (N - 1)
    radius = 3.0 - 2.2 * t
    cx = cy = 3.5
    for y in range(8):
        for x in range(8):
            r = math.hypot(x - cx, y - cy)
            if r > radius + 0.6:
                continue
            k = r / max(0.6, radius)
            if k < 0.35:
                col, a = GRACE[5] if t < 0.5 else GRACE[4], 255
            elif k < 0.7:
                col, a = GRACE[3], 235
            elif k < 1.0:
                col, a = GRACE[2], 170
            else:
                col, a = GRACE[1], 90
            a = int(a * (1 - 0.55 * t))
            c.put(x, y, with_alpha(col, a))
    return c.image()


STAR = {
    0: [(0, 0, 3), (1, 0, 2), (-1, 0, 2), (0, 1, 2), (0, -1, 2), (2, 0, 1), (-2, 0, 1), (0, 2, 1), (0, -2, 1),
        (3, 0, 0), (-3, 0, 0), (0, 3, 0), (0, -3, 0), (1, 1, 0), (-1, -1, 0), (1, -1, 0), (-1, 1, 0)],
    1: [(0, 0, 3), (1, 1, 2), (-1, -1, 2), (1, -1, 2), (-1, 1, 2), (2, 2, 0), (-2, -2, 0), (2, -2, 0), (-2, 2, 0),
        (1, 0, 1), (-1, 0, 1), (0, 1, 1), (0, -1, 1)],
    2: [(0, 0, 3), (1, 0, 2), (-1, 0, 2), (0, 1, 2), (0, -1, 2), (2, 0, 0), (-2, 0, 0), (0, 2, 0), (0, -2, 0)],
    3: [(0, 0, 2), (1, 0, 1), (-1, 0, 1), (0, 1, 1), (0, -1, 1)],
    4: [(0, 0, 2), (1, 1, 0), (-1, -1, 0)],
    5: [(0, 0, 0)],
}


def eclipse_spark(i):
    """Cyan-violet four-point spark that twinkles (straight / diagonal) and shrinks."""
    c = Canvas(8, 8)
    tones = [with_alpha(VIOLET[4], 200), with_alpha(VIOLET[5], 230), with_alpha(SOUL[2], 255), with_alpha(SOUL[3], 255)]
    for dx, dy, tone in STAR[i]:
        c.put(3 + dx, 3 + dy, tones[tone] if i < 5 else with_alpha(VIOLET[4], 170))
    return c.image()


def build():
    for name, fn in (('grace_mote', grace_mote), ('eclipse_spark', eclipse_spark)):
        frames = []
        for i in range(N):
            save_png(fn(i), f'textures/particle/{name}_{i}.png')
            frames.append(rl(f'{name}_{i}'))
        save_json({'textures': frames}, f'particles/{name}.json')
