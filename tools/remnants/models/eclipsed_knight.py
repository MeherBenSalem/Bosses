"""The Eclipsed Knight — Champion of the Final War (Vael).

Vael's champion struck the Oracle during the Last Eclipse and was caught inside that moment. Lean,
long-limbed duelist silhouette: violet-black plate with pale silver trim, a helm whose ceremonial
crescent crest is broken, a torn wine-red cloak, a slender silver longsword, and behind the head
the eclipse itself: a dark disc with a burning corona that deepens in phase two.
Editor conventions as in hollow_sovereign.py.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paint import Material  # noqa: E402
from rig import Rig  # noqa: E402

NAME = 'eclipsed_knight'
PALETTE = {
    'eclipse': Material(['#060609', '#0D0D13', '#15151E', '#1F1F2B', '#2C2C3B', '#3E3E54', '#575774'], 'plate', wear=0.5),
    'silver': Material(['#383C46', '#565C68', '#7A818E', '#A3AAB6', '#CCD2DC', '#EEF2F6'], 'gold'),
    'corona': Material(['#5A3A10', '#9C6A1E', '#E0A845', '#FFE19A', '#FFF8E0', '#FFFFFF'], 'soul', glow=True),
    'umbra': Material(['#000000', '#030305', '#07070B', '#0C0C12'], 'void'),
}
TRIM = ('band', {'row': 0, 'size': 1, 'mat': 'silver'})
HEM = ('band', {'row': -1, 'size': 1, 'mat': 'silver'})


def build():
    r = Rig(NAME, (256, 128), palette=PALETTE)
    r.mark_upright('torso', 'head')
    r.bone('hips', (0, 24, 0))
    r.bone_pair('leg_{side}', (2.6, 23.5, 0), 'hips')
    r.bone_pair('shin_{side}', (2.8, 13, 0), 'leg_{side}')
    r.bone('torso', (0, 25.5, 0), 'hips')
    r.bone('head', (0, 38.5, 0), 'torso')
    r.bone('halo', (0, 42, 5.2), 'head')
    r.bone('corona', (0, 42, 5.4), 'halo')
    r.bone('cape', (0, 37.5, 3), 'torso')
    r.bone('cape_lower', (0, 26, 3.4), 'cape')
    r.bone_pair('arm_{side}', (5.6, 36.5, 0), 'torso')
    r.bone_pair('forearm_{side}', (6.2, 29.5, 0), 'arm_{side}')
    r.bone('sword', (-6.2, 22.5, -0.4), 'forearm_right')

    # long legs in articulated plate
    r.box('hips', 'faulds', (0, 23.6, 0), (8, 3, 5), 'eclipse', decals=[HEM])
    r.box('hips', 'tasset_front', (0, 20.6, -2.6), (5, 4, 0.8), 'eclipse', decals=[HEM])
    r.pair('leg_{side}', '{side}_cuisse', (2.8, 18.6, 0), (3.6, 9.4, 4), 'eclipse', decals=[TRIM])
    r.pair('shin_{side}', '{side}_poleyn', (2.8, 13, -2.1), (3.2, 2.6, 1.2), 'silver')
    r.pair('shin_{side}', '{side}_greave', (2.8, 7, 0.2), (3.6, 10, 3.8), 'eclipse',
           decals=[('vband', {'mat': 'silver', 'faces': 'north', 'size': 1})])
    r.pair('shin_{side}', '{side}_sabaton', (2.8, 1.2, -1), (3.8, 2.4, 6), 'eclipse', decals=[TRIM])

    # narrow-waisted cuirass with the eclipse sigil
    r.box('torso', 'waist', (0, 27.6, 0.2), (6.4, 4, 4), 'eclipse')
    r.box('torso', 'chest', (0, 33, 0.2), (9.6, 7, 5), 'eclipse', decals=[TRIM, ('filigree', {'mat': 'silver', 'row': 2, 'size': 1, 'faces': 'north'})])
    r.box('torso', 'sigil_disc', (0, 33.4, -2.45), (3, 3, 0.4), 'umbra')
    r.box('torso', 'sigil_rim', (0, 33.4, -2.35), (3.8, 3.8, 0.2), 'corona')
    r.box('torso', 'gorget', (0, 37.4, 0.2), (6, 1.8, 4.6), 'silver')
    r.pair('arm_{side}', '{side}_pauldron', (6.6, 37, 0), (4.6, 3.2, 5.6), 'eclipse', rot=(0, 0, -18), decals=[TRIM, HEM])
    r.pair('arm_{side}', '{side}_pauldron_fin', (7.8, 39.4, 0), (1, 2.4, 4.6), 'silver', rot=(0, 0, -30))
    r.pair('arm_{side}', '{side}_rerebrace', (6.2, 32.6, 0), (3, 6, 3.2), 'eclipse')
    r.pair('forearm_{side}', '{side}_vambrace', (6.2, 26.4, 0), (3.2, 6, 3.4), 'eclipse', decals=[TRIM])
    r.pair('forearm_{side}', '{side}_gauntlet', (6.2, 22.6, -0.3), (3, 2.6, 3.4), 'eclipse', decals=[('rivets', {'mat': 'silver', 'spacing': 2})])

    # visored helm; the crescent crest is broken off on the right
    r.box('head', 'helm', (0, 42.4, 0), (5.4, 6.6, 5.8), 'eclipse', decals=[HEM])
    r.box('head', 'visor', (0, 42, -3.05), (4.4, 3.4, 0.6), 'eclipse')
    r.box('head', 'visor_slit', (0, 42.8, -3.4), (3.6, 0.5, 0.2), 'umbra')
    r.pair('head', 'eye_{side}', (0.9, 42.8, -3.5), (0.8, 0.4, 0.2), 'corona')
    r.box('head', 'crest_base', (0, 46, 0.2), (1.2, 1, 5.4), 'silver')
    r.box('head', 'crest_horn_left', (2.6, 47.6, 0.2), (1, 4.6, 1.2), 'silver', rot=(0, 0, -30))
    r.box('head', 'crest_horn_left_tip', (4.4, 50.2, 0.2), (0.8, 2.2, 1), 'silver', rot=(0, 0, -12))
    r.box('head', 'crest_horn_right_stub', (-2.0, 47, 0.2), (1, 1.8, 1.2), 'silver', rot=(0, 0, 30))
    # eclipse halo: a black disc ringed by a burning corona (spins, flares in phase 2)
    r.box('halo', 'umbra_disc', (0, 42, 5.4), (12, 12, 0.4), 'umbra')
    r.box('halo', 'umbra_ring', (0, 42, 5.5), (13.4, 13.4, 0.2), 'corona')
    for i in range(12):
        a = 2 * math.pi * i / 12
        rad = 8.0 if i % 2 else 7.4
        x, y = math.sin(a) * rad, math.cos(a) * rad
        r.box('corona', f'flare_{i}', (x, 42 + y, 5.7), (1.4, 3.6 if i % 2 else 2.2, 0.3), 'corona', rot=(0, 0, -math.degrees(a)))

    # torn wine cloak
    r.box('cape', 'cape_upper', (0, 32, 3.5), (9, 11, 0.6), 'royal_lining', decals=[('band', {'row': 0, 'size': 1, 'mat': 'silver'})])
    r.box('cape_lower', 'cape_tail', (0, 18.6, 3.8), (8.4, 15, 0.6), 'royal_lining')
    r.box('cape_lower', 'cape_shred', (2.6, 9, 3.8), (2.4, 4.6, 0.6), 'royal_lining')

    # slender silver longsword, eclipse gem in the guard
    w = -6.2
    r.box('sword', 'grip', (w, 23, -0.4), (1, 6, 1), 'leather')
    r.box('sword', 'pommel', (w, 26.6, -0.4), (1.6, 1.4, 1.6), 'silver')
    r.box('sword', 'guard', (w, 19.6, -0.4), (6.4, 1, 1.2), 'silver')
    r.box('sword', 'guard_gem', (w, 19.6, -1.1), (1.2, 1.2, 0.3), 'corona')
    r.box('sword', 'blade', (w, 7.6, -0.4), (1.8, 23, 0.6), 'blade')
    r.box('sword', 'fuller', (w, 9, -0.4), (0.5, 18, 0.7), 'umbra')
    r.box('sword', 'tip', (w, -4.6, -0.4), (1, 1.6, 0.6), 'blade')

    clips(r)
    return r


def merge(*poses):
    out = {}
    for p in poses:
        for bone, chans in p.items():
            out.setdefault(bone, {}).update(chans)
    return out


def clips(r):
    # ready stance: blade low and forward, weight on the back foot
    ready = {'arm_right': {'rot': [36, 0, 8]}, 'forearm_right': {'rot': [30, 0, 0]}, 'sword': {'rot': [40, 0, 0]},
             'arm_left': {'rot': [10, 0, 14]}, 'forearm_left': {'rot': [26, 0, 0]}, 'torso': {'rot': [4, 14, 0]},
             'head': {'rot': [0, -14, 0]}, 'leg_left': {'rot': [12, 0, 0]}, 'leg_right': {'rot': [-10, 0, 0]},
             'shin_left': {'rot': [-6, 0, 0]}, 'shin_right': {'rot': [-8, 0, 0]}, 'hips': {'pos': [0, -0.6, 0]}}
    halo_still = {'corona': {'rot': [0, 0, 0], 'scale': 1}}
    r.clip('idle', 3.0, True).poses([
        (0.0, merge(ready, halo_still, {'cape_lower': {'rot': [0, 0, 0]}})),
        (1.5, {'torso': {'rot': [6, 16, 0]}, 'corona': {'rot': [0, 0, 60], 'scale': 1.06}, 'cape_lower': {'rot': [-6, 0, 2]}}),
        (3.0, {'torso': {'rot': [4, 14, 0]}, 'corona': {'rot': [0, 0, 120], 'scale': 1}, 'cape_lower': {'rot': [0, 0, 0]}}),
    ])
    r.clips['idle'].seam_period['corona'] = 60   # twelve flares alternating in size: a 60-degree symmetry

    def stride(name, length, swing, lift, lean, cape):
        h, q = length / 2, length / 4
        base = {'arm_right': {'rot': [24, 0, 8]}, 'forearm_right': {'rot': [26, 0, 0]}, 'sword': {'rot': [50, 0, 0]},
                'torso': {'rot': [lean, 0, 0]}, 'head': {'rot': [-lean * 0.5, 0, 0]}}
        r.clip(name, length, True).poses([
            (0.0, merge(base, {'leg_left': {'rot': [swing, 0, 0]}, 'leg_right': {'rot': [-swing, 0, 0]}, 'shin_left': {'rot': [0, 0, 0]},
                               'shin_right': {'rot': [-6, 0, 0]}, 'arm_left': {'rot': [-swing * 0.7, 0, 10]}, 'hips': {'pos': [0, -0.6, 0]},
                               'cape_lower': {'rot': [-cape, 0, 0]}})),
            (q, {'shin_right': {'rot': [-lift, 0, 0]}, 'hips': {'pos': [0, 0.6, 0]}, 'cape_lower': {'rot': [-cape * 1.4, 0, 0]}}),
            (h, {'leg_left': {'rot': [-swing, 0, 0]}, 'leg_right': {'rot': [swing, 0, 0]}, 'shin_left': {'rot': [-6, 0, 0]},
                 'shin_right': {'rot': [0, 0, 0]}, 'arm_left': {'rot': [swing * 0.7, 0, 10]}, 'hips': {'pos': [0, -0.6, 0]},
                 'cape_lower': {'rot': [-cape, 0, 0]}}),
            (h + q, {'shin_left': {'rot': [-lift, 0, 0]}, 'hips': {'pos': [0, 0.6, 0]}, 'cape_lower': {'rot': [-cape * 1.4, 0, 0]}}),
            (length, {'leg_left': {'rot': [swing, 0, 0]}, 'leg_right': {'rot': [-swing, 0, 0]}, 'shin_left': {'rot': [0, 0, 0]},
                      'shin_right': {'rot': [-6, 0, 0]}, 'arm_left': {'rot': [-swing * 0.7, 0, 10]}, 'hips': {'pos': [0, -0.6, 0]},
                      'cape_lower': {'rot': [-cape, 0, 0]}}),
        ])
    stride('walk', 1.2, 20, 30, 4, 8)
    stride('run', 0.7, 34, 56, 14, 30)

    # three fast cuts: hits at 0.35, 0.65, 0.95
    r.clip('combo', 1.5).poses([
        (0.0, ready),
        (0.25, {'arm_right': {'rot': [150, 0, -30]}, 'sword': {'rot': [0, 0, 0]}, 'torso': {'rot': [0, 30, 0]}}, 'easeOutCubic'),
        (0.35, {'arm_right': {'rot': [60, 0, 30]}, 'sword': {'rot': [30, 0, 0]}, 'torso': {'rot': [8, -26, 0]}, 'hips': {'pos': [0, -0.8, -1]}}, 'easeInExpo'),
        (0.55, {'arm_right': {'rot': [90, 0, 50]}, 'sword': {'rot': [0, 0, 60]}, 'torso': {'rot': [4, -34, 0]}}, 'easeOutSine'),
        (0.65, {'arm_right': {'rot': [80, 0, -50]}, 'sword': {'rot': [0, 0, -60]}, 'torso': {'rot': [6, 30, 0]}}, 'easeInExpo'),
        (0.85, {'arm_right': {'rot': [40, 0, 10]}, 'sword': {'rot': [80, 0, 0]}, 'torso': {'rot': [10, 10, 0]}}, 'easeOutSine'),
        (0.95, {'arm_right': {'rot': [92, 0, 2]}, 'sword': {'rot': [88, 0, 0]}, 'torso': {'rot': [16, 0, 0]}, 'hips': {'pos': [0, -1.4, -2.4]},
                'leg_left': {'rot': [30, 0, 0]}, 'leg_right': {'rot': [-26, 0, 0]}}, 'easeInExpo'),
        (1.5, ready),
    ])
    r.clip('lunge', 1.3).poses([
        (0.0, ready),
        (0.5, {'arm_right': {'rot': [-20, 0, -10]}, 'sword': {'rot': [80, 0, 0]}, 'torso': {'rot': [22, 18, 0]}, 'hips': {'pos': [0, -2.4, 1]},
               'leg_left': {'rot': [36, 0, 0]}, 'shin_left': {'rot': [-40, 0, 0]}}, 'easeOutCubic'),
        (0.7, {'arm_right': {'rot': [94, 0, 0]}, 'sword': {'rot': [86, 0, 0]}, 'torso': {'rot': [26, -10, 0]}, 'hips': {'pos': [0, -1.4, -3]},
               'leg_left': {'rot': [44, 0, 0]}, 'leg_right': {'rot': [-40, 0, 0]}, 'cape_lower': {'rot': [-44, 0, 0]}}, 'easeInExpo'),
        (1.3, merge(ready, {'cape_lower': {'rot': [0, 0, 0]}})),
    ])
    parry = {'arm_right': {'rot': [80, 0, 40]}, 'forearm_right': {'rot': [50, 0, 0]}, 'sword': {'rot': [0, -16, 82]},
             'torso': {'rot': [2, -16, 0]}, 'hips': {'pos': [0, -1, 0]}, 'arm_left': {'rot': [30, 0, 30]}}
    r.clip('parry', 1.0).poses([(0.0, ready), (0.12, parry, 'easeOutBack'), (0.8, parry), (1.0, ready)])
    r.clip('riposte', 0.6).poses([
        (0.0, parry),
        (0.2, {'arm_right': {'rot': [96, 0, -20]}, 'forearm_right': {'rot': [4, 0, 0]}, 'sword': {'rot': [86, 0, -10]},
               'torso': {'rot': [16, 24, 0]}, 'hips': {'pos': [0, -1.4, -2]}}, 'easeInExpo'),
        (0.6, ready),
    ])
    for name, s in (('sidestep_left', 1), ('sidestep_right', -1)):
        r.clip(name, 0.6).poses([
            (0.0, ready),
            (0.25, {'hips': {'pos': [s * 4, -1.4, 0]}, 'torso': {'rot': [4, 14, -s * 12]}, 'leg_left': {'rot': [0, 0, s * 20]},
                    'leg_right': {'rot': [0, 0, s * 20]}, 'cape_lower': {'rot': [0, 0, -s * 30]}}, 'easeOutExpo'),
            (0.6, merge(ready, {'cape_lower': {'rot': [0, 0, 0]}})),
        ])
    # the eclipse deepens: the corona flares and the disc swallows the light
    r.clip('eclipse', 3.0).poses([
        (0.0, merge(ready, {'halo': {'scale': 1}, 'corona': {'scale': 1, 'rot': [0, 0, 0]}})),
        (1.0, {'torso': {'rot': [-12, 0, 0]}, 'head': {'rot': [-24, 0, 0]}, 'arm_left': {'rot': [40, 0, 60]},
               'arm_right': {'rot': [40, 0, -60]}, 'halo': {'scale': 1.6}, 'corona': {'scale': 1.3, 'rot': [0, 0, 180]}}, 'easeOutCubic'),
        (2.0, {'halo': {'scale': 2.0}, 'corona': {'scale': 1.6, 'rot': [0, 0, 360]}}, 'easeOutBack'),
        (3.0, merge(ready, {'halo': {'scale': 1.3}, 'corona': {'scale': 1.2, 'rot': [0, 0, 540]}})),
    ])
    r.clip('hurt', 0.4).poses([(0.0, ready), (0.1, {'torso': {'rot': [-12, 20, 0]}, 'head': {'rot': [-12, -14, 0]}}, 'easeOutExpo'), (0.4, ready)])
    r.clip('stagger', 1.0).poses([
        (0.0, ready),
        (0.2, {'torso': {'rot': [-18, 8, 8]}, 'hips': {'pos': [0, -1, 2]}, 'arm_right': {'rot': [0, 0, -40]}, 'sword': {'rot': [20, 0, -40]}}, 'easeOutExpo'),
        (1.0, ready),
    ])
    # death: kneels, plants the blade, the eclipse passes and the corona goes out
    r.clip('death', 4.0, 'hold').poses([
        (0.0, merge(ready, {'halo': {'scale': 1}, 'corona': {'scale': 1}})),
        (0.6, {'torso': {'rot': [-14, 0, 0]}, 'head': {'rot': [-20, 0, 0]}}, 'easeOutCubic'),
        (1.8, {'hips': {'pos': [0, -9, 0]}, 'leg_left': {'rot': [80, 0, 0]}, 'shin_left': {'rot': [-84, 0, 0]},
               'leg_right': {'rot': [-6, 0, 0]}, 'shin_right': {'rot': [-88, 0, 0]}, 'torso': {'rot': [16, 0, 0]},
               'arm_right': {'rot': [60, 0, -4]}, 'forearm_right': {'rot': [10, 0, 0]}, 'sword': {'rot': [-34, 0, 0]},
               'arm_left': {'rot': [54, 0, -24]}, 'forearm_left': {'rot': [40, 0, 0]}, 'head': {'rot': [26, 0, 0]}}, 'easeInOutSine'),
        (3.2, {'corona': {'scale': 0.2}, 'halo': {'scale': 0.6}}),
        (4.0, {'corona': {'scale': 0}, 'halo': {'scale': 0}}),
    ])


if __name__ == '__main__':
    from bbmcp import Blockbench
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants' / NAME
    rig = build()
    problems = rig.validate()
    if problems:
        raise SystemExit('\n'.join(problems))
    poses = [('idle', 0), ('run', 0.18), ('combo', 0.25), ('combo', 0.95), ('lunge', 0.7), ('parry', 0.5), ('sidestep_left', 0.25),
             ('eclipse', 2.0), ('death', 4.0)]
    print(rig.build(Blockbench(), out, preview_clips=poses))
