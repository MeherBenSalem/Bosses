"""The Cryptborn: what crawls, marches and stands guard in the Ossuary Covenant's catacombs.

Shared language with the Gravebound Warden: rusted black iron, old bone, grave cloth, tomb stone,
and the cold remnant light of souls the Covenant locked away.
Editor conventions as in hollow_sovereign.py; torso/head-like bones are marked upright.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402
from gravebound_warden import PALETTE as WARDEN_PALETTE  # noqa: E402

RIM = ('band', {'row': 0, 'size': 1, 'mat': 'iron_dark'})
HEM = ('band', {'row': -1, 'size': 1, 'mat': 'iron_dark'})
SKULL = ['.###.', '#+#+#', '.###.', '.#.#.']


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def catacomb_rat():
    r = Rig('catacomb_rat', (64, 32), palette=WARDEN_PALETTE)
    r.bone('body', (0, 4, 0))
    r.bone('head', (0, 4.6, -3.6), 'body')
    r.bone('tail', (0, 4.4, 4.4), 'body')
    r.bone('tail_tip', (0, 4.2, 9.4), 'tail')
    r.bone('candle', (0, 6, 1), 'body')
    for name, (x, z) in {'leg_fl': (1.8, -2.2), 'leg_fr': (-1.8, -2.2), 'leg_bl': (1.9, 2.8), 'leg_br': (-1.9, 2.8)}.items():
        r.bone(name, (x, 2.6, z), 'body')
        r.box(name, f'{name}_paw', (x, 1.3, z - 0.2), (1.2, 2.6, 1.6), 'fur')
    r.box('body', 'torso', (0, 4, 0.6), (5, 4, 8.6), 'fur')
    r.box('body', 'haunch', (0, 4.4, 3.6), (5.6, 3.6, 3), 'fur')
    r.box('body', 'spine_bones', (0, 6.1, 0.4), (1, 0.4, 6), 'bone')
    r.box('head', 'skull', (0, 4.5, -5.6), (4, 3.4, 4), 'fur')
    r.box('head', 'snout', (0, 3.9, -8.4), (2.4, 2, 2.2), 'fur')
    r.box('head', 'nose', (0, 4.2, -9.6), (1, 0.8, 0.4), 'void')
    r.box('head', 'teeth', (0, 2.8, -9.2), (1, 0.8, 0.4), 'bone')
    r.pair('head', 'ear_{side}', (1.5, 6.8, -5.2), (1.6, 1.8, 0.5), 'fur', rot=(0, 0, -16))
    r.pair('head', 'eye_{side}', (1.3, 5.2, -7.3), (0.6, 0.6, 0.3), 'soul')
    r.box('tail', 'tail_base', (0, 4.2, 6.9), (1, 1, 5), 'wax')
    r.box('tail_tip', 'tail_end', (0, 4.0, 11.9), (0.6, 0.6, 5), 'wax')
    r.box('candle', 'stub', (0, 7.4, 1), (1.6, 2.8, 1.6), 'wax')
    r.box('candle', 'wick', (0, 9.1, 1), (0.3, 0.6, 0.3), 'void')
    r.box('candle', 'flame', (0, 10, 1), (0.8, 1.4, 0.8), 'soul')
    r.box('candle', 'drip_a', (0.9, 6.2, 1.2), (0.4, 1.2, 0.4), 'wax')
    r.box('candle', 'drip_b', (-0.9, 6.6, 0.6), (0.4, 0.8, 0.4), 'wax')
    r.mark_upright('candle')
    legs = ['leg_fl', 'leg_fr', 'leg_bl', 'leg_br']
    r.clip('idle', 2.0, True).poses([
        (0.0, {'head': {'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}, 'tail_tip': {'rot': [0, 0, 0]}, 'candle': {'rot': [0, 0, 0]}}),
        (0.5, {'head': {'rot': [-8, 10, 0]}, 'tail': {'rot': [0, 14, 0]}, 'tail_tip': {'rot': [0, 14, 0]}}),
        (1.0, {'head': {'rot': [4, -6, 0]}, 'tail': {'rot': [0, -10, 0]}, 'tail_tip': {'rot': [0, -16, 0]}, 'candle': {'rot': [0, 0, 3]}}),
        (2.0, {'head': {'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}, 'tail_tip': {'rot': [0, 0, 0]}, 'candle': {'rot': [0, 0, 0]}}),
    ])
    r.clip('walk', 0.5, True).poses([
        (0.0, {'leg_fl': {'rot': [30, 0, 0]}, 'leg_br': {'rot': [30, 0, 0]}, 'leg_fr': {'rot': [-30, 0, 0]}, 'leg_bl': {'rot': [-30, 0, 0]},
               'body': {'pos': [0, 0, 0]}, 'tail': {'rot': [0, 10, 0]}}),
        (0.25, {'leg_fl': {'rot': [-30, 0, 0]}, 'leg_br': {'rot': [-30, 0, 0]}, 'leg_fr': {'rot': [30, 0, 0]}, 'leg_bl': {'rot': [30, 0, 0]},
                'body': {'pos': [0, 0.5, 0]}, 'tail': {'rot': [0, -10, 0]}}),
        (0.5, {'leg_fl': {'rot': [30, 0, 0]}, 'leg_br': {'rot': [30, 0, 0]}, 'leg_fr': {'rot': [-30, 0, 0]}, 'leg_bl': {'rot': [-30, 0, 0]},
               'body': {'pos': [0, 0, 0]}, 'tail': {'rot': [0, 10, 0]}}),
    ], 'linear')
    r.clip('attack', 0.6).poses([
        (0.0, {'head': {'rot': [0, 0, 0]}, 'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}}),
        (0.2, {'head': {'rot': [-24, 0, 0]}, 'body': {'pos': [0, 0.4, 1.4], 'rot': [8, 0, 0]}}, 'easeOutCubic'),
        (0.3, {'head': {'rot': [20, 0, 0]}, 'body': {'pos': [0, 0, -2.4], 'rot': [-6, 0, 0]}}, 'easeInExpo'),
        (0.6, {'head': {'rot': [0, 0, 0]}, 'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}}),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'body': {'rot': [0, 0, 0]}}), (0.1, {'body': {'rot': [-12, 0, 8]}}, 'easeOutExpo'), (0.3, {'body': {'rot': [0, 0, 0]}})])
    r.clip('death', 1.0, 'hold').poses([
        (0.0, {'body': {'rot': [0, 0, 0], 'pos': [0, 0, 0]}, 'candle': {'scale': 1}}),
        (0.5, {'body': {'rot': [0, 0, 90], 'pos': [1, -1.5, 0]}, **{l: {'rot': [0, 0, 20]} for l in legs}}, 'easeOutBounce'),
        (1.0, {'candle': {'scale': [1, 0.2, 1]}}),
    ])
    return r


def crypt_lurker():
    r = Rig('crypt_lurker', (128, 64), palette=WARDEN_PALETTE)
    r.bone('body', (0, 7, 0))
    r.bone('head', (0, 7.5, -8), 'body')
    r.bone('jaw', (0, 5.8, -9.4), 'head')
    r.bone('abdomen', (0, 7.4, 4), 'body')
    r.bone('tail', (0, 7, 14), 'abdomen')
    # shrouded, long-muzzled skull with four sunken soul eyes
    r.box('head', 'skull', (0, 8.0, -11.0), (5, 4.4, 5), 'bone')
    r.box('head', 'muzzle', (0, 7.0, -14.6), (3.6, 2.6, 2.6), 'bone')
    r.box('head', 'brow_ridge', (0, 9.6, -13.4), (5.2, 0.8, 0.8), 'bone')
    r.box('head', 'shroud', (0, 10.8, -10.4), (5.8, 1.6, 6.2), 'grave_cloth')
    r.box('head', 'shroud_drape', (0, 8.8, -7.4), (6.4, 4.4, 2.4), 'grave_cloth')
    for i, (x, y) in enumerate([(1.2, 9.0), (-1.2, 9.0), (2.0, 8.3), (-2.0, 8.3)]):
        r.box('head', f'socket_{i}', (x, y, -13.55), (1.0, 0.8, 0.2), 'void')
        r.box('head', f'eye_{i}', (x, y, -13.65), (0.5, 0.4, 0.1), 'soul_dim')
    # ribcage thorax
    r.box('body', 'spine', (0, 8.6, -2), (2, 2, 12), 'bone')
    for i, z in enumerate((-6, -3.4, -0.8, 1.8)):
        r.pair('body', f'rib_{i}_{{side}}', (2.6, 6.9, z), (4.4, 1, 1), 'bone', rot=(0, 0, -38))
    r.box('body', 'chain_wrap', (0, 7.8, -2.2), (5.6, 3.2, 1), 'chain')
    # chained vertebrae abdomen
    for i, z in enumerate((5.5, 9, 12.5)):
        size = 4.4 - i * 0.6
        r.box('abdomen', f'seg_{i}', (0, 7.4 - i * 0.3, z), (size, size * 0.75, 3), 'bone',
              decals=[('band', {'row': 1, 'size': 1, 'mat': 'rust'})])
    r.box('tail', 'stinger', (0, 7.4, 16), (1.4, 1.4, 4), 'rust', rot=(-12, 0, 0))
    r.box('tail', 'stinger_tip', (0, 8.2, 18.4), (0.8, 0.8, 1.6), 'blade')
    # six splayed legs: upper bone out to the side, lower bone down to the ground
    for i, z in enumerate((-6, -2.4, 1.2)):
        for side, s in (('left', 1), ('right', -1)):
            up, lo = f'leg{i}_{side}', f'shin{i}_{side}'
            r.bone(up, (s * 2.6, 7, z), 'body')
            r.bone(lo, (s * 9, 10, z), up)
            r.box(up, f'{up}_femur', (s * 5.8, 8.5, z), (7.4, 1.2, 1.2), 'bone', rot=(0, 0, s * 24))
            r.box(lo, f'{lo}_tibia', (s * 9.2, 5, z), (1, 10, 1), 'bone')
            r.box(lo, f'{lo}_claw', (s * 9.2, 0.6, z - 0.4), (1.4, 1.4, 1.8), 'rust')
    legs_l = [f'leg{i}_left' for i in range(3)]
    legs_r = [f'leg{i}_right' for i in range(3)]
    r.mark_upright('head')

    def gait(phase):
        pose = {}
        for i in range(3):
            a = 22 * (1 if (i % 2 == 0) == phase else -1)
            pose[f'leg{i}_left'] = {'rot': [0, a, 0]}
            pose[f'leg{i}_right'] = {'rot': [0, a, 0]}
            lift = -16 if (i % 2 == 0) == phase else 0
            pose[f'shin{i}_left'] = {'rot': [0, 0, lift]}
            pose[f'shin{i}_right'] = {'rot': [0, 0, -lift]}
        return pose
    r.clip('idle', 2.4, True).poses([
        (0.0, {'body': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}}),
        (1.2, {'body': {'pos': [0, -0.6, 0]}, 'head': {'rot': [-6, 8, 0]}, 'jaw': {'rot': [14, 0, 0]}, 'tail': {'rot': [10, 8, 0]}}),
        (2.4, {'body': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}}),
    ])
    r.clip('walk', 0.6, True).poses([
        (0.0, merge(gait(True), {'body': {'pos': [0, 0, 0]}, 'abdomen': {'rot': [0, 6, 0]}})),
        (0.3, merge(gait(False), {'body': {'pos': [0, 0.6, 0]}, 'abdomen': {'rot': [0, -6, 0]}})),
        (0.6, merge(gait(True), {'body': {'pos': [0, 0, 0]}, 'abdomen': {'rot': [0, 6, 0]}})),
    ], 'linear')
    # Pounce: crouch (windup to 0.6), leap 0.6-0.9 (hit window 0.85-1.25), land
    crouch = {'body': {'pos': [0, -2.4, 1.6], 'rot': [8, 0, 0]}, 'head': {'rot': [-14, 0, 0]}, 'jaw': {'rot': [30, 0, 0]},
              'tail': {'rot': [30, 0, 0]}}
    r.clip('pounce', 1.6).poses([
        (0.0, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}}),
        (0.6, crouch, 'easeOutCubic'),
        (0.85, {'body': {'pos': [0, 3, -4], 'rot': [-14, 0, 0]}, 'jaw': {'rot': [36, 0, 0]}, 'tail': {'rot': [-20, 0, 0]},
                **{l: {'rot': [0, -30 if 'left' in l else 30, 0]} for l in legs_l + legs_r}}, 'easeOutExpo'),
        (1.25, {'body': {'pos': [0, 0, -1], 'rot': [4, 0, 0]}, 'jaw': {'rot': [0, 0, 0]},
                **{l: {'rot': [0, 0, 0]} for l in legs_l + legs_r}}, 'easeInCubic'),
        (1.6, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]}, 'tail': {'rot': [0, 0, 0]}}),
    ])
    r.clip('bite', 0.8).poses([
        (0.0, {'head': {'rot': [0, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}, 'body': {'pos': [0, 0, 0]}}),
        (0.3, {'head': {'rot': [-18, 0, 0]}, 'jaw': {'rot': [40, 0, 0]}, 'body': {'pos': [0, 0.6, 1]}}, 'easeOutCubic'),
        (0.4, {'head': {'rot': [16, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}, 'body': {'pos': [0, -0.4, -1.6]}}, 'easeInExpo'),
        (0.8, {'head': {'rot': [0, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}, 'body': {'pos': [0, 0, 0]}}),
    ])
    r.clip('emerge', 1.2).poses([
        (0.0, {'root': {'pos': [0, -12, 0], 'rot': [30, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}}),
        (0.9, {'root': {'pos': [0, 0.6, 0], 'rot': [-6, 0, 0]}, 'jaw': {'rot': [34, 0, 0]}}, 'easeOutBack'),
        (1.2, {'root': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'jaw': {'rot': [0, 0, 0]}}),
    ])
    r.clip('hurt', 0.3).poses([(0.0, {'body': {'rot': [0, 0, 0]}}), (0.1, {'body': {'rot': [-10, 8, 0]}}, 'easeOutExpo'), (0.3, {'body': {'rot': [0, 0, 0]}})])
    curl = {l: {'rot': [0, 0, 0]} for l in legs_l + legs_r}
    for i in range(3):
        curl[f'shin{i}_left'] = {'rot': [0, 0, -70]}
        curl[f'shin{i}_right'] = {'rot': [0, 0, 70]}
    r.clip('death', 1.5, 'hold').poses([
        (0.0, {'body': {'rot': [0, 0, 0], 'pos': [0, 0, 0]}, **{k: {'rot': [0, 0, 0]} for k in curl}}),
        (0.6, {'body': {'rot': [0, 0, 170], 'pos': [0, 4, 0]}}, 'easeOutCubic'),
        (1.5, merge(curl, {'body': {'pos': [0, 3, 0]}}), 'easeOutBounce'),
    ])
    return r


def gravebound_soldier():
    r = Rig('gravebound_soldier', (128, 64), palette=WARDEN_PALETTE)
    r.mark_upright('torso', 'head')
    r.bone('hips', (0, 16, 0))
    r.bone_pair('leg_{side}', (2.2, 15.5, 0), 'hips')
    r.bone_pair('shin_{side}', (2.2, 9, 0), 'leg_{side}')
    r.bone('torso', (0, 17.5, 0), 'hips')
    r.bone('head', (0, 27.5, 0), 'torso')
    r.bone_pair('arm_{side}', (5, 26, 0), 'torso')
    r.bone_pair('forearm_{side}', (5.4, 20.6, 0), 'arm_{side}')
    r.bone('polearm', (-5.4, 14.2, -0.4), 'forearm_right')
    r.bone('buckler', (6.4, 17, -1.6), 'forearm_left')
    # bones under rusted half-plate
    r.box('hips', 'pelvis_bone', (0, 16.2, 0), (6, 2, 3.6), 'bone')
    r.box('hips', 'tasset', (0, 14.6, -1.7), (6.4, 4, 0.8), 'rust', decals=[HEM])
    r.box('hips', 'cloth', (0, 12.8, 1.7), (5, 7, 0.6), 'grave_cloth')
    r.pair('leg_{side}', '{side}_femur', (2.2, 12.4, 0), (1.6, 6.4, 1.6), 'bone')
    r.pair('shin_{side}', '{side}_greave', (2.2, 5.4, 0), (2.8, 7, 2.8), 'rust', decals=[RIM])
    r.pair('shin_{side}', '{side}_foot', (2.2, 1, -0.8), (3, 2, 4.6), 'iron_dark')
    r.box('torso', 'spine', (0, 20.4, 0.8), (1.4, 6, 1.4), 'bone')
    for i, y in enumerate((19.4, 21, 22.6)):
        r.box('torso', f'ribs_{i}', (0, y, 0.2), (5.6 - i * 0.2, 0.8, 3.4), 'bone')
    r.box('torso', 'breastplate', (0, 24.4, -0.3), (7.6, 5.6, 4.6), 'rust',
          decals=[RIM, ('glyph', {'bitmap': SKULL, 'mat': 'bone', 'faces': 'north'})])
    r.pair('arm_{side}', '{side}_spaulder', (5.4, 26.4, 0), (3.6, 2.4, 4.4), 'rust', rot=(0, 0, -14), decals=[HEM])
    r.pair('arm_{side}', '{side}_humerus', (5.4, 23.2, 0), (1.4, 5.4, 1.4), 'bone')
    r.pair('forearm_{side}', '{side}_bracer', (5.4, 18.4, 0), (2.6, 4.6, 2.6), 'rust')
    r.pair('forearm_{side}', '{side}_hand', (5.4, 15.2, -0.2), (1.8, 2, 2), 'bone')
    # skull under a nasal helm and a grave hood
    r.box('head', 'skull', (0, 30, -0.2), (4.4, 4.6, 4.6), 'bone')
    r.box('head', 'sockets', (0, 30.6, -2.55), (3.4, 1.2, 0.2), 'void')
    r.pair('head', 'eye_{side}', (0.9, 30.6, -2.7), (0.7, 0.5, 0.2), 'soul_dim')
    r.box('head', 'teeth', (0, 28.4, -2.5), (2.6, 0.8, 0.3), 'bone',
          decals=[('glyph', {'bitmap': ['#.#.'], 'mat': 'void', 'faces': 'north'})])
    r.box('head', 'helm', (0, 32.9, 0), (5.2, 2.6, 5.2), 'rust', decals=[HEM])
    r.box('head', 'nasal', (0, 31, -2.75), (0.8, 2.8, 0.5), 'rust')
    r.box('head', 'hood', (0, 31.2, 1.6), (5.8, 6, 3), 'grave_cloth')
    # guisarme: long haft with a hooked blade
    r.box('polearm', 'haft', (-5.4, 16, -0.4), (1, 28, 1), 'heartwood')
    r.box('polearm', 'ferrule', (-5.4, 1.6, -0.4), (1.4, 1.2, 1.4), 'iron_dark')
    r.box('polearm', 'blade', (-5.4, 32.6, -1.6), (0.6, 6, 3), 'blade')
    r.box('polearm', 'hook', (-5.4, 30.6, 1.0), (0.6, 2.6, 2.2), 'rust', rot=(-30, 0, 0))
    r.box('polearm', 'socket', (-5.4, 29, -0.4), (1.6, 2, 1.6), 'iron_dark')
    r.box('buckler', 'disc', (6.4, 17, -2.2), (5.6, 5.6, 0.8), 'rust',
          decals=[('rim', {'mat': 'iron_dark', 'faces': ('north', 'south')}), ('glyph', {'bitmap': SKULL, 'mat': 'bone', 'faces': 'north'})])
    r.box('buckler', 'boss', (6.4, 17, -2.8), (1.6, 1.6, 0.6), 'iron_dark')

    rest = {'arm_right': {'rot': [14, 0, -4]}, 'forearm_right': {'rot': [30, 0, 0]}, 'polearm': {'rot': [-34, 0, 0]},
            'arm_left': {'rot': [16, 0, 6]}, 'forearm_left': {'rot': [30, 0, 0]}, 'buckler': {'rot': [-30, 0, 0]}}
    neutral = merge(rest, {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'head': {'rot': [0, 0, 0]},
                           'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'shin_right': {'rot': [0, 0, 0]}})
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(rest, {'head': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}})),
        (1.5, {'head': {'rot': [6, 12, 4]}, 'torso': {'rot': [4, 0, 0]}}),
        (3.0, {'head': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 0, 0]}}),
    ])
    L = 1.4
    r.clip('walk', L, True).poses([
        (0.0, merge(rest, {'leg_left': {'rot': [22, 0, 0]}, 'leg_right': {'rot': [-22, 0, 0]}, 'shin_right': {'rot': [-6, 0, 0]},
                           'shin_left': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, -0.4, 0]}, 'torso': {'rot': [6, -4, 0]}})),
        (L / 4, {'shin_right': {'rot': [-34, 0, 0]}, 'hips': {'pos': [0, 0.4, 0]}}),
        (L / 2, {'leg_left': {'rot': [-22, 0, 0]}, 'leg_right': {'rot': [22, 0, 0]}, 'shin_left': {'rot': [-6, 0, 0]},
                 'shin_right': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, -0.4, 0]}, 'torso': {'rot': [6, 4, 0]}}),
        (3 * L / 4, {'shin_left': {'rot': [-34, 0, 0]}, 'hips': {'pos': [0, 0.4, 0]}}),
        (L, {'leg_left': {'rot': [22, 0, 0]}, 'leg_right': {'rot': [-22, 0, 0]}, 'shin_right': {'rot': [-6, 0, 0]},
             'shin_left': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, -0.4, 0]}, 'torso': {'rot': [6, -4, 0]}}),
    ])
    # thrust: hit at 0.5
    r.clip('thrust', 1.2).poses([
        (0.0, neutral),
        (0.4, {'arm_right': {'rot': [40, 0, -10]}, 'forearm_right': {'rot': [60, 0, 0]}, 'polearm': {'rot': [-70, 0, 0]},
               'torso': {'rot': [-4, 18, 0]}, 'leg_right': {'rot': [-12, 0, 0]}}, 'easeOutCubic'),
        (0.5, {'arm_right': {'rot': [86, 0, 0]}, 'forearm_right': {'rot': [4, 0, 0]}, 'polearm': {'rot': [-90, 0, 0]},
               'torso': {'rot': [12, -8, 0]}, 'leg_left': {'rot': [22, 0, 0]}, 'hips': {'pos': [0, -0.6, -1.4]}}, 'easeInExpo'),
        (1.2, neutral),
    ])
    # hooking sweep: hit at 0.65
    r.clip('sweep', 1.4).poses([
        (0.0, neutral),
        (0.5, {'arm_right': {'rot': [110, 0, -50]}, 'polearm': {'rot': [-60, 0, -30]}, 'torso': {'rot': [0, 34, 0]}}, 'easeOutCubic'),
        (0.65, {'arm_right': {'rot': [80, 0, 40]}, 'polearm': {'rot': [-80, 0, 40]}, 'torso': {'rot': [6, -36, 0]}}, 'easeInExpo'),
        (1.4, neutral),
    ])
    r.clip('hurt', 0.4).poses([(0.0, neutral), (0.1, {'torso': {'rot': [-12, 6, 0]}, 'head': {'rot': [-14, 0, 0]}}, 'easeOutExpo'), (0.4, neutral)])
    r.clip('rise', 1.5).poses([
        (0.0, merge(neutral, {'root': {'pos': [0, -30, 0]}, 'torso': {'rot': [30, 0, 0]}, 'arm_left': {'rot': [170, 0, 10]}})),
        (1.1, {'root': {'pos': [0, 0, 0]}, 'torso': {'rot': [12, 0, 0]}, 'arm_left': {'rot': [60, 0, 10]}}, 'easeOutCubic'),
        (1.5, merge(neutral, {'root': {'pos': [0, 0, 0]}})),
    ])
    # collapses into a heap of bones and rusted plate
    r.clip('death', 1.6, 'hold').poses([
        (0.0, merge(neutral, {'head': {'pos': [0, 0, 0]}})),
        (0.5, {'torso': {'rot': [-16, 0, 8]}, 'head': {'rot': [-24, 0, 0]}}, 'easeOutCubic'),
        (1.1, {'hips': {'pos': [0, -9, 0]}, 'leg_left': {'rot': [80, 0, -14]}, 'shin_left': {'rot': [-90, 0, 0]},
               'leg_right': {'rot': [76, 0, 16]}, 'shin_right': {'rot': [-88, 0, 0]}, 'torso': {'rot': [50, 0, 10]},
               'arm_left': {'rot': [0, 0, 40]}, 'arm_right': {'rot': [0, 0, -40]}, 'polearm': {'rot': [-90, 0, -40]}}, 'easeInCubic'),
        (1.6, {'head': {'pos': [4, -10, -6], 'rot': [90, 40, 30]}}, 'easeOutBounce'),
    ])
    return r


def burial_guardian():
    r = Rig('burial_guardian', (256, 128), palette=WARDEN_PALETTE)
    r.mark_upright('torso', 'lid')
    r.bone('hips', (0, 18, 0))
    r.bone_pair('leg_{side}', (5.5, 17, 0), 'hips')
    r.bone('torso', (0, 20, 0), 'hips')
    r.bone('lid', (7, 28, -5), 'torso')
    r.bone('souls', (0, 32, -2), 'torso')
    r.bone_pair('arm_{side}', (11.5, 40, 0), 'torso')
    r.bone_pair('fist_{side}', (13, 24, 0), 'arm_{side}')
    # pillar legs of stacked tomb stone
    r.box('hips', 'plinth', (0, 18.5, 0), (14, 4, 10), 'tomb', decals=[('band', {'row': 1, 'size': 1, 'mat': 'iron_dark'})])
    r.pair('leg_{side}', '{side}_pillar_top', (5.5, 13, 0), (7, 8, 7.4), 'tomb')
    r.pair('leg_{side}', '{side}_pillar_base', (5.5, 4.2, -0.4), (8.4, 8.4, 8.8), 'tomb', decals=[('band', {'row': 0, 'size': 1, 'mat': 'iron_dark'})])
    # the sarcophagus torso: a coffin standing on end; the effigy lid is its face
    r.box('torso', 'coffin', (0, 33, 1.6), (16, 22, 9), 'tomb', decals=[('band', {'row': 1, 'size': 1, 'mat': 'iron_dark'}),
                                                                         ('band', {'row': -2, 'size': 1, 'mat': 'iron_dark'})])
    r.box('torso', 'interior', (0, 33, -2.6), (11, 17, 1), 'void')
    r.box('souls', 'soul_core', (0, 34, -2.2), (5, 9, 1.4), 'soul')
    r.box('souls', 'soul_wisp', (1.6, 39, -2.4), (2, 3, 1), 'soul_dim')
    r.box('lid', 'lid_slab', (0, 33, -4.3), (14, 20, 2.4), 'marble',
          decals=[('glow_edge', {'mat': 'soul', 'edge': 'left', 'width': 1, 'strength': 0.5, 'faces': 'north'}),
                  ('glow_edge', {'mat': 'soul', 'edge': 'right', 'width': 1, 'strength': 0.5, 'faces': 'north'})])
    # carved effigy: a sleeping knight's face and crossed arms in relief
    r.box('lid', 'effigy_head', (0, 39.6, -5.9), (5, 5, 1.2), 'marble')
    r.pair('lid', 'effigy_eye_{side}', (1.1, 40.2, -6.6), (1.2, 0.6, 0.3), 'soul')
    r.box('lid', 'effigy_brow', (0, 41.4, -6.6), (5.2, 1, 0.4), 'marble')
    r.box('lid', 'effigy_arms', (0, 33.8, -5.9), (8, 2, 1.2), 'marble', rot=(0, 0, 0))
    r.box('lid', 'effigy_sword', (0, 30, -6.1), (1.2, 9, 0.8), 'marble')
    r.box('lid', 'effigy_hilt', (0, 34.8, -6.4), (3.6, 0.8, 0.6), 'marble')
    r.box('lid', 'lid_crack', (-3.6, 30, -5.6), (0.4, 7, 0.3), 'soul')
    # headstones rising from the shoulders
    r.pair('torso', '{side}_headstone', (7, 46.5, 3.2), (5, 7, 2), 'tomb', rot=(0, 0, -10),
           decals=[('glyph', {'bitmap': ['.#.', '###', '.#.', '.#.'], 'mat': 'bone', 'faces': 'north'})])
    r.pair('torso', '{side}_headstone_cap', (7.6, 50.4, 3.2), (3.6, 1.2, 2), 'tomb', rot=(0, 0, -10))
    r.box('torso', 'urn', (0, 46.4, 2.6), (5, 4, 5), 'bone', decals=[('band', {'row': 1, 'size': 1, 'mat': 'rust'})])
    r.box('torso', 'urn_flame', (0, 49.4, 2.6), (2.6, 2.4, 2.6), 'soul')
    # slab arms ending in chained grave-stone fists
    r.pair('arm_{side}', '{side}_shoulder', (12, 40.5, 0.6), (7.4, 6, 9), 'tomb')
    r.pair('arm_{side}', '{side}_upper', (13, 33, 0.6), (6, 10, 6), 'tomb')
    r.pair('fist_{side}', '{side}_fist', (13.4, 21, 0), (9, 9, 9), 'tomb', decals=[('band', {'row': 2, 'size': 1, 'mat': 'chain'}),
                                                                                    ('band', {'row': 6, 'size': 1, 'mat': 'chain'})])
    neutral = {'torso': {'rot': [0, 0, 0]}, 'hips': {'pos': [0, 0, 0]}, 'arm_left': {'rot': [0, 0, 4]},
               'arm_right': {'rot': [0, 0, -4]}, 'lid': {'rot': [0, 0, 0]}, 'souls': {'scale': 1},
               'leg_left': {'rot': [0, 0, 0]}, 'leg_right': {'rot': [0, 0, 0]}}
    r.clip('idle', 4.0, True).poses([
        (0.0, merge(neutral, {'souls': {'scale': 1}})),
        (2.0, {'torso': {'rot': [2, 0, 0]}, 'souls': {'scale': 1.12}, 'arm_left': {'rot': [2, 0, 6]}, 'arm_right': {'rot': [2, 0, -6]}}),
        (4.0, merge(neutral, {'souls': {'scale': 1}})),
    ])
    r.clip('walk', 2.4, True).poses([
        (0.0, merge(neutral, {'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-14, 0, 0]}, 'torso': {'rot': [0, -6, 4]},
                              'hips': {'pos': [0, -0.6, 0]}})),
        (0.6, {'hips': {'pos': [0, 0.6, 0]}}),
        (1.2, {'leg_left': {'rot': [-14, 0, 0]}, 'leg_right': {'rot': [14, 0, 0]}, 'torso': {'rot': [0, 6, -4]}, 'hips': {'pos': [0, -0.6, 0]}}),
        (1.8, {'hips': {'pos': [0, 0.6, 0]}}),
        (2.4, {'leg_left': {'rot': [14, 0, 0]}, 'leg_right': {'rot': [-14, 0, 0]}, 'torso': {'rot': [0, -6, 4]}, 'hips': {'pos': [0, -0.6, 0]}}),
    ])
    # Entombing slam: both fists overhead, ground impact at 1.3
    r.clip('slam', 2.4).poses([
        (0.0, neutral),
        (1.1, {'arm_left': {'rot': [168, 0, -18]}, 'arm_right': {'rot': [168, 0, 18]}, 'torso': {'rot': [-14, 0, 0]}}, 'easeOutCubic'),
        (1.3, {'arm_left': {'rot': [40, 0, -10]}, 'arm_right': {'rot': [40, 0, 10]}, 'torso': {'rot': [26, 0, 0]},
               'hips': {'pos': [0, -2, -1]}}, 'easeInExpo'),
        (1.8, {'torso': {'rot': [24, 0, 0]}}),
        (2.4, neutral),
    ])
    # Backhand sweep: hit at 0.9
    r.clip('sweep', 1.8).poses([
        (0.0, neutral),
        (0.7, {'arm_right': {'rot': [70, 0, -60]}, 'torso': {'rot': [0, 34, 0]}}, 'easeOutCubic'),
        (0.9, {'arm_right': {'rot': [70, 0, 50]}, 'torso': {'rot': [4, -36, 0]}}, 'easeInExpo'),
        (1.8, neutral),
    ])
    # The lid swings open and the interred souls burst out (blast at 1.2)
    r.clip('unseal', 2.6).poses([
        (0.0, neutral),
        (0.8, {'arm_left': {'rot': [30, 0, 50]}, 'arm_right': {'rot': [30, 0, -50]}, 'torso': {'rot': [-10, 0, 0]}}, 'easeOutCubic'),
        (1.2, {'lid': {'rot': [0, -100, 0]}, 'souls': {'scale': 1.8}}, 'easeOutBack'),
        (1.9, {'souls': {'scale': 1}}),
        (2.6, neutral),
    ])
    r.clip('hurt', 0.4).poses([(0.0, neutral), (0.12, {'torso': {'rot': [-6, 6, 0]}}, 'easeOutExpo'), (0.4, neutral)])
    r.clip('death', 2.4, 'hold').poses([
        (0.0, merge(neutral, {'arm_left': {'pos': [0, 0, 0]}, 'arm_right': {'pos': [0, 0, 0]}})),
        (0.6, {'lid': {'rot': [0, -100, 0]}, 'souls': {'scale': 2}}, 'easeOutBack'),
        (1.4, {'souls': {'scale': 0}, 'torso': {'rot': [-10, 0, 8]}}),
        (2.4, {'hips': {'pos': [0, -12, 0]}, 'torso': {'rot': [70, 0, 10]}, 'arm_left': {'pos': [4, -14, -4], 'rot': [60, 0, 40]},
               'arm_right': {'pos': [-4, -14, -2], 'rot': [40, 0, -50]}, 'leg_left': {'rot': [0, 0, 20]},
               'leg_right': {'rot': [0, 0, -20]}}, 'easeOutBounce'),
    ])
    return r


MODELS = {
    'catacomb_rat': (catacomb_rat, [('idle', 0.5), ('walk', 0.12), ('attack', 0.3), ('death', 1.0)]),
    'crypt_lurker': (crypt_lurker, [('idle', 1.2), ('walk', 0.15), ('pounce', 0.6), ('pounce', 0.85), ('bite', 0.3), ('emerge', 0.5), ('death', 1.5)]),
    'gravebound_soldier': (gravebound_soldier, [('idle', 0), ('walk', 0.35), ('thrust', 0.4), ('thrust', 0.5), ('sweep', 0.5), ('rise', 0.6), ('death', 1.6)]),
    'burial_guardian': (burial_guardian, [('idle', 0), ('walk', 0.6), ('slam', 1.1), ('slam', 1.3), ('sweep', 0.9), ('unseal', 1.2), ('death', 2.4)]),
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
