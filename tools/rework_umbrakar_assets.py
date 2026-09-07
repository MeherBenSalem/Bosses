"""Build Umbrakar's articulated regalia and authored animation clips for both ports.

Retains the original UV atlas and base rig. Safe to rerun: regalia bones are replaced.
Animation time is in seconds (20 server ticks per second).
"""
import copy
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET = Path('common/src/main/resources/assets/remnant_bosses')


def cube(origin, size, rotation=(0, 0, 0), pivot=None, bright=False):
    # Existing opaque teal armour / pale blue facet regions; no new atlas required.
    uv = [115, 62] if bright else [29, 0]
    return dict(origin=origin, size=size, pivot=pivot or origin, rotation=list(rotation),
                uv={face: dict(uv=uv, uv_size=[3, 4])
                    for face in ('north', 'south', 'east', 'west', 'up', 'down')})


def geometry(source):
    geo = copy.deepcopy(source)
    bones = geo['minecraft:geometry'][0]['bones']
    bones[:] = [b for b in bones if not b['name'].startswith('regalia_')]
    # A broken crown silhouette, with swept layered plates framing the face.
    for side in (-1, 1):
        name = 'regalia_mane_' + ('left' if side == 1 else 'right')
        plates = []
        for i in range(4):
            x, y, z = side * (9 + i * 2.2), 29 - i * 4, -5 + i * 2
            plates.append(cube([x - 2.5, y - 5, z], [5, 12 - i, 9],
                               [22 + i * 5, side * 12, -side * (22 + i * 9)], [x, y, z]))
            plates.append(cube([x - 1, y + 2, z - .3], [2, 5, 2],
                               [22, 0, -side * 30], [x, y, z], True))
        bones.append(dict(name=name, parent='h_head', pivot=[side * 9, 25, 0], cubes=plates))
        bones.append(dict(name='regalia_crown_' + str(side), parent='h_head', pivot=[side * 6, 29, -4],
                          cubes=[cube([side * 6 - 2, 28, -5], [4, 11, 5], [-18, 0, -side * 18]),
                                 cube([side * 9 - 1, 36, -2], [2, 7, 3], [-25, 0, -side * 12], bright=True)]))
        # Long upper canines keep the lower jaw free to articulate.
        bones.append(dict(name='regalia_fang_' + str(side), parent='h_head', pivot=[side * 7, 19, -20],
                          cubes=[cube([side * 7 - 1, 12, -21], [2, 8, 2.5], [-12, 0, side * 7], bright=True)]))
        bones.append(dict(name='regalia_pauldron_' + str(side),
                          parent='left_frontleg' if side == 1 else 'right_frontleg',
                          pivot=[side * 12, 27, 12], cubes=[
                              cube([side * 14 - 4, 25, 8], [8, 6, 12], [0, 0, -side * 18]),
                              cube([side * 17 - 1.5, 29, 12], [3, 9, 4], [-22, 0, -side * 25], bright=True)]))
    bones.append(dict(name='regalia_crest', parent='body', pivot=[0, 37, 21], cubes=[
        cube([-2.5, 36 - i, 15 + i * 9], [5, 9 - i, 6], [25, 0, 0], bright=i % 2 == 0)
        for i in range(4)]))
    bones.append(dict(name='regalia_tail_crown', parent='endtail', pivot=[0, 35, 110], cubes=[
        cube([-2, 32, 109], [4, 10, 9], [25, 0, 0], bright=True),
        cube([-7, 34, 108], [14, 3, 7], [0, 0, 0])]))
    return geo


