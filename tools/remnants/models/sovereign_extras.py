"""Hollow Sovereign helpers: the flying pauldron (phase 2) and the spectral crescent projectile."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from rig import Rig  # noqa: E402

GOLD_RIM = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
GOLD_HEM = ('band', {'row': -1, 'size': 1, 'mat': 'gold'})


def pauldron():
    """One of the Sovereign's floating shoulder plates, torn loose and driven by his soul."""
    r = Rig('sovereign_pauldron', (64, 64))
    r.bone('body', (0, 6, 0))
    r.bone('wisps', (0, 2, 0), 'body')
    r.box('body', 'plate', (0, 7.5, 0), (9, 6, 11), 'iron', rot=(0, 0, -14), decals=[GOLD_RIM, GOLD_HEM])
    r.box('body', 'lame', (1, 3.8, 0), (8, 3, 10), 'iron', rot=(0, 0, -22),
          decals=[GOLD_HEM, ('glow_edge', {'mat': 'soul', 'edge': 'bottom', 'width': 1, 'strength': 0.35})])
    r.box('body', 'ridge', (-0.4, 11.1, 0), (5, 1.5, 9), 'gold', rot=(0, 0, -14))
    r.box('body', 'binding', (-1, 4.2, 0), (4, 1.2, 6), 'soul')
    for i, (x, z, h) in enumerate([(-2, -2, 3), (1, 1.5, 4), (-0.5, 2.5, 2.5), (2, -1.5, 2)]):
        r.box('wisps', f'wisp_{i}', (x, 1.8 - h / 2, z), (0.8, h, 0.8), 'soul_dim')
    r.clip('idle', 2.0, True).poses([
        (0.0, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'wisps': {'scale': 1}}),
        (1.0, {'body': {'pos': [0, 1.2, 0], 'rot': [4, 0, -3]}, 'wisps': {'scale': [1, 1.3, 1]}}),
        (2.0, {'body': {'pos': [0, 0, 0], 'rot': [0, 0, 0]}, 'wisps': {'scale': 1}}),
    ])
    shake = [(i * 0.1, {'body': {'rot': [(-1) ** i * 6, 0, (-1) ** (i + 1) * 5], 'pos': [0, 1 + 0.15 * i, 0]},
                        'wisps': {'scale': [1, 1 + 0.1 * i, 1]}}, 'linear') for i in range(10)]
    r.clip('telegraph', 1.0).poses([(0.0, {'body': {'rot': [0, 0, 0], 'pos': [0, 0, 0]}, 'wisps': {'scale': 1}})] + shake[1:] +
                                   [(1.0, {'body': {'rot': [-30, 0, 0], 'pos': [0, 2, 0]}, 'wisps': {'scale': [1, 2, 1]}})])
    r.clip('dive', 0.5, True).poses([
        (0.0, {'body': {'rot': [-30, 0, 0]}, 'wisps': {'scale': [1, 2, 1]}}),
        (0.25, {'body': {'rot': [-30, 0, 6]}, 'wisps': {'scale': [1, 2.4, 1]}}),
        (0.5, {'body': {'rot': [-30, 0, 0]}, 'wisps': {'scale': [1, 2, 1]}}),
    ])
    return r


def crescent():
    """A horizontal crescent of remnant light, about 1.6 blocks across, travelling toward -Z."""
    r = Rig('spectral_crescent', (64, 32))
    r.bone('arc', (0, 2, 0))
    radius = 11
    for i, deg in enumerate(range(-70, 71, 20)):
        a = math.radians(deg)
        x, z = math.sin(a) * radius, -math.cos(a) * radius + radius - 2
        thick = 1.8 - abs(deg) / 70 * 1.0
        r.box('arc', f'seg_{i}', (x, 2, z), (4.2, thick, 1.6 if abs(deg) < 50 else 1.0), 'soul', rot=(0, -deg, 0))
    for i, deg in enumerate(range(-50, 51, 25)):
        a = math.radians(deg)
        x, z = math.sin(a) * (radius + 1.4), -math.cos(a) * (radius + 1.4) + radius - 2
        r.box('arc', f'edge_{i}', (x, 2, z), (3.6, 0.6, 0.6), 'soul_dim', rot=(0, -deg, 0))
    r.clip('spin', 0.5, True).poses([
        (0.0, {'arc': {'rot': [0, 0, -6], 'scale': [1, 1, 1]}}),
        (0.25, {'arc': {'rot': [0, 0, 6], 'scale': [1.06, 1.2, 1.06]}}),
        (0.5, {'arc': {'rot': [0, 0, -6], 'scale': [1, 1, 1]}}),
    ])
    return r


MODELS = {'sovereign_pauldron': (pauldron, [('telegraph', 0.6), ('dive', 0.1)]),
          'spectral_crescent': (crescent, [('spin', 0.25)])}

if __name__ == '__main__':
    from bbmcp import Blockbench
    out_root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3] / 'docs/models/remnants'
    bb = Blockbench()
    for name in sys.argv[2:] or MODELS:
        fn, poses = MODELS[name]
        rig = fn()
        problems = rig.validate()
        if problems:
            raise SystemExit('\n'.join(problems))
        print(name, rig.build(bb, Path(out_root) / name, preview_clips=poses), flush=True)
