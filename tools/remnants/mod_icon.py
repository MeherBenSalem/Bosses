"""Mod icon: a crowned Remnant skull under the black eclipse sun, on the Realm's night sky.

Drawn on a 40 x 40 pixel grid and scaled up without filtering. `python tools/remnants/mod_icon.py` writes
docs/models/remnants/icon/ (600, 400, 128 and 64 px previews); `--install` also copies the 600 px icon over
logo.png and textures/logo.png in both workspaces.
"""
import argparse
import math
import random
import shutil
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent / 'realm'))
from common import ECLIPSE_BLACK, GOLD, GRACE, SOUL, VIOLET, hexc, mix  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/models/remnants/icon'
SIZE = 40
BONE = [hexc(c) for c in ('#3A3442', '#5E5664', '#8E8590', '#BDB4B4', '#E2DACF', '#F6F0E4')]

# Crowned skull, 18 wide. Upper case and digits are opaque; '.' is transparent.
#   k crown dark  c crown mid  C crown light  * gem
#   1..5 bone, dark to light  e eye socket  g eye glow  G eye core  n nose  t tooth gap
SKULL = [
    '..k.....kk.....k..',
    '.kCk...kCCk...kCk.',
    '.kCck.kcCCck.kcCk.',
    '.kccckcc**cckccck.',
    '.kcccccccccccccck.',
    '.kc*cc*cccc*cc*ck.',
    '.KKKKKKKKKKKKKKKK.',
    '..23445555554432..',
    '.2344555555554432.',
    '234455555555554432',
    '234455555555554431',
    '23eeee455554eeee31',
    '2egGge455554egGge1',
    '2egGge445544egGge1',
    '23eeee344443eeee21',
    '.2333334nn4333321.',
    '..22333nnnn33321..',
    '...223333333321...',
    '...24t4t44t4t42...',
    '...233444444332...',
    '....2222222222....',
]

COLOURS = {
    'k': GOLD[2], 'K': GOLD[1], 'c': GOLD[3], 'C': GOLD[5], '*': SOUL[2],
    '1': BONE[0], '2': BONE[1], '3': BONE[2], '4': BONE[3], '5': BONE[4],
    'e': hexc('#0B0812'), 'g': SOUL[1], 'G': SOUL[3], 'n': hexc('#140E1E'), 't': hexc('#140E1E'),
}
CX, CY, DISC = 20.0, 13.5, 8.5
HORIZON = 35


def sky(x, y):
    """Violet night overhead, warming to the Realm's gold dusk at the horizon, lit around the eclipse."""
    t = min(1.0, max(0.0, (y - 8) / (HORIZON - 8)))
    base = mix(VIOLET[1], VIOLET[2], min(1.0, t * 1.6))
    base = mix(base, GOLD[1], max(0.0, t - 0.55) * 1.4)
    glow = max(0.0, 1 - math.hypot(x + 0.5 - CX, y + 0.5 - CY) / 19)
    return mix(base, GRACE[1], glow * glow * 0.45)


def corona(rng):
    """Flame lengths around the disc, smooth but uneven, like the in-game eclipse."""
    lobes = [rng.uniform(0.0, 1.0) for _ in range(10)]

    def length(angle):
        t = (angle / (2 * math.pi)) * len(lobes)
        i = int(t) % len(lobes)
        f = t - int(t)
        f = f * f * (3 - 2 * f)
        return 2.4 + 2.4 * (lobes[i] * (1 - f) + lobes[(i + 1) % len(lobes)] * f)
    return length


def draw():
    rng = random.Random(1907)
    im = Image.new('RGBA', (SIZE, SIZE))
    px = im.load()
    flame = corona(rng)
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = sky(x, y)
    for _ in range(22):
        x, y = rng.randrange(2, SIZE - 2), rng.randrange(2, 24)
        if math.hypot(x + 0.5 - CX, y + 0.5 - CY) > DISC + 7:
            px[x, y] = GRACE[5] if rng.random() < 0.3 else hexc('#CFC6E8')
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x + 0.5 - CX, y + 0.5 - CY
            r = math.hypot(dx, dy)
            reach = flame(math.atan2(dy, dx) % (2 * math.pi))
            if r <= DISC:
                px[x, y] = ECLIPSE_BLACK
            elif r <= DISC + 1.0:
                px[x, y] = GRACE[5]
            elif r <= DISC + reach:
                t = (r - DISC - 1.0) / max(0.5, reach - 1.0)
                px[x, y] = GRACE[4] if t < 0.25 else GRACE[3] if t < 0.55 else GRACE[2] if t < 0.8 else GRACE[1]
            elif r <= DISC + reach + 1.5:
                px[x, y] = mix(px[x, y], GRACE[1], 0.45)
    # Short soul-light rays in the corona's upper half, as on the Realm sky.
    for angle in (3.55, 4.25, 5.05, 5.85):
        for step in (DISC + 1.5, DISC + 2.5, DISC + 3.5):
            x = int(CX + math.cos(angle) * step)
            y = int(CY + math.sin(angle) * step)
            px[x, y] = SOUL[1] if step < DISC + 3 else SOUL[0]
    # Ridge with a headstone and a grave cross, silhouetted against the dusk.
    for x in range(SIZE):
        top = HORIZON + round(1.2 * math.sin(x * 0.42 + 0.6))
        for y in range(top, SIZE):
            px[x, y] = mix(VIOLET[1], ECLIPSE_BLACK, min(1.0, (y - top) / 5))
    shade = VIOLET[0]
    for x, y in ((5, 31), (6, 31)):
        px[x, y] = shade
    for x in range(4, 8):
        for y in range(32, 35):
            px[x, y] = shade
    for x, y in ((34, 29), (34, 30), (33, 31), (34, 31), (35, 31), (34, 32), (34, 33), (34, 34)):
        px[x, y] = shade
    ox, oy = 11, 14
    for j, row in enumerate(SKULL):
        for i, ch in enumerate(row):
            if ch in COLOURS:
                px[ox + i, oy + j] = COLOURS[ch]
    # Rim light along the top of the skull, from the eclipse behind it.
    for i, ch in enumerate(SKULL[7]):
        if ch == '5':
            px[ox + i, oy + 7] = BONE[5]
    return rounded(im, 4)


def rounded(im, radius):
    """Transparent rounded corners and a one-pixel dark rim."""
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            ex = max(radius - x, x - (w - 1 - radius), 0)
            ey = max(radius - y, y - (h - 1 - radius), 0)
            d = math.hypot(ex, ey)
            if d > radius + 0.2:
                px[x, y] = (0, 0, 0, 0)
            elif d > radius - 0.8 or x in (0, w - 1) or y in (0, h - 1):
                px[x, y] = VIOLET[0]
    return im


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--install', action='store_true', help='copy the icon into both workspaces')
    args = parser.parse_args()
    icon = draw()
    OUT.mkdir(parents=True, exist_ok=True)
    for size in (600, 400, 128, 64):
        path = OUT / f'icon_{size}.png'
        icon.resize((size, size), Image.NEAREST).save(path)
        print(path.relative_to(ROOT))
    if args.install:
        for version in ('1.20.1', '1.21.1'):
            res = ROOT / version / 'common/src/main/resources'
            for target in (res / 'logo.png', res / 'assets/remnant_bosses/textures/logo.png'):
                shutil.copyfile(OUT / 'icon_600.png', target)
                print('installed', target.relative_to(ROOT))


if __name__ == '__main__':
    main()
