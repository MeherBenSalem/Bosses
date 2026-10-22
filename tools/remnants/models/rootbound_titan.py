"""The Rootbound Titan — The Guardian Reclaimed by Nature (the Thornwood Wardens).

The Thornwood's last stone guardian stood still to wait out the eclipse; a forest grew through it.
Knuckle-walking on long root-wrapped arms, a half-broken stone mask with amber rune light behind
it, branching antlers, a living tree rising from its back, and a rune heart buried under bark
plates that the game can break away (bones `bark_left`/`bark_right` hidden when broken).
Editor conventions as in hollow_sovereign.py.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402

NAME = 'rootbound_titan'
RUNE = ['.+.', '+#+', '.+.']
RUNE_LINE = ['#.#.#']


def root(r, bone, name, start, end, thick=1.4):
    """A root strand as two or three stepped segments."""
    sx, sy, sz = start
    ex, ey, ez = end
    n = 3
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        a = (sx + (ex - sx) * t0, sy + (ey - sy) * t0, sz + (ez - sz) * t0)
        b = (sx + (ex - sx) * t1, sy + (ey - sy) * t1, sz + (ez - sz) * t1)
        c = tuple((p + q) / 2 for p, q in zip(a, b))
        length = math.dist(a, b)
        dx, dy, dz = (q - p for p, q in zip(a, b))
        rz = -math.degrees(math.atan2(dx, dy)) if abs(dx) + abs(dy) > 0.01 else 0
        rx = math.degrees(math.atan2(-dz, math.hypot(dx, dy))) if abs(dz) > 0.01 else 0
        size = thick * (1 - 0.22 * i)
        r.box(bone, f'{name}_{i}', c, (size, length + 0.4, size), 'bark', rot=(rx, 0, rz))


def build():
    r = Rig(NAME, (256, 256))
    r.mark_upright('torso', 'head', 'tree')
    r.bone('hips', (0, 26, 6))
    r.bone_pair('leg_{side}', (8, 25, 6), 'hips')
    r.bone('torso', (0, 28, 6), 'hips', rotation=(18, 0, 0))
    r.bone('heart', (0, 44, -6), 'torso')
    r.bone_pair('bark_{side}', (3.6, 44, -8), 'torso')
    r.bone('head', (0, 54, -8), 'torso')
    r.bone_pair('antler_{side}', (5, 64, -6), 'head')
    r.bone('tree', (0, 56, 10), 'torso')
    r.bone('canopy', (0, 82, 12), 'tree')
    r.bone_pair('arm_{side}', (16, 52, -2), 'torso', rotation=(34, 0, 0))
    r.bone_pair('forearm_{side}', (18, 32, -4), 'arm_{side}')

    # short trunk-like legs sunk in roots
    r.box('hips', 'pelvis', (0, 26, 6), (18, 8, 14), 'stone', decals=[('band', {'row': 2, 'size': 1, 'mat': 'moss'})])
    r.pair('leg_{side}', '{side}_thigh', (8.5, 18, 6), (10, 12, 11), 'stone')
    r.pair('leg_{side}', '{side}_foot', (8.5, 5.5, 4.5), (12, 11, 14), 'stone', decals=[('band', {'row': 0, 'size': 1, 'mat': 'moss'})])
    for side, s in (('left', 1), ('right', -1)):
        root(r, f'leg_{side}', f'{side}_foot_root_a', (s * 13, 10, 4), (s * 18, 0.5, 2), 1.8)
        root(r, f'leg_{side}', f'{side}_foot_root_b', (s * 6, 8, -2), (s * 5, 0.5, -7), 1.4)

    # hunched torso; rune heart behind two bark plates
    r.box('torso', 'chest', (0, 44, 4), (28, 26, 20), 'stone', decals=[('glyph', {'bitmap': RUNE, 'mat': 'amber', 'faces': ('east', 'west')})])
    r.box('torso', 'moss_mantle', (0, 57.6, 6), (30, 3, 22), 'leaves')
    r.box('torso', 'heart_hollow', (0, 44, -6.2), (10, 12, 1), 'void')
    r.box('heart', 'rune_heart', (0, 44, -6.6), (6, 8, 2), 'amber')
    r.box('heart', 'heart_glyph', (0, 44, -7.7), (4, 4, 0.2), 'amber')
    r.pair('bark_{side}', 'bark_{side}_plate', (3, 44, -7.8), (6.4, 14, 2), 'bark', rot=(0, -10, 0),
           decals=[('glow_edge', {'mat': 'amber', 'edge': 'right', 'width': 1, 'strength': 0.5, 'faces': 'north'})])
    root(r, 'torso', 'chest_root_a', (-12, 54, -6), (6, 32, -6.6), 1.8)
    root(r, 'torso', 'chest_root_b', (12, 52, -6), (-3, 34, -6.8), 1.4)

    # broken mask: the right half is gone, amber light inside; grand branching antlers
    r.box('head', 'skull_block', (0, 59, -6), (15, 13, 12), 'stone', decals=[('band', {'row': 0, 'size': 2, 'mat': 'moss'})])
    r.box('head', 'mask_left', (3.8, 58.6, -12.6), (7.4, 12, 1.6), 'marble',
          decals=[('glyph', {'bitmap': RUNE_LINE, 'mat': 'moss', 'faces': 'north', 'dy': 4}),
                  ('glow_edge', {'mat': 'amber', 'edge': 'right', 'width': 1, 'strength': 0.5, 'faces': 'north'})])
    r.box('head', 'mask_brow', (3.8, 64.4, -13.2), (7.6, 1.6, 1), 'marble')
    r.box('head', 'mask_shard', (-1.4, 62.2, -12.6), (2.8, 5, 1.4), 'marble', rot=(0, 0, 18))
    r.box('head', 'mask_hollow', (-3.6, 58.4, -12.3), (7, 10, 0.4), 'void')
    r.box('head', 'eye_left', (3.8, 60.4, -13.5), (3, 1.4, 0.3), 'amber')
    r.box('head', 'eye_right', (-3.6, 60, -12.5), (3.2, 3.2, 0.3), 'amber')
    r.box('head', 'jaw', (0, 51.6, -9.4), (11, 3, 7), 'stone')
    r.box('head', 'beard_moss', (0, 49, -10.6), (10, 4, 2.4), 'leaves')
    for side, s in (('left', 1), ('right', -1)):
        a = f'antler_{side}'
        r.box(a, f'{a}_beam', (s * 9, 71, -6), (2.4, 14, 2.4), 'heartwood', rot=(0, 0, -s * 34))
        r.box(a, f'{a}_tine_a', (s * 15.4, 79.6, -6), (1.8, 9, 1.8), 'heartwood', rot=(0, 0, -s * 6))
        r.box(a, f'{a}_tine_b', (s * 10.6, 81, -7.6), (1.6, 8, 1.6), 'heartwood', rot=(-12, 0, s * 16))
        r.box(a, f'{a}_tine_c', (s * 18.6, 72.6, -6), (1.6, 6, 1.6), 'heartwood', rot=(0, 0, -s * 72))
        r.box(a, f'{a}_tine_d', (s * 6.4, 76, -4.6), (1.2, 5, 1.2), 'heartwood', rot=(14, 0, s * 26))
        r.box(a, f'{a}_leaves_a', (s * 15.6, 85, -6.2), (5.4, 3.6, 5.4), 'leaves')
        r.box(a, f'{a}_leaves_b', (s * 21.6, 72.4, -6), (4, 3, 4), 'leaves')

    # the tree on its back
    r.box('tree', 'trunk', (0, 68, 12), (6, 22, 6), 'heartwood', decals=[('band', {'row': 4, 'size': 1, 'mat': 'moss'})])
    r.box('tree', 'trunk_fork', (3, 80, 12), (4, 6, 4), 'heartwood', rot=(0, 0, -22))
    root(r, 'tree', 'back_root_a', (-3, 58, 10), (-12, 50, 14), 1.8)
    root(r, 'tree', 'back_root_b', (3, 58, 14), (10, 48, 16), 1.6)
    r.box('canopy', 'crown_main', (0, 86, 12), (22, 8, 20), 'leaves')
    r.box('canopy', 'crown_top', (1, 92, 12), (14, 5, 13), 'leaves')
    r.box('canopy', 'crown_side', (-9, 83, 14), (8, 6, 10), 'leaves')

    # long arms, root-wrapped forearms, stone fists with root fingers
    for side, s in (('left', 1), ('right', -1)):
        a, f = f'arm_{side}', f'forearm_{side}'
        r.box(a, f'{a}_shoulder', (s * 17, 54, -1), (12, 11, 13), 'stone', decals=[('band', {'row': 0, 'size': 2, 'mat': 'moss'})])
        r.box(a, f'{a}_shoulder_moss', (s * 17, 60, -1), (12.4, 2, 13.4), 'leaves')
        r.box(a, f'{a}_upper', (s * 18, 42, -3), (8, 14, 8), 'stone')
        r.box(f, f'{f}_forearm', (s * 18.5, 24, -4), (9, 16, 9), 'stone', decals=[('glyph', {'bitmap': RUNE, 'mat': 'amber', 'faces': ('north',)})])
        root(r, f, f'{f}_wrap_a', (s * 23.4, 30, -9), (s * 13.6, 18, 0.6), 1.4)
        root(r, f, f'{f}_wrap_b', (s * 13.6, 31, -9), (s * 23.4, 17, 0.4), 1.2)
        r.box(f, f'{f}_fist', (s * 18.5, 11, -5), (11, 10, 11), 'stone', decals=[('band', {'row': -1, 'size': 1, 'mat': 'moss'})])
        root(r, f, f'{f}_finger_a', (s * 15, 7, -9), (s * 12, 0.5, -12), 1.4)
        root(r, f, f'{f}_finger_b', (s * 22, 7, -9), (s * 24, 0.5, -12.6), 1.4)

    clips(r)
    return r


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    neutral = {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'heart': {'scale': 1},
               'arm_left': {'rot': [0, 0, 0]}, 'arm_right': {'rot': [0, 0, 0]}, 'forearm_left': {'rot': [0, 0, 0]},
               'forearm_right': {'rot': [0, 0, 0]}, 'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
               'tree': {'rot': [0, 0, 0]}, 'canopy': {'rot': [0, 0, 0]}}
    r.clip('idle', 5.0, True).poses([
        (0.0, merge(neutral, {'heart': {'scale': 1}})),
        (2.5, {'torso': {'rot': [-3, 0, 0]}, 'head': {'rot': [-4, 8, 0]}, 'heart': {'scale': 1.15}, 'canopy': {'rot': [2, 0, 3]},
               'tree': {'rot': [0, 0, 2]}}),
        (5.0, merge(neutral, {'heart': {'scale': 1}})),
    ])
    L = 3.0
    r.clip('walk', L, True).poses([
        (0.0, merge(neutral, {'arm_left': {'rot': [22, 0, 0]}, 'arm_right': {'rot': [-16, 0, 0]}, 'leg_left': {'rot': [-12, 0, 0]},
                              'leg_right': {'rot': [12, 0, 0]}, 'torso': {'rot': [0, -6, 4]}, 'hips': {'pos': [0, -1, 0]},
                              'canopy': {'rot': [0, 0, -4]}})),
        (L / 2, {'arm_left': {'rot': [-16, 0, 0]}, 'arm_right': {'rot': [22, 0, 0]}, 'leg_left': {'rot': [12, 0, 0]},
                 'leg_right': {'rot': [-12, 0, 0]}, 'torso': {'rot': [0, 6, -4]}, 'hips': {'pos': [0, 1, 0]}, 'canopy': {'rot': [0, 0, 4]}}),
        (L, {'arm_left': {'rot': [22, 0, 0]}, 'arm_right': {'rot': [-16, 0, 0]}, 'leg_left': {'rot': [-12, 0, 0]},
             'leg_right': {'rot': [12, 0, 0]}, 'torso': {'rot': [0, -6, 4]}, 'hips': {'pos': [0, -1, 0]}, 'canopy': {'rot': [0, 0, -4]}}),
    ])
    # hammer slam: both fists raised then driven down (impact 1.6)
    r.clip('slam', 3.0).poses([
        (0.0, neutral),
        (1.4, {'arm_left': {'rot': [150, 0, -10]}, 'arm_right': {'rot': [150, 0, 10]}, 'forearm_left': {'rot': [20, 0, 0]},
               'forearm_right': {'rot': [20, 0, 0]}, 'torso': {'rot': [-24, 0, 0]}, 'head': {'rot': [-16, 0, 0]}, 'heart': {'scale': 1.3}}, 'easeOutCubic'),
        (1.6, {'arm_left': {'rot': [36, 0, -6]}, 'arm_right': {'rot': [36, 0, 6]}, 'forearm_left': {'rot': [0, 0, 0]},
               'forearm_right': {'rot': [0, 0, 0]}, 'torso': {'rot': [18, 0, 0]}, 'hips': {'pos': [0, -3, 0]}, 'heart': {'scale': 1},
               'canopy': {'rot': [12, 0, 0]}}, 'easeInExpo'),
        (2.2, {'canopy': {'rot': [-6, 0, 0]}}),
        (3.0, neutral),
    ])
    # wide sweep with the right arm (hit 1.2)
    r.clip('sweep', 2.4).poses([
        (0.0, neutral),
        (1.0, {'arm_right': {'rot': [60, 0, -70]}, 'torso': {'rot': [0, 40, 0]}}, 'easeOutCubic'),
        (1.2, {'arm_right': {'rot': [60, 0, 50]}, 'torso': {'rot': [4, -40, 0]}, 'canopy': {'rot': [0, 0, 14]}}, 'easeInExpo'),
        (2.4, neutral),
    ])
    # root snare: hands plunge into the soil (1.3), roots erupt under targets after
    r.clip('root_snare', 2.8).poses([
        (0.0, neutral),
        (1.1, {'arm_left': {'rot': [70, 0, -10]}, 'arm_right': {'rot': [70, 0, 10]}, 'torso': {'rot': [16, 0, 0]}}, 'easeOutCubic'),
        (1.3, {'arm_left': {'rot': [10, 0, -4]}, 'arm_right': {'rot': [10, 0, 4]}, 'forearm_left': {'rot': [-30, 0, 0]},
               'forearm_right': {'rot': [-30, 0, 0]}, 'torso': {'rot': [26, 0, 0]}, 'hips': {'pos': [0, -4, 0]}, 'heart': {'scale': 1.4}}, 'easeInExpo'),
        (2.2, {'heart': {'scale': 1}}),
        (2.8, neutral),
    ])
    # bramble wall: roar, arms flung wide (walls rise at 1.4)
    r.clip('bramble_wall', 3.0).poses([
        (0.0, neutral),
        (1.2, {'arm_left': {'rot': [40, 0, 70]}, 'arm_right': {'rot': [40, 0, -70]}, 'torso': {'rot': [-24, 0, 0]}, 'head': {'rot': [-26, 0, 0]},
               'canopy': {'rot': [-10, 0, 0]}}, 'easeOutCubic'),
        (1.4, {'heart': {'scale': 1.5}}, 'easeOutExpo'),
        (3.0, neutral),
    ])
    # sprout: shakes its back tree, seeds fall and corrupted saplings sprout (1.2)
    r.clip('sprout', 2.4).poses([
        (0.0, neutral),
        (0.4, {'tree': {'rot': [0, 0, 10]}, 'canopy': {'rot': [0, 0, 12]}, 'torso': {'rot': [0, 0, -6]}}),
        (0.8, {'tree': {'rot': [0, 0, -10]}, 'canopy': {'rot': [0, 0, -12]}, 'torso': {'rot': [0, 0, 6]}}),
        (1.2, {'tree': {'rot': [0, 0, 8]}, 'canopy': {'rot': [0, 0, 10]}, 'torso': {'rot': [0, 0, -4]}}),
        (2.4, neutral),
    ])
    # expose: bark plates torn away (Java hides `bark_*` after), heart blazes
    r.clip('expose', 2.4).poses([
        (0.0, merge(neutral, {'bark_left': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'bark_right': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
        (0.8, {'torso': {'rot': [-20, 0, 0]}, 'head': {'rot': [-30, 0, 0]}, 'heart': {'scale': 1.4}}, 'easeOutCubic'),
        (1.2, {'bark_left': {'pos': [8, -4, -10], 'rot': [40, 30, 50]}, 'bark_right': {'pos': [-8, -4, -10], 'rot': [40, -30, -50]},
               'heart': {'scale': 1.8}}, 'easeOutExpo'),
        (2.4, merge(neutral, {'bark_left': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'bark_right': {'pos': [0, 0, 0], 'rot': [0, 0, 0]},
                              'heart': {'scale': 1.3}})),
    ])
    r.clip('stagger', 1.4).poses([
        (0.0, neutral),
        (0.3, {'torso': {'rot': [-16, 8, 6]}, 'hips': {'pos': [0, -1, 2]}, 'head': {'rot': [-14, 0, 0]}}, 'easeOutExpo'),
        (1.4, neutral),
    ])
    # death: settles into the earth; the tree stays standing
    r.clip('death', 5.0, 'hold').poses([
        (0.0, merge(neutral, {'heart': {'scale': 1}})),
        (1.2, {'torso': {'rot': [-10, 0, 0]}, 'heart': {'scale': 1.6}}, 'easeOutCubic'),
        (3.4, {'hips': {'pos': [0, -12, 0]}, 'torso': {'rot': [26, 0, 0]}, 'arm_left': {'rot': [-10, 0, 30]}, 'arm_right': {'rot': [-10, 0, -30]},
               'head': {'rot': [30, 0, 0]}, 'heart': {'scale': 0.4}, 'tree': {'rot': [-20, 0, 0]}}, 'easeInOutCubic'),
        (5.0, {'heart': {'scale': 0}}),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 0), ('walk', 0.75), ('slam', 1.4), ('slam', 1.6), ('sweep', 1.2), ('root_snare', 1.3), ('bramble_wall', 1.2),
             ('expose', 1.2), ('death', 5.0)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
