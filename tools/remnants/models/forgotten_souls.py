"""Forgotten Souls: the Seyr memories that still drift through the world.

Spirit material is translucent and lit from within; anything that was an object (tome, staff,
lantern, the Collector's machinery) stays solid so the eye reads what is ghost and what is relic.
Editor conventions as in hollow_sovereign.py.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paint import Material  # noqa: E402
from rig import Rig  # noqa: E402

PALETTE = {
    'spirit': Material(['#15202E', '#22344A', '#334C66', '#4A6C88', '#6E93AE', '#A3C4D6', '#D8ECF4'], 'cloth', torn=True, alpha=185),
    'spirit_glow': Material(['#1C3A4A', '#2B5A6E', '#4A8EA4', '#7CC3D4', '#BDEBF2'], 'soul', glow=True, alpha=200),
    'page': Material(['#6B6150', '#8F846C', '#B5AA8E', '#D8CFB2', '#F1EBD7'], 'bone'),
}
RUNES = ['#.#', '.#.', '#.#']


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def memory_wraith():
    r = Rig('memory_wraith', (128, 64), palette=PALETTE)
    r.mark_upright('torso', 'head')
    r.bone('torso', (0, 14, 0))
    r.bone('head', (0, 27, 0), 'torso')
    r.bone('tail', (0, 14, 0.5), 'torso')
    r.bone('tail_tip', (0, 8, 1), 'tail')
    r.bone_pair('arm_{side}', (4.6, 25, 0), 'torso')
    r.bone('tome', (6, 18, -3), 'arm_left')
    r.bone('pages', (0, 22, 0), 'torso')
    # robed body that thins into wisps
    r.box('torso', 'chest', (0, 22, 0.4), (8, 9, 5), 'spirit', decals=[('band', {'row': 0, 'size': 1, 'mat': 'crystal'})])
    r.box('torso', 'stole', (0, 22, -2.3), (2.6, 9, 0.6), 'crystal', decals=[('glyph', {'bitmap': RUNES, 'mat': 'page', 'faces': 'north'})])
    r.box('tail', 'robe_lower', (0, 14.5, 0.6), (7, 7, 4.6), 'spirit')
    r.box('tail_tip', 'wisp', (0, 6.5, 1), (4.4, 7, 3), 'spirit')
    r.box('tail_tip', 'wisp_end', (0.6, 1.6, 1.4), (2, 3.4, 1.6), 'spirit_glow')
    # deep hood, two candle-eyes
    r.box('head', 'hood', (0, 30.4, 0.4), (6.6, 6.6, 6.6), 'spirit')
    r.box('head', 'hood_peak', (0, 34.2, 1.6), (3.6, 2, 3.6), 'spirit', rot=(-18, 0, 0))
    r.box('head', 'face_void', (0, 29.8, -2.95), (4.4, 4, 0.2), 'void')
    r.pair('head', 'eye_{side}', (1.0, 30.4, -3.1), (0.9, 0.6, 0.2), 'spirit_glow')
    # sleeves and hands; an open tome in the left
    r.pair('arm_{side}', '{side}_sleeve', (5.4, 21, 0), (3.2, 8, 3.4), 'spirit', rot=(0, 0, -6))
    r.pair('arm_{side}', '{side}_hand', (5.8, 16.4, -0.4), (1.8, 2, 2), 'spirit_glow')
    r.box('tome', 'cover', (6, 18, -4.6), (6, 0.6, 4.4), 'leather', rot=(-20, 0, 0))
    r.box('tome', 'leaves_l', (7.4, 18.6, -4.6), (2.6, 0.6, 4), 'page', rot=(-20, 0, -14))
    r.box('tome', 'leaves_r', (4.6, 18.6, -4.6), (2.6, 0.6, 4), 'page', rot=(-20, 0, 14))
    r.box('tome', 'rune_light', (6, 19.6, -4.8), (2, 0.4, 2), 'crystal')
    for i, (a, h) in enumerate([(40, 2), (160, -3), (280, 4)]):
        x, z = math.sin(math.radians(a)) * 9, math.cos(math.radians(a)) * 9
        r.box('pages', f'page_{i}', (x, 22 + h, z), (2.4, 3, 0.3), 'page', rot=(8, a, 12),
              decals=[('glyph', {'bitmap': ['#.#', '...', '#.#'], 'mat': 'void', 'faces': ('north', 'south')})])

    r.clip('idle', 3.0, True).poses([
        (0.0, {'torso': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}, 'tail_tip': {'rot': [0, 0, 0]}, 'pages': {'rot': [0, 0, 0]},
               'arm_left': {'rot': [40, 0, 10]}, 'arm_right': {'rot': [10, 0, -8]}}),
        (1.5, {'torso': {'pos': [0, 1.4, 0], 'rot': [4, 0, 0]}, 'tail': {'rot': [10, 0, 4]}, 'tail_tip': {'rot': [16, 0, -6]}, 'pages': {'rot': [0, 180, 0]}}),
        (3.0, {'torso': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}, 'tail_tip': {'rot': [0, 0, 0]}, 'pages': {'rot': [0, 360, 0]}}),
    ])
    r.clip('drift', 1.2, True).poses([
        (0.0, {'torso': {'rot': [14, 0, 0]}, 'tail': {'rot': [26, 0, 0]}, 'tail_tip': {'rot': [30, 0, 0]}, 'arm_left': {'rot': [40, 0, 10]}}),
        (0.6, {'torso': {'rot': [16, 0, 0]}, 'tail': {'rot': [32, 0, 6]}, 'tail_tip': {'rot': [40, 0, -6]}}),
        (1.2, {'torso': {'rot': [14, 0, 0]}, 'tail': {'rot': [26, 0, 0]}, 'tail_tip': {'rot': [30, 0, 0]}}),
    ])
    # cast: right hand draws back, soul bolt released at 0.7
    r.clip('cast', 1.2).poses([
        (0.0, {'arm_right': {'rot': [10, 0, -8]}, 'torso': {'rot': [0, 0, 0]}, 'tome': {'rot': [0, 0, 0]}}),
        (0.55, {'arm_right': {'rot': [60, 0, -50]}, 'torso': {'rot': [-6, 20, 0]}, 'tome': {'rot': [-20, 0, 0]}}, 'easeOutCubic'),
        (0.7, {'arm_right': {'rot': [100, 0, 10]}, 'torso': {'rot': [8, -14, 0]}}, 'easeInExpo'),
        (1.2, {'arm_right': {'rot': [10, 0, -8]}, 'torso': {'rot': [0, 0, 0]}, 'tome': {'rot': [0, 0, 0]}}),
    ])
    # blink-dodge: collapses into wisps and reforms (Java moves it at 0.3)
    r.clip('blink', 0.6).poses([(0.0, {'torso': {'scale': 1}}), (0.3, {'torso': {'scale': [0.1, 1.6, 0.1]}}, 'easeInBack'), (0.6, {'torso': {'scale': 1}}, 'easeOutBack')])
    r.clip('hurt', 0.4).poses([(0.0, {'torso': {'rot': [0, 0, 0]}}), (0.1, {'torso': {'rot': [-14, 8, 0]}}, 'easeOutExpo'), (0.4, {'torso': {'rot': [0, 0, 0]}})])
    r.clip('death', 1.6, 'hold').poses([
        (0.0, {'torso': {'scale': 1, 'pos': [0, 0, 0]}, 'pages': {'scale': 1}}),
        (0.8, {'torso': {'scale': [1.3, 0.6, 1.3], 'pos': [0, -4, 0]}, 'pages': {'scale': 2}}, 'easeOutCubic'),
        (1.6, {'torso': {'scale': 0, 'pos': [0, 6, 0]}, 'pages': {'scale': 0}}, 'easeInCubic'),
    ])
    return r


def lost_pilgrim():
    r = Rig('lost_pilgrim', (128, 64), palette=PALETTE)
    r.mark_upright('torso', 'head')
    r.bone('hips', (0, 12, 0))
    r.bone_pair('leg_{side}', (1.8, 12, 0), 'hips')
    r.bone('torso', (0, 13, 0), 'hips')
    r.bone('head', (0, 24, 0), 'torso')
    r.bone_pair('arm_{side}', (4.4, 22.5, 0), 'torso')
    r.bone('staff', (-4.8, 13.6, -1), 'arm_right')
    r.bone('lantern', (-4.8, 31, -1), 'staff')
    r.box('hips', 'robe_skirt', (0, 8.6, 0.2), (7, 9, 5), 'spirit')
    r.pair('leg_{side}', '{side}_foot_wisp', (1.8, 2.2, 0), (2.4, 4, 2.6), 'spirit_glow')
    r.box('torso', 'cloak', (0, 18.6, 0.2), (7.6, 10, 5.4), 'spirit', decals=[('band', {'row': 2, 'size': 1, 'mat': 'leather'})])
    r.box('torso', 'pack', (0, 19.6, 4.2), (6, 6.4, 3), 'leather', decals=[('stitch', {'mat': 'page', 'row': 1})])
    r.box('torso', 'bedroll', (0, 23.6, 4.2), (7, 2, 2.4), 'royal_lining')
    r.box('torso', 'cord', (0, 21, -2.8), (5, 0.6, 0.3), 'leather', rot=(0, 0, 30))
    r.box('head', 'hood', (0, 27.4, 0.3), (6, 6.2, 6.2), 'spirit')
    r.box('head', 'hat_brim', (0, 30.4, 0.3), (9, 0.8, 9), 'leather')
    r.box('head', 'hat_crown', (0, 31.8, 0.3), (5, 2.2, 5), 'leather')
    r.box('head', 'face_void', (0, 26.8, -2.85), (4, 3.6, 0.2), 'void')
    r.pair('head', 'eye_{side}', (0.9, 27.2, -3), (0.7, 0.5, 0.2), 'spirit_glow')
    r.pair('arm_{side}', '{side}_sleeve', (4.8, 19, 0), (2.8, 7.6, 3), 'spirit')
    r.pair('arm_{side}', '{side}_hand', (4.8, 14.6, -0.4), (1.6, 1.8, 1.8), 'spirit_glow')
    r.box('staff', 'shaft', (-4.8, 16, -1), (1, 30, 1), 'heartwood')
    r.box('staff', 'crook', (-4.8, 31.4, -2.2), (1, 1, 3.4), 'heartwood')
    r.box('lantern', 'lantern_cage', (-4.8, 28.4, -3.4), (2.6, 3.4, 2.6), 'verdigris')
    r.box('lantern', 'lantern_soul', (-4.8, 28.4, -3.4), (1.6, 2.2, 1.6), 'soul')
    r.box('lantern', 'bell', (-4.8, 26, -3.4), (1.4, 1.2, 1.4), 'gold')
    rest = {'arm_right': {'rot': [16, 0, -6]}, 'staff': {'rot': [-16, 0, 6]}, 'arm_left': {'rot': [6, 0, 4]}}
    r.clip('idle', 4.0, True).poses([
        (0.0, merge(rest, {'head': {'rot': [10, 0, 0]}, 'torso': {'rot': [8, 0, 0]}, 'lantern': {'rot': [0, 0, 0]}})),
        (2.0, {'head': {'rot': [16, 14, 0]}, 'torso': {'rot': [10, 0, 0]}, 'lantern': {'rot': [6, 0, 8]}}),
        (4.0, {'head': {'rot': [10, 0, 0]}, 'torso': {'rot': [8, 0, 0]}, 'lantern': {'rot': [0, 0, 0]}}),
    ])
    r.clip('walk', 2.0, True).poses([
        (0.0, merge(rest, {'leg_left': {'rot': [16, 0, 0]}, 'leg_right': {'rot': [-16, 0, 0]}, 'torso': {'rot': [10, 0, 0]}, 'lantern': {'rot': [8, 0, 0]},
                           'hips': {'pos': [0, 0, 0]}})),
        (0.5, {'hips': {'pos': [0, 0.6, 0]}, 'lantern': {'rot': [-8, 0, 0]}}),
        (1.0, {'leg_left': {'rot': [-16, 0, 0]}, 'leg_right': {'rot': [16, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'lantern': {'rot': [8, 0, 0]}}),
        (1.5, {'hips': {'pos': [0, 0.6, 0]}, 'lantern': {'rot': [-8, 0, 0]}}),
        (2.0, {'leg_left': {'rot': [16, 0, 0]}, 'leg_right': {'rot': [-16, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'lantern': {'rot': [8, 0, 0]}}),
    ])
    # disturbed: the bell rings (0.6) and the pilgrim turns on you
    r.clip('disturbed', 1.5).poses([
        (0.0, merge(rest, {'head': {'rot': [10, 0, 0]}, 'torso': {'rot': [8, 0, 0]}})),
        (0.6, {'staff': {'rot': [-40, 0, 20]}, 'lantern': {'rot': [30, 0, 40]}, 'head': {'rot': [-14, 0, 0]}, 'torso': {'rot': [-10, 0, 0]}}, 'easeOutBack'),
        (1.5, merge(rest, {'head': {'rot': [0, 0, 0]}, 'torso': {'rot': [4, 0, 0]}})),
    ])
    r.clip('strike', 1.2).poses([
        (0.0, merge(rest, {'torso': {'rot': [4, 0, 0]}})),
        (0.5, {'arm_right': {'rot': [150, 0, -10]}, 'staff': {'rot': [0, 0, 0]}, 'torso': {'rot': [-10, 14, 0]}}, 'easeOutCubic'),
        (0.65, {'arm_right': {'rot': [50, 0, 4]}, 'staff': {'rot': [40, 0, 0]}, 'torso': {'rot': [18, -8, 0]}}, 'easeInExpo'),
        (1.2, merge(rest, {'torso': {'rot': [4, 0, 0]}})),
    ])
    r.clip('hurt', 0.4).poses([(0.0, {'torso': {'rot': [8, 0, 0]}}), (0.1, {'torso': {'rot': [-10, 8, 0]}}, 'easeOutExpo'), (0.4, {'torso': {'rot': [8, 0, 0]}})])
    r.clip('death', 2.0, 'hold').poses([
        (0.0, {'root': {'scale': 1, 'pos': [0, 0, 0]}, 'torso': {'rot': [8, 0, 0]}}),
        (1.0, {'torso': {'rot': [40, 0, 0]}, 'root': {'pos': [0, -3, 0], 'scale': [1, 0.8, 1]}}, 'easeOutCubic'),
        (2.0, {'root': {'pos': [0, 4, 0], 'scale': 0}}, 'easeInCubic'),
    ])
    return r


def soul_collector():
    r = Rig('soul_collector', (128, 128), palette=PALETTE)
    r.mark_upright('body')
    r.bone('body', (0, 24, 0))
    r.bone('mask', (0, 33, -3.4), 'body')
    r.bone('cage_souls', (0, 24, 0), 'body')
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        x, z = math.sin(a) * 4.6, math.cos(a) * 4.6
        r.bone(f'hook_{i}', (x, 18, z), 'body')
        r.bone(f'hook_tip_{i}', (x * 2.0, 8, z * 2.0), f'hook_{i}')
    for i in range(3):
        r.bone(f'jar_{i}', ((i - 1) * 4.2, 17, 4.6), 'body')
    # lantern body: a verdigris cage holding harvested souls
    r.box('body', 'roof', (0, 31, 0), (11, 2, 11), 'verdigris', decals=[('band', {'row': 0, 'size': 1, 'mat': 'gold'})])
    r.box('body', 'roof_peak', (0, 33.6, 0), (6, 3, 6), 'bronze')
    r.box('body', 'roof_ring', (0, 36.4, 0), (2.6, 2.6, 1), 'bronze')
    r.box('body', 'floor', (0, 17.6, 0), (11, 1.6, 11), 'verdigris')
    for i, (x, z) in enumerate([(-5, -5), (5, -5), (5, 5), (-5, 5)]):
        r.box('body', f'post_{i}', (x, 24.4, z), (1.4, 12, 1.4), 'bronze')
    for i, (x, z, w, d) in enumerate([(0, -5, 9, 0.6), (0, 5, 9, 0.6), (-5, 0, 0.6, 9), (5, 0, 0.6, 9)]):
        r.box('body', f'bars_{i}', (x, 24.4, z), (w, 11, d), 'verdigris_grille')
    r.box('cage_souls', 'harvest', (0, 24.4, 0), (6, 8, 6), 'soul')
    r.box('cage_souls', 'wisp_a', (1.6, 27.6, -1.4), (2, 2.4, 2), 'spirit_glow')
    r.box('cage_souls', 'wisp_b', (-1.8, 21.6, 1.2), (2, 2, 2), 'spirit_glow')
    # a harvester's beaked mask of hammered copper, two round soul-lit eyes
    r.box('mask', 'mask_plate', (0, 30.2, -6.2), (6, 6.4, 1), 'bronze', decals=[('rim', {'mat': 'verdigris', 'faces': 'north'})])
    r.box('mask', 'mask_brow', (0, 32.9, -6.9), (6.4, 1, 0.6), 'verdigris')
    r.box('mask', 'beak', (0, 28.4, -8.2), (2, 2, 3.6), 'bronze', rot=(28, 0, 0))
    r.box('mask', 'beak_tip', (0, 26.6, -9.8), (1.2, 1.4, 1.8), 'bronze', rot=(40, 0, 0))
    for side, x in (('left', 1.6), ('right', -1.6)):
        r.box('mask', f'eye_ring_{side}', (x, 31.2, -6.85), (2, 2, 0.4), 'verdigris')
        r.box('mask', f'eye_{side}', (x, 31.2, -7.1), (1.2, 1.2, 0.2), 'soul')
    # four hooked harvester arms
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        x, z = math.sin(a) * 4.6, math.cos(a) * 4.6
        deg = math.degrees(a)
        r.box(f'hook_{i}', f'arm_{i}', (x * 1.5, 13, z * 1.5), (1.2, 10, 1.2), 'bronze', rot=(0, deg, 0))
        r.box(f'hook_tip_{i}', f'blade_{i}', (x * 2.0, 5.4, z * 2.0), (0.8, 5.4, 3), 'blade', rot=(-20, deg, 0))
        r.box(f'hook_tip_{i}', f'barb_{i}', (x * 2.0, 3.2, z * 2.0 - 1.4), (0.8, 1.4, 1.2), 'blade', rot=(0, deg, 0))
    for i in range(3):
        x = (i - 1) * 4.2
        r.box(f'jar_{i}', f'jar_chain_{i}', (x, 14.6, 4.6), (0.5, 5, 0.5), 'chain')
        r.box(f'jar_{i}', f'jar_{i}_glass', (x, 10.6, 4.6), (2.4, 3.4, 2.4), 'spirit_glow')
        r.box(f'jar_{i}', f'jar_{i}_lid', (x, 12.6, 4.6), (2.6, 0.8, 2.6), 'bronze')
    hooks = {f'hook_{i}': {'rot': [0, 0, 0]} for i in range(4)}
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(hooks, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'cage_souls': {'rot': [0, 0, 0], 'scale': 1},
                            **{f'jar_{i}': {'rot': [0, 0, 0]} for i in range(3)}})),
        (1.5, {'body': {'pos': [0, 1.4, 0], 'rot': [0, 20, 0]}, 'cage_souls': {'rot': [0, 180, 0], 'scale': 1.1},
               **{f'hook_{i}': {'rot': [(-1) ** i * 6, 0, 0]} for i in range(4)},
               **{f'jar_{i}': {'rot': [8 * (-1) ** i, 0, 6]} for i in range(3)}}),
        (3.0, merge(hooks, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'cage_souls': {'rot': [0, 360, 0], 'scale': 1},
                            **{f'jar_{i}': {'rot': [0, 0, 0]} for i in range(3)}})),
    ])
    # mark: the mask fixes on a spot (Java places the dodgeable soul-burst mark at 0.8)
    r.clip('mark', 1.6).poses([
        (0.0, {'mask': {'rot': [0, 0, 0]}, 'cage_souls': {'scale': 1}, 'body': {'rot': [0, 0, 0]}}),
        (0.8, {'mask': {'rot': [24, 0, 0]}, 'cage_souls': {'scale': 1.4}, 'body': {'rot': [16, 0, 0]}}, 'easeOutCubic'),
        (1.6, {'mask': {'rot': [0, 0, 0]}, 'cage_souls': {'scale': 1}, 'body': {'rot': [0, 0, 0]}}),
    ])
    # harvest: hooks splay open and snap shut (hit at 0.9)
    open_hooks = {f'hook_{i}': {'rot': [-50 if i < 2 else 50, 0, 0]} for i in range(4)}
    r.clip('harvest', 1.6).poses([
        (0.0, merge(hooks, {'body': {'pos': [0, 0, 0]}})),
        (0.7, merge(open_hooks, {'body': {'pos': [0, 2, 0]}}), 'easeOutCubic'),
        (0.9, merge({f'hook_{i}': {'rot': [20 if i < 2 else -20, 0, 0]} for i in range(4)}, {'body': {'pos': [0, -2, -2]}}), 'easeInExpo'),
        (1.6, merge(hooks, {'body': {'pos': [0, 0, 0]}})),
    ])
    # summon lesser spirits: the cage flares and jars swing out (spawn at 1.0)
    r.clip('summon', 2.0).poses([
        (0.0, {'cage_souls': {'scale': 1}, **{f'jar_{i}': {'rot': [0, 0, 0]} for i in range(3)}}),
        (1.0, {'cage_souls': {'scale': 1.8}, **{f'jar_{i}': {'rot': [-60, 0, (i - 1) * 30]} for i in range(3)}}, 'easeOutBack'),
        (2.0, {'cage_souls': {'scale': 1}, **{f'jar_{i}': {'rot': [0, 0, 0]} for i in range(3)}}),
    ])
    r.clip('hurt', 0.4).poses([(0.0, {'body': {'rot': [0, 0, 0]}}), (0.1, {'body': {'rot': [-10, 0, 10]}}, 'easeOutExpo'), (0.4, {'body': {'rot': [0, 0, 0]}})])
    r.clip('death', 2.0, 'hold').poses([
        (0.0, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'cage_souls': {'scale': 1}}),
        (0.6, {'cage_souls': {'scale': 2.2}}, 'easeOutBack'),
        (1.2, {'cage_souls': {'scale': 0}, 'body': {'rot': [20, 40, 30]}}),
        (2.0, {'body': {'pos': [0, -18, 0], 'rot': [80, 40, 60]}}, 'easeInCubic'),
    ])
    return r


MODELS = {
    'memory_wraith': (memory_wraith, [('idle', 0.75), ('cast', 0.55), ('cast', 0.7), ('blink', 0.2), ('death', 0.8)]),
    'lost_pilgrim': (lost_pilgrim, [('idle', 0), ('walk', 0.5), ('disturbed', 0.6), ('strike', 0.65), ('death', 1.0)]),
    'soul_collector': (soul_collector, [('idle', 0.75), ('mark', 0.8), ('harvest', 0.7), ('harvest', 0.9), ('summon', 1.0), ('death', 1.2)]),
}

if __name__ == '__main__':
    from bbmcp import Blockbench
    out_root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants'
    bb = Blockbench()
    for name in sys.argv[2:] or MODELS:
        fn, poses = MODELS[name]
        rig = fn()
        problems = rig.validate()
        if problems:
            raise SystemExit(name + ':\n' + '\n'.join(problems))
        print(name, rig.build(bb, Path(out_root) / name, preview_clips=poses), flush=True)
