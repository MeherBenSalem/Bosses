"""The Hollow Sovereign — the Last King of Vael.

Editor conventions (verified with an axis test render):
  front = -Z, +X = the model's left.  Rotation X+ swings a bone's lower end forward (-Z);
  Z+ swings it toward +X; Y+ turns the front toward -X (the model's right).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402

NAME = 'hollow_sovereign'
GOLD_RIM = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
GOLD_HEM = ('band', {'row': -1, 'size': 1, 'mat': 'gold'})
SOUL_SPILL = lambda edge, w=2: ('glow_edge', {'mat': 'soul', 'edge': edge, 'width': w, 'faces': 'north'})


def build():
    r = Rig(NAME, (256, 128))
    r.bone('hips', (0, 30, 0.5))
    r.bone_pair('leg_{side}', (4, 29, 0.5), 'hips')
    r.bone_pair('shin_{side}', (4, 17.5, 0.5), 'leg_{side}')
    r.bone_pair('foot_{side}', (4, 4, 0.5), 'shin_{side}')
    r.bone('torso', (0, 33, 0.5), 'hips')
    r.bone('breastplate', (0, 44, -2), 'torso')
    r.bone('soul', (0, 44.5, -1), 'torso')
    r.bone('cape', (0, 51, 5.3), 'torso')
    r.bone('cape_lower', (0, 38, 5.6), 'cape')
    r.bone('head', (0, 52.5, 0.2), 'torso')
    r.bone('crown', (0, 64.5, 0.2), 'head')
    r.bone_pair('pauldron_{side}', (12.5, 51.5, 0.6), 'torso')
    r.bone_pair('arm_{side}', (9.5, 48, 0.6), 'torso')
    r.bone_pair('forearm_{side}', (10, 39, 0.6), 'arm_{side}')
    r.bone_pair('hand_{side}', (10, 30.5, 0.6), 'forearm_{side}')
    r.bone('sword', (-10, 28.5, 0.6), 'hand_right')

    r.box('hips', 'pelvis', (0, 31, 0.5), (12, 5, 8), 'iron')
    r.box('hips', 'belt', (0, 33.5, 0.5), (13, 2, 9), 'leather', decals=[('rivets', {'mat': 'gold', 'spacing': 4})])
    r.box('hips', 'buckle', (0, 33.5, -4.3), (3, 3, 0.6), 'gold', decals=[('glyph', {'bitmap': ['#+#', '+.+', '#+#'], 'mat': 'gold', 'faces': 'north'})])
    r.box('hips', 'fauld_front', (0, 28.3, -3.6), (9, 5, 1.5), 'iron', rot=(-8, 0, 0), decals=[GOLD_HEM])
    r.box('hips', 'fauld_back', (0, 28.3, 4.6), (10, 5, 1.5), 'iron', rot=(8, 0, 0), decals=[GOLD_HEM])
    r.pair('hips', '{side}_tasset', (6.2, 27.2, 0.5), (3, 7, 8), 'iron', rot=(0, 0, 10), decals=[GOLD_HEM])
    r.pair('leg_{side}', '{side}_cuisse', (4, 23.2, 0.5), (6, 11, 7), 'iron')
    r.pair('shin_{side}', '{side}_greave', (4, 10.6, 0.9), (5.5, 12, 6), 'iron',
           decals=[('vband', {'mat': 'gold', 'faces': 'north', 'size': 1})])
    r.pair('shin_{side}', '{side}_poleyn', (4, 17.4, -2.5), (5, 5, 3), 'iron', decals=[('rim', {'mat': 'gold', 'faces': 'north'})])
    r.pair('foot_{side}', '{side}_sabaton', (4, 2, -1.2), (6, 4, 10), 'iron')
    r.pair('foot_{side}', '{side}_sabaton_tip', (4, 1.2, -7), (4.5, 2.4, 2), 'iron_dark')

    # torso: a hollow cuirass
    r.box('torso', 'waist', (0, 37, 0.8), (11, 6, 8), 'iron',
          decals=[('band', {'row': 1, 'size': 1, 'mat': 'iron_dark'}), ('band', {'row': 4, 'size': 1, 'mat': 'iron_dark'})])
    r.box('torso', 'cuirass_back', (0, 45, 3), (17, 12, 5), 'iron', decals=[GOLD_RIM, ('filigree', {'mat': 'gold', 'row': 2, 'size': 2, 'faces': 'south'})])
    r.box('torso', 'hollow', (0, 44.5, -0.4), (6, 7.5, 3.2), 'void')
    r.pair('breastplate', 'plate_{side}', (5.1, 44.6, -1.9), (6.5, 11, 4), 'iron', rot=(0, -12, 0),
           decals=[GOLD_RIM, ('filigree', {'mat': 'gold', 'row': 2, 'size': 2, 'faces': 'north'}), SOUL_SPILL('right', 3)])
    r.box('breastplate', 'plate_collar', (0, 49.6, -1.3), (8, 2.5, 4), 'iron', decals=[GOLD_RIM, SOUL_SPILL('bottom', 1)])
    r.box('breastplate', 'plate_lower', (0, 39.6, -1.6), (8, 3, 4), 'iron', decals=[GOLD_HEM, SOUL_SPILL('top', 2)])
    r.box('breastplate', 'plate_shard', (2.3, 46.6, -3.7), (2, 3, 0.8), 'iron', rot=(0, 0, 28))
    r.box('breastplate', 'crest_vael', (-4.9, 45.5, -4.2), (3, 3, 0.4), 'gold', rot=(0, -12, 0),
          decals=[('glyph', {'bitmap': ['.#.', '#+#', '.#.'], 'mat': 'gold', 'faces': 'north'})])
    r.box('soul', 'soul_core', (0, 44.5, -1.4), (3, 4, 2), 'soul')
    r.box('soul', 'soul_flare', (0, 45, -1.7), (4.6, 5.6, 0.6), 'soul_dim')
    r.box('torso', 'gorget', (0, 51.4, 0.6), (10, 3, 8), 'iron', decals=[GOLD_RIM])
    r.pair('torso', '{side}_bind', (8.8, 48.6, 0.6), (2, 2, 3), 'soul_dim')
    r.pair('torso', '{side}_clasp', (6.6, 51.6, 4.2), (2.4, 2.4, 1.4), 'gold')

    r.box('cape', 'cape_upper', (0, 44.5, 5.9), (18, 13, 1), 'royal',
          decals=[('band', {'row': 0, 'size': 2, 'mat': 'gold', 'studs': 3})])
    r.box('cape_lower', 'cape_tail', (0, 28.5, 6.1), (17, 19, 1), 'royal',
          decals=[('band', {'row': 1, 'size': 1, 'mat': 'gold'})])

    # floating pauldrons
    r.pair('pauldron_{side}', '{side}_pauldron', (12.6, 52.6, 0.6), (9, 6, 11), 'iron', rot=(0, 0, -14),
           decals=[GOLD_RIM, GOLD_HEM])
    r.pair('pauldron_{side}', '{side}_pauldron_lame', (13.6, 48.8, 0.6), (8, 3, 10), 'iron', rot=(0, 0, -22),
           decals=[GOLD_HEM, ('glow_edge', {'mat': 'soul', 'edge': 'bottom', 'width': 1, 'strength': 0.22, 'faces': 'sides'})])
    r.pair('pauldron_{side}', '{side}_pauldron_ridge', (12.2, 56.2, 0.6), (5, 1.5, 9), 'gold', rot=(0, 0, -14))
    r.pair('torso', '{side}_pauldron_glow', (10.4, 49.6, 0.6), (4, 0.6, 7), 'soul_dim')

    r.pair('arm_{side}', '{side}_rerebrace', (10, 43.2, 0.6), (5, 8, 5), 'iron')
    r.pair('forearm_{side}', '{side}_couter', (10, 39, 1.3), (5.5, 3.5, 5.5), 'iron', decals=[('rim', {'mat': 'gold', 'faces': 'north'})])
    r.pair('forearm_{side}', '{side}_vambrace', (10, 34.4, 0.6), (5.5, 8, 5.5), 'iron', decals=[GOLD_RIM])
    r.pair('hand_{side}', '{side}_cuff', (10, 31, 0.6), (6, 2, 6), 'iron', decals=[GOLD_RIM])
    r.pair('hand_{side}', '{side}_gauntlet', (10, 28.4, 0.6), (5, 5, 5.5), 'iron_dark')
    r.pair('hand_{side}', '{side}_knuckles', (10, 26.3, -0.6), (4.5, 1.5, 4), 'iron')

    # the greatsword (blade down in bind pose)
    r.box('sword', 'grip', (-10, 28.5, 0.6), (1.6, 8, 1.6), 'leather')
    r.box('sword', 'pommel', (-10, 33.6, 0.6), (2.6, 2.2, 2.6), 'gold')
    r.box('sword', 'guard', (-10, 23.6, 0.6), (11, 2, 2.4), 'gold')
    r.box('sword', 'guard_left_tip', (-4.2, 24.6, 0.6), (1.4, 2.6, 1.8), 'gold')
    r.box('sword', 'guard_right_tip', (-15.8, 24.6, 0.6), (1.4, 2.6, 1.8), 'gold')
    r.box('sword', 'guard_gem', (-10, 23.6, -0.75), (1.4, 1.4, 0.4), 'soul')
    r.box('sword', 'ricasso', (-10, 21.6, 0.6), (3.2, 2, 1.4), 'steel')
    r.box('sword', 'blade', (-10, 7.5, 0.6), (4.2, 26, 1), 'blade')
    r.box('sword', 'fuller', (-10, 9.5, 0.6), (1, 21, 1.2), 'soul_dim')
    r.box('sword', 'blade_tip', (-10, -7.5, 0.6), (2.8, 4, 1), 'blade')
    r.box('sword', 'blade_point', (-10, -10.3, 0.6), (1.4, 1.6, 1), 'blade')

    r.box('head', 'aventail', (0, 52.9, 0.5), (9, 2, 8.5), 'iron_dark')
    r.box('head', 'helm', (0, 57.6, 0.2), (8, 9.4, 8.4), 'iron', decals=[('band', {'row': -2, 'size': 1, 'mat': 'iron_dark'}),
          ('glow_edge', {'mat': 'soul', 'edge': 'top', 'width': 1, 'strength': 0.25, 'faces': 'north'})])
    r.box('head', 'visor_slit', (0, 58.6, -4.05), (6, 1.2, 0.4), 'void')
    r.box('head', 'visor_vslit', (0, 56.2, -4.05), (1.2, 3.6, 0.4), 'void')
    r.pair('head', 'eye_{side}', (1.6, 58.6, -4.3), (1.4, 0.8, 0.3), 'soul')
    r.box('head', 'helm_brow', (0, 60.3, -4.1), (8.4, 1.2, 1), 'gold')
    r.box('head', 'helm_crest', (0, 62.9, 0.4), (1.4, 1.6, 7), 'gold')
    r.pair('head', '{side}_cheek', (4.1, 55.4, -2.2), (0.8, 4.4, 4), 'iron', rot=(0, 8, 0))

    # shattered floating crown
    y = 64.4
    r.box('crown', 'band_front', (0, y, -4.6), (10, 1.6, 1), 'gold')
    r.box('crown', 'band_back', (0, y, 5), (10, 1.6, 1), 'gold')
    r.box('crown', 'band_left', (4.6, y, 0.2), (1, 1.6, 8.6), 'gold')
    r.box('crown', 'band_right', (-4.6, y, 2.6), (1, 1.6, 3.8), 'gold')
    # Spiked points: three stepped cubes each (base, shoulder, tip) so they read as spikes, not fingers
    for name, (x, z), height in [('front', (0, -4.6), 4.6), ('front_left', (3.4, -4.6), 3.0), ('left', (4.6, 0.2), 3.4),
                                 ('back_left', (3.4, 5), 2.6), ('back', (0, 5), 3.2), ('back_right', (-3.4, 5), 2.2)]:
        thin = name == 'left'
        base = (1.0, height * 0.45, 2.0) if thin else (2.0, height * 0.45, 1.0)
        mid = (0.8, height * 0.35, 1.2) if thin else (1.2, height * 0.35, 0.8)
        y0 = y + 0.8
        r.box('crown', f'point_{name}', (x, y0 + base[1] / 2, z), base, 'gold')
        r.box('crown', f'spike_{name}', (x, y0 + base[1] + mid[1] / 2, z), mid, 'gold')
        r.box('crown', f'tip_{name}', (x, y0 + base[1] + mid[1] + height * 0.1, z), (0.5, height * 0.2, 0.5), 'gold')
    r.box('crown', 'stub_front_right', (-3.4, y + 1.2, -4.6), (1.2, 0.8, 1), 'gold')
    r.box('crown', 'crown_gem', (0, y + 0.2, -5.25), (1.4, 1.4, 0.4), 'soul')
    r.box('crown', 'shard_a', (-6.4, y + 2.4, -1.2), (0.8, 1.8, 0.8), 'gold', rot=(0, 0, 30))
    r.box('crown', 'shard_b', (-6.0, y - 0.6, -3.4), (0.8, 1.2, 0.8), 'gold', rot=(20, 0, -15))
    r.box('crown', 'shard_c', (-5.0, y + 3.6, -3.0), (0.6, 1, 0.6), 'gold', rot=(10, 0, 45))

    r.mark_upright('torso', 'head')
    clips(r)
    return r


# Animation. Hit and spawn ticks below match the Java attack timings (20 ticks per second).
CONTRACT = {
    'combo': {'length_ticks': 48, 'hits': [14, 26, 38]},
    'throne_slam': {'length_ticks': 44, 'hits': [22]},
    'guard': {'length_ticks': 40, 'window': [4, 32]},
    'riposte': {'length_ticks': 18, 'hits': [6]},
    'rift_cleave': {'length_ticks': 40, 'hits': [18]},
    'summon': {'length_ticks': 40, 'spawn': [24]},
    'spectral_slash': {'length_ticks': 32, 'release': [16]},
    'soul_lunge': {'length_ticks': 30, 'dash': [12, 18], 'hits': [16]},
    'last_command': {'length_ticks': 120, 'marks': [30, 50, 70], 'strikes': [60, 80, 100]},
    'phase_shatter': {'length_ticks': 60, 'spawn': [36]},
    'phase_soul': {'length_ticks': 60},
    'stagger': {'length_ticks': 20},
    'awaken': {'length_ticks': 60},
    'death': {'length_ticks': 80},
}

# Held-sword rest arm used by idle and walk: right arm slightly forward and out, sword angled forward-down.
REST_R = {'arm_right': {'rot': [10, 0, -8]}, 'forearm_right': {'rot': [16, 0, 0]}, 'sword': {'rot': [16, 0, -10]}}
REST_L = {'arm_left': {'rot': [4, 0, 6]}, 'forearm_left': {'rot': [10, 0, 0]}}


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    rest = merge(REST_R, REST_L)

    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'torso': {'rot': [0, 0, 0]}, 'soul': {'scale': 1}, 'crown': {'pos': [0, 0, 0], 'rot': [0, 0, 0]},
                           'pauldron_left': {'pos': [0, 0, 0]}, 'pauldron_right': {'pos': [0, 0.6, 0]},
                           'cape_lower': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}})),
        (1.5, {'torso': {'rot': [2.5, 0, 0]}, 'soul': {'scale': 1.15}, 'crown': {'pos': [0, 1.2, 0], 'rot': [0, 6, 0]},
               'pauldron_left': {'pos': [0, 0.8, 0]}, 'pauldron_right': {'pos': [0, -0.2, 0]},
               'cape_lower': {'rot': [-3, 0, 0]}, 'head': {'rot': [3, 0, 0]}}),
        (3.0, {'torso': {'rot': [0, 0, 0]}, 'soul': {'scale': 1}, 'crown': {'pos': [0, 0, 0], 'rot': [0, 0, 0]},
               'pauldron_left': {'pos': [0, 0, 0]}, 'pauldron_right': {'pos': [0, 0.6, 0]},
               'cape_lower': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}}),
    ])

    def stride(name, length, swing, lift, bob, lean, arm, cape):
        h = length / 2
        q = length / 4
        r.clip(name, length, True).poses([
            (0.0, merge(REST_R, {'leg_left': {'rot': [swing, 0, 0]}, 'leg_right': {'rot': [-swing, 0, 0]},
                                 'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [-4, 0, 0]},
                                 'hips': {'pos': [0, -bob, 0]}, 'torso': {'rot': [lean, -3, 0]},
                                 'arm_left': {'rot': [-arm, 0, 6]}, 'forearm_left': {'rot': [12, 0, 0]},
                                 'arm_right': {'rot': [10 + arm * 0.3, 0, -8]},
                                 'cape_lower': {'rot': [-cape, 0, 0]}, 'crown': {'pos': [0, 0, 0]}})),
            (q, {'shin_right': {'rot': [-lift, 0, 0]}, 'hips': {'pos': [0, bob, 0]}, 'cape_lower': {'rot': [-cape * 2, 0, 0]},
                 'crown': {'pos': [0, 0.8, 0]}}),
            (h, {'leg_left': {'rot': [-swing, 0, 0]}, 'leg_right': {'rot': [swing, 0, 0]},
                 'shin_left': {'rot': [-4, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]},
                 'hips': {'pos': [0, -bob, 0]}, 'torso': {'rot': [lean, 3, 0]},
                 'arm_left': {'rot': [arm, 0, 6]}, 'arm_right': {'rot': [10 - arm * 0.3, 0, -8]},
                 'cape_lower': {'rot': [-cape, 0, 0]}, 'crown': {'pos': [0, 0, 0]}}),
            (h + q, {'shin_left': {'rot': [-lift, 0, 0]}, 'hips': {'pos': [0, bob, 0]}, 'cape_lower': {'rot': [-cape * 2, 0, 0]},
                     'crown': {'pos': [0, 0.8, 0]}}),
            (length, {'leg_left': {'rot': [swing, 0, 0]}, 'leg_right': {'rot': [-swing, 0, 0]},
                      'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [-4, 0, 0]},
                      'hips': {'pos': [0, -bob, 0]}, 'torso': {'rot': [lean, -3, 0]},
                      'arm_left': {'rot': [-arm, 0, 6]}, 'arm_right': {'rot': [10 + arm * 0.3, 0, -8]},
                      'cape_lower': {'rot': [-cape, 0, 0]}, 'crown': {'pos': [0, 0, 0]}}),
        ])

    stride('walk', 1.6, 18, 30, 0.6, 3, 14, 4)
    stride('run', 1.0, 28, 48, 1.0, 10, 22, 9)

    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}})

    # Royal combo: diagonal slash (t14), backhand (t26), overhead chop (t38)
    r.clip('combo', 2.4).poses([
        (0.0, neutral),
        (0.55, {'arm_right': {'rot': [150, 0, -30]}, 'forearm_right': {'rot': [20, 0, 0]}, 'sword': {'rot': [0, 0, 0]},
                'torso': {'rot': [-4, 26, 0]}, 'leg_right': {'rot': [-10, 0, 0]}, 'leg_left': {'rot': [12, 0, 0]}}, 'easeOutCubic'),
        (0.7, {'arm_right': {'rot': [45, 0, 25]}, 'forearm_right': {'rot': [5, 0, 0]}, 'torso': {'rot': [8, -30, 0]}}, 'easeInQuart'),
        (1.05, {'arm_right': {'rot': [95, 0, 40]}, 'torso': {'rot': [4, -34, 0]}}, 'easeOutSine'),
        (1.3, {'arm_right': {'rot': [65, 0, -50]}, 'torso': {'rot': [6, 30, 0]}}, 'easeInQuart'),
        (1.7, {'arm_right': {'rot': [175, 0, -6]}, 'arm_left': {'rot': [160, 0, -10]}, 'forearm_right': {'rot': [10, 0, 0]},
               'torso': {'rot': [-12, 0, 0]}, 'head': {'rot': [-8, 0, 0]}}, 'easeOutCubic'),
        (1.9, {'arm_right': {'rot': [55, 0, -4]}, 'arm_left': {'rot': [50, 0, -14]}, 'torso': {'rot': [26, 0, 0]}, 'sword': {'rot': [62, 0, 0]},
               'hips': {'pos': [0, -2, -1]}, 'head': {'rot': [10, 0, 0]}, 'leg_left': {'rot': [24, 0, 0]},
               'shin_left': {'rot': [-20, 0, 0]}}, 'easeInQuart'),
        (2.4, neutral),
    ])

    # Throne slam: two-handed overhead raise, slam at t22, sword stuck until ~t32 (punish window)
    r.clip('throne_slam', 2.2).poses([
        (0.0, neutral),
        (0.95, {'arm_right': {'rot': [172, 0, -4]}, 'arm_left': {'rot': [165, 0, -16]}, 'forearm_right': {'rot': [8, 0, 0]},
                'forearm_left': {'rot': [12, 0, 0]}, 'sword': {'rot': [-10, 0, 0]}, 'torso': {'rot': [-14, 0, 0]},
                'head': {'rot': [-12, 0, 0]}, 'crown': {'pos': [0, 3, 0]}, 'soul': {'scale': 1.3}}, 'easeOutCubic'),
        (1.1, {'arm_right': {'rot': [40, 0, -4]}, 'arm_left': {'rot': [38, 0, -16]}, 'sword': {'rot': [40, 0, 0]},
               'torso': {'rot': [34, 0, 0]}, 'hips': {'pos': [0, -3, -1]}, 'head': {'rot': [12, 0, 0]},
               'leg_left': {'rot': [28, 0, 0]}, 'shin_left': {'rot': [-26, 0, 0]}, 'leg_right': {'rot': [-12, 0, 0]},
               'shin_right': {'rot': [-18, 0, 0]}, 'crown': {'pos': [0, -1, 0]}, 'soul': {'scale': 1}}, 'easeInExpo'),
        (1.6, {'torso': {'rot': [32, 0, 0]}, 'hips': {'pos': [0, -3, -1]}}),
        (2.2, merge(neutral, {'crown': {'pos': [0, 0, 0]}, 'arm_left': {'rot': [4, 0, 6]}, 'forearm_left': {'rot': [10, 0, 0]}})),
    ])

    # Guard: sword held across the body; Java switches to riposte if struck in melee during the window
    guard_pose = {'arm_right': {'rot': [62, 0, 34]}, 'forearm_right': {'rot': [46, 0, 0]}, 'sword': {'rot': [0, -10, 78]},
                  'arm_left': {'rot': [58, 0, -20]}, 'forearm_left': {'rot': [50, 0, 0]}, 'torso': {'rot': [6, -8, 0]},
                  'hips': {'pos': [0, -1, 0]}, 'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-12, 0, 0]}}
    r.clip('guard', 2.0).poses([
        (0.0, neutral), (0.2, guard_pose, 'easeOutBack'), (1.6, guard_pose), (2.0, neutral),
    ])
    r.clip('riposte', 0.9).poses([
        (0.0, guard_pose),
        (0.3, {'arm_right': {'rot': [92, 0, 6]}, 'forearm_right': {'rot': [4, 0, 0]}, 'sword': {'rot': [88, 0, 0]},
               'arm_left': {'rot': [20, 0, 20]}, 'forearm_left': {'rot': [20, 0, 0]}, 'torso': {'rot': [14, 12, 0]},
               'hips': {'pos': [0, -1.5, -3]}, 'leg_left': {'rot': [30, 0, 0]}, 'leg_right': {'rot': [-24, 0, 0]}}, 'easeInExpo'),
        (0.9, neutral),
    ])

    # Rift cleave: drag the sword back low, then an upward-forward cleave sending a ground wave (t18)
    r.clip('rift_cleave', 2.0).poses([
        (0.0, neutral),
        (0.8, {'arm_right': {'rot': [-38, 0, -18]}, 'forearm_right': {'rot': [10, 0, 0]}, 'sword': {'rot': [30, 0, -20]},
               'torso': {'rot': [16, 32, 0]}, 'hips': {'pos': [0, -2.5, 0]}, 'leg_right': {'rot': [-18, 0, 0]},
               'leg_left': {'rot': [22, 0, 0]}, 'shin_left': {'rot': [-24, 0, 0]}, 'soul': {'scale': 1.25}}, 'easeOutCubic'),
        (0.9, {'arm_right': {'rot': [128, 0, 8]}, 'forearm_right': {'rot': [0, 0, 0]}, 'sword': {'rot': [0, 0, 0]},
               'torso': {'rot': [-10, -22, 0]}, 'hips': {'pos': [0, 0, -1]}, 'soul': {'scale': 1}}, 'easeInExpo'),
        (1.3, {'torso': {'rot': [-8, -20, 0]}}),
        (2.0, neutral),
    ])

    # Spectral slash (phase 3): wide horizontal sweep releasing crescents at t16
    r.clip('spectral_slash', 1.6).poses([
        (0.0, neutral),
        (0.7, {'arm_right': {'rot': [96, 0, -62]}, 'forearm_right': {'rot': [12, 0, 0]}, 'sword': {'rot': [0, 0, -70]},
               'torso': {'rot': [2, 38, 0]}, 'soul': {'scale': 1.4}}, 'easeOutCubic'),
        (0.8, {'arm_right': {'rot': [92, 0, 48]}, 'sword': {'rot': [0, 0, 60]}, 'torso': {'rot': [4, -40, 0]},
               'soul': {'scale': 1}}, 'easeInExpo'),
        (1.1, {'torso': {'rot': [4, -36, 0]}}),
        (1.6, neutral),
    ])

    # Soul lunge (phase 3): crouch, dash between t12 and t18, thrust lands t16
    r.clip('soul_lunge', 1.5).poses([
        (0.0, neutral),
        (0.6, {'arm_right': {'rot': [-30, 0, -10]}, 'forearm_right': {'rot': [20, 0, 0]}, 'sword': {'rot': [70, 0, 0]},
               'torso': {'rot': [26, 14, 0]}, 'hips': {'pos': [0, -3, 0]}, 'leg_left': {'rot': [32, 0, 0]},
               'shin_left': {'rot': [-34, 0, 0]}, 'leg_right': {'rot': [-20, 0, 0]}, 'shin_right': {'rot': [-24, 0, 0]},
               'cape_lower': {'rot': [-10, 0, 0]}}, 'easeOutCubic'),
        (0.8, {'arm_right': {'rot': [94, 0, 2]}, 'forearm_right': {'rot': [0, 0, 0]}, 'sword': {'rot': [86, 0, 0]},
               'torso': {'rot': [30, -6, 0]}, 'hips': {'pos': [0, -2, -2]}, 'leg_left': {'rot': [40, 0, 0]},
               'leg_right': {'rot': [-34, 0, 0]}, 'cape_lower': {'rot': [-34, 0, 0]}}, 'easeInExpo'),
        (1.0, {'cape_lower': {'rot': [-20, 0, 0]}}),
        (1.5, merge(neutral, {'cape_lower': {'rot': [0, 0, 0]}})),
    ])

    # Summon (phase 2+): sword raised skyward; guards rise at t24
    r.clip('summon', 2.0).poses([
        (0.0, neutral),
        (0.9, {'arm_right': {'rot': [178, 0, -6]}, 'forearm_right': {'rot': [0, 0, 0]}, 'sword': {'rot': [180, 0, 0]},
               'arm_left': {'rot': [30, 0, 40]}, 'torso': {'rot': [-10, 0, 0]}, 'head': {'rot': [-24, 0, 0]},
               'crown': {'pos': [0, 2, 0], 'rot': [0, 0, 0]}}, 'easeOutCubic'),
        (1.2, {'soul': {'scale': 1.5}, 'crown': {'pos': [0, 4, 0], 'rot': [0, 90, 0]}}, 'easeOutBack'),
        (1.5, {'soul': {'scale': 1}, 'crown': {'rot': [0, 180, 0]}}),
        (2.0, merge(neutral, {'crown': {'pos': [0, 0, 0], 'rot': [0, 360, 0]}, 'arm_left': {'rot': [4, 0, 6]}})),
    ])

    # Phase 2 transition: armour tears loose, pauldrons fly (bones hidden in Java after this), guards called
    r.clip('phase_shatter', 3.0).poses([
        (0.0, merge(neutral, {'pauldron_left': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'pauldron_right': {'pos': [0, 0, 0], 'rot': [0, 0, 0]},
                              'crown': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
        (0.5, {'torso': {'rot': [22, 0, 0]}, 'head': {'rot': [24, 0, 0]}, 'hips': {'pos': [0, -2, 1]},
               'arm_left': {'rot': [20, 0, 10]}, 'arm_right': {'rot': [30, 0, -14]}}, 'easeOutCubic'),
        (1.0, {'torso': {'rot': [-18, 0, 0]}, 'head': {'rot': [-26, 0, 0]}, 'hips': {'pos': [0, 0, 0]},
               'arm_left': {'rot': [40, 0, 70]}, 'arm_right': {'rot': [40, 0, -70]},
               'pauldron_left': {'pos': [8, 6, 0], 'rot': [0, 0, 30]}, 'pauldron_right': {'pos': [-8, 6, 0], 'rot': [0, 0, -30]},
               'crown': {'pos': [0, 6, 0], 'rot': [0, 180, 0]}, 'soul': {'scale': 1.6}}, 'easeOutExpo'),
        (1.8, {'pauldron_left': {'pos': [14, 10, 2], 'rot': [0, 90, 50]}, 'pauldron_right': {'pos': [-14, 10, 2], 'rot': [0, -90, -50]},
               'crown': {'pos': [0, 5, 0], 'rot': [0, 360, 0]}, 'soul': {'scale': 1.3}}),
        (3.0, merge(neutral, {'crown': {'pos': [0, 0, 0], 'rot': [0, 360, 0]}, 'soul': {'scale': 1},
                              'pauldron_left': {'pos': [0, 0, 0], 'rot': [0, 0, 0]},
                              'pauldron_right': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
    ])

    # Phase 3 transition: the breastplate bursts away and the soul flares (breastplate hidden in Java after)
    r.clip('phase_soul', 3.0).poses([
        (0.0, merge(neutral, {'breastplate': {'pos': [0, 0, 0], 'scale': 1}, 'soul': {'scale': 1}})),
        (0.8, {'torso': {'rot': [24, 0, 0]}, 'hips': {'pos': [0, -6, 0]}, 'leg_left': {'rot': [60, 0, 0]},
               'shin_left': {'rot': [-60, 0, 0]}, 'leg_right': {'rot': [-6, 0, 0]}, 'shin_right': {'rot': [-70, 0, 0]},
               'arm_left': {'rot': [10, 0, 10]}, 'head': {'rot': [26, 0, 0]}}, 'easeOutCubic'),
        (1.3, {'torso': {'rot': [-28, 0, 0]}, 'head': {'rot': [-30, 0, 0]}, 'arm_left': {'rot': [30, 0, 80]},
               'arm_right': {'rot': [30, 0, -80]}, 'breastplate': {'pos': [0, 2, -9], 'scale': 0.4},
               'soul': {'scale': 2.4}}, 'easeOutExpo'),
        (1.5, {'breastplate': {'pos': [0, 3, -12], 'scale': 0}}),
        (2.2, {'soul': {'scale': 1.8}, 'hips': {'pos': [0, -6, 0]}}),
        (3.0, merge(neutral, {'breastplate': {'pos': [0, 0, 0], 'scale': 1}, 'soul': {'scale': 1.6}})),
    ])

    # The Last Command (once, at 10%): kneel, plant the sword, three waves of marked blades
    kneel = {'hips': {'pos': [0, -9, 0]}, 'leg_left': {'rot': [80, 0, 0]}, 'shin_left': {'rot': [-82, 0, 0]},
             'leg_right': {'rot': [-4, 0, 0]}, 'shin_right': {'rot': [-86, 0, 0]}, 'torso': {'rot': [18, 0, 0]},
             'arm_right': {'rot': [62, 0, -4]}, 'forearm_right': {'rot': [10, 0, 0]}, 'sword': {'rot': [-30, 0, 0]},
             'arm_left': {'rot': [56, 0, -26]}, 'forearm_left': {'rot': [40, 0, 0]}, 'head': {'rot': [26, 0, 0]}}
    r.clip('last_command', 6.0).poses([
        (0.0, merge(neutral, {'soul': {'scale': 1}, 'crown': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
        (1.0, kneel, 'easeOutCubic'),
        (1.5, {'head': {'rot': [-20, 0, 0]}, 'soul': {'scale': 1.8}, 'crown': {'pos': [0, 5, 0]}}, 'easeOutBack'),
        (3.0, {'crown': {'pos': [0, 6, 0], 'rot': [0, 360, 0]}, 'soul': {'scale': 1.5}}, 'linear'),
        (4.0, {'crown': {'pos': [0, 6, 0], 'rot': [0, 720, 0]}, 'soul': {'scale': 2}}, 'linear'),
        (5.0, {'crown': {'pos': [0, 5, 0], 'rot': [0, 1080, 0]}, 'soul': {'scale': 1.6}, 'head': {'rot': [10, 0, 0]}}, 'linear'),
        (6.0, merge(neutral, {'soul': {'scale': 1}, 'crown': {'pos': [0, 0, 0], 'rot': [0, 1080, 0]}})),
    ])

    r.clip('stagger', 1.0).poses([
        (0.0, neutral),
        (0.2, {'torso': {'rot': [-16, 10, 6]}, 'head': {'rot': [-14, 0, 0]}, 'hips': {'pos': [0, -1, 2]},
               'arm_left': {'rot': [26, 0, 30]}, 'arm_right': {'rot': [-10, 0, -26]}, 'leg_left': {'rot': [-10, 0, 0]}}, 'easeOutExpo'),
        (1.0, neutral),
    ])

    r.clip('awaken', 3.0).poses([
        (0.0, merge(kneel, {'torso': {'rot': [34, 0, 0]}, 'head': {'rot': [34, 0, 0]}, 'soul': {'scale': 0.2},
                            'crown': {'pos': [0, -2, 0], 'rot': [0, 0, 0]}})),
        (1.0, {'soul': {'scale': 1}}),
        (1.6, {'head': {'rot': [-10, 0, 0]}, 'crown': {'pos': [0, 4, 0], 'rot': [0, 180, 0]}, 'soul': {'scale': 1.5}}, 'easeOutBack'),
        (2.5, merge(neutral, {'crown': {'pos': [0, 1, 0], 'rot': [0, 360, 0]}, 'soul': {'scale': 1.1}})),
        (3.0, merge(neutral, {'crown': {'pos': [0, 0, 0], 'rot': [0, 360, 0]}, 'soul': {'scale': 1}})),
    ])

    r.clip('death', 4.0, 'hold').poses([
        (0.0, merge(neutral, {'soul': {'scale': 1}, 'crown': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
        (0.6, {'torso': {'rot': [-14, 0, 4]}, 'head': {'rot': [-16, 0, 0]}, 'soul': {'scale': 1.6}}, 'easeOutCubic'),
        (1.8, merge(kneel, {'torso': {'rot': [30, 0, -4]}, 'head': {'rot': [32, 0, 0]}, 'sword': {'rot': [-30, 0, 0]},
                            'soul': {'scale': 1.2}})),
        (2.6, {'arm_right': {'rot': [20, 0, -20]}, 'sword': {'rot': [40, 0, -70]}, 'arm_left': {'rot': [10, 0, 10]},
               'forearm_left': {'rot': [6, 0, 0]}, 'crown': {'pos': [-6, -6, -6], 'rot': [30, 40, 60]}, 'head': {'rot': [40, 0, 6]}}),
        (3.6, {'soul': {'scale': 0}, 'torso': {'rot': [42, 0, -4]}}, 'easeInCubic'),
        (4.0, {'soul': {'scale': 0}}),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 0.0), ('combo', 0.55), ('combo', 1.9), ('throne_slam', 0.95), ('throne_slam', 1.2),
             ('guard', 1.0), ('spectral_slash', 0.7), ('phase_soul', 2.2), ('last_command', 2.0), ('death', 4.0),
             ('walk', 0.4)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
