"""The Ashen Colossus — The Furnace That Never Died (Kharum, the Ashen Forges).

Kharum's great furnace-guardian: basalt bulk sheathed in weathered bronze, a cracked furnace
chest behind a grille door, chimney stacks venting from the shoulders, piston forearms ending
in anvil fists, gear-jointed knees. Arm and back plates are separate bones so the game can break
them off (destructible armor); the core scales with heat and is exposed when it overheats.
Editor conventions as in hollow_sovereign.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paint import Material  # noqa: E402
from rig import Rig  # noqa: E402

NAME = 'ashen_colossus'
PALETTE = {
    'basalt': Material(['#0F0E0E', '#181616', '#221F1E', '#2E2A28', '#3B3633', '#4B4541'], 'stone', accent='soot'),
}
RIM = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
SOOT = ('gradient', {'mat': 'soot', 'size': 3})
GEAR_TEETH = 8


def gear(r, bone, name, center, radius, thickness, axis='x', mat='bronze'):
    cx, cy, cz = center
    if axis == 'x':
        r.box(bone, f'{name}_hub', center, (thickness, radius, radius), mat)
        for k in range(GEAR_TEETH // 2):
            r.box(bone, f'{name}_tooth_{k}', center, (thickness * 0.8, radius * 2, 1.2), mat, rot=(45 * k, 0, 0))
    else:
        r.box(bone, f'{name}_hub', center, (radius, radius, thickness), mat)
        for k in range(GEAR_TEETH // 2):
            r.box(bone, f'{name}_tooth_{k}', center, (1.2, radius * 2, thickness * 0.8), mat, rot=(0, 0, 45 * k))


def build():
    r = Rig(NAME, (256, 256), palette=PALETTE)
    r.mark_upright('torso', 'head')
    r.bone('hips', (0, 30, 2))
    r.bone_pair('leg_{side}', (8, 29, 2), 'hips')
    r.bone_pair('shin_{side}', (8.5, 16, 1), 'leg_{side}')
    r.bone('torso', (0, 33, 2), 'hips')
    r.bone('core', (0, 47, -2), 'torso')
    r.bone('door', (-6, 47, -8.4), 'torso')
    r.bone('head', (0, 63, -1), 'torso')
    r.bone_pair('chimney_{side}', (9, 62, 6), 'torso')
    r.bone('plate_back', (0, 50, 10), 'torso')
    r.bone_pair('arm_{side}', (17, 58, 1), 'torso')
    r.bone_pair('plate_arm_{side}', (19, 58, 1), 'arm_{side}')
    r.bone_pair('piston_{side}', (19.5, 44, 0), 'arm_{side}')
    r.bone_pair('fist_{side}', (19.5, 30, 0), 'piston_{side}')

    # legs: basalt pillars with bronze greaves and gear knees
    r.box('hips', 'pelvis', (0, 30, 2), (20, 7, 14), 'bronze', decals=[RIM, SOOT])
    r.box('hips', 'belly_grate', (0, 30, -5.2), (10, 4, 0.6), 'iron_dark',
          decals=[('glyph', {'bitmap': ['+.+.+.+.+', '*.*.*.*.*'], 'mat': 'ember', 'faces': 'north'})])
    r.pair('leg_{side}', '{side}_thigh', (8.5, 22.5, 2), (10, 13, 10), 'basalt')
    r.pair('leg_{side}', '{side}_thigh_plate', (8.5, 23, -3.4), (9, 10, 1.2), 'bronze', decals=[RIM])
    gear(r, 'shin_left', 'left_knee_gear', (14, 16, 1), 5, 1.4, axis='x')
    gear(r, 'shin_right', 'right_knee_gear', (-14, 16, 1), 5, 1.4, axis='x')
    r.pair('shin_{side}', '{side}_shin', (8.5, 9, 1), (9.4, 13, 9.4), 'basalt')
    r.pair('shin_{side}', '{side}_greave', (8.5, 9.5, -4), (8.4, 11, 1.4), 'bronze', decals=[RIM, SOOT])
    r.pair('shin_{side}', '{side}_foot', (8.5, 2, -1.4), (12, 4, 15), 'iron_dark', decals=[('rivets', {'mat': 'bronze', 'spacing': 3})])

    # torso: a furnace in a bronze carapace
    r.box('torso', 'barrel', (0, 47, 2), (26, 24, 18), 'basalt')
    r.box('torso', 'carapace', (0, 51, 1), (28, 16, 19), 'bronze',
          decals=[RIM, ('filigree', {'mat': 'gold', 'row': 2, 'size': 2, 'faces': ('east', 'west')}), SOOT])
    r.box('torso', 'firebox_frame', (0, 46, -7.8), (14, 14, 1.6), 'iron_dark', decals=[('rim', {'mat': 'bronze', 'faces': 'north'})])
    r.box('torso', 'firebox', (0, 46, -7.2), (11, 11, 1), 'void')
    r.box('torso', 'chest_crack', (8, 54, -8.6), (1, 9, 0.4), 'ember', rot=(0, 0, 18))
    r.box('core', 'heart', (0, 46, -5), (7, 7, 4), 'ember')
    r.box('core', 'heart_flare', (0, 46, -6.6), (9, 9, 0.4), 'ember')
    r.box('door', 'door_grille', (0, 46, -8.9), (11.4, 11.4, 0.6), 'iron_dark',
          decals=[('glyph', {'bitmap': ['#.#.#.#.#.#'] * 11, 'mat': 'soot', 'faces': 'north'})])
    r.box('door', 'door_handle', (5, 46, -9.6), (1, 3, 1), 'bronze')
    r.box('torso', 'gorget', (0, 61, 0), (14, 4, 12), 'bronze', decals=[RIM])
    for side, s in (('left', 1), ('right', -1)):
        c = f'chimney_{side}'
        r.box(c, f'{c}_stack', (s * 9, 66, 6), (4.4, 10, 4.4), 'iron_dark', decals=[('band', {'row': 2, 'size': 1, 'mat': 'bronze'})])
        r.box(c, f'{c}_cap', (s * 9, 71.6, 6), (6, 1.4, 6), 'iron_dark', decals=[RIM])
        r.box(c, f'{c}_glow', (s * 9, 71, 6), (3, 0.4, 3), 'ember')
    r.box('plate_back', 'back_plate', (0, 50, 11.6), (22, 18, 2), 'bronze', decals=[RIM, SOOT, ('rivets', {'mat': 'gold', 'spacing': 4})])
    r.box('plate_back', 'back_vent', (0, 44, 12.8), (12, 3, 0.6), 'iron_dark',
          decals=[('glyph', {'bitmap': ['+.+.+.+.+.+', '*.*.*.*.*.*', '+.+.+.+.+.+'], 'mat': 'ember', 'faces': 'south'})])
    # head: a heavy furnace-hood with a single ember slit
    r.box('head', 'hood', (0, 66, -2), (10, 8, 10), 'bronze', decals=[RIM])
    r.box('head', 'brow', (0, 68.6, -7.4), (11, 2, 1.6), 'bronze')
    r.box('head', 'slit', (0, 66.4, -7.1), (7, 1.2, 0.3), 'ember')
    r.box('head', 'jaw', (0, 62.4, -5), (8, 2.4, 6), 'iron_dark')
    r.box('head', 'crest', (0, 71, -2), (2, 2, 8), 'iron_dark')

    # arms: shoulder slab, removable plates, pistons, anvil fists
    for side, s in (('left', 1), ('right', -1)):
        a, p, pi, f = f'arm_{side}', f'plate_arm_{side}', f'piston_{side}', f'fist_{side}'
        r.box(a, f'{a}_shoulder', (s * 19, 57, 1), (10, 10, 11), 'basalt')
        r.box(a, f'{a}_upper', (s * 19.5, 49, 0.5), (7.4, 10, 7.4), 'iron_dark')
        r.box(p, f'{p}_pauldron', (s * 20, 60.4, 1), (12, 5, 13), 'bronze', rot=(0, 0, -s * 12), decals=[RIM, SOOT])
        r.box(p, f'{p}_cuff', (s * 19.5, 49, 0.5), (8.6, 4, 8.6), 'bronze', decals=[RIM])
        r.box(pi, f'{pi}_cylinder', (s * 19.5, 40, 0), (6.4, 9, 6.4), 'bronze', decals=[RIM, SOOT])
        r.box(pi, f'{pi}_rod', (s * 19.5, 34, 0), (3, 5, 3), 'steel')
        r.box(f, f'{f}_anvil', (s * 19.5, 27, 0), (11, 8, 11), 'iron_dark', decals=[('band', {'row': 1, 'size': 1, 'mat': 'bronze'}),
                                                                                   ('rivets', {'mat': 'bronze', 'spacing': 3})])
        r.box(f, f'{f}_face', (s * 19.5, 22.6, 0), (12, 1.4, 12), 'steel')

    clips(r)
    return r


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    neutral = {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
               'arm_left': {'rot': [0, 0, 4]}, 'arm_right': {'rot': [0, 0, -4]}, 'piston_left': {'rot': [12, 0, 0]},
               'piston_right': {'rot': [12, 0, 0]}, 'core': {'scale': 1}, 'door': {'rot': [0, 0, 0]},
               'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]}, 'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}}
    r.clip('idle', 4.0, True).poses([
        (0.0, merge(neutral, {'core': {'scale': 1}})),
        (1.0, {'core': {'scale': 1.12}, 'torso': {'rot': [-1.5, 0, 0]}}),
        (2.0, {'core': {'scale': 1}, 'torso': {'rot': [0, 0, 0]}}),
        (3.0, {'core': {'scale': 1.12}, 'torso': {'rot': [-1.5, 0, 0]}}),
        (4.0, {'core': {'scale': 1}, 'torso': {'rot': [0, 0, 0]}}),
    ])
    L = 2.6
    r.clip('walk', L, True).poses([
        (0.0, merge(neutral, {'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-14, 0, 0]}, 'torso': {'rot': [2, -5, 4]},
                              'hips': {'pos': [0, -1, 0]}, 'arm_left': {'rot': [-8, 0, 4]}, 'arm_right': {'rot': [8, 0, -4]}})),
        (L / 4, {'shin_right': {'rot': [-26, 0, 0]}, 'hips': {'pos': [0, 0.8, 0]}}),
        (L / 2, {'leg_left': {'rot': [-14, 0, 0]}, 'leg_right': {'rot': [14, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}, 'torso': {'rot': [2, 5, -4]},
                 'hips': {'pos': [0, -1, 0]}, 'arm_left': {'rot': [8, 0, 4]}, 'arm_right': {'rot': [-8, 0, -4]}}),
        (3 * L / 4, {'shin_left': {'rot': [-26, 0, 0]}, 'hips': {'pos': [0, 0.8, 0]}}),
        (L, {'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-14, 0, 0]}, 'shin_left': {'rot': [0, 0, 0]}, 'torso': {'rot': [2, -5, 4]},
             'hips': {'pos': [0, -1, 0]}, 'arm_left': {'rot': [-8, 0, 4]}, 'arm_right': {'rot': [8, 0, -4]}}),
    ])
    # double-fist crush: impact at 1.5 (fists stay buried to 2.1)
    r.clip('crush', 2.8).poses([
        (0.0, neutral),
        (1.3, {'arm_left': {'rot': [170, 0, -14]}, 'arm_right': {'rot': [170, 0, 14]}, 'piston_left': {'rot': [0, 0, 0]},
               'piston_right': {'rot': [0, 0, 0]}, 'torso': {'rot': [-14, 0, 0]}, 'core': {'scale': 1.3}}, 'easeOutCubic'),
        (1.5, {'arm_left': {'rot': [46, 0, -8]}, 'arm_right': {'rot': [46, 0, 8]}, 'torso': {'rot': [30, 0, 0]}, 'hips': {'pos': [0, -4, -1]},
               'leg_left': {'rot': [22, 0, 0]}, 'shin_left': {'rot': [-24, 0, 0]}, 'core': {'scale': 1}}, 'easeInExpo'),
        (2.1, {'torso': {'rot': [28, 0, 0]}}),
        (2.8, neutral),
    ])
    # sweeping backhand with the right fist: hit at 1.0
    r.clip('sweep', 2.0).poses([
        (0.0, neutral),
        (0.8, {'arm_right': {'rot': [70, 0, -70]}, 'torso': {'rot': [0, 36, 0]}}, 'easeOutCubic'),
        (1.0, {'arm_right': {'rot': [70, 0, 50]}, 'torso': {'rot': [4, -36, 0]}}, 'easeInExpo'),
        (2.0, neutral),
    ])
    # molten volley: the door swings open, the core flares, globs launched at 1.2/1.4/1.6
    r.clip('volley', 2.4).poses([
        (0.0, neutral),
        (0.8, {'door': {'rot': [0, -110, 0]}, 'torso': {'rot': [-10, 0, 0]}, 'arm_left': {'rot': [20, 0, 40]},
               'arm_right': {'rot': [20, 0, -40]}, 'core': {'scale': 1.2}}, 'easeOutBack'),
        (1.2, {'core': {'scale': 1.5}, 'torso': {'rot': [4, 0, 0]}}, 'easeOutExpo'),
        (1.4, {'core': {'scale': 1.2}, 'torso': {'rot': [-4, 0, 0]}}),
        (1.6, {'core': {'scale': 1.5}, 'torso': {'rot': [4, 0, 0]}}),
        (2.4, neutral),
    ])
    # vent: steam ring at 0.9 (bracing stance, chimneys flare)
    r.clip('vent', 2.0).poses([
        (0.0, merge(neutral, {'chimney_left': {'scale': 1}, 'chimney_right': {'scale': 1}})),
        (0.7, {'hips': {'pos': [0, -3, 0]}, 'leg_left': {'rot': [16, 0, 10]}, 'leg_right': {'rot': [16, 0, -10]},
               'shin_left': {'rot': [-20, 0, 0]}, 'shin_right': {'rot': [-20, 0, 0]}, 'arm_left': {'rot': [10, 0, 40]},
               'arm_right': {'rot': [10, 0, -40]}, 'chimney_left': {'scale': 1}, 'chimney_right': {'scale': 1}}, 'easeOutCubic'),
        (0.9, {'chimney_left': {'scale': [1.2, 1.3, 1.2]}, 'chimney_right': {'scale': [1.2, 1.3, 1.2]}, 'torso': {'rot': [-8, 0, 0]}}, 'easeOutExpo'),
        (2.0, merge(neutral, {'chimney_left': {'scale': 1}, 'chimney_right': {'scale': 1}})),
    ])
    # overheat: slumps forward, door hangs open, core exposed (vulnerable) — loops while overheated
    slump = {'torso': {'rot': [26, 0, 0]}, 'head': {'rot': [20, 0, 0]}, 'hips': {'pos': [0, -5, 0]}, 'door': {'rot': [0, -120, 0]},
             'arm_left': {'rot': [20, 0, 10]}, 'arm_right': {'rot': [20, 0, -10]}, 'leg_left': {'rot': [24, 0, 0]},
             'leg_right': {'rot': [24, 0, 0]}, 'shin_left': {'rot': [-30, 0, 0]}, 'shin_right': {'rot': [-30, 0, 0]}}
    r.clip('overheat', 1.4).poses([(0.0, neutral), (1.4, merge(slump, {'core': {'scale': 1.4}}), 'easeOutBounce')])
    r.clip('overheated', 2.0, True).poses([
        (0.0, merge(slump, {'core': {'scale': 1.4}})), (1.0, {'core': {'scale': 1.6}, 'torso': {'rot': [24, 0, 2]}}),
        (2.0, {'core': {'scale': 1.4}, 'torso': {'rot': [26, 0, 0]}}),
    ])
    r.clip('recover', 1.6).poses([(0.0, merge(slump, {'core': {'scale': 1.4}})), (1.6, neutral, 'easeInOutCubic')])
    # enrage: plates gone, it runs hot permanently
    r.clip('enrage', 3.0).poses([
        (0.0, merge(neutral, {'core': {'scale': 1}})),
        (1.0, {'torso': {'rot': [-20, 0, 0]}, 'head': {'rot': [-24, 0, 0]}, 'arm_left': {'rot': [60, 0, 70]},
               'arm_right': {'rot': [60, 0, -70]}, 'core': {'scale': 1.8}}, 'easeOutCubic'),
        (1.6, {'torso': {'rot': [20, 0, 0]}, 'arm_left': {'rot': [30, 0, 10]}, 'arm_right': {'rot': [30, 0, -10]},
               'hips': {'pos': [0, -3, 0]}}, 'easeInExpo'),
        (3.0, merge(neutral, {'core': {'scale': 1.3}})),
    ])
    r.clip('stagger', 1.2).poses([
        (0.0, neutral),
        (0.3, {'torso': {'rot': [-14, 10, 6]}, 'hips': {'pos': [0, -1, 2]}, 'arm_left': {'rot': [20, 0, 30]}, 'arm_right': {'rot': [0, 0, -30]}}, 'easeOutExpo'),
        (1.2, neutral),
    ])
    r.clip('death', 5.0, 'hold').poses([
        (0.0, merge(neutral, {'core': {'scale': 1}})),
        (1.0, {'core': {'scale': 2.0}, 'door': {'rot': [0, -130, 0]}, 'torso': {'rot': [-14, 0, 0]}}, 'easeOutCubic'),
        (2.0, {'core': {'scale': 0.6}}),
        (3.4, {'hips': {'pos': [0, -12, 0]}, 'leg_left': {'rot': [80, 0, -8]}, 'shin_left': {'rot': [-84, 0, 0]},
               'leg_right': {'rot': [76, 0, 10]}, 'shin_right': {'rot': [-82, 0, 0]}, 'torso': {'rot': [34, 0, 6]},
               'arm_left': {'rot': [10, 0, 20]}, 'arm_right': {'rot': [10, 0, -20]}, 'core': {'scale': 0.2}}, 'easeInCubic'),
        (5.0, {'core': {'scale': 0}, 'torso': {'rot': [40, 0, 6]}, 'head': {'rot': [30, 0, 0]}}, 'easeOutBounce'),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 1.0), ('walk', 0.65), ('crush', 1.3), ('crush', 1.5), ('sweep', 1.0), ('volley', 1.2), ('vent', 0.9),
             ('overheat', 1.4), ('enrage', 1.0), ('death', 5.0)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
