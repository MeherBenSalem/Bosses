"""Corrupted Relics: sacred objects of the Sanctum, animated by the Oracle's unstable light.

Chalk marble, gilt and rose-violet Oracle crystal; every relic shows where the corruption got in.
Editor conventions as in hollow_sovereign.py.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402
from shattered_oracle import PALETTE as SANCTUM  # noqa: E402

GILT = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
GILT_HEM = ('band', {'row': -1, 'size': 1, 'mat': 'gold'})
SIGIL = ['.#.', '#+#', '.#.']


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def shardling():
    r = Rig('shardling', (64, 32), palette=SANCTUM)
    r.bone('cluster', (0, 12, 0))
    r.bone('chips', (0, 12, 0), 'cluster')
    for i, (x, y, z, h, rx, rz) in enumerate([(0, 12, 0, 7, 0, 0), (2.4, 10.6, 0.6, 5, 10, -30), (-2.2, 10.8, -0.4, 5, -8, 28),
                                              (0.6, 9.4, 2.2, 4, 34, 6), (-0.8, 14.4, -1.6, 3.6, -20, -14)]):
        r.box('cluster', f'crystal_{i}', (x, y, z), (2.2, h, 2.2), 'oracle', rot=(rx, 45, rz))
    r.box('cluster', 'eye_plate', (0, 12.4, -1.6), (3, 3, 0.6), 'marble', decals=[('rim', {'mat': 'gold', 'faces': 'north'})])
    r.box('cluster', 'eye', (0, 12.4, -2.0), (1, 1, 0.2), 'oracle')
    for i in range(4):
        a = 2 * math.pi * i / 4
        r.box('chips', f'chip_{i}', (math.sin(a) * 6, 12 + (i % 2) * 2 - 1, math.cos(a) * 6), (1, 1.6, 1), 'oracle', rot=(20, math.degrees(a), 30))
    r.clip('idle', 2.0, True).poses([
        (0.0, {'cluster': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'chips': {'rot': [0, 0, 0]}}),
        (1.0, {'cluster': {'pos': [0, 1, 0], 'rot': [0, 20, 0]}, 'chips': {'rot': [0, 180, 0]}}),
        (2.0, {'cluster': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'chips': {'rot': [0, 360, 0]}}),
    ], 'linear')
    r.clip('shoot', 0.8).poses([
        (0.0, {'cluster': {'scale': 1}}), (0.35, {'cluster': {'scale': [0.8, 1.2, 0.8]}}, 'easeInCubic'),
        (0.4, {'cluster': {'scale': [1.2, 0.9, 1.2]}}, 'easeOutExpo'), (0.8, {'cluster': {'scale': 1}}),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'cluster': {'rot': [0, 0, 0]}}), (0.1, {'cluster': {'rot': [-20, 0, 16]}}, 'easeOutExpo'), (0.3, {'cluster': {'rot': [0, 0, 0]}})])
    r.clip('death', 0.6, 'hold').poses([(0.0, {'cluster': {'scale': 1}, 'chips': {'scale': 1}}), (0.6, {'cluster': {'scale': 0}, 'chips': {'scale': 2.5}}, 'easeOutExpo')])
    return r


def ancient_vessel():
    r = Rig('ancient_vessel', (64, 64), palette=SANCTUM)
    r.bone('urn', (0, 2, 0))
    r.bone('lid', (0, 17, 0), 'urn')
    r.bone('glow', (0, 15.4, 0), 'urn')
    r.bone_pair('foot_{side}', (2.6, 2, 0), 'urn')
    r.box('urn', 'base', (0, 3.4, 0), (6, 2, 6), 'marble', decals=[GILT_HEM])
    r.box('urn', 'belly', (0, 8, 0), (9, 7, 9), 'marble', decals=[GILT, GILT_HEM, ('glyph', {'bitmap': SIGIL, 'mat': 'gold', 'faces': 'sides'})])
    r.box('urn', 'shoulder', (0, 12.4, 0), (7, 2, 7), 'marble')
    r.box('urn', 'neck', (0, 14.2, 0), (4.6, 2, 4.6), 'marble', decals=[GILT])
    r.box('urn', 'crack', (2.4, 8.4, -4.55), (0.6, 5, 0.2), 'oracle', rot=(0, 0, 16))
    r.box('urn', 'crack_b', (-3.2, 7.4, 1.4), (0.2, 4, 0.6), 'oracle')
    r.pair('urn', 'handle_{side}', (5, 10.4, 0), (1, 4, 1.4), 'gold')
    r.box('glow', 'spill', (0, 15.6, 0), (3.4, 1.2, 3.4), 'oracle')
    r.box('lid', 'lid_cap', (0, 18, 0), (5.4, 1.4, 5.4), 'marble', decals=[GILT])
    r.box('lid', 'lid_knob', (0, 19.4, 0), (1.6, 1.6, 1.6), 'gold')
    r.pair('foot_{side}', '{side}_foot', (2.6, 1, -0.4), (2.6, 2, 3.4), 'marble')
    r.clip('idle', 3.0, True).poses([
        (0.0, {'lid': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'glow': {'scale': 1}}),
        (1.5, {'lid': {'pos': [0, 1.4, 0], 'rot': [0, 90, 4]}, 'glow': {'scale': 1.4}}),
        (3.0, {'lid': {'pos': [0, 0, 0], 'rot': [0, 180, 0]}, 'glow': {'scale': 1}}),
    ])
    r.clips['idle'].seam_period['lid'] = 90  # square lid
    r.clip('walk', 0.8, True).poses([
        (0.0, {'foot_left': {'rot': [24, 0, 0]}, 'foot_right': {'rot': [-24, 0, 0]}, 'urn': {'rot': [0, 0, 5]}}),
        (0.4, {'foot_left': {'rot': [-24, 0, 0]}, 'foot_right': {'rot': [24, 0, 0]}, 'urn': {'rot': [0, 0, -5]}}),
        (0.8, {'foot_left': {'rot': [24, 0, 0]}, 'foot_right': {'rot': [-24, 0, 0]}, 'urn': {'rot': [0, 0, 5]}}),
    ])
    # pulse: lid lifts, unstable light ring released at 0.7
    r.clip('pulse', 1.4).poses([
        (0.0, {'lid': {'pos': [0, 0, 0]}, 'glow': {'scale': 1}, 'urn': {'scale': 1}}),
        (0.6, {'lid': {'pos': [0, 4, 0]}, 'glow': {'scale': 2}, 'urn': {'scale': [1.05, 0.95, 1.05]}}, 'easeOutCubic'),
        (0.7, {'glow': {'scale': 3}, 'urn': {'scale': [0.95, 1.05, 0.95]}}, 'easeOutExpo'),
        (1.4, {'lid': {'pos': [0, 0, 0]}, 'glow': {'scale': 1}, 'urn': {'scale': 1}}),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'urn': {'rot': [0, 0, 0]}}), (0.1, {'urn': {'rot': [-8, 0, 10]}}, 'easeOutExpo'), (0.3, {'urn': {'rot': [0, 0, 0]}})])
    # shatter: swells, then bursts (Java releases the unstable blast at 0.9)
    r.clip('death', 1.2, 'hold').poses([
        (0.0, {'urn': {'scale': 1}, 'lid': {'pos': [0, 0, 0]}, 'glow': {'scale': 1}}),
        (0.9, {'urn': {'scale': [1.2, 1.1, 1.2]}, 'lid': {'pos': [0, 8, 0]}, 'glow': {'scale': 3}}, 'easeInCubic'),
        (1.2, {'urn': {'scale': 0}, 'glow': {'scale': 0}}, 'easeOutExpo'),
    ])
    return r


def relic_stalker():
    r = Rig('relic_stalker', (128, 64), palette=SANCTUM)
    r.mark_upright('thorax', 'head')
    r.bone('body', (0, 22, 2))
    r.bone('thorax', (0, 24, 0), 'body', rotation=(10, 0, 0))
    r.bone('head', (0, 37, -2), 'thorax')
    r.bone('censer', (0, 33, -5), 'head')
    r.bone_pair('blade_arm_{side}', (4.4, 33, -1), 'thorax')
    r.bone_pair('blade_{side}', (8, 25, -4), 'blade_arm_{side}')
    legs = []
    for i, z in enumerate((-1.4, 4.4)):
        for side, s in (('left', 1), ('right', -1)):
            up, lo = f'leg{i}_{side}', f'shin{i}_{side}'
            legs += [up, lo]
            r.bone(up, (s * 3, 22, z + 2), 'body')
            r.bone(lo, (s * 9, 26, z + 2), up)
            r.box(up, f'{up}_candlestick', (s * 6, 24, z + 2), (1.2, 1.2, 7.2), 'gold', rot=(0, s * 90, -s * 30))
            r.box(lo, f'{lo}_shaft', (s * 9.4, 13, z + 2), (1.2, 26, 1.2), 'gold', decals=[('band', {'row': 3, 'size': 1, 'mat': 'marble'})])
            r.box(lo, f'{lo}_drip', (s * 9.4, 26.4, z + 2), (2.4, 1, 2.4), 'marble')
            r.box(lo, f'{lo}_foot', (s * 9.4, 0.4, z + 2), (2.4, 0.8, 2.4), 'gold')
    # reliquary-box abdomen and torso
    r.box('body', 'reliquary', (0, 22, 4), (6, 6, 9), 'marble', decals=[GILT, GILT_HEM, ('glyph', {'bitmap': SIGIL, 'mat': 'oracle', 'faces': ('east', 'west')})])
    r.box('body', 'reliquary_glow', (0, 22, 8.6), (3, 3, 0.3), 'oracle')
    r.box('thorax', 'thorax_box', (0, 30, 0), (5.6, 10, 4.4), 'marble', decals=[GILT, ('glow_edge', {'mat': 'oracle', 'edge': 'left', 'width': 1})])
    r.box('thorax', 'spine_rod', (0, 30, 2.6), (1, 12, 1), 'gold')
    r.box('thorax', 'joint_core', (0, 35, 0), (3, 3, 3), 'oracle', rot=(45, 45, 0))
    # censer head hung from a crook: a lidded thurible with smouldering rose light
    r.box('head', 'crook', (0, 39.6, -1), (1, 5, 1), 'gold', rot=(-30, 0, 0))
    r.box('head', 'crook_top', (0, 41.6, -3.6), (1, 1, 4), 'gold')
    r.box('censer', 'chain', (0, 38.6, -5.4), (0.4, 3, 0.4), 'chain')
    r.box('censer', 'bowl', (0, 35, -5.4), (4.4, 3.4, 4.4), 'gold', decals=[('glyph', {'bitmap': ['#.#.', '.#.#'], 'mat': 'oracle', 'faces': 'sides'})])
    r.box('censer', 'lid', (0, 37.4, -5.4), (3.4, 1.4, 3.4), 'gold')
    r.box('censer', 'ember', (0, 35, -7.7), (2, 1.6, 0.3), 'oracle')
    # scepter-blade arms (mantis-like)
    for side, s in (('left', 1), ('right', -1)):
        a, b = f'blade_arm_{side}', f'blade_{side}'
        r.box(a, f'{a}_scepter', (s * 6, 29, -2.4), (1.2, 9, 1.2), 'gold', rot=(30, 0, -s * 20))
        r.box(a, f'{a}_knob', (s * 4.6, 33.4, -1), (2, 2, 2), 'oracle', rot=(45, 45, 0))
        r.box(b, f'{b}_sword', (s * 8, 17, -6), (0.6, 14, 2.2), 'blade', rot=(-20, 0, 0))
        r.box(b, f'{b}_hilt', (s * 8, 24.4, -3.8), (2.6, 1, 1), 'gold')
    rest = {'blade_arm_left': {'rot': [40, 0, 0]}, 'blade_arm_right': {'rot': [40, 0, 0]}, 'blade_left': {'rot': [-90, 0, 0]},
            'blade_right': {'rot': [-90, 0, 0]}, 'censer': {'rot': [0, 0, 0]}, 'body': {'pos': [0, 0, 0]}}
    def gait(phase):
        out = {}
        for i in range(2):
            a = 20 * (1 if (i % 2 == 0) == phase else -1)
            out[f'leg{i}_left'] = {'rot': [0, a, 0]}
            out[f'leg{i}_right'] = {'rot': [0, -a, 0]}
            lift = -14 if (i % 2 == 0) == phase else 0
            out[f'shin{i}_left'] = {'rot': [0, 0, lift]}
            out[f'shin{i}_right'] = {'rot': [0, 0, -lift]}
        return out
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'censer': {'rot': [0, 0, 0]}, 'thorax': {'rot': [0, 0, 0]}})),
        (1.5, {'censer': {'rot': [10, 0, 12]}, 'thorax': {'rot': [4, 8, 0]}, 'blade_arm_left': {'rot': [44, 0, 0]}}),
        (3.0, {'censer': {'rot': [0, 0, 0]}, 'thorax': {'rot': [0, 0, 0]}, 'blade_arm_left': {'rot': [40, 0, 0]}}),
    ])
    r.clip('walk', 0.7, True).poses([
        (0.0, merge(rest, gait(True), {'body': {'pos': [0, 0, 0]}, 'censer': {'rot': [12, 0, 0]}})),
        (0.35, merge(gait(False), {'body': {'pos': [0, 0.8, 0]}, 'censer': {'rot': [-12, 0, 0]}})),
        (0.7, merge(gait(True), {'body': {'pos': [0, 0, 0]}, 'censer': {'rot': [12, 0, 0]}})),
    ], 'linear')
    # ambush: folds low (0-0.6), springs (0.6-0.8), double scissor strike at 0.75
    r.clip('ambush', 1.6).poses([
        (0.0, rest),
        (0.6, {'body': {'pos': [0, -8, 2]}, 'thorax': {'rot': [26, 0, 0]}, 'blade_arm_left': {'rot': [80, 0, 30]},
               'blade_arm_right': {'rot': [80, 0, -30]}, **{f'shin{i}_{s}': {'rot': [0, 0, 40 if s == 'left' else -40]} for i in range(2) for s in ('left', 'right')}}, 'easeOutCubic'),
        (0.75, {'body': {'pos': [0, 2, -6]}, 'thorax': {'rot': [-10, 0, 0]}, 'blade_arm_left': {'rot': [20, 0, -20]},
                'blade_arm_right': {'rot': [20, 0, 20]}, **{f'shin{i}_{s}': {'rot': [0, 0, 0]} for i in range(2) for s in ('left', 'right')}}, 'easeInExpo'),
        (1.6, merge(rest, {'thorax': {'rot': [0, 0, 0]}})),
    ])
    r.clip('slash', 1.0).poses([
        (0.0, rest),
        (0.4, {'blade_arm_right': {'rot': [110, 0, -40]}, 'thorax': {'rot': [0, 24, 0]}}, 'easeOutCubic'),
        (0.5, {'blade_arm_right': {'rot': [30, 0, 30]}, 'thorax': {'rot': [8, -24, 0]}}, 'easeInExpo'),
        (1.0, merge(rest, {'thorax': {'rot': [0, 0, 0]}})),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'thorax': {'rot': [0, 0, 0]}}), (0.1, {'thorax': {'rot': [-14, 10, 0]}}, 'easeOutExpo'), (0.3, {'thorax': {'rot': [0, 0, 0]}})])
    r.clip('death', 1.8, 'hold').poses([
        (0.0, merge(rest, {'thorax': {'rot': [0, 0, 0]}, 'head': {'pos': [0, 0, 0]}})),
        (1.0, {'body': {'pos': [0, -14, 0]}, 'thorax': {'rot': [40, 0, 20]}, **{f'leg{i}_{s}': {'rot': [0, 0, 50 if s == 'left' else -50]} for i in range(2) for s in ('left', 'right')}}, 'easeInCubic'),
        (1.8, {'head': {'pos': [4, -10, -6]}, 'censer': {'rot': [80, 0, 40]}}, 'easeOutBounce'),
    ])
    return r


MODELS = {
    'shardling': (shardling, [('idle', 0.5), ('shoot', 0.4)]),
    'ancient_vessel': (ancient_vessel, [('idle', 0.75), ('pulse', 0.7), ('death', 0.9)]),
    'relic_stalker': (relic_stalker, [('idle', 0), ('walk', 0.1), ('ambush', 0.6), ('ambush', 0.75), ('slash', 0.4), ('death', 1.8)]),
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
