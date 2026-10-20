"""16x16 item icons (textures/item/) for the Eclipsed Realm: hand-placed character grids drawn from
the realm ramps, then given vanilla-style selective dark outlines."""
import math

from common import (GOLD, GRACE, SOUL, STEEL, STONE, VIOLET, ECLIPSE_BLACK, Canvas, grid_image, save_png,
                    with_alpha, hexc)

OUT = (14, 10, 22, 255)

INK = {
    'a': VIOLET[1], 'b': VIOLET[2], 'c': VIOLET[3], 'd': VIOLET[4], 'e': VIOLET[5], 'k': ECLIPSE_BLACK,
    'G': GOLD[2], 'H': GOLD[3], 'I': GOLD[4], 'J': GRACE[4], 'K': GRACE[5],
    '1': GRACE[1], '2': GRACE[2], '3': GRACE[3],
    'L': STONE[5], 'M': STONE[4], 'N': STONE[3], 'D': STONE[2], 'E': STONE[1],
    'o': STEEL[2], 'p': STEEL[3], 'q': STEEL[4], 'r': STEEL[5],
    'x': hexc('#3A2A1E'), 'y': hexc('#5A4430'),
    's': SOUL[1], 't': SOUL[2],
}


def eclipse_shard():
    """A dark crystal shard (lit upper facet, shadowed lower facet) with a gold eclipse ring glinting in it."""
    rows = [
        '..............K.',
        '............eed.',
        '...........edddc',
        '..........eddccb',
        '.........edHIHcb',
        '........eddIkJcb',
        '.......edcdHIHb.',
        '......edcccdcba.',
        '.....edccbbbba..',
        '....edcbbbaaa...',
        '...edcbbaaa.....',
        '..ddcbaaa.......',
        '..dcbaa.........',
        '..cba...........',
        '..ba............',
        '................',
    ]
    return grid_image(rows, INK, OUT)


def eclipse_waygate():
    rows = [
        '................',
        '.....MMMMMM.....',
        '.MMMMLLLLLLMMMM.',
        '.NNNNNNNNNNNNNN.',
        '.DHDDDDDDDDDDHD.',
        '..MND..II..MND..',
        '..MsD.IkkI.MsD..',
        '..MND3kkkkHMND..',
        '..MND3kkkkHMND..',
        '..MsD.GkkG.MsD..',
        '..MND.aGGa.MND..',
        '..MND.abba.MND..',
        '..MND.aaaa.MND..',
        '.MMNDDHHHHMMNDD.',
        '.DDDDDDDDDDDDDD.',
        '................',
    ]
    return grid_image(rows, INK, OUT)


def fog_gate():
    """A curl of gold-white fog: a spiral of soft translucent bands with a few bright motes."""
    c = Canvas(16, 16)
    cx, cy = 7.5, 8.0
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, (y - cy) * 1.1
            r = math.hypot(dx, dy)
            th = math.atan2(dy, dx)
            if r > 7.4:
                continue
            # distance to an Archimedean spiral r = 1.15 * (th + 2 pi k)
            best = 9.0
            for k in range(-1, 4):
                rs = 1.15 * (th + math.pi + 2 * math.pi * k)
                best = min(best, abs(r - rs))
            if best < 0.75:
                col, a = (hexc('#FFFCF0'), 245) if r < 4.5 else (hexc('#FFF0C4'), 225)
            elif best < 1.5:
                col, a = (hexc('#F6D068'), 200) if r < 5.5 else (hexc('#E8B848'), 170)
            elif r < 6.5:
                col, a = hexc('#E0A838'), 90
            else:
                continue
            c.put(x, y, with_alpha(col, a))
    for x, y in ((12, 2), (2, 12), (13, 12)):
        c.put(x, y, (255, 250, 230, 255))
    return c.image()


def build():
    save_png(eclipse_shard(), 'textures/item/eclipse_shard.png')
    save_png(eclipse_waygate(), 'textures/item/eclipse_waygate.png')
    save_png(fog_gate(), 'textures/item/fog_gate.png')
