"""The Forgotten Watcher — Guardian of the Last Memory (Seyr, the Lantern Archive).

A floating sandstone obelisk with a violet crystal eye in a verdigris frame. Three gyroscope rings
of rune-cut stone spin in their own planes (tilt bone -> spin child, so euler keys stay simple),
memory tablets orbit, side gears turn, and an aegis of shield plates can close around the core.
Editor conventions as in hollow_sovereign.py.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402

NAME = 'forgotten_watcher'
RUNES = ['#.#', '.#.', '#.#']
RUNE_B = ['##.', '.#.', '.##']
RUNE_C = ['.#.', '###', '.#.']
CY = 34  # core centre height (floats ~1 block above its feet)


def ring(r, name, radius, segments, tilt, seg_size, mat='sandstone', stud='crystal', parent='core'):
    """A ring of stone segments in the XZ plane of a spin bone nested under a tilt bone."""
    r.bone(f'{name}_tilt', (0, CY, 0), parent, rotation=tilt)
    r.bone(f'{name}_spin', (0, CY, 0), f'{name}_tilt')
    for i in range(segments):
        a = 2 * math.pi * i / segments
        x, z = math.sin(a) * radius, math.cos(a) * radius
        deg = math.degrees(a)
        decal = [('glyph', {'bitmap': (RUNES, RUNE_B, RUNE_C)[i % 3], 'mat': 'crystal', 'faces': ('north', 'south')})]
        r.box(f'{name}_spin', f'{name}_seg_{i}', (x, CY, z), seg_size, mat, rot=(0, deg, 0), decals=decal)
        if i % 2 == 0:
            r.box(f'{name}_spin', f'{name}_stud_{i}', (x * 1.0, CY + seg_size[1] / 2 + 0.5, z * 1.0), (1, 1, 1), stud, rot=(0, deg, 0))


def build():
    r = Rig(NAME, (256, 128))
    r.bone('core', (0, CY, 0))
    r.bone('eye', (0, CY, -5), 'core')
    r.bone('lid_top', (0, CY + 3.6, -5.6), 'eye')
    r.bone('lid_bottom', (0, CY - 3.6, -5.6), 'eye')
    r.bone('pendant', (0, CY - 14, 0), 'core')
    r.bone('fragments', (0, CY, 0), 'core')
    r.bone_pair('gear_{side}', (7.4, CY, 0), 'core')
    r.bone('aegis', (0, CY, 0), 'core')

    # obelisk core: stepped capstone, banded body, inverted base
    r.box('core', 'body', (0, CY, 0), (10, 14, 10), 'sandstone',
          decals=[('band', {'row': 1, 'size': 1, 'mat': 'verdigris'}), ('band', {'row': -2, 'size': 1, 'mat': 'verdigris'}),
                  ('glyph', {'bitmap': RUNES, 'mat': 'crystal', 'faces': ('east', 'west', 'south')})])
    r.box('core', 'cap_1', (0, CY + 8, 0), (8, 2, 8), 'sandstone', decals=[('band', {'row': 0, 'size': 1, 'mat': 'verdigris'})])
    r.box('core', 'cap_2', (0, CY + 10, 0), (5.6, 2, 5.6), 'sandstone')
    r.box('core', 'cap_3', (0, CY + 12, 0), (3, 2, 3), 'verdigris')
    r.box('core', 'cap_crystal', (0, CY + 14.4, 0), (1.6, 2.8, 1.6), 'crystal', rot=(0, 45, 0))
    r.box('core', 'base_1', (0, CY - 8, 0), (8, 2, 8), 'sandstone', decals=[('band', {'row': -1, 'size': 1, 'mat': 'verdigris'})])
    r.box('core', 'base_2', (0, CY - 10, 0), (5.6, 2, 5.6), 'sandstone')
    r.box('core', 'base_3', (0, CY - 12, 0), (3, 2, 3), 'verdigris')
    r.box('pendant', 'pendant_crystal', (0, CY - 15.4, 0), (2, 3.4, 2), 'crystal', rot=(0, 45, 0))
    r.box('pendant', 'pendant_tip', (0, CY - 17.8, 0), (1, 1.6, 1), 'crystal', rot=(0, 45, 0))

    # the eye: verdigris frame, violet iris, a dark slit pupil, shutter lids
    r.box('eye', 'frame', (0, CY, -5.4), (8, 8, 1.2), 'verdigris', decals=[('rim', {'mat': 'gold', 'faces': 'north'})])
    r.box('eye', 'iris', (0, CY, -6.1), (5, 5, 0.6), 'crystal')
    r.box('eye', 'pupil', (0, CY, -6.45), (1, 3.4, 0.2), 'void')
    r.box('lid_top', 'lid_top_plate', (0, CY + 4.6, -6.2), (8.4, 1.6, 1.0), 'sandstone', decals=[('band', {'row': -1, 'size': 1, 'mat': 'verdigris'})])
    r.box('lid_bottom', 'lid_bottom_plate', (0, CY - 4.6, -6.2), (8.4, 1.6, 1.0), 'sandstone', decals=[('band', {'row': 0, 'size': 1, 'mat': 'verdigris'})])

    # side gears
    for side, s in (('left', 1), ('right', -1)):
        g = f'gear_{side}'
        r.box(g, f'{g}_hub', (s * 5.8, CY, 0), (1.4, 3, 3), 'verdigris')
        for k in range(4):
            r.box(g, f'{g}_tooth_{k}', (s * 5.8, CY, 0), (1, 8, 1.4), 'verdigris', rot=(45 * k, 0, 0))

    # gyroscope rings
    ring(r, 'ring_a', 15, 12, (0, 0, 0), (7.8, 2.4, 1.4), 'sandstone')
    ring(r, 'ring_b', 12, 10, (62, 0, 0), (7.4, 2, 1.2), 'verdigris', stud='crystal')
    ring(r, 'ring_c', 10.5, 8, (0, 0, 58), (7.8, 1.6, 1.0), 'sandstone', stud='crystal')

    # orbiting memory tablets
    for i, (a, h) in enumerate([(30, 4), (150, -3), (260, 6), (330, -6)]):
        x, z = math.sin(math.radians(a)) * 20, math.cos(math.radians(a)) * 20
        r.box('fragments', f'tablet_{i}', (x, CY + h, z), (3, 4, 0.8), 'sandstone', rot=(10, a, 8),
              decals=[('glyph', {'bitmap': RUNE_C if i % 2 else RUNES, 'mat': 'crystal', 'faces': ('north', 'south')})])

    # aegis plates (hidden by Java outside shield phases). One child bone per plate: while shielded,
    # Java hides aegis_0 (the open segment) and turns `aegis` so that gap faces the synced bearing.
    # Plate i sits at angle 2*pi*i/6 (plate 0 at +Z); clips animate `aegis` only.
    for i in range(6):
        a = 2 * math.pi * i / 6
        x, z = math.sin(a) * 8.6, math.cos(a) * 8.6
        r.bone(f'aegis_{i}', (0, CY, 0), 'aegis')
        r.box(f'aegis_{i}', f'aegis_plate_{i}', (x, CY, z), (6, 16, 0.8), 'crystal', rot=(0, math.degrees(a), 0),
              decals=[('rim', {'mat': 'verdigris', 'faces': ('north', 'south')})])

    r.preview_hidden = {'aegis'}
    clips(r)
    return r


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    def rings(k, scale=1.0):
        return merge({'ring_a_spin': {'rot': [0, 360 * k * scale, 0]}}, {'ring_b_spin': {'rot': [0, -720 * k * scale, 0]}},
                     {'ring_c_spin': {'rot': [0, 1080 * k * scale, 0]}}, {'fragments': {'rot': [0, -360 * k * scale, 0]}},
                     {'gear_left': {'rot': [720 * k * scale, 0, 0]}}, {'gear_right': {'rot': [-720 * k * scale, 0, 0]}})
    # idle: 4 s, rings at different speeds, core bobbing, the eye scanning
    r.clip('idle', 4.0, True).poses([
        (0.0, merge(rings(0), {'core': {'pos': [0, 0, 0]}, 'eye': {'rot': [0, 0, 0]}})),
        (1.0, merge(rings(0.25), {'core': {'pos': [0, 1.2, 0]}, 'eye': {'rot': [0, 18, 0]}})),
        (2.0, merge(rings(0.5), {'core': {'pos': [0, 0, 0]}, 'eye': {'rot': [0, 0, 0]}})),
        (3.0, merge(rings(0.75), {'core': {'pos': [0, -1.2, 0]}, 'eye': {'rot': [0, -18, 0]}})),
        (4.0, merge(rings(1.0), {'core': {'pos': [0, 0, 0]}, 'eye': {'rot': [0, 0, 0]}})),
    ], 'linear')
    # volley: rings accelerate, eye flares, projectiles released at 1.0
    r.clip('volley', 1.8).poses([
        (0.0, merge(rings(0), {'eye': {'scale': 1}, 'lid_top': {'pos': [0, 0, 0]}, 'lid_bottom': {'pos': [0, 0, 0]}})),
        (0.9, merge(rings(0.6), {'eye': {'scale': 1.25}, 'lid_top': {'pos': [0, 1.2, 0]}, 'lid_bottom': {'pos': [0, -1.2, 0]}}), 'easeInCubic'),
        (1.0, merge(rings(0.8), {'eye': {'scale': 0.9}}), 'easeOutExpo'),
        (1.8, merge(rings(1.0), {'eye': {'scale': 1}, 'lid_top': {'pos': [0, 0, 0]}, 'lid_bottom': {'pos': [0, 0, 0]}})),
    ], 'linear')
    # beam: lids open wide, charge 0-1.5, sweep 1.5-3.5, recover
    r.clip('beam', 4.4).poses([
        (0.0, merge(rings(0), {'lid_top': {'pos': [0, 0, 0]}, 'lid_bottom': {'pos': [0, 0, 0]}, 'eye': {'scale': 1, 'rot': [0, 0, 0]},
                               'core': {'rot': [0, 0, 0]}})),
        (1.5, merge(rings(1.5), {'lid_top': {'pos': [0, 2.4, 0]}, 'lid_bottom': {'pos': [0, -2.4, 0]}, 'eye': {'scale': 1.35}}), 'easeInCubic'),
        (2.5, merge(rings(2.0), {'core': {'rot': [0, 30, 0]}}), 'linear'),
        (3.5, merge(rings(2.5), {'core': {'rot': [0, -30, 0]}}), 'linear'),
        (4.4, merge(rings(3.0), {'lid_top': {'pos': [0, 0, 0]}, 'lid_bottom': {'pos': [0, 0, 0]}, 'eye': {'scale': 1}, 'core': {'rot': [0, 0, 0]}}), 'easeOutCubic'),
    ], 'linear')
    # teleport: everything folds into the eye, then unfolds (Java moves it at 0.8)
    fold = {'ring_a_tilt': {'scale': 0.05}, 'ring_b_tilt': {'scale': 0.05}, 'ring_c_tilt': {'scale': 0.05}, 'fragments': {'scale': 0.05},
            'core': {'scale': 0.2}}
    unfold = {'ring_a_tilt': {'scale': 1}, 'ring_b_tilt': {'scale': 1}, 'ring_c_tilt': {'scale': 1}, 'fragments': {'scale': 1}, 'core': {'scale': 1}}
    r.clip('teleport', 1.6).poses([(0.0, unfold), (0.8, fold, 'easeInBack'), (1.6, unfold, 'easeOutBack')])
    # shield: aegis plates close (Java shows the aegis bone during shield phases)
    r.clip('shield', 1.2).poses([
        (0.0, {'aegis': {'scale': [0.2, 0.2, 0.2], 'rot': [0, -90, 0]}, 'lid_top': {'pos': [0, 0, 0]}, 'lid_bottom': {'pos': [0, 0, 0]}}),
        (1.0, {'aegis': {'scale': [1, 1, 1], 'rot': [0, 0, 0]}, 'lid_top': {'pos': [0, -3.4, 0]}, 'lid_bottom': {'pos': [0, 3.4, 0]}}, 'easeOutBack'),
        (1.2, {'aegis': {'scale': [1, 1, 1], 'rot': [0, 0, 0]}}),
    ])
    r.clip('shielded', 4.0, True).poses([
        (0.0, merge(rings(0), {'aegis': {'rot': [0, 0, 0]}})),
        (4.0, merge(rings(1.0), {'aegis': {'rot': [0, 360, 0]}})),
    ], 'linear')
    # summon memory constructs: tablets flung outward at 1.0
    r.clip('summon', 2.0).poses([
        (0.0, {'fragments': {'scale': 1, 'rot': [0, 0, 0]}, 'eye': {'scale': 1}}),
        (0.8, {'fragments': {'scale': 0.6, 'rot': [0, 180, 0]}, 'eye': {'scale': 1.3}}, 'easeInCubic'),
        (1.0, {'fragments': {'scale': 1.8, 'rot': [0, 220, 0]}, 'eye': {'scale': 1}}, 'easeOutExpo'),
        (2.0, {'fragments': {'scale': 1, 'rot': [0, 360, 0]}}),
    ])
    # phase two: the eye cracks open wider; rings re-seat at new angles
    r.clip('awaken_memory', 3.0).poses([
        (0.0, merge(rings(0), {'core': {'pos': [0, 0, 0]}, 'eye': {'scale': 1}})),
        (1.0, merge(rings(0.2), {'core': {'pos': [0, -3, 0]}, 'eye': {'scale': 0.7}}), 'easeOutCubic'),
        (1.6, merge(rings(1.5), {'core': {'pos': [0, 4, 0]}, 'eye': {'scale': 1.6}}), 'easeOutBack'),
        (3.0, merge(rings(2.0), {'core': {'pos': [0, 0, 0]}, 'eye': {'scale': 1}})),
    ], 'linear')
    r.clip('hurt', 0.4).poses([(0.0, {'core': {'rot': [0, 0, 0]}}), (0.1, {'core': {'rot': [-8, 6, 4]}}, 'easeOutExpo'), (0.4, {'core': {'rot': [0, 0, 0]}})])
    # death: rings drop and shatter, the eye goes dark, the obelisk falls
    r.clip('death', 3.0, 'hold').poses([
        (0.0, merge(unfold, {'core': {'pos': [0, 0, 0], 'rot': [0, 0, 0], 'scale': 1}, 'eye': {'scale': 1},
                             'ring_a_tilt': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'ring_b_tilt': {'pos': [0, 0, 0], 'rot': [62, 0, 0]},
                             'ring_c_tilt': {'pos': [0, 0, 0], 'rot': [0, 0, 58]}})),
        (1.0, {'ring_a_tilt': {'pos': [0, -16, 0], 'rot': [8, 0, 6]}, 'ring_b_tilt': {'pos': [0, -12, 0], 'rot': [80, 0, 10]},
               'ring_c_tilt': {'pos': [0, -10, 0], 'rot': [10, 0, 80]}, 'eye': {'scale': 1.4}}, 'easeInCubic'),
        (2.0, {'core': {'pos': [0, -18, 0], 'rot': [0, 0, 70]}, 'eye': {'scale': 0.3}}, 'easeInExpo'),
        (3.0, {'core': {'pos': [0, -20, 0], 'rot': [0, 0, 90]}, 'eye': {'scale': 0}}, 'easeOutBounce'),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 0.6), ('volley', 0.9), ('beam', 1.5), ('teleport', 0.6), ('shield', 1.2), ('summon', 1.0), ('death', 2.0)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