def track(times, values):
    # Bake the spline: GeckoLib 4.4 drops Bedrock's lerp_mode on post keyframes.
    # Explicit vector keys interpolate identically on both supported GeckoLib ports.
    samples = sorted(set(times + [round(i / 30, 5) for i in range(math.ceil(times[-1] * 30))]))
    result = {}
    for t in samples:
        i = next((j for j in range(1, len(times)) if times[j] >= t), len(times) - 1)
        u = (t - times[i - 1]) / (times[i] - times[i - 1])
        p0, p1, p2, p3 = values[max(0, i - 2)], values[i - 1], values[i], values[min(len(values) - 1, i + 1)]
        value = [.5 * (2 * p1[k] + (-p0[k] + p2[k]) * u
                 + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * u * u
                 + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * u ** 3) for k in range(3)]
        # Held windups remain still; avoid spline overshoot during a telegraph.
        if p1 == p2:
            value = p1
        result[str(round(t, 5))] = {'vector': [round(n, 4) for n in value]}
    return result


def animations():
    clips = {}
    def clip(name, duration, loop=False):
        value = dict(loop=loop, animation_length=duration, bones={})
        clips[name] = value
        return value['bones']

    def add(bones, name, times, values, channel='rotation'):
        bones.setdefault(name, {})[channel] = track(times, values)

    # Slow breathing and travelling tail waves. Loops meet at identical poses.
    for name, duration, stride in [('idle', 3.2, 0), ('walk', 1.6, 22), ('run', .9, 34)]:
        b = clip(name, duration, True)
        ts = [duration * i / 8 for i in range(9)]
        wave = lambda amplitude, phase=0: [round(amplitude * math.sin(2 * math.pi * i / 8 + phase), 4) for i in range(9)]
        add(b, 'body', ts, [[0, y, 0] for y in wave(.65 if not stride else 1.25)], 'position')
        add(b, 'body', ts, [[x, 0, z] for x, z in zip(wave(1.2), wave(0 if not stride else 1.5))])
        add(b, 'h_head', ts, [[x - 3, 0, 0] for x in wave(1.7)])
        add(b, 'h_jaw', ts, [[x + 3, 0, 0] for x in wave(1)])
        for j, tail in enumerate(['tail', 'premidtail', 'midtail', 'endtail']):
            add(b, tail, ts, [[-3, y, 0] for y in wave(5 + j * 2, -j * .55)])
        for side in ('left', 'right'):
            sign = 1 if side == 'left' else -1
            add(b, 'regalia_mane_' + side, ts, [[x, 0, sign * 2] for x in wave(2, -.4)])
        if stride:
            for limb, phase in [('left_frontleg', 0), ('right_frontleg', math.pi),
                                ('left_backleg', math.pi), ('right_backleg', 0)]:
                add(b, limb, ts, [[x, 0, 0] for x in wave(stride, phase)])
                foot = limb.replace('frontleg', 'foot').replace('backleg', 'backfoot')
                add(b, foot, ts, [[-x * .55, 0, 0] for x in wave(stride, phase)])

    # Anticipation / held charge / fast contact / recoil / settle. Contact matches Java.
    specs = {
        'bite': (1.25, .5, [-4, 0, -3], [7, 0, 3], [-12, 0, 0], [17, 0, 0]),
        'frontslam': (2.5, 1.1, [-24, 0, 0], [11, 0, 0], [-15, 0, 0], [14, 0, 0]),
        'tailslam': (2, .9, [0, -15, -5], [0, 19, 6], [0, 12, 0], [0, -16, 0]),
        'tailorb': (1.75, .8, [-6, 0, 0], [4, 0, 0], [-9, 0, 0], [5, 0, 0]),
        'roar': (3.5, 1.2, [-12, 0, 0], [-7, 0, 0], [-25, 0, 0], [-32, 0, 0]),
        'riftclaw': (2.6, 1.2, [-8, -12, -8], [10, 14, 9], [-5, 10, 0], [13, -12, 0]),
    }
    for name, (duration, hit, wind, strike, headwind, headstrike) in specs.items():
        b = clip(name, duration)
        ts = [0, hit * .6, hit - .1, hit, hit + .16, duration - .25, duration]
        zero = [0, 0, 0]
        add(b, 'body', ts, [zero, wind, wind, strike, [v * .65 for v in strike], zero, zero])
        lift = 4 if name == 'frontslam' else 1.5
        add(b, 'body', ts, [zero, [0, lift, 1], [0, lift, 1], [0, -1.8, -1], [0, -.6, 0], zero, zero], 'position')
        add(b, 'h_head', ts, [zero, headwind, headwind, headstrike, headstrike, zero, zero])
        jaws = [0, 24, 32, 4, 10, 0, 0] if name == 'bite' else [0, 12, 20, 32 if name == 'roar' else 16, 24, 0, 0]
        add(b, 'h_jaw', ts, [[x, 0, 0] for x in jaws])
        for side in ('left', 'right'):
            sign = 1 if side == 'left' else -1
            raised = -65 if name == 'frontslam' else (-48 if name == 'riftclaw' and side == 'right' else -8)
            add(b, side + '_frontleg', ts, [zero, [raised, 0, sign * 8], [raised, 0, sign * 8], [14, 0, 0], [6, 0, 0], zero, zero])
            add(b, side + '_foot', ts, [zero, [20, 0, 0], [20, 0, 0], [-10, 0, 0], zero, zero, zero])
            add(b, side + '_backleg', ts, [zero, [8, 0, sign * 4], [8, 0, sign * 4], [-5, 0, 0], zero, zero, zero])
            add(b, side + '_backfoot', ts, [zero, [-5, 0, 0], [-5, 0, 0], [5, 0, 0], zero, zero, zero])
            add(b, 'regalia_mane_' + side, ts, [zero, [-6, 0, sign * 8], [-8, 0, sign * 10], [12, 0, sign * 18], [-6, 0, sign * 10], zero, zero])
        for j, tail in enumerate(['tail', 'premidtail', 'midtail', 'endtail']):
            yaw = 24 if name == 'tailslam' else 4
            pitch = -16 if name == 'tailorb' else -6
            add(b, tail, ts, [zero, [pitch, -yaw, 0], [pitch - j * 2, -yaw, 0], [6, yaw, 0], [-3, yaw * .5, 0], zero, zero])
    b = clip('death', 4.6)
    ts = [0, .4, 1.1, 1.7, 2.1, 3.2, 4.6]
    add(b, 'body', ts, [[0, 0, 0], [0, -3, 0], [0, -5, 0], [0, -12, 0], [0, -11, 0], [0, -12, 0], [0, -12, 0]], 'position')
    add(b, 'body', ts, [[0, 0, 0], [-5, 0, 5], [4, 0, 12], [0, 0, 35], [0, 0, 30], [0, 0, 32], [0, 0, 32]])
    add(b, 'h_head', ts, [[0, 0, 0], [-12, 0, 0], [10, 0, 0], [18, -8, 0], [12, -8, 0], [16, -8, 0], [16, -8, 0]])
    add(b, 'h_jaw', ts, [[0, 0, 0], [22, 0, 0], [12, 0, 0], [18, 0, 0], [14, 0, 0], [14, 0, 0], [14, 0, 0]])
    for side in ('left', 'right'):
        for limb in ('frontleg', 'backleg'):
            add(b, side + '_' + limb, ts, [[0, 0, 0], [10, 0, 0], [-20, 0, 0], [-65, 0, 0], [-58, 0, 0], [-60, 0, 0], [-60, 0, 0]])
    for tail in ('tail', 'premidtail', 'midtail', 'endtail'):
        add(b, tail, ts, [[0, 0, 0], [-8, 5, 0], [5, -8, 0], [10, 12, 0], [6, 9, 0], [8, 10, 0], [8, 10, 0]])
    return dict(format_version='1.8.0', animations=clips)


if __name__ == '__main__':
    source = json.loads((ROOT / '1.21.1' / ASSET / 'geo/entity/umbrakar.geo.json').read_text())
    for version in ('1.20.1', '1.21.1'):
        base = ROOT / version / ASSET
        (base / 'geo/entity/umbrakar.geo.json').write_text(json.dumps(geometry(source), indent=2) + '\n')
        (base / 'animations/entity/umbrakar.animation.json').write_text(json.dumps(animations(), indent=2) + '\n')
    print('Built matching Umbrakar geometry and 10 animation clips for both ports.')
