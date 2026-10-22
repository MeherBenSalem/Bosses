"""Ashbound Constructs: the furnace-driven machines still working Kharum's abandoned forges.

Shared language with the Ashen Colossus: weathered bronze, basalt, soot and ember light.
Editor conventions as in hollow_sovereign.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402
from ashen_colossus import PALETTE as KHARUM  # noqa: E402

RIM = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
SOOT = ('gradient', {'mat': 'soot', 'size': 2})
GRATE = ['+.+.+.+', '*.*.*.*']


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def ashbound_crawler():
    r = Rig('ashbound_crawler', (64, 64), palette=KHARUM)
    r.bone('body', (0, 6, 0))
    r.bone('head', (0, 6, -5), 'body')
    r.bone('stack', (0, 9, 3), 'body')
    r.box('body', 'shell', (0, 7, 0.6), (8, 5, 10), 'bronze', decals=[RIM, SOOT, ('rivets', {'mat': 'gold', 'spacing': 3})])
    r.box('body', 'belly', (0, 4.2, 0.6), (6.4, 1.4, 8), 'iron_dark')
    r.box('body', 'furnace', (0, 4.6, -3.5), (4.4, 2.4, 0.6), 'ember')
    r.box('body', 'grate', (0, 7.4, -4.5), (5, 2, 0.4), 'iron_dark', decals=[('glyph', {'bitmap': GRATE, 'mat': 'ember', 'faces': 'north'})])
    r.box('head', 'snout', (0, 6, -6.6), (5, 3.4, 3.4), 'bronze', decals=[RIM])
    r.pair('head', 'eye_{side}', (1.4, 6.6, -8.35), (1, 0.8, 0.3), 'ember')
    r.pair('head', 'mandible_{side}', (1.8, 4.4, -8.6), (0.8, 1, 2.4), 'steel', rot=(0, -14, 0))
    r.box('stack', 'chimney', (0, 11, 3.4), (2, 4, 2), 'iron_dark', decals=[RIM])
    r.box('stack', 'chimney_glow', (0, 13.1, 3.4), (1.2, 0.3, 1.2), 'ember')
    legs = []
    for i, z in enumerate((-2.6, 0.6, 3.8)):
        for side, s in (('left', 1), ('right', -1)):
            b = f'leg{i}_{side}'
            legs.append(b)
            r.bone(b, (s * 3.6, 5, z), 'body')
            r.box(b, f'{b}_femur', (s * 5.4, 5.6, z), (3.4, 0.9, 0.9), 'bronze', rot=(0, 0, s * 26))
            r.box(b, f'{b}_tip', (s * 7, 2.6, z), (0.8, 4.4, 0.8), 'steel')
    def gait(phase):
        out = {}
        for i in range(3):
            a = 26 * (1 if (i % 2 == 0) == phase else -1)
            out[f'leg{i}_left'] = {'rot': [0, a, 0]}
            out[f'leg{i}_right'] = {'rot': [0, a, 0]}
        return out
    r.clip('idle', 2.0, True).poses([
        (0.0, {'body': {'pos': [0, 0, 0]}, 'stack': {'scale': 1}, 'head': {'rot': [0, 0, 0]}}),
        (1.0, {'body': {'pos': [0, -0.4, 0]}, 'stack': {'scale': [1, 1.15, 1]}, 'head': {'rot': [0, 10, 0]}}),
        (2.0, {'body': {'pos': [0, 0, 0]}, 'stack': {'scale': 1}, 'head': {'rot': [0, 0, 0]}}),
    ])
    r.clip('walk', 0.4, True).poses([(0.0, merge(gait(True), {'body': {'pos': [0, 0, 0]}})), (0.2, merge(gait(False), {'body': {'pos': [0, 0.5, 0]}})),
                                      (0.4, merge(gait(True), {'body': {'pos': [0, 0, 0]}}))], 'linear')
    # scorch: rears up and breathes a short flame (0.4-0.9)
    r.clip('scorch', 1.2).poses([
        (0.0, {'body': {'rot': [0, 0, 0], 'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}}),
        (0.4, {'body': {'rot': [-24, 0, 0], 'pos': [0, 2, 1]}, 'head': {'rot': [16, 0, 0]}}, 'easeOutCubic'),
        (0.9, {'body': {'rot': [-20, 0, 0], 'pos': [0, 2, 1]}}),
        (1.2, {'body': {'rot': [0, 0, 0], 'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}}),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'body': {'rot': [0, 0, 0]}}), (0.1, {'body': {'rot': [-10, 0, 8]}}, 'easeOutExpo'), (0.3, {'body': {'rot': [0, 0, 0]}})])
    # death: swells and bursts (Java detonates a small ember burst at 0.8)
    r.clip('death', 1.0, 'hold').poses([
        (0.0, {'body': {'scale': 1, 'rot': [0, 0, 0]}}),
        (0.8, {'body': {'scale': 1.25, 'rot': [0, 0, 12]}}, 'easeInCubic'),
        (1.0, {'body': {'scale': 0}}, 'easeOutExpo'),
    ])
    return r


def ember_servitor():
    r = Rig('ember_servitor', (64, 64), palette=KHARUM)
    r.mark_upright('body')
    r.bone('body', (0, 14, 0))
    r.bone('eye', (0, 15, -3), 'body')
    r.bone('rotor', (0, 21, 0), 'body')
    r.bone_pair('arm_{side}', (4, 13, -1), 'body')
    r.bone_pair('claw_{side}', (5.4, 8.4, -2.4), 'arm_{side}')
    r.box('body', 'hull', (0, 14.6, 0), (7, 7, 7), 'bronze', decals=[RIM, SOOT])
    r.box('body', 'hull_band', (0, 14.6, 0), (7.6, 1.2, 7.6), 'iron_dark')
    r.box('body', 'underside', (0, 10.4, 0), (4.4, 1.6, 4.4), 'iron_dark')
    r.box('body', 'exhaust', (0, 9.2, 0), (2.4, 0.6, 2.4), 'ember')
    r.box('eye', 'lens_ring', (0, 15, -3.7), (4, 4, 0.6), 'gold')
    r.box('eye', 'lens', (0, 15, -4.05), (2.4, 2.4, 0.3), 'ember')
    r.box('rotor', 'hub', (0, 19.4, 0), (2, 2.4, 2), 'iron_dark')
    r.box('rotor', 'blade_a', (0, 20.8, 0), (13, 0.4, 1.6), 'bronze')
    r.box('rotor', 'blade_b', (0, 20.8, 0), (1.6, 0.4, 13), 'bronze')
    r.pair('body', 'fin_{side}', (4, 16, 2.6), (1, 4, 3), 'bronze', rot=(0, 0, -20))
    for side, s in (('left', 1), ('right', -1)):
        a, c = f'arm_{side}', f'claw_{side}'
        r.box(a, f'{a}_strut', (s * 5, 11, -1.6), (1, 4.6, 1), 'steel', rot=(20, 0, 0))
        r.box(c, f'{c}_tool', (s * 5.4, 7.4, -2.6), (1.4, 2.4, 1.4), 'iron_dark')
        r.box(c, f'{c}_tip', (s * 5.4, 5.8, -2.6), (0.6, 1, 0.6), 'ember' if side == 'right' else 'steel')
    r.clip('idle', 2.0, True).poses([
        (0.0, {'rotor': {'rot': [0, 0, 0]}, 'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}}),
        (1.0, {'rotor': {'rot': [0, 720, 0]}, 'body': {'pos': [0, 1, 0], 'rot': [4, 0, 3]}}),
        (2.0, {'rotor': {'rot': [0, 1440, 0]}, 'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}}),
    ], 'linear')
    r.clip('fly', 1.0, True).poses([
        (0.0, {'rotor': {'rot': [0, 0, 0]}, 'body': {'rot': [16, 0, 0]}}),
        (1.0, {'rotor': {'rot': [0, 1080, 0]}, 'body': {'rot': [16, 0, 0]}}),
    ], 'linear')
    # ember shot: lens flares, bolt released at 0.5
    r.clip('shoot', 1.0).poses([
        (0.0, {'eye': {'scale': 1}, 'body': {'rot': [0, 0, 0]}}),
        (0.45, {'eye': {'scale': 1.4}, 'body': {'rot': [-10, 0, 0]}}, 'easeInCubic'),
        (0.5, {'eye': {'scale': 0.8}, 'body': {'rot': [8, 0, 0]}}, 'easeOutExpo'),
        (1.0, {'eye': {'scale': 1}, 'body': {'rot': [0, 0, 0]}}),
    ])
    # repair: welding claw pulses against an ally (heal ticks 0.4/0.8/1.2)
    r.clip('repair', 1.6, True).poses([
        (0.0, {'arm_right': {'rot': [50, 0, 10]}, 'claw_right': {'rot': [0, 0, 0]}}),
        (0.4, {'arm_right': {'rot': [56, 0, 14]}, 'claw_right': {'rot': [10, 0, 0]}}),
        (0.8, {'arm_right': {'rot': [50, 0, 10]}, 'claw_right': {'rot': [0, 0, 0]}}),
        (1.2, {'arm_right': {'rot': [56, 0, 14]}, 'claw_right': {'rot': [10, 0, 0]}}),
        (1.6, {'arm_right': {'rot': [50, 0, 10]}, 'claw_right': {'rot': [0, 0, 0]}}),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'body': {'rot': [0, 0, 0]}}), (0.1, {'body': {'rot': [-14, 0, 12]}}, 'easeOutExpo'), (0.3, {'body': {'rot': [0, 0, 0]}})])
    r.clip('death', 1.4, 'hold').poses([
        (0.0, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'rotor': {'rot': [0, 0, 0]}}),
        (1.4, {'body': {'pos': [0, -9, 0], 'rot': [60, 0, 90]}, 'rotor': {'rot': [0, 200, 0]}}, 'easeInCubic'),
    ])
    return r


def rusted_guardian():
    r = Rig('rusted_guardian', (128, 128), palette=KHARUM)
    r.mark_upright('torso', 'head')
    r.bone('hips', (0, 18, 1))
    r.bone_pair('leg_{side}', (3.6, 17.5, 1), 'hips')
    r.bone_pair('shin_{side}', (4, 9.5, 0.6), 'leg_{side}')
    r.bone('torso', (0, 20, 1), 'hips')
    r.bone('gauge', (2.6, 33, -4.2), 'torso')
    r.bone('head', (0, 37, 0), 'torso')
    r.bone_pair('arm_{side}', (8, 33, 0.5), 'torso')
    r.bone_pair('forearm_{side}', (9, 25, 0), 'arm_{side}')
    r.bone('weapon', (-9, 17.5, -0.6), 'forearm_right')
    r.box('hips', 'pelvis', (0, 18, 1), (10, 4, 7), 'bronze', decals=[RIM, SOOT])
    r.pair('leg_{side}', '{side}_thigh', (4, 13.6, 1), (5, 8, 5), 'iron_dark')
    r.pair('shin_{side}', '{side}_knee', (4, 9.5, -1.6), (4, 3, 2), 'bronze')
    r.pair('shin_{side}', '{side}_greave', (4, 5, 0.8), (5.4, 8, 5.6), 'bronze', decals=[RIM, SOOT])
    r.pair('shin_{side}', '{side}_foot', (4, 1, -0.6), (6, 2, 8), 'iron_dark')
    r.box('torso', 'chest', (0, 28, 1), (14, 12, 9), 'bronze', decals=[RIM, SOOT, ('rivets', {'mat': 'gold', 'spacing': 3})])
    r.box('torso', 'boiler', (0, 28, 7.6), (9, 12, 5), 'iron_dark', decals=[('band', {'row': 2, 'size': 1, 'mat': 'bronze'}), ('band', {'row': -3, 'size': 1, 'mat': 'bronze'})])
    r.box('torso', 'boiler_vent', (0, 35.4, 7.6), (3, 3, 3), 'iron_dark', decals=[RIM])
    r.box('torso', 'firebox', (-2.6, 25, -3.7), (5, 4, 0.6), 'iron_dark', decals=[('glyph', {'bitmap': GRATE, 'mat': 'ember', 'faces': 'north'})])
    r.box('gauge', 'gauge_dial', (2.6, 33, -4.2), (3, 3, 0.6), 'gold')
    r.box('gauge', 'gauge_face', (2.6, 33, -4.55), (2.2, 2.2, 0.2), 'bone')
    r.box('gauge', 'gauge_needle', (2.6, 33.4, -4.7), (0.3, 1.2, 0.1), 'ember')
    r.box('head', 'helm', (0, 39.4, -0.6), (6.4, 5.4, 6.4), 'bronze', decals=[RIM])
    r.box('head', 'visor', (0, 39.2, -3.95), (5, 1, 0.3), 'ember')
    r.box('head', 'grill', (0, 37.4, -3.95), (4, 1.6, 0.3), 'iron_dark')
    r.box('head', 'antenna', (2.4, 43.4, 0.6), (0.6, 3, 0.6), 'steel')
    r.pair('arm_{side}', '{side}_shoulder', (9, 33.4, 0.5), (5.6, 5, 6.4), 'bronze', decals=[RIM, SOOT])
    r.pair('arm_{side}', '{side}_upper', (9, 28.6, 0.5), (3.6, 5, 3.6), 'iron_dark')
    r.pair('forearm_{side}', '{side}_piston', (9, 22, 0), (4.6, 6.4, 4.6), 'bronze', decals=[RIM])
    r.pair('forearm_{side}', '{side}_fist', (9, 17.6, -0.4), (4, 3, 4.4), 'iron_dark')
    r.box('weapon', 'haft', (-9, 17.6, -0.6), (1.2, 22, 1.2), 'iron_dark')
    r.box('weapon', 'hammer', (-9, 4.4, -0.6), (3.6, 5, 7), 'bronze', decals=[RIM, SOOT])
    r.box('weapon', 'spike', (-9, 4.4, 3.8), (1.4, 1.4, 2.4), 'steel')
    r.box('weapon', 'blade', (-9, 4.4, -5.6), (0.8, 6, 3), 'blade')
    rest = {'arm_right': {'rot': [20, 0, -6]}, 'forearm_right': {'rot': [40, 0, 0]}, 'weapon': {'rot': [-40, 0, 0]},
            'arm_left': {'rot': [8, 0, 8]}, 'forearm_left': {'rot': [16, 0, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]}, 'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}})
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'gauge': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}})),
        (1.5, {'gauge': {'rot': [0, 0, 30]}, 'torso': {'rot': [2, 0, 0]}}),
        (3.0, {'gauge': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}}),
    ])
    L = 1.8
    r.clip('walk', L, True).poses([
        (0.0, merge(rest, {'leg_left': {'rot': [18, 0, 0]}, 'leg_right': {'rot': [-18, 0, 0]}, 'hips': {'pos': [0, -0.6, 0]}, 'torso': {'rot': [2, -6, 0]}})),
        (L / 4, {'shin_right': {'rot': [-24, 0, 0]}, 'hips': {'pos': [0, 0.6, 0]}}),
        (L / 2, {'leg_left': {'rot': [-18, 0, 0]}, 'leg_right': {'rot': [18, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, -0.6, 0]}, 'torso': {'rot': [2, 6, 0]}}),
        (3 * L / 4, {'shin_left': {'rot': [-24, 0, 0]}, 'hips': {'pos': [0, 0.6, 0]}}),
        (L, {'leg_left': {'rot': [18, 0, 0]}, 'leg_right': {'rot': [-18, 0, 0]}, 'shin_left': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, -0.6, 0]}, 'torso': {'rot': [2, -6, 0]}}),
    ])
    # pile-driver: a long windup, impact at 1.3, a long recovery (punish window to ~2.2)
    r.clip('smash', 2.6).poses([
        (0.0, neutral),
        (1.1, {'arm_right': {'rot': [170, 0, -6]}, 'arm_left': {'rot': [160, 0, -16]}, 'weapon': {'rot': [-10, 0, 0]},
               'torso': {'rot': [-14, 0, 0]}, 'gauge': {'rot': [0, 0, 80]}}, 'easeOutCubic'),
        (1.3, {'arm_right': {'rot': [46, 0, -4]}, 'arm_left': {'rot': [40, 0, -14]}, 'weapon': {'rot': [50, 0, 0]},
               'torso': {'rot': [26, 0, 0]}, 'hips': {'pos': [0, -2, -1]}}, 'easeInExpo'),
        (2.2, {'torso': {'rot': [24, 0, 0]}, 'gauge': {'rot': [0, 0, 0]}}),
        (2.6, neutral),
    ])
    r.clip('sweep', 2.0).poses([
        (0.0, neutral),
        (0.8, {'arm_right': {'rot': [80, 0, -70]}, 'weapon': {'rot': [0, 0, -70]}, 'torso': {'rot': [0, 40, 0]}}, 'easeOutCubic'),
        (1.0, {'arm_right': {'rot': [80, 0, 50]}, 'weapon': {'rot': [0, 0, 60]}, 'torso': {'rot': [4, -40, 0]}}, 'easeInExpo'),
        (2.0, neutral),
    ])
    r.clip('hurt', 0.4).poses([(0.0, neutral), (0.12, {'torso': {'rot': [-8, 8, 0]}}, 'easeOutExpo'), (0.4, neutral)])
    r.clip('death', 2.4, 'hold').poses([
        (0.0, merge(neutral, {'gauge': {'rot': [0, 0, 0]}})),
        (0.6, {'gauge': {'rot': [0, 0, 160]}, 'torso': {'rot': [-10, 0, 0]}}, 'easeOutCubic'),
        (2.0, {'hips': {'pos': [0, -8, 0]}, 'leg_left': {'rot': [80, 0, -6]}, 'shin_left': {'rot': [-86, 0, 0]},
               'leg_right': {'rot': [76, 0, 8]}, 'shin_right': {'rot': [-84, 0, 0]}, 'torso': {'rot': [40, 0, 0]},
               'arm_left': {'rot': [0, 0, 20]}, 'arm_right': {'rot': [0, 0, -20]}, 'head': {'rot': [30, 0, 0]}}, 'easeInCubic'),
        (2.4, {'torso': {'rot': [44, 0, 4]}}, 'easeOutBounce'),
    ])
    return r


MODELS = {
    'ashbound_crawler': (ashbound_crawler, [('idle', 0), ('walk', 0.1), ('scorch', 0.4), ('death', 0.8)]),
    'ember_servitor': (ember_servitor, [('idle', 0.5), ('shoot', 0.45), ('repair', 0.4), ('death', 1.4)]),
    'rusted_guardian': (rusted_guardian, [('idle', 0), ('walk', 0.45), ('smash', 1.1), ('smash', 1.3), ('sweep', 1.0), ('death', 2.4)]),
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
