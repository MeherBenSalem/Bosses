"""The Gravebound Warden — Keeper of the Forgotten Dead (Ossuary Covenant).

A hunched jailer-executioner in rusted plate, chains wound round its torso, a hooded iron mask with
one slit, a shackle for a left hand and a slab-like headsman's blade chained to the right wrist. On
its back it carries a prison cell — the reliquary where the Covenant locked the souls the Oracle
dropped. Breaking the cell open (phase 2, death) frees them.
Editor conventions as in hollow_sovereign.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paint import Material  # noqa: E402
from rig import Rig  # noqa: E402

NAME = 'gravebound_warden'
PALETTE = {
    'grave_cloth': Material(['#0E0F0C', '#161813', '#20231B', '#2B2F24', '#383D2F', '#474D3B'], 'cloth', torn=True),
    # rusted black iron: dark, desaturated body with rust blooming in the weathering accent
    'rust': Material(['#110C0A', '#1B1411', '#271C17', '#35261E', '#463227', '#5A4031', '#73533F'], 'plate', accent='rust_bloom', wear=1.0),
    'rust_bloom': Material(['#2E1408', '#4E220E', '#743416', '#97481F', '#B8612C'], 'flat'),
}
RIM = ('band', {'row': 0, 'size': 1, 'mat': 'iron_dark'})
HEM = ('band', {'row': -1, 'size': 1, 'mat': 'iron_dark'})
TALLY = ['#.#.#.#', '#.#.#.#', '#.#.#.#', '#######']
SKULL = ['.###.', '#+#+#', '.###.', '.#.#.']


def chain_strip(r, bone, name, start, end, link=1.4, mat='chain'):
    """A chain of alternating links between two points (straight run)."""
    sx, sy, sz = start
    ex, ey, ez = end
    n = max(2, int(((ex - sx) ** 2 + (ey - sy) ** 2 + (ez - sz) ** 2) ** 0.5 / link))
    for i in range(n):
        t = (i + 0.5) / n
        c = (sx + (ex - sx) * t, sy + (ey - sy) * t, sz + (ez - sz) * t)
        size = (1.0, link * 1.1, 0.5) if i % 2 == 0 else (0.5, link * 1.1, 1.0)
        dx, dy = ex - sx, ey - sy
        import math
        angle = math.degrees(math.atan2(dx, dy)) if abs(dx) + abs(dy) > 0.01 else 0
        r.box(bone, f'{name}_{i}', c, size, mat, rot=(0, 0, -angle))


def build():
    r = Rig(NAME, (256, 256), palette=PALETTE)
    r.bone('hips', (0, 30, 2))
    r.bone_pair('leg_{side}', (6.5, 29, 2), 'hips')
    r.bone_pair('shin_{side}', (7, 17, 2), 'leg_{side}')
    r.bone('torso', (0, 33, 2), 'hips', rotation=(14, 0, 0))
    r.bone('head', (0, 54, -4), 'torso')
    r.bone('hood', (0, 62, 0), 'head')
    r.bone('cage', (0, 46, 12), 'torso')
    r.bone('cage_door', (-6.5, 46, 16), 'cage')
    r.bone('souls', (0, 51, 12), 'cage')
    r.bone('arm_left', (15, 51, 1), 'torso')
    r.bone('forearm_left', (17, 37, 0), 'arm_left')
    r.bone('shackle', (17.5, 24, -0.5), 'forearm_left')
    r.bone('shackle_chain', (17.5, 21, -0.5), 'shackle')
    r.bone('arm_right', (-15, 51, 1), 'torso')
    r.bone('forearm_right', (-17, 37, 0), 'arm_right')
    r.bone('hand_right', (-17.5, 24.5, -0.5), 'forearm_right')
    r.bone('blade', (-17.5, 22.5, -0.5), 'hand_right')
    r.bone('chains', (0, 40, 2), 'torso')

    # legs: rusted greaves, masonry knees
    r.box('hips', 'pelvis', (0, 30, 2), (17, 6, 12), 'rust', decals=[HEM])
    r.box('hips', 'loincloth_front', (0, 22.5, -4.5), (8, 12, 0.8), 'grave_cloth', decals=[('glyph', {'bitmap': SKULL, 'mat': 'bone', 'faces': 'north', 'dy': -2})])
    r.box('hips', 'loincloth_back', (0, 23, 8.4), (10, 11, 0.8), 'grave_cloth')
    chain_strip(r, 'hips', 'hip_chain_l', (5.8, 27.5, -4.2), (7.8, 19, -4.4))
    chain_strip(r, 'hips', 'hip_chain_r', (-6.2, 27.5, -4.2), (-8.6, 21, -4.4))
    r.pair('leg_{side}', '{side}_thigh', (7, 22.5, 2), (8.4, 12, 8.6), 'rust', decals=[RIM])
    r.pair('shin_{side}', '{side}_knee_stone', (7, 17, -2.6), (7.4, 6, 4), 'stone',
           decals=[('band', {'row': 2, 'size': 1, 'mat': 'iron_dark'})])
    r.pair('shin_{side}', '{side}_greave', (7, 9.5, 2.2), (8.6, 12, 8.6), 'rust',
           decals=[('vband', {'mat': 'iron_dark', 'faces': 'north', 'size': 1})])
    r.pair('shin_{side}', '{side}_boot', (7, 2.2, 0.6), (9.6, 4.4, 13), 'iron_dark', decals=[('rivets', {'mat': 'rust', 'spacing': 3})])
    r.pair('shin_{side}', '{side}_boot_spike', (7, 2.2, -6.6), (2, 2, 2), 'iron')

    # hunched torso wrapped in chains
    r.box('torso', 'belly', (0, 38, 2), (17, 10, 13), 'rust',
          decals=[('band', {'row': 3, 'size': 1, 'mat': 'iron_dark'}), ('glyph', {'bitmap': TALLY, 'mat': 'bone', 'faces': 'south'})])
    r.box('torso', 'chest', (0, 48.5, 2), (25, 13, 15), 'rust', decals=[RIM, ('rivets', {'mat': 'iron', 'spacing': 4})])
    r.box('torso', 'chest_plate', (0, 49, -5.8), (15, 10, 1.2), 'iron_dark',
          decals=[('rim', {'mat': 'rust', 'faces': 'north'}), ('glyph', {'bitmap': SKULL, 'mat': 'bone', 'faces': 'north'})])
    r.box('torso', 'gorget', (0, 55.6, 0), (14, 3, 12), 'iron_dark')
    chain_strip(r, 'chains', 'wrap_a', (-12.6, 53, -6.2), (12.6, 41, -6.2))
    chain_strip(r, 'chains', 'wrap_b', (12.6, 53, -6.4), (-12.6, 41, -6.4))
    chain_strip(r, 'chains', 'wrap_c', (-13, 43, -0.8), (-13, 43, 9), link=1.6)
    chain_strip(r, 'chains', 'wrap_d', (13, 43, -0.8), (13, 43, 9), link=1.6)
    r.box('chains', 'padlock', (0, 47, -7.4), (3, 3.4, 1.4), 'iron', decals=[('glyph', {'bitmap': ['.#.', '#.#', '###'], 'mat': 'soul_dim', 'faces': 'north'})])

    # the cell on its back
    r.box('cage', 'cell_roof', (0, 56.5, 12), (15, 2, 11), 'stone', decals=[HEM])
    r.box('cage', 'cell_floor', (0, 40.5, 12), (15, 2, 11), 'stone')
    for i, (x, z) in enumerate([(-6.5, 7), (6.5, 7), (6.5, 17), (-2, 17), (2.2, 17), (-6.5, 12), (6.5, 12), (0, 7)]):
        r.box('cage', f'bar_{i}', (x, 48.5, z), (1, 14, 1), 'iron_dark')
    r.box('cage', 'bar_beam', (0, 52, 17), (14, 1, 1), 'iron_dark')
    r.box('cage_door', 'door_bar_a', (-4.6, 48.5, 17), (1, 14, 1), 'iron_dark')
    r.box('cage_door', 'door_beam', (-5.6, 45, 17), (2.6, 1, 1), 'iron_dark')
    r.box('cage', 'skull', (2.4, 43, 13), (3.4, 3, 3.2), 'bone', rot=(0, 20, 10),
          decals=[('glyph', {'bitmap': ['#.#', '...'], 'mat': 'void', 'faces': 'north'})])
    r.box('souls', 'soul_a', (-1.6, 50, 11.4), (3, 4.4, 3), 'soul')
    r.box('souls', 'soul_b', (2.2, 47.4, 12.6), (2.2, 3.2, 2.2), 'soul_dim')
    r.box('souls', 'soul_c', (-0.4, 53.4, 13.4), (1.8, 2.2, 1.8), 'soul')

    # executioner's hood and mask
    r.box('head', 'mask', (0, 57.5, -5), (10, 10, 10), 'rust', decals=[('rivets', {'mat': 'iron', 'spacing': 3})])
    r.box('head', 'slit', (0, 58.6, -10.1), (7, 1.2, 0.4), 'void')
    r.pair('head', 'eye_{side}', (2, 58.6, -10.35), (1.6, 0.8, 0.3), 'soul_dim')
    r.box('head', 'mask_nose', (0, 56, -10.4), (1.4, 3, 1), 'rust')
    r.box('head', 'jaw_grate', (0, 53.6, -10.1), (6, 2.4, 0.6), 'iron_dark',
          decals=[('glyph', {'bitmap': ['#.#.#.', '#.#.#.'], 'mat': 'void', 'faces': 'north'})])
    # Executioner's cowl: a tall pointed hood drawn over the iron mask, folding back at the tip
    r.box('hood', 'cowl_base', (0, 62.8, -4.4), (12, 4, 12), 'grave_cloth')
    r.box('hood', 'cowl_mid', (0, 66.4, -3.4), (9, 4, 9.4), 'grave_cloth')
    r.box('hood', 'cowl_peak', (0, 69.6, -2.0), (5.6, 3.6, 6), 'grave_cloth', rot=(-16, 0, 0))
    r.box('hood', 'cowl_tip', (0, 71.4, 1.2), (3, 2.4, 4), 'grave_cloth', rot=(-40, 0, 0))
    r.box('hood', 'hood_back', (0, 57, 1.6), (12.4, 12, 2), 'grave_cloth')
    r.pair('hood', 'hood_{side}', (6.2, 58.5, -4.5), (1.2, 9, 11), 'grave_cloth')
    r.box('hood', 'hood_mantle', (0, 54.2, -1), (16, 3, 14), 'grave_cloth')

    # left arm: the shackle
    r.box('arm_left', 'l_pauldron_stone', (16.5, 53, 1), (11, 7, 12), 'stone', rot=(0, 0, -12),
          decals=[('band', {'row': 3, 'size': 1, 'mat': 'iron_dark'}), ('glyph', {'bitmap': TALLY, 'mat': 'bone', 'faces': 'north'})])
    r.box('arm_left', 'l_upper', (17, 44, 0.5), (7.6, 13, 7.6), 'rust')
    r.box('forearm_left', 'l_vambrace', (17.5, 31, 0), (8.6, 12, 8.6), 'rust', decals=[RIM, HEM])
    r.box('shackle', 'manacle', (17.5, 22.5, -0.5), (11, 4.4, 11), 'iron_dark', decals=[('rivets', {'mat': 'iron', 'spacing': 3})])
    r.box('shackle', 'manacle_lock', (17.5, 22.5, -6.6), (3, 3.4, 1.4), 'iron')
    chain_strip(r, 'shackle_chain', 'dangle', (17.5, 20.4, -0.5), (16, 4, -1), link=1.6)
    r.box('shackle_chain', 'broken_link', (16, 3, -1), (1.8, 2, 0.6), 'rust')

    # right arm: the headsman's blade
    r.box('arm_right', 'r_pauldron', (-16.5, 53.5, 1), (11, 6, 12), 'rust', rot=(0, 0, 12), decals=[RIM, HEM])
    r.box('arm_right', 'r_upper', (-17, 44, 0.5), (7.6, 13, 7.6), 'rust')
    r.box('forearm_right', 'r_vambrace', (-17.5, 31, 0), (8.6, 12, 8.6), 'rust', decals=[RIM, HEM])
    r.box('hand_right', 'r_gauntlet', (-17.5, 23, -0.5), (7, 6, 8), 'iron_dark')
    r.box('blade', 'grip', (-17.5, 22.5, -0.5), (2, 10, 2), 'leather')
    r.box('blade', 'pommel_ring', (-17.5, 28.4, -0.5), (1, 3, 3), 'iron_dark')
    r.box('blade', 'guard', (-17.5, 17, -0.5), (3, 2, 9), 'iron_dark')
    r.box('blade', 'slab', (-17.5, 2.5, -0.5), (1.6, 27, 9), 'rust',
          decals=[('band', {'row': 1, 'size': 1, 'mat': 'iron_dark'}), ('glyph', {'bitmap': ['#', '.', '#', '.', '#', '.', '#'], 'mat': 'soul_dim', 'faces': ('east', 'west')})])
    r.box('blade', 'edge', (-17.5, 2.5, -5.3), (1.0, 27, 0.8), 'blade')
    r.box('blade', 'blunt_tip', (-17.5, -11.5, -0.5), (1.6, 1.4, 9.8), 'rust')
    r.box('blade', 'hole_rim', (-17.5, -7, 1.6), (1.8, 3, 3), 'iron_dark')
    chain_strip(r, 'blade', 'wrist_chain', (-17.5, 28.8, 1), (-17.5, 33, 4.6), link=1.3)

    r.mark_upright('torso', 'head', 'hood')
    clips(r)
    return r


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    rest = {'arm_left': {'rot': [6, 0, 6]}, 'forearm_left': {'rot': [12, 0, 0]},
            'arm_right': {'rot': [10, 0, -6]}, 'forearm_right': {'rot': [18, 0, 0]}, 'blade': {'rot': [26, 0, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]},
                           'shackle': {'pos': [0, 0, 0]}, 'souls': {'scale': 1}, 'cage_door': {'rot': [0, 0, 0]}})
    r.clip('idle', 4.0, True).poses([
        (0.0, merge(rest, {'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'souls': {'scale': 1, 'rot': [0, 0, 0]},
                           'shackle_chain': {'rot': [0, 0, 0]}, 'hood': {'rot': [0, 0, 0]}})),
        (2.0, {'torso': {'rot': [4, 0, 0]}, 'head': {'rot': [-4, 6, 0]}, 'souls': {'scale': 1.15, 'rot': [0, 90, 0]},
               'shackle_chain': {'rot': [4, 0, 3]}, 'hood': {'rot': [-3, 0, 0]}}),
        (4.0, {'torso': {'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'souls': {'scale': 1, 'rot': [0, 180, 0]},
               'shackle_chain': {'rot': [0, 0, 0]}, 'hood': {'rot': [0, 0, 0]}}),
    ])
    # idle soul rotation must loop: 180 -> treat as symmetric pose; reset seam
    r.clips['idle'].channels['souls']['rotation'][-1] = (4.0, [0.0, 0.0, 0.0], 'easeInOutSine')

    h, q, L = 1.0, 0.5, 2.0
    def stride(t, s):
        return {'leg_left': {'rot': [16 * s, 0, 0]}, 'leg_right': {'rot': [-16 * s, 0, 0]},
                'arm_left': {'rot': [6 - 10 * s, 0, 6]}, 'torso': {'rot': [0, -5 * s, 2 * s]},
                'hips': {'pos': [0, -1, 0]}, 'shackle_chain': {'rot': [10 * s, 0, 0]}}
    r.clip('walk', L, True).poses([
        (0.0, merge(rest, stride(0, 1), {'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [-6, 0, 0]}})),
        (q, {'shin_right': {'rot': [-34, 0, 0]}, 'hips': {'pos': [0, 0.8, 0]}}),
        (h, merge(stride(h, -1), {'shin_left': {'rot': [-6, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}})),
        (h + q, {'shin_left': {'rot': [-34, 0, 0]}, 'hips': {'pos': [0, 0.8, 0]}}),
        (L, merge(stride(L, 1), {'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [-6, 0, 0]}})),
    ])
    # Executioner's sweep: wide horizontal arc, hit at t=1.25
    r.clip('sweep', 2.4).poses([
        (0.0, neutral),
        (1.0, {'arm_right': {'rot': [80, 0, -70]}, 'forearm_right': {'rot': [10, 0, 0]}, 'blade': {'rot': [0, 0, -80]},
               'torso': {'rot': [0, 40, 0]}, 'leg_right': {'rot': [-14, 0, 0]}, 'leg_left': {'rot': [14, 0, 0]}}, 'easeOutCubic'),
        (1.25, {'arm_right': {'rot': [80, 0, 50]}, 'blade': {'rot': [0, 0, 70]}, 'torso': {'rot': [6, -42, 0]}}, 'easeInExpo'),
        (1.7, {'torso': {'rot': [6, -38, 0]}}),
        (2.4, neutral),
    ])
    # Execution chop: overhead, ground impact at t=1.4 (wedged until ~1.9)
    r.clip('chop', 2.6).poses([
        (0.0, neutral),
        (1.15, {'arm_right': {'rot': [170, 0, -8]}, 'arm_left': {'rot': [150, 0, -20]}, 'forearm_right': {'rot': [10, 0, 0]},
                'blade': {'rot': [-20, 0, 0]}, 'torso': {'rot': [-18, 0, 0]}, 'head': {'rot': [-14, 0, 0]}}, 'easeOutCubic'),
        (1.4, {'arm_right': {'rot': [48, 0, -4]}, 'arm_left': {'rot': [40, 0, -16]}, 'blade': {'rot': [50, 0, 0]},
               'torso': {'rot': [30, 0, 0]}, 'hips': {'pos': [0, -3, -1]}, 'head': {'rot': [10, 0, 0]},
               'leg_left': {'rot': [26, 0, 0]}, 'shin_left': {'rot': [-24, 0, 0]}}, 'easeInExpo'),
        (1.9, {'torso': {'rot': [28, 0, 0]}}),
        (2.6, neutral),
    ])
    # Chain grab: the shackle is hurled forward on its chain (t=1.0 out, 1.4 latched, pulls back)
    r.clip('chain_grab', 2.2).poses([
        (0.0, neutral),
        (0.7, {'arm_left': {'rot': [-30, 0, 20]}, 'torso': {'rot': [0, -20, 0]}}, 'easeOutCubic'),
        (1.0, {'arm_left': {'rot': [90, 0, -10]}, 'forearm_left': {'rot': [0, 0, 0]}, 'torso': {'rot': [8, 18, 0]},
               'shackle': {'pos': [0, 0, -0]}}, 'easeInExpo'),
        (1.1, {'shackle': {'pos': [0, -22, 0]}}, 'easeOutCubic'),
        (1.5, {'shackle': {'pos': [0, -22, 0]}, 'arm_left': {'rot': [80, 0, -10]}}),
        (1.9, {'shackle': {'pos': [0, 0, 0]}, 'arm_left': {'rot': [20, 0, 10]}, 'torso': {'rot': [-6, -10, 0]}}, 'easeInCubic'),
        (2.2, neutral),
    ])
    # Grave eruption: shackle fist slams the ground (t=1.2), rows of graves erupt after
    r.clip('eruption', 2.8).poses([
        (0.0, neutral),
        (1.0, {'arm_left': {'rot': [160, 0, -10]}, 'torso': {'rot': [-14, -10, 0]}, 'head': {'rot': [-10, 0, 0]}}, 'easeOutCubic'),
        (1.2, {'arm_left': {'rot': [40, 0, -6]}, 'forearm_left': {'rot': [30, 0, 0]}, 'torso': {'rot': [36, 10, 0]},
               'hips': {'pos': [0, -4, 0]}, 'leg_left': {'rot': [30, 0, 0]}, 'shin_left': {'rot': [-34, 0, 0]},
               'leg_right': {'rot': [-10, 0, 0]}, 'shin_right': {'rot': [-20, 0, 0]}}, 'easeInExpo'),
        (2.0, {'torso': {'rot': [32, 8, 0]}}),
        (2.8, neutral),
    ])
    # Summon: the cell door swings open and the soldiers' souls pour out (t=1.3)
    r.clip('summon', 2.5).poses([
        (0.0, neutral),
        (0.8, {'torso': {'rot': [24, 0, 0]}, 'head': {'rot': [20, 0, 0]}, 'arm_left': {'rot': [20, 0, 30]},
               'arm_right': {'rot': [20, 0, -30]}}, 'easeOutCubic'),
        (1.3, {'cage_door': {'rot': [0, -110, 0]}, 'souls': {'scale': 1.8}}, 'easeOutBack'),
        (1.9, {'souls': {'scale': 0.6}, 'torso': {'rot': [-10, 0, 0]}, 'head': {'rot': [-20, 0, 0]}}),
        (2.5, neutral),
    ])
    # Chain wall: arms flung wide, chains erupt around the arena (t=1.5)
    r.clip('chain_wall', 3.0).poses([
        (0.0, neutral),
        (1.0, {'arm_left': {'rot': [30, 0, 80]}, 'arm_right': {'rot': [30, 0, -80]}, 'torso': {'rot': [-20, 0, 0]},
               'head': {'rot': [-30, 0, 0]}, 'chains': {'scale': 1.08}}, 'easeOutCubic'),
        (1.5, {'arm_left': {'rot': [10, 0, 40]}, 'arm_right': {'rot': [10, 0, -40]}, 'torso': {'rot': [22, 0, 0]},
               'hips': {'pos': [0, -2, 0]}, 'chains': {'scale': 1}}, 'easeInExpo'),
        (3.0, neutral),
    ])
    # Phase 2: the cell breaks; trapped souls swirl free and feed its rage
    r.clip('cell_break', 3.0).poses([
        (0.0, merge(neutral, {'cage': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}})),
        (0.6, {'torso': {'rot': [26, 0, 0]}, 'head': {'rot': [26, 0, 0]}, 'hips': {'pos': [0, -3, 0]}}, 'easeOutCubic'),
        (1.2, {'torso': {'rot': [-24, 0, 0]}, 'head': {'rot': [-34, 0, 0]}, 'arm_left': {'rot': [40, 0, 70]},
               'arm_right': {'rot': [40, 0, -70]}, 'cage_door': {'rot': [0, -150, 0]}, 'souls': {'scale': 2.6},
               'cage': {'pos': [0, 2, 2], 'rot': [-10, 0, 0]}}, 'easeOutExpo'),
        (2.0, {'souls': {'scale': 0.2}}),
        (3.0, merge(neutral, {'cage': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'cage_door': {'rot': [0, -150, 0]}, 'souls': {'scale': 0.2}})),
    ])
    r.clip('stagger', 1.2).poses([
        (0.0, neutral),
        (0.25, {'torso': {'rot': [-16, 8, 6]}, 'head': {'rot': [-16, 0, 0]}, 'hips': {'pos': [0, -1, 2]},
                'arm_left': {'rot': [20, 0, 30]}, 'arm_right': {'rot': [0, 0, -30]}}, 'easeOutExpo'),
        (1.2, neutral),
    ])
    r.clip('death', 5.0, 'hold').poses([
        (0.0, merge(neutral, {'cage_door': {'rot': [0, 0, 0]}})),
        (0.8, {'torso': {'rot': [-14, 0, 6]}, 'head': {'rot': [-20, 0, 0]}}, 'easeOutCubic'),
        (2.0, {'hips': {'pos': [0, -11, 0]}, 'leg_left': {'rot': [82, 0, -6]}, 'shin_left': {'rot': [-86, 0, 0]},
               'leg_right': {'rot': [78, 0, 8]}, 'shin_right': {'rot': [-84, 0, 0]}, 'torso': {'rot': [30, 0, 0]},
               'arm_right': {'rot': [16, 0, -20]}, 'blade': {'rot': [60, 0, -40]}, 'arm_left': {'rot': [10, 0, 16]}}, 'easeInCubic'),
        (2.8, {'cage_door': {'rot': [0, -150, 0]}, 'souls': {'scale': 2.2}}, 'easeOutBack'),
        (4.0, {'souls': {'scale': 0}, 'torso': {'rot': [56, 0, 0]}, 'head': {'rot': [24, 0, 10]}}, 'easeInCubic'),
        (5.0, {'souls': {'scale': 0}}),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 0.0), ('walk', 0.5), ('sweep', 1.0), ('sweep', 1.25), ('chop', 1.15), ('chop', 1.4),
             ('chain_grab', 1.2), ('eruption', 1.2), ('summon', 1.3), ('chain_wall', 1.0), ('cell_break', 1.2), ('death', 5.0)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
