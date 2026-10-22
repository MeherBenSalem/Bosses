"""The Shattered Oracle — The Voice of a Dead God (the Sanctum of the Silent God).

A serene marble ceremonial mask, cracked and leaking rose soul light, floats above a body of
separated marble shards around an unstable crystal core. Its arms are suspended rose-crystal
segments with long fingers; a fragmented gilt halo turns behind it; a tattered priest's stole
trails into drifting shards. Stage three (Reconstruction) reveals hidden bones: marble-feather
wings and the missing halo arcs (`wings`, `halo_restored`; hidden by game logic until then).
Editor conventions as in hollow_sovereign.py.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paint import Material  # noqa: E402
from rig import Rig  # noqa: E402

NAME = 'shattered_oracle'
PALETTE = {
    'vestment': Material(['#2A1724', '#3E2034', '#572B48', '#743C60', '#934F7A'], 'cloth', torn=True),
}
INSCRIPTION = ['#.##.#', '##.#.#']
SIGIL = ['.#.', '#+#', '.#.']
CY = 52  # mask height


def build():
    r = Rig(NAME, (256, 256), palette=PALETTE)
    r.bone('float', (0, 40, 0))
    r.bone('mask', (0, CY, -2), 'float')
    r.bone('halo', (0, CY + 2, 6), 'float')
    r.bone('halo_restored', (0, CY + 2, 6), 'halo')
    r.bone('body', (0, 36, 0), 'float')
    r.bone('core', (0, 38, -1), 'body')
    r.bone('shards', (0, 36, 0), 'float')
    r.bone('stole', (0, 30, -1), 'body')
    r.bone('stole_lower', (0, 20, -1), 'stole')
    r.bone('wings', (0, 44, 6), 'body')
    r.bone_pair('wing_{side}', (5, 44, 6), 'wings')
    r.bone_pair('arm_{side}', (11, 44, 0), 'body')
    r.bone_pair('forearm_{side}', (15, 34, -1), 'arm_{side}')
    r.bone_pair('hand_{side}', (17, 25, -2), 'forearm_{side}')

    # the mask: serene face, gilt eye-lines, a split down the middle leaking rose light
    r.box('mask', 'face', (0, CY, -2), (12, 15, 4), 'marble', decals=[('glyph', {'bitmap': SIGIL, 'mat': 'gold', 'faces': 'north', 'dy': -5})])
    r.box('mask', 'brow', (0, CY + 5.4, -4.4), (12.4, 1.6, 1), 'marble')
    r.pair('mask', 'eye_{side}', (2.8, CY + 2.4, -4.15), (3, 0.8, 0.3), 'oracle')
    r.pair('mask', 'lash_{side}', (2.8, CY + 3.4, -4.2), (3.6, 0.5, 0.3), 'gold')
    r.box('mask', 'nose', (0, CY, -4.6), (1.6, 3.6, 1.2), 'marble')
    r.box('mask', 'lips', (0, CY - 3.6, -4.15), (3, 0.8, 0.3), 'gold')
    r.box('mask', 'split', (0.6, CY - 1, -4.25), (0.6, 13, 0.3), 'oracle')
    r.box('mask', 'chin_piece', (-2.2, CY - 6.6, -2), (5, 2.4, 3), 'marble', rot=(0, 0, 12))
    r.pair('mask', 'crown_spire_{side}', (4, CY + 9, -1.4), (1.6, 6, 1.6), 'gold', rot=(0, 0, -14))
    r.box('mask', 'crown_spire_mid', (0, CY + 10.4, -1.4), (1.8, 7, 1.8), 'gold')
    r.box('mask', 'crown_gem', (0, CY + 14.4, -1.4), (1.4, 1.8, 1.4), 'oracle', rot=(0, 45, 0))

    # fragmented halo: arcs with gaps (restored arcs live in a hidden child bone)
    def arc(bone, i, n, radius, mat='gold'):
        a = 2 * math.pi * i / n
        x, y = math.sin(a) * radius, math.cos(a) * radius
        r.box(bone, f'{bone}_arc_{i}', (x, CY + 2 + y, 6.2), (5.6, 1.4, 1), mat, rot=(0, 0, -math.degrees(a)))
    for i in range(16):
        if i in (3, 4, 9, 13):
            arc('halo_restored', i, 16, 14, 'oracle')
        else:
            arc('halo', i, 16, 14)
    for i in range(8):
        a = 2 * math.pi * i / 8 + 0.2
        r.box('halo', f'halo_ray_{i}', (math.sin(a) * 17, CY + 2 + math.cos(a) * 17, 6.4), (0.8, 3, 0.6), 'gold',
              rot=(0, 0, -math.degrees(a)))

    # broken body: separated marble plates around an unstable crystal core
    r.box('body', 'collar', (0, 45.4, 0), (14, 3, 8), 'marble', decals=[('glyph', {'bitmap': INSCRIPTION, 'mat': 'gold', 'faces': 'north'})])
    r.pair('body', 'chest_shard_{side}', (5, 40.4, -1), (6, 7, 6), 'marble', rot=(0, -8, -6),
           decals=[('glow_edge', {'mat': 'oracle', 'edge': 'right', 'width': 2, 'strength': 0.5, 'faces': 'north'})])
    r.box('body', 'back_plate', (0, 39, 4), (12, 10, 3), 'marble', decals=[('glyph', {'bitmap': INSCRIPTION, 'mat': 'gold', 'faces': 'south'})])
    r.box('body', 'waist_shard', (1, 32.6, 0), (8, 4, 5), 'marble', rot=(0, 12, 6))
    r.box('core', 'soul_core', (0, 38, -1), (5, 6, 5), 'oracle', rot=(0, 45, 0))
    r.box('core', 'core_shell', (0, 38, -1), (3, 9, 3), 'oracle', rot=(45, 0, 0))
    r.box('stole', 'stole_upper', (0, 25, -4.4), (5, 10, 0.6), 'vestment', decals=[('band', {'row': 0, 'size': 1, 'mat': 'gold'}),
                                                                                    ('glyph', {'bitmap': SIGIL, 'mat': 'gold', 'faces': 'north'})])
    r.box('stole_lower', 'stole_tail', (0, 13, -4.6), (4.4, 14, 0.6), 'vestment')
    for i, (x, y, z, s) in enumerate([(-3, 26, 2, 4), (3.4, 20, 1, 3.2), (-1, 14, 3, 2.6), (2, 9, 0, 2), (-2.4, 5, 1.4, 1.4)]):
        r.box('shards', f'trail_{i}', (x, y, z), (s, s * 1.2, s), 'marble', rot=(i * 17, i * 31, i * 11),
              decals=[('glow_edge', {'mat': 'oracle', 'edge': 'bottom', 'width': 1, 'strength': 0.4})])
    for i in range(6):
        a = 2 * math.pi * i / 6
        r.box('shards', f'orbit_{i}', (math.sin(a) * 15, 38 + (i % 2) * 4, math.cos(a) * 15), (3, 4.4, 1.2), 'marble',
              rot=(0, math.degrees(a), 10), decals=[('glyph', {'bitmap': SIGIL, 'mat': 'oracle', 'faces': ('north', 'south')})])

    # suspended crystal limbs: segments float apart from each other
    for side, s in (('left', 1), ('right', -1)):
        a, f, h = f'arm_{side}', f'forearm_{side}', f'hand_{side}'
        r.box(a, f'{a}_shoulder_orb', (s * 11, 44, 0), (4, 4, 4), 'oracle', rot=(45, 45, 0))
        r.box(a, f'{a}_upper', (s * 13, 39, -0.5), (2.6, 8, 2.6), 'oracle', rot=(0, 0, -s * 20))
        r.box(f, f'{f}_lower', (s * 16, 29.6, -1.5), (2.4, 8, 2.4), 'oracle', rot=(0, 0, -s * 10))
        r.box(f, f'{f}_bracer', (s * 15.6, 32, -1.4), (3.6, 2, 3.6), 'gold')
        r.box(h, f'{h}_palm', (s * 17, 23.6, -2), (3.4, 2.6, 1.6), 'oracle')
        for k, dx in enumerate((-1.2, 0, 1.2)):
            r.box(h, f'{h}_finger_{k}', (s * 17 + dx, 19.6, -2.2 - abs(dx) * 0.3), (0.7, 5, 0.7), 'oracle')

    # stage three: marble-feather wings
    for side, s in (('left', 1), ('right', -1)):
        w = f'wing_{side}'
        for k in range(5):
            length = 16 - k * 2
            r.box(w, f'{w}_feather_{k}', (s * (10 + k * 3.4), 46 - k * 3, 7), (2.4, length, 1), 'marble', rot=(0, 0, -s * (60 - k * 12)),
                  decals=[('glow_edge', {'mat': 'oracle', 'edge': 'left', 'width': 1, 'strength': 0.6})])
        r.box(w, f'{w}_root', (s * 7, 45, 6.6), (4, 4, 2), 'gold')

    r.preview_hidden = {'wings', 'halo_restored'}
    clips(r)
    return r


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    still = {'float': {'pos': [0, 0, 0]}, 'halo': {'rot': [0, 0, 0]}, 'shards': {'rot': [0, 0, 0]}, 'core': {'scale': 1, 'rot': [0, 0, 0]},
             'mask': {'rot': [0, 0, 0]}, 'arm_left': {'rot': [0, 0, 10]}, 'arm_right': {'rot': [0, 0, -10]},
             'stole': {'rot': [0, 0, 0]}, 'stole_lower': {'rot': [0, 0, 0]}}
    r.clip('idle', 6.0, True).poses([
        (0.0, still),
        (1.5, {'float': {'pos': [0, 1.5, 0]}, 'halo': {'rot': [0, 0, 22.5]}, 'shards': {'rot': [0, 90, 0]}, 'core': {'scale': 1.15, 'rot': [0, 90, 0]},
               'mask': {'rot': [4, 6, 0]}, 'stole_lower': {'rot': [-8, 0, 4]}}),
        (3.0, {'float': {'pos': [0, 0, 0]}, 'halo': {'rot': [0, 0, 45]}, 'shards': {'rot': [0, 180, 0]}, 'core': {'scale': 1, 'rot': [0, 180, 0]},
               'mask': {'rot': [0, 0, 0]}, 'stole_lower': {'rot': [0, 0, 0]}}),
        (4.5, {'float': {'pos': [0, 1.5, 0]}, 'halo': {'rot': [0, 0, 67.5]}, 'shards': {'rot': [0, 270, 0]}, 'core': {'scale': 1.15, 'rot': [0, 270, 0]},
               'mask': {'rot': [4, -6, 0]}, 'stole_lower': {'rot': [-8, 0, -4]}}),
        (6.0, merge(still, {'halo': {'rot': [0, 0, 90]}, 'shards': {'rot': [0, 360, 0]}, 'core': {'scale': 1, 'rot': [0, 360, 0]}})),
    ], 'linear')
    r.clips['idle'].seam_period['halo'] = 22.5   # sixteen halo arcs
    # formation volley: hands raised, released at 1.1
    r.clip('volley', 2.2).poses([
        (0.0, still),
        (1.0, {'arm_left': {'rot': [60, 0, 60]}, 'arm_right': {'rot': [60, 0, -60]}, 'core': {'scale': 1.4}, 'halo': {'rot': [0, 0, 30]}}, 'easeOutCubic'),
        (1.1, {'arm_left': {'rot': [90, 0, 20]}, 'arm_right': {'rot': [90, 0, -20]}, 'core': {'scale': 0.9}}, 'easeInExpo'),
        (2.2, merge(still, {'halo': {'rot': [0, 0, 45]}})),
    ])
    # fragment shield: the orbiting shards close in and spin fast
    r.clip('shield', 1.4).poses([
        (0.0, {'shards': {'scale': 1, 'rot': [0, 0, 0]}}),
        (1.0, {'shards': {'scale': [0.6, 1, 0.6], 'rot': [0, 360, 0]}}, 'easeOutBack'),
        (1.4, {'shards': {'scale': [0.6, 1, 0.6], 'rot': [0, 540, 0]}}),
    ])
    # absorb: arms spread wide, pulling souls inward (drain ticks 0.8-2.8)
    r.clip('absorb', 3.6).poses([
        (0.0, still),
        (0.8, {'arm_left': {'rot': [20, 0, 90]}, 'arm_right': {'rot': [20, 0, -90]}, 'mask': {'rot': [-14, 0, 0]}, 'core': {'scale': 1.3}}, 'easeOutCubic'),
        (2.0, {'core': {'scale': 1.8, 'rot': [0, 360, 0]}}, 'linear'),
        (2.8, {'core': {'scale': 2.0, 'rot': [0, 540, 0]}}, 'linear'),
        (3.6, still, 'easeInOutCubic'),
    ])
    # smite: area strike at 1.5
    r.clip('smite', 2.6).poses([
        (0.0, still),
        (1.2, {'arm_left': {'rot': [170, 0, 10]}, 'arm_right': {'rot': [170, 0, -10]}, 'float': {'pos': [0, 4, 0]}, 'halo': {'rot': [0, 0, 60]}}, 'easeOutCubic'),
        (1.5, {'arm_left': {'rot': [40, 0, 20]}, 'arm_right': {'rot': [40, 0, -20]}, 'float': {'pos': [0, -3, 0]}, 'core': {'scale': 1.6}}, 'easeInExpo'),
        (2.6, merge(still, {'halo': {'rot': [0, 0, 90]}})),
    ])
    # illusion: the body splinters into three (Java spawns decoys at 0.6)
    r.clip('illusion', 1.2).poses([
        (0.0, {'body': {'scale': 1}, 'mask': {'scale': 1}}),
        (0.6, {'body': {'scale': [1.4, 0.7, 1.4]}, 'mask': {'scale': [1.2, 0.9, 1.2]}}, 'easeOutExpo'),
        (1.2, {'body': {'scale': 1}, 'mask': {'scale': 1}}, 'easeOutBack'),
    ])
    # stage two: the mask cracks wider
    r.clip('fracture', 2.4).poses([
        (0.0, merge(still, {'mask': {'rot': [0, 0, 0], 'scale': 1}})),
        (0.8, {'mask': {'rot': [-20, 0, 8], 'scale': 1.1}, 'core': {'scale': 2}}, 'easeOutCubic'),
        (1.2, {'mask': {'rot': [10, 0, -6]}}, 'easeOutBounce'),
        (2.4, merge(still, {'mask': {'rot': [0, 0, 0], 'scale': 1}})),
    ])
    # stage three: Reconstruction — wings unfold, the halo closes (Java shows hidden bones at 0.6)
    r.clip('reconstruct', 4.0).poses([
        (0.0, merge(still, {'wings': {'scale': 0.1}, 'halo_restored': {'scale': 0.1}})),
        (1.2, {'float': {'pos': [0, -4, 0]}, 'core': {'scale': 0.5}, 'arm_left': {'rot': [-20, 0, 20]}, 'arm_right': {'rot': [-20, 0, -20]}}, 'easeInCubic'),
        (2.4, {'float': {'pos': [0, 6, 0]}, 'core': {'scale': 2.2}, 'wings': {'scale': 1}, 'halo_restored': {'scale': 1},
               'arm_left': {'rot': [40, 0, 80]}, 'arm_right': {'rot': [40, 0, -80]}, 'halo': {'rot': [0, 0, 90]}}, 'easeOutBack'),
        (4.0, merge(still, {'wings': {'scale': 1}, 'halo_restored': {'scale': 1}, 'halo': {'rot': [0, 0, 180]}})),
    ])
    r.clip('hurt', 0.4).poses([(0.0, {'mask': {'rot': [0, 0, 0]}}), (0.1, {'mask': {'rot': [-10, 10, 6]}}, 'easeOutExpo'), (0.4, {'mask': {'rot': [0, 0, 0]}})])
    # death: everything scatters and falls, the mask last
    r.clip('death', 5.0, 'hold').poses([
        (0.0, merge(still, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'mask': {'pos': [0, 0, 0]}, 'arm_left': {'pos': [0, 0, 0]},
                            'arm_right': {'pos': [0, 0, 0]}, 'halo': {'pos': [0, 0, 0]}})),
        (1.0, {'core': {'scale': 2.6}, 'halo': {'rot': [0, 0, 120]}}, 'easeOutCubic'),
        (1.6, {'core': {'scale': 0}}, 'easeInExpo'),
        (3.0, {'body': {'pos': [0, -26, 2], 'rot': [30, 20, 40]}, 'arm_left': {'pos': [8, -20, -4], 'rot': [80, 0, 40]},
               'arm_right': {'pos': [-8, -22, 2], 'rot': [60, 0, -60]}, 'halo': {'pos': [0, -30, 6], 'rot': [80, 0, 200]},
               'shards': {'pos': [0, -30, 0], 'scale': [1.4, 0.4, 1.4]}}, 'easeInCubic'),
        (5.0, {'mask': {'pos': [0, -44, -4], 'rot': [80, 0, 10]}}, 'easeOutBounce'),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 0.0), ('volley', 1.0), ('absorb', 2.0), ('smite', 1.2), ('fracture', 0.8), ('reconstruct', 2.4), ('reconstruct', 4.0),
             ('death', 3.0)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
