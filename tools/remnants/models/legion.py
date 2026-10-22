"""The Hollow Legion of Vael: shared soldier body plus the four unit types.

Every Legion soldier is armor with no body inside: soul light shows only at the eye slits and
joint gaps. Units are told apart by silhouette first: tower shield (Sentinel), heavy crossbow
(Arbalist), banner pole (Standard Bearer), plumed bascinet and soul cape (Soulbound Knight).
Editor conventions as in hollow_sovereign.py (front -Z, +X left, X+ swings lower ends forward).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paint import Material  # noqa: E402
from rig import Rig  # noqa: E402

GOLD_RIM = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
GOLD_HEM = ('band', {'row': -1, 'size': 1, 'mat': 'gold'})
CREST = ['..#..', '.#+#.', '#+#+#', '.#+#.', '..#..']


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def soldier(r, armor='iron', torso_mat='iron', tabard=True, scale_y=1.0, broad=1.0):
    """Common Vael soldier: hips, legs, torso, arms (no head, no gear). Height to the neck ~30."""
    s = scale_y
    r.mark_upright('torso', 'head')
    r.bone('hips', (0, 18 * s, 0))
    r.bone_pair('leg_{side}', (2.3, 17.5 * s, 0), 'hips')
    r.bone_pair('shin_{side}', (2.3, 11 * s, 0), 'leg_{side}')
    r.bone('torso', (0, 20 * s, 0), 'hips')
    r.bone('head', (0, 30 * s, 0), 'torso')
    r.bone_pair('arm_{side}', (5.4 * broad, 28.5 * s, 0), 'torso')
    r.bone_pair('forearm_{side}', (6 * broad, 23.5 * s, 0), 'arm_{side}')

    r.box('hips', 'pelvis', (0, 18.2 * s, 0), (8, 3, 5), armor)
    r.box('hips', 'belt', (0, 19.6 * s, 0), (8.4, 1.2, 5.4), 'leather', decals=[('rivets', {'mat': 'gold', 'spacing': 3})])
    if tabard:
        r.box('hips', 'tabard_front', (0, 14.4 * s, -2.75), (4.4, 7, 0.5), 'royal',
              decals=[('band', {'row': 0, 'size': 1, 'mat': 'gold'})])
        r.box('hips', 'tabard_back', (0, 14.4 * s, 2.75), (4.4, 7, 0.5), 'royal')
    r.pair('hips', '{side}_fauld', (3.4, 16.6 * s, 0), (2, 3.5, 5.2), armor, rot=(0, 0, 8), decals=[GOLD_HEM])
    r.pair('leg_{side}', '{side}_cuisse', (2.3, 14.2 * s, 0), (4, 6.2, 4.6), armor)
    r.pair('leg_{side}', '{side}_knee_glow', (2.3, 11.2 * s, 0.2), (2.6, 0.8, 2.8), 'soul_dim')
    r.pair('shin_{side}', '{side}_poleyn', (2.3, 10.6 * s, -2.3), (3.4, 2.6, 1.4), armor, decals=[('rim', {'mat': 'gold', 'faces': 'north'})])
    r.pair('shin_{side}', '{side}_greave', (2.3, 7 * s, 0.2), (4, 7.8, 4.4), armor)
    r.pair('shin_{side}', '{side}_sabaton', (2.3, 1.5, -0.8), (4.4, 3, 6.6), armor)

    r.box('torso', 'waist', (0, 21.6 * s, 0), (7.4, 3.2, 4.8), torso_mat)
    r.box('torso', 'chest', (0, 26 * s, 0.2), (9 * broad, 6.4, 5.6), torso_mat, decals=[GOLD_RIM])
    r.box('torso', 'chest_crack', (1.4, 26 * s, -2.65), (0.6, 4, 0.3), 'soul')
    r.box('torso', 'gorget', (0, 29.6 * s, 0.2), (6.6, 1.6, 5), armor)
    r.pair('arm_{side}', '{side}_spaulder', (6 * broad, 28.6 * s, 0), (4.4, 3, 5.6), armor, rot=(0, 0, -12), decals=[GOLD_HEM])
    r.pair('arm_{side}', '{side}_rerebrace', (6 * broad, 25.4 * s, 0), (3.4, 5.4, 3.4), armor)
    r.pair('arm_{side}', '{side}_elbow_glow', (6 * broad, 23.4 * s, 0), (2.4, 0.8, 2.4), 'soul_dim')
    r.pair('forearm_{side}', '{side}_vambrace', (6 * broad, 20.4 * s, 0), (3.6, 5.4, 3.6), armor, decals=[GOLD_RIM])
    r.pair('forearm_{side}', '{side}_gauntlet', (6 * broad, 16.4 * s, -0.2), (3.4, 2.8, 3.6), 'iron_dark')


def kettle_helm(r, s=1.0):
    r.box('head', 'coif', (0, 32.6 * s, 0.2), (5.4, 5, 5.4), 'chain')
    r.box('head', 'face_void', (0, 33 * s, -2.55), (3.4, 2.8, 0.3), 'void')
    r.pair('head', 'eye_{side}', (0.8, 33.4 * s, -2.75), (0.9, 0.6, 0.3), 'soul')
    r.box('head', 'helm_dome', (0, 36.4 * s, 0.2), (5.8, 2.8, 5.8), 'iron', decals=[('band', {'row': -1, 'size': 1, 'mat': 'gold'})])
    r.box('head', 'helm_ridge', (0, 38.0 * s, 0.2), (0.8, 0.8, 5.6), 'iron')
    r.box('head', 'brim_front', (0, 34.9 * s, -3.6), (7.6, 0.6, 2), 'iron', rot=(-18, 0, 0))
    r.box('head', 'brim_back', (0, 34.9 * s, 4.0), (7.6, 0.6, 2), 'iron', rot=(18, 0, 0))
    r.pair('head', 'brim_{side}', (3.8, 34.9 * s, 0.2), (2, 0.6, 5.6), 'iron', rot=(0, 0, -18))


def sentinel(name='hollow_sentinel', palette=None):
    r = Rig(name, (128, 64), palette=palette)
    soldier(r)
    kettle_helm(r)
    r.bone('shield', (6.4, 20, -2.8), 'forearm_left')
    r.bone('weapon', (-6, 16.4, -0.2), 'forearm_right')
    # Tower shield (face toward -Z), Vael crest in gold on faded blue
    r.box('shield', 'shield_board', (4.2, 18.4, -4.2), (8.6, 14, 1.2), 'iron',
          decals=[('rim', {'mat': 'gold', 'faces': ('north', 'south')})])
    r.box('shield', 'shield_field', (4.2, 18.6, -4.9), (6.4, 10.6, 0.3), 'heraldry',
          decals=[('glyph', {'bitmap': CREST, 'mat': 'gold', 'faces': 'north', 'dy': -2}),
                  ('band', {'row': -2, 'size': 1, 'mat': 'gold', 'faces': 'north'})])
    r.box('shield', 'shield_boss', (4.2, 15.8, -5.2), (2, 2, 0.6), 'gold')
    r.box('shield', 'shield_spike', (4.2, 15.8, -5.9), (0.8, 0.8, 1), 'iron')
    r.box('shield', 'shield_crack', (6.6, 20.6, -4.85), (0.4, 4, 0.2), 'soul')
    # Falchion, blade down, flat to the front
    r.box('weapon', 'grip', (-6, 16.4, -0.2), (0.9, 4, 0.9), 'leather')
    r.box('weapon', 'pommel', (-6, 18.8, -0.2), (1.4, 1, 1.4), 'gold')
    r.box('weapon', 'guard', (-6, 14, -0.2), (3.6, 0.9, 1.1), 'gold')
    r.box('weapon', 'blade', (-6.3, 8.4, -0.2), (2.6, 10.4, 0.6), 'blade')
    r.box('weapon', 'blade_belly', (-5.2, 5, -0.2), (1.2, 4.4, 0.6), 'blade')
    r.box('weapon', 'blade_tip', (-5.8, 2.6, -0.2), (1.6, 1.2, 0.6), 'steel')

    rest = {'arm_left': {'rot': [8, 0, 8]}, 'forearm_left': {'rot': [16, 0, 0]}, 'shield': {'rot': [-24, 0, 0]},
            'arm_right': {'rot': [6, 0, -4]}, 'forearm_right': {'rot': [12, 0, 0]}, 'weapon': {'rot': [14, 0, 0]}}
    guard = {'arm_left': {'rot': [58, -6, -22]}, 'forearm_left': {'rot': [32, 0, 0]}, 'shield': {'rot': [-90, 0, 14]},
             'torso': {'rot': [8, -10, 0]}, 'hips': {'pos': [0, -1, 0]}, 'leg_left': {'rot': [16, 0, 0]},
             'leg_right': {'rot': [-12, 0, 0]}, 'shin_left': {'rot': [-12, 0, 0]}, 'head': {'rot': [6, 10, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}})

    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}})),
        (1.5, {'torso': {'rot': [2, 0, 0]}, 'head': {'rot': [3, -4, 0]}, 'shield': {'rot': [-24, 3, 0]}}),
        (3.0, {'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'shield': {'rot': [-24, 0, 0]}}),
    ])
    walk(r, 1.5, 22, 30, 0.5, 4, rest, swing_arm='right')
    r.clip('block', 1.0, True).poses([
        (0.0, guard), (0.5, {'hips': {'pos': [0, -1.4, 0]}, 'shield': {'rot': [-90, 0, 11]}}), (1.0, {'hips': {'pos': [0, -1, 0]}, 'shield': {'rot': [-90, 0, 14]}}),
    ])
    r.clip('bash', 1.0).poses([
        (0.0, neutral),
        (0.4, merge(guard, {'torso': {'rot': [4, 18, 0]}, 'arm_left': {'rot': [50, 0, -10]}}), 'easeOutCubic'),
        (0.5, {'torso': {'rot': [14, -26, 0]}, 'arm_left': {'rot': [84, 0, -26]}, 'hips': {'pos': [0, -1, -2.5]},
               'leg_left': {'rot': [30, 0, 0]}}, 'easeInExpo'),
        (1.0, neutral),
    ])
    r.clip('strike', 1.8).poses([
        (0.0, neutral),
        (0.75, {'arm_right': {'rot': [168, 0, -10]}, 'forearm_right': {'rot': [20, 0, 0]}, 'weapon': {'rot': [-10, 0, 0]},
                'torso': {'rot': [-10, 16, 0]}, 'head': {'rot': [-6, 0, 0]}, 'leg_right': {'rot': [-14, 0, 0]}}, 'easeOutCubic'),
        (0.9, {'arm_right': {'rot': [46, 0, 6]}, 'forearm_right': {'rot': [0, 0, 0]}, 'weapon': {'rot': [60, 0, 0]},
               'torso': {'rot': [22, -14, 0]}, 'hips': {'pos': [0, -1.5, -1]}, 'head': {'rot': [10, 0, 0]},
               'leg_left': {'rot': [22, 0, 0]}, 'shin_left': {'rot': [-18, 0, 0]}}, 'easeInExpo'),
        (1.3, {'torso': {'rot': [20, -12, 0]}}),
        (1.8, neutral),
    ])
    hurt_and_death(r, neutral)
    r.clip('rise', 1.5).poses([
        (0.0, merge(neutral, {'root': {'pos': [0, -34, 0]}, 'torso': {'rot': [30, 0, 0]}, 'head': {'rot': [30, 0, 0]}})),
        (1.1, {'root': {'pos': [0, 0, 0]}, 'torso': {'rot': [12, 0, 0]}, 'head': {'rot': [-12, 0, 0]}}, 'easeOutCubic'),
        (1.5, merge(neutral, {'root': {'pos': [0, 0, 0]}})),
    ])
    return r


def walk(r, length, swing, lift, bob, arm, rest, swing_arm='left', lean=2):
    h, q = length / 2, length / 4
    free = f'arm_{swing_arm}'
    r.clip('walk', length, True).poses(stride_frames(length, swing, lift, bob, arm, rest, free, lean))


def stride_frames(length, swing, lift, bob, arm, rest, free, lean):
    h, q = length / 2, length / 4
    base = rest.get(free, {}).get('rot', [0, 0, 0])
    return [
        (0.0, merge(rest, {'leg_left': {'rot': [swing, 0, 0]}, 'leg_right': {'rot': [-swing, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [-4, 0, 0]},
                           'hips': {'pos': [0, -bob, 0]}, 'torso': {'rot': [lean, -3, 0]},
                           free: {'rot': [base[0] - arm, base[1], base[2]]}})),
        (q, {'shin_right': {'rot': [-lift, 0, 0]}, 'hips': {'pos': [0, bob, 0]}}),
        (h, {'leg_left': {'rot': [-swing, 0, 0]}, 'leg_right': {'rot': [swing, 0, 0]},
             'shin_left': {'rot': [-4, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, -bob, 0]},
             'torso': {'rot': [lean, 3, 0]}, free: {'rot': [base[0] + arm, base[1], base[2]]}}),
        (h + q, {'shin_left': {'rot': [-lift, 0, 0]}, 'hips': {'pos': [0, bob, 0]}}),
        (length, {'leg_left': {'rot': [swing, 0, 0]}, 'leg_right': {'rot': [-swing, 0, 0]},
                  'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [-4, 0, 0]}, 'hips': {'pos': [0, -bob, 0]},
                  'torso': {'rot': [lean, -3, 0]}, free: {'rot': [base[0] - arm, base[1], base[2]]}}),
    ]


def hurt_and_death(r, neutral, death_len=2.0, drop=None):
    r.clip('hurt', 0.4).poses([
        (0.0, neutral),
        (0.1, {'torso': {'rot': [-10, 6, 0]}, 'head': {'rot': [-12, 0, 0]}, 'hips': {'pos': [0, 0, 0.8]}}, 'easeOutExpo'),
        (0.4, neutral),
    ])
    # Hollow armor collapses: legs buckle, torso folds, helm tumbles off, soul light goes out
    r.clip('death', death_len, 'hold').poses([
        (0.0, merge(neutral, {'head': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
        (death_len * 0.3, {'torso': {'rot': [-14, 0, 6]}, 'head': {'rot': [-20, 0, 0]}, 'hips': {'pos': [0, -1, 1]}}, 'easeOutCubic'),
        (death_len * 0.65, {'hips': {'pos': [0, -9, 0]}, 'leg_left': {'rot': [76, 0, -10]}, 'shin_left': {'rot': [-84, 0, 0]},
                            'leg_right': {'rot': [70, 0, 12]}, 'shin_right': {'rot': [-80, 0, 0]},
                            'torso': {'rot': [38, 0, 4]}, 'arm_left': {'rot': [10, 0, 26]}, 'arm_right': {'rot': [10, 0, -26]},
                            'head': {'rot': [30, 0, 0]}}, 'easeInCubic'),
        (death_len, {'head': {'pos': [3, -7, -5], 'rot': [80, 30, 40]}, 'torso': {'rot': [52, 0, 8]}}, 'easeOutBounce'),
    ])


def arbalist():
    r = Rig('hollow_arbalist', (128, 64))
    soldier(r, armor='iron', torso_mat='leather')
    r.box('torso', 'brigandine_studs', (0, 26, -2.95), (7, 5, 0.3), 'leather', decals=[('rivets', {'mat': 'gold', 'spacing': 2, 'faces': 'north'})])
    # Sallet with a long tail and a raised visor shadow
    r.box('head', 'face_void', (0, 32.6, 0), (4.4, 4, 4.4), 'void')
    r.pair('head', 'eye_{side}', (1.1, 33.2, -2.25), (1, 0.6, 0.3), 'soul')
    r.box('head', 'sallet', (0, 35.4, 0.4), (5.6, 3.4, 6), 'iron', decals=[('band', {'row': -1, 'size': 1, 'mat': 'gold'})])
    r.box('head', 'sallet_tail', (0, 33.6, 3.8), (5.4, 1.2, 3), 'iron', rot=(-18, 0, 0))
    r.box('head', 'visor', (0, 34.3, -3.0), (5.2, 1.3, 1), 'iron')
    r.box('hips', 'quiver', (-3.6, 15.6, 2.6), (2.4, 6, 2.4), 'leather', rot=(0, 0, 14))
    for i, x in enumerate((-4.2, -3.4, -2.6)):
        r.box('hips', f'bolt_{i}', (x - 0.6, 19.4 + (i % 2) * 0.6, 2.6), (0.5, 2.4, 0.5), 'heartwood', rot=(0, 0, 14))
    # Heavy crossbow: in bind pose the stock hangs below the right hand (like a sword), so raising the
    # arm forward aims it. The prod (bow) is at the far end, across X.
    r.bone('crossbow', (-6, 16.4, -0.2), 'forearm_right')
    c = -6
    r.box('crossbow', 'stock', (c, 12.4, -0.4), (1.8, 10, 1.8), 'heartwood', decals=[('band', {'row': 1, 'size': 1, 'mat': 'iron'})])
    r.box('crossbow', 'butt', (c, 17.4, 0.2), (2, 2.4, 2.6), 'heartwood')
    r.box('crossbow', 'prod', (c, 7.6, -1.2), (13, 1.2, 1), 'iron', decals=[GOLD_RIM])
    r.box('crossbow', 'prod_tip_l', (c + 6.8, 7.9, -1.0), (1, 1.8, 1.4), 'gold')
    r.box('crossbow', 'prod_tip_r', (c - 6.8, 7.9, -1.0), (1, 1.8, 1.4), 'gold')
    r.box('crossbow', 'string', (c, 9.4, -1.2), (11.6, 0.4, 0.3), 'bone')
    r.box('crossbow', 'loaded_bolt', (c, 9.2, -1.5), (0.5, 6, 0.5), 'heartwood')
    r.box('crossbow', 'bolt_head', (c, 6.0, -1.5), (0.9, 1, 0.9), 'steel')
    r.box('crossbow', 'stirrup', (c, 6.2, -1.2), (2.6, 0.6, 0.6), 'iron')
    r.box('crossbow', 'crank', (c - 1.4, 14.4, -0.4), (1, 2.4, 1), 'iron')
    r.box('crossbow', 'crank_handle', (c - 2.4, 15.4, -0.4), (1.2, 0.8, 0.8), 'iron')

    rest = {'arm_right': {'rot': [18, 0, -4]}, 'forearm_right': {'rot': [52, 0, 0]}, 'crossbow': {'rot': [10, 0, 38]},
            'arm_left': {'rot': [36, 0, -18]}, 'forearm_left': {'rot': [40, 0, 0]}}
    aim = {'arm_right': {'rot': [84, 0, 4]}, 'forearm_right': {'rot': [6, 0, 0]}, 'crossbow': {'rot': [0, 0, 0]},
           'arm_left': {'rot': [80, 0, -40]}, 'forearm_left': {'rot': [26, 0, 0]}, 'torso': {'rot': [0, 14, 0]},
           'head': {'rot': [4, -14, 0]}, 'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-10, 0, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}})
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}})),
        (1.5, {'torso': {'rot': [2, 4, 0]}, 'head': {'rot': [2, -10, 0]}}),
        (3.0, {'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}}),
    ])
    r.clip('walk', 1.4, True).poses(stride_frames(1.4, 20, 28, 0.5, 0, rest, 'arm_right', 4))
    r.clip('aim', 0.8).poses([(0.0, neutral), (0.45, aim, 'easeOutCubic'), (0.8, merge(aim, {'head': {'rot': [6, -14, 0]}}))])
    r.clip('fire', 0.5).poses([
        (0.0, aim),
        (0.08, {'arm_right': {'rot': [98, 0, 4]}, 'crossbow': {'rot': [-10, 0, 0]}, 'torso': {'rot': [-6, 14, 0]}}, 'easeOutExpo'),
        (0.5, aim),
    ])
    r.clip('reload', 2.0).poses([
        (0.0, neutral),
        (0.3, {'crossbow': {'rot': [-40, 0, 0]}, 'arm_right': {'rot': [20, 0, -6]}, 'forearm_right': {'rot': [30, 0, 0]},
               'arm_left': {'rot': [40, 0, 12]}, 'torso': {'rot': [16, 0, 0]}, 'head': {'rot': [16, 0, 0]}}, 'easeOutCubic'),
        (0.7, {'forearm_left': {'rot': [80, 0, 0]}}),
        (1.0, {'forearm_left': {'rot': [40, 0, 0]}}),
        (1.3, {'forearm_left': {'rot': [80, 0, 0]}}),
        (1.6, {'forearm_left': {'rot': [40, 0, 0]}, 'torso': {'rot': [12, 0, 0]}}),
        (2.0, neutral),
    ])
    r.clip('kick', 0.8).poses([
        (0.0, neutral),
        (0.3, {'leg_right': {'rot': [-30, 0, 0]}, 'shin_right': {'rot': [-50, 0, 0]}, 'torso': {'rot': [-6, 0, 0]}}, 'easeOutCubic'),
        (0.4, {'leg_right': {'rot': [74, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}, 'torso': {'rot': [-14, 0, 0]}}, 'easeInExpo'),
        (0.8, neutral),
    ])
    hurt_and_death(r, neutral)
    return r


def standard_bearer():
    r = Rig('fallen_standard_bearer', (128, 128))
    soldier(r, scale_y=1.04)
    s = 1.04
    # Open-faced helm with a tall horsehair crest in faded royal blue
    r.box('head', 'face_void', (0, 32.8 * s, 0), (4.6, 4.2, 4.6), 'void')
    r.pair('head', 'eye_{side}', (1.1, 33.4 * s, -2.35), (1, 0.6, 0.3), 'soul')
    r.box('head', 'helm', (0, 35.4 * s, 0.2), (5.8, 3.6, 5.8), 'iron', decals=[GOLD_HEM])
    r.pair('head', '{side}_cheekplate', (2.6, 32.6 * s, -1), (0.8, 3.6, 3.4), 'iron')
    r.box('head', 'crest_base', (0, 38 * s, 0.4), (1.2, 1.4, 5.4), 'gold')
    r.box('head', 'crest_plume', (0, 39.6 * s, 1.6), (1.4, 2.4, 6.4), 'royal')
    r.box('head', 'crest_tail', (0, 37.4 * s, 4.6), (1.4, 4, 1.6), 'royal', rot=(14, 0, 0))
    r.box('torso', 'sash', (0, 26 * s, -2.95), (2, 6.6, 0.3), 'royal', rot=(0, 0, 28))
    # Banner pole in the right hand, planted at the side; torn banner hangs from a crossbar
    r.bone('banner', (-6, 16.8 * s, -0.4), 'forearm_right')
    r.bone('banner_cloth', (-6, 50, -0.4), 'banner')
    r.box('banner', 'pole', (-6, 25, -0.4), (1.2, 56, 1.2), 'heartwood', decals=[('band', {'row': 3, 'size': 1, 'mat': 'gold'})])
    r.box('banner', 'pole_foot', (-6, -2.5, -0.4), (1.6, 2, 1.6), 'iron')
    r.box('banner', 'finial', (-6, 54.4, -0.4), (2.4, 2.4, 0.8), 'gold', decals=[('glyph', {'bitmap': ['.#.', '#+#', '.#.'], 'mat': 'gold', 'faces': 'north'})])
    r.box('banner', 'finial_spike', (-6, 56.6, -0.4), (0.8, 2, 0.8), 'gold')
    r.box('banner', 'crossbar', (-6, 50.6, -0.4), (12, 0.9, 0.9), 'gold')
    r.box('banner_cloth', 'cloth', (-6, 40.6, -0.4), (11, 19, 0.5), 'royal',
          decals=[('band', {'row': 1, 'size': 1, 'mat': 'gold'}), ('glyph', {'bitmap': CREST, 'mat': 'gold', 'faces': ('north', 'south'), 'dy': -2})])
    r.box('banner_cloth', 'cloth_tail_l', (-2.6, 29.6, -0.4), (2.4, 3.4, 0.5), 'royal')
    r.box('banner_cloth', 'cloth_tail_r', (-9.2, 30.2, -0.4), (3, 2.2, 0.5), 'royal')

    rest = {'arm_right': {'rot': [12, 0, -10]}, 'forearm_right': {'rot': [24, 0, 0]}, 'banner': {'rot': [-30, 0, 10]},
            'arm_left': {'rot': [4, 0, 8]}, 'forearm_left': {'rot': [16, 0, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]},
                           'banner_cloth': {'rot': [0, 0, 0]}})
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'banner_cloth': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}})),
        (1.5, {'banner_cloth': {'rot': [-6, 0, 3]}, 'torso': {'rot': [2, 0, 0]}, 'head': {'rot': [-4, 0, 0]}}),
        (3.0, {'banner_cloth': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}}),
    ])
    frames = stride_frames(1.6, 18, 26, 0.5, 14, rest, 'arm_left', 2)
    frames[0][1]['banner_cloth'] = {'rot': [-8, 0, 0]}
    frames[2][1]['banner_cloth'] = {'rot': [-14, 0, 0]}
    frames[4][1]['banner_cloth'] = {'rot': [-8, 0, 0]}
    r.clip('walk', 1.6, True).poses(frames)
    r.clip('sweep', 1.2).poses([
        (0.0, neutral),
        (0.5, {'arm_right': {'rot': [90, 0, -50]}, 'forearm_right': {'rot': [10, 0, 0]}, 'banner': {'rot': [-90, 0, -40]},
               'torso': {'rot': [0, 34, 0]}, 'banner_cloth': {'rot': [0, 0, 30]}}, 'easeOutCubic'),
        (0.6, {'arm_right': {'rot': [80, 0, 40]}, 'banner': {'rot': [-90, 0, 40]}, 'torso': {'rot': [4, -34, 0]},
               'banner_cloth': {'rot': [0, 0, -40]}}, 'easeInExpo'),
        (1.2, neutral),
    ])
    r.clip('rally', 2.5).poses([
        (0.0, neutral),
        (0.9, {'arm_right': {'rot': [150, 0, -6]}, 'banner': {'rot': [-150, 0, 0]}, 'torso': {'rot': [-12, 0, 0]},
               'head': {'rot': [-20, 0, 0]}, 'arm_left': {'rot': [40, 0, 50]}}, 'easeOutCubic'),
        (1.5, {'arm_right': {'rot': [30, 0, -10]}, 'banner': {'rot': [-30, 0, 0]}, 'torso': {'rot': [14, 0, 0]},
               'hips': {'pos': [0, -1.5, 0]}, 'banner_cloth': {'rot': [-20, 0, 0]}}, 'easeInExpo'),
        (2.0, {'banner_cloth': {'rot': [6, 0, 0]}}),
        (2.5, neutral),
    ])
    hurt_and_death(r, neutral, death_len=2.5)
    return r


def soulbound_knight():
    r = Rig('soulbound_knight', (128, 128))
    soldier(r, armor='steel', torso_mat='steel', tabard=True, scale_y=1.08, broad=1.06)
    s = 1.08
    r.box('torso', 'plackart', (0, 23.4 * s, -2.8), (6.4, 3, 0.8), 'steel', decals=[GOLD_RIM])
    r.pair('arm_{side}', '{side}_rondel', (7.0, 29.6 * s, 0), (5.2, 3.4, 6.4), 'steel', rot=(0, 0, -16),
           decals=[GOLD_RIM, GOLD_HEM, ('filigree', {'mat': 'gold', 'row': 1, 'size': 1})])
    r.box('torso', 'chest_crest', (0, 26.6 * s, -3.1), (2.6, 2.6, 0.4), 'gold', decals=[('glyph', {'bitmap': ['.#.', '#+#', '.#.'], 'mat': 'gold', 'faces': 'north'})])
    # Pointed "hounskull" bascinet: a snout visor reads instantly as an elite knight
    r.box('head', 'bascinet', (0, 34.6 * s, 0.4), (5.6, 5.6, 5.8), 'steel', decals=[GOLD_HEM])
    r.box('head', 'bascinet_peak', (0, 38.2 * s, 1.0), (3, 1.6, 4), 'steel')
    r.box('head', 'visor_base', (0, 34.4 * s, -2.9), (5.2, 4.2, 1.2), 'steel', decals=[('rivets', {'mat': 'gold', 'spacing': 4, 'faces': 'north'})])
    r.box('head', 'visor_snout', (0, 33.6 * s, -3.9), (3.6, 2.8, 1.2), 'steel')
    r.box('head', 'visor_tip', (0, 33.2 * s, -4.8), (2, 1.6, 1), 'steel')
    r.box('head', 'visor_slit', (0, 35.6 * s, -3.55), (4.6, 0.6, 0.2), 'void')
    r.pair('head', 'eye_{side}', (1.2, 35.6 * s, -3.7), (0.9, 0.5, 0.2), 'soul')
    r.pair('head', 'breath_{side}', (1.0, 33.2 * s, -4.55), (0.4, 0.4, 0.2), 'void')
    r.box('head', 'plume', (0, 39.6 * s, 2.6), (1.2, 1.6, 4.4), 'soul_dim', rot=(-20, 0, 0))
    # Soul cape: cloth woven from remnant light
    r.bone('cape', (0, 30 * s, 3.2), 'torso')
    r.box('cape', 'cape_cloth', (0, 20 * s, 3.5), (9, 19, 0.5), 'soul_cloth', decals=[('band', {'row': 0, 'size': 1, 'mat': 'gold'})])
    # Longsword in the right hand
    r.bone('weapon', (-6.4, 16.8 * s, -0.2), 'forearm_right')
    w = -6.4
    r.box('weapon', 'grip', (w, 17.4, -0.2), (1, 5, 1), 'leather')
    r.box('weapon', 'pommel', (w, 20.4, -0.2), (1.6, 1.2, 1.6), 'gold')
    r.box('weapon', 'guard', (w, 14.4, -0.2), (6, 1, 1.2), 'gold')
    r.box('weapon', 'blade', (w, 5.4, -0.2), (2, 17, 0.6), 'blade')
    r.box('weapon', 'fuller', (w, 6.4, -0.2), (0.6, 13, 0.8), 'soul_dim')
    r.box('weapon', 'tip', (w, -3.8, -0.2), (1.2, 1.6, 0.6), 'steel')

    rest = {'arm_right': {'rot': [8, 0, -6]}, 'forearm_right': {'rot': [18, 0, 0]}, 'weapon': {'rot': [30, 0, 0]},
            'arm_left': {'rot': [4, 0, 8]}, 'forearm_left': {'rot': [12, 0, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}, 'cape': {'rot': [0, 0, 0]}})
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'cape': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}})),
        (1.5, {'cape': {'rot': [-6, 0, 2]}, 'torso': {'rot': [2, 0, 0]}}),
        (3.0, {'cape': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}}),
    ])
    for name, length, swing, lift, arm, lean, cape in (('walk', 1.4, 22, 30, 16, 3, 8), ('run', 0.9, 34, 52, 30, 12, 26)):
        frames = stride_frames(length, swing, lift, 0.6 if name == 'walk' else 1.0, arm, rest, 'arm_left', lean)
        for i in (0, 2, 4):
            frames[i][1]['cape'] = {'rot': [-cape, 0, 0]}
        for i in (1, 3):
            frames[i][1]['cape'] = {'rot': [-cape * 1.4, 0, 0]}
        r.clip(name, length, True).poses(frames)
    r.clip('combo', 1.7).poses([
        (0.0, neutral),
        (0.45, {'arm_right': {'rot': [140, 0, -40]}, 'weapon': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 30, 0]}}, 'easeOutCubic'),
        (0.6, {'arm_right': {'rot': [50, 0, 30]}, 'weapon': {'rot': [40, 0, 0]}, 'torso': {'rot': [8, -30, 0]},
               'leg_left': {'rot': [20, 0, 0]}}, 'easeInExpo'),
        (1.0, {'arm_right': {'rot': [100, 0, 50]}, 'torso': {'rot': [4, -34, 0]}}, 'easeOutSine'),
        (1.2, {'arm_right': {'rot': [70, 0, -50]}, 'torso': {'rot': [8, 30, 0]}, 'cape': {'rot': [-10, 0, -8]}}, 'easeInExpo'),
        (1.7, neutral),
    ])
    r.clip('lunge', 1.5).poses([
        (0.0, neutral),
        (0.7, {'arm_right': {'rot': [-24, 0, -10]}, 'weapon': {'rot': [80, 0, 0]}, 'torso': {'rot': [22, 16, 0]},
               'hips': {'pos': [0, -2.5, 0]}, 'leg_left': {'rot': [30, 0, 0]}, 'shin_left': {'rot': [-30, 0, 0]},
               'leg_right': {'rot': [-24, 0, 0]}}, 'easeOutCubic'),
        (0.9, {'arm_right': {'rot': [92, 0, 0]}, 'weapon': {'rot': [86, 0, 0]}, 'torso': {'rot': [26, -8, 0]},
               'hips': {'pos': [0, -1.5, -2]}, 'leg_left': {'rot': [40, 0, 0]}, 'leg_right': {'rot': [-36, 0, 0]},
               'cape': {'rot': [-40, 0, 0]}}, 'easeInExpo'),
        (1.5, neutral),
    ])
    stance = {'arm_right': {'rot': [70, 0, 30]}, 'forearm_right': {'rot': [40, 0, 0]}, 'weapon': {'rot': [0, 0, 80]},
              'torso': {'rot': [6, -10, 0]}, 'hips': {'pos': [0, -1, 0]}, 'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-12, 0, 0]}}
    r.clip('counter', 1.2).poses([
        (0.0, neutral), (0.15, stance, 'easeOutBack'), (0.7, stance),
        (0.9, {'arm_right': {'rot': [80, 0, -40]}, 'forearm_right': {'rot': [10, 0, 0]}, 'weapon': {'rot': [40, 0, -30]},
               'torso': {'rot': [10, 30, 0]}}, 'easeInExpo'),
        (1.2, neutral),
    ])
    hurt_and_death(r, neutral, death_len=2.3)
    return r


SPECTRAL = {
    'iron': Material(['#0E3B3E', '#16585A', '#2E8C8C', '#4FC9C1', '#9CF2E6'], 'plate', alpha=170),
    'iron_dark': Material(['#0A2A2C', '#0E3B3E', '#16585A', '#2E8C8C'], 'plate', alpha=170),
    'gold': Material(['#2E8C8C', '#4FC9C1', '#9CF2E6', '#E0FFFA'], 'gold', alpha=190),
    'royal': Material(['#0E3B3E', '#16585A', '#2E8C8C', '#4FC9C1'], 'cloth', torn=True, alpha=150),
    'leather': Material(['#0E3B3E', '#16585A', '#2E8C8C'], 'leather', alpha=170),
    'steel': Material(['#16585A', '#2E8C8C', '#4FC9C1', '#9CF2E6'], 'plate', alpha=180),
    'blade': Material(['#2E8C8C', '#4FC9C1', '#8FEDE0', '#D4FFF8', '#FFFFFF'], 'blade', alpha=200),
    'chain': Material(['#0A2A2C', '#0E3B3E', '#16585A', '#2E8C8C'], 'chain', alpha=160),
    'heraldry': Material(['#0A2A2C', '#0E3B3E', '#16585A', '#2E8C8C', '#4FC9C1'], 'enamel', alpha=170),
}

EXTRA = {'soul_cloth': Material(['#0F3236', '#1C5E60', '#2E8C8C', '#4FC9C1', '#9CF2E6'], 'cloth', glow=True, torn=True, alpha=210)}


MODELS = {
    'hollow_sentinel': (sentinel, [('idle', 0), ('walk', 0.37), ('block', 0.2), ('bash', 0.4), ('bash', 0.5),
                                   ('strike', 0.75), ('strike', 0.9), ('death', 2.0), ('rise', 0.6)]),
    'hollow_arbalist': (arbalist, [('idle', 0), ('walk', 0.35), ('aim', 0.8), ('reload', 0.7), ('kick', 0.4), ('death', 2.0)]),
    'fallen_standard_bearer': (standard_bearer, [('idle', 0), ('walk', 0.4), ('sweep', 0.5), ('sweep', 0.6), ('rally', 0.9), ('rally', 1.5), ('death', 2.5)]),
    'soulbound_knight': (soulbound_knight, [('idle', 0), ('run', 0.22), ('combo', 0.45), ('combo', 0.6), ('lunge', 0.7), ('lunge', 0.9), ('counter', 0.5), ('death', 2.3)]),
}


def build_one(name, out_root, bb):
    fn, poses = MODELS[name]
    rig = fn()
    rig.palette.update(EXTRA)
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    info = rig.build(bb, Path(out_root) / name, preview_clips=poses)
    if name == 'hollow_sentinel':
        # The spectral royal guard shares the sentinel's geometry and UVs: repaint with a soul palette.
        ghost = sentinel('hollow_sentinel_spectral', palette=SPECTRAL)
        ghost.palette.update(EXTRA)
        atlas = ghost.paint()
        atlas.save(Path(out_root) / name / 'hollow_sentinel_spectral.png')
    return info


if __name__ == '__main__':
    from bbmcp import Blockbench
    out_root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants'
    names = sys.argv[2:] or list(MODELS)
    bb = Blockbench()
    for n in names:
        print(n, build_one(n, out_root, bb), flush=True)
