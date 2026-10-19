"""Seyr helper assets, generated without Blockbench.

  python tools/remnants/models/seyr_helpers.py            # everything below
  python tools/remnants/models/seyr_helpers.py aegis      # only the aegis split + Watcher previews
  python tools/remnants/models/seyr_helpers.py illusion | helpers | previews

- Aegis split: the Watcher's `aegis` bone (six plates) gets one child bone per plate, `aegis_0` ...
  `aegis_5` (pivot = the aegis pivot, no rotation), plate i at angle 2*pi*i/6 (plate 0 at +Z). Cube
  JSON is moved unchanged, so the geometry and UVs stay byte-identical. Idempotent; a Blockbench rebuild
  with forgotten_watcher.py already emits the per-plate bones.
- Illusion texture: forgotten_watcher_illusion.png, the Watcher atlas recoloured paler and more violet
  (same UVs, opaque where the original is opaque; the illusion renderer applies the alpha).
- memory_bolt and soul_wisp: small GeckoLib models (geo 1.12.0, animation, 32x16 atlas + glowmask).
- Previews rendered with tools/remnants/realm/render3d.py, which reads the files the way GeckoLib does.

Geometry is authored in render space (the space GeckoLib draws in after mirroring bedrock X) and
exported with x' = -x, as in realm/waygate.py. Face textures are painted "solid": a painter receives
the model-space position of each texel centre on the face, so neighbouring faces line up.
"""
import copy
import hashlib
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'realm'))
import render3d as R  # noqa: E402

ROOT = HERE.parents[2]
DOCS = ROOT / 'docs/models/remnants'
WATCHER = DOCS / 'forgotten_watcher'
BG = (46, 50, 64, 255)
AEGIS_PIVOT = [0, 34, 0]


def hexc(value, alpha=255):
    value = value.lstrip('#')
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def ramp(*values):
    return [hexc(v) for v in values]


# Seyr ramps + remnant light
VIOLET = ramp('#2A1F4A', '#45337A', '#6A52B0', '#9E86E0', '#D2C6FF')
SOUL = ramp('#2E8C8C', '#4FC9C1', '#9CF2E6', '#E0FFFA')
SOUL_DEEP = ramp('#0E3A3C', '#1C5E60')


def r4(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def write_json(path, data):
    Path(path).write_text(json.dumps(data, indent=2), encoding='utf-8')


def plate_index(cube):
    """Angle index of an aegis plate cube (bedrock JSON; render x = -bedrock x)."""
    if 'pivot' in cube:
        bx, _, bz = cube['pivot']
    else:
        bx = cube['origin'][0] + cube['size'][0] / 2
        bz = cube['origin'][2] + cube['size'][2] / 2
    a = math.atan2(-bx, bz)
    return int(round(a / (math.pi / 3))) % 6


def split_aegis(path=WATCHER / 'forgotten_watcher.geo.json'):
    geo = json.loads(Path(path).read_text(encoding='utf-8'))
    bones = geo['minecraft:geometry'][0]['bones']
    names = [b['name'] for b in bones]
    if 'aegis_0' in names:
        return 'aegis already split'
    k = names.index('aegis')
    aegis = bones[k]
    cubes = aegis.pop('cubes', [])
    if len(cubes) != 6:
        raise SystemExit(f'aegis has {len(cubes)} cubes, expected 6')
    by_index = {}
    for c in cubes:
        i = plate_index(c)
        if i in by_index:
            raise SystemExit(f'two aegis plates at index {i}')
        by_index[i] = c
    children = [{'name': f'aegis_{i}', 'parent': 'aegis', 'pivot': list(AEGIS_PIVOT), 'cubes': [by_index[i]]}
                for i in range(6)]
    bones[k + 1:k + 1] = children
    write_json(path, geo)
    return 'aegis split into aegis_0..aegis_5'


# Original ramps (tools/remnants/paint.py PALETTE) -> paler, more violet, spectral ramps of equal length.
ILLUSION_MAP = {
    'sandstone': (['#3E382C', '#5A5241', '#7C7259', '#A09574', '#C2B894', '#DED6B9'],
                  ['#46425E', '#615C7C', '#7F7898', '#9F98B4', '#C1BAD0', '#E4DDEC']),   # pale lilac-grey
    'verdigris': (['#16302B', '#22463F', '#336657', '#4B8B73', '#6FAE95', '#9CCDB6'],
                  ['#373066', '#4D4386', '#685DA6', '#877BC2', '#A99EDA', '#CEC5F0']),   # pale violet
    'crystal': (['#1E1438', '#33255E', '#4D3A8E', '#6E58BA', '#9D86E0', '#CDBFFF', '#F1EBFF'],
                ['#3B2D72', '#5643A0', '#7560C8', '#977FE4', '#BBA6F6', '#DDD2FF', '#FBF8FF']),  # brighter
    'gold': (['#2A1E0C', '#4A3516', '#6E5021', '#95702E', '#BC9440', '#DDBA5E', '#F6E19A'],
             ['#3C3A58', '#4C4A6C', '#64628A', '#7E7DA4', '#A2A2C6', '#C6C6E2', '#ECECFF']),     # spectral silver
    'void': (['#030406', '#06070B', '#0A0C12', '#10131A', '#171B25'],
             ['#120E22', '#17122B', '#1D1734', '#241D40', '#2B234B']),
}


def illusion_texture():
    src = Image.open(WATCHER / 'forgotten_watcher.png').convert('RGBA')
    a = np.asarray(src).copy()
    lut = {}
    for old, new in ILLUSION_MAP.values():
        for o, n in zip(old, new):
            lut[hexc(o)[:3]] = hexc(n)[:3]
    fallback_ramp = [hexc(c)[:3] for c in ILLUSION_MAP['sandstone'][1]]
    out = np.zeros_like(a)
    opaque = a[..., 3] > 0
    misses = set()
    for y, x in zip(*np.nonzero(opaque)):
        rgb = tuple(int(v) for v in a[y, x, :3])
        n = lut.get(rgb)
        if n is None:   # unknown colour: keep its brightness, take the lilac-grey ramp's hue
            misses.add(rgb)
            lum = (0.3 * rgb[0] + 0.59 * rgb[1] + 0.11 * rgb[2]) / 255
            n = fallback_ramp[min(len(fallback_ramp) - 1, int(lum * len(fallback_ramp)))]
        out[y, x] = (*n, 255)
    Image.fromarray(out, 'RGBA').save(WATCHER / 'forgotten_watcher_illusion.png')
    return f'illusion texture written ({int(opaque.sum())} px, {len(misses)} unmapped colours)'


FACE_DIMS = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}


class Model:
    def __init__(self, name, tw, th, bounds=(1, 1, 0.25)):
        self.name, self.tw, self.th, self.bounds = name, tw, th, bounds
        self.bones, self.cubes = [], []

    def bone(self, name, pivot, parent='root'):
        self.bones.append({'name': name, 'pivot': tuple(pivot), 'parent': parent if name != 'root' else None})

    def box(self, bone, lo, hi, painter, rot=None, pivot=None, skip=()):
        """painter(face, x, y, z) -> (rgba, glow) or None, given the texel centre in model space."""
        self.cubes.append({'bone': bone, 'lo': tuple(lo), 'hi': tuple(hi), 'painter': painter,
                           'rot': rot, 'pivot': pivot, 'skip': set(skip)})

    def _paint_face(self, c, face):
        lo, hi = c['lo'], c['hi']
        size = [h - l for l, h in zip(lo, hi)]
        ia, ib = FACE_DIMS[face]
        w, h = size[ia], size[ib]
        if abs(w - round(w)) > 1e-6 or abs(h - round(h)) > 1e-6:
            raise ValueError(f'{self.name}: face {face} of {lo}-{hi} is not a whole number of texels')
        w, h = int(round(w)), int(round(h))
        tl, tr, bl = (np.array(p, dtype=float) for p in R.FACE_CORNERS[face])
        base = np.zeros((h, w, 4), np.uint8)
        glow = np.zeros((h, w, 4), np.uint8)
        for y in range(h):
            for x in range(w):
                p = tl + (x + 0.5) / w * (tr - tl) + (y + 0.5) / h * (bl - tl)
                pos = [lo[k] + p[k] * size[k] for k in range(3)]
                res = c['painter'](face, *pos)
                if res is None:
                    continue
                rgba, lit = res
                base[y, x] = rgba
                if lit:
                    glow[y, x] = (*rgba[:3], 255)
        return base, glow

    def build(self):
        regions, by_hash = [], {}
        for c in self.cubes:
            c['uv'] = {}
            for face in ('north', 'east', 'south', 'west', 'up', 'down'):
                if face in c['skip']:
                    continue
                base, glow = self._paint_face(c, face)
                key = hashlib.sha1(base.tobytes() + glow.tobytes() + bytes(base.shape[:2])).hexdigest()
                if key not in by_hash:      # identical faces share one atlas region
                    by_hash[key] = len(regions)
                    regions.append({'base': base, 'glow': glow, 'w': base.shape[1], 'h': base.shape[0]})
                c['uv'][face] = by_hash[key]
        # shelf packing, tallest first
        order = sorted(range(len(regions)), key=lambda i: (-regions[i]['h'], -regions[i]['w'], i))
        x = y = shelf = 0
        for i in order:
            r = regions[i]
            if x + r['w'] > self.tw:
                x, y, shelf = 0, y + shelf, 0
            r['u'], r['v'] = x, y
            x += r['w']
            shelf = max(shelf, r['h'])
        if y + shelf > self.th:
            raise ValueError(f'{self.name}: atlas {self.tw}x{self.th} too small (needs {y + shelf} rows)')
        base = np.zeros((self.th, self.tw, 4), np.uint8)
        glow = np.zeros((self.th, self.tw, 4), np.uint8)
        for r in regions:
            base[r['v']:r['v'] + r['h'], r['u']:r['u'] + r['w']] = r['base']
            glow[r['v']:r['v'] + r['h'], r['u']:r['u'] + r['w']] = r['glow']
        self.regions = regions
        self.used = (y + shelf, sum(r['w'] * r['h'] for r in regions))
        return Image.fromarray(base, 'RGBA'), Image.fromarray(glow, 'RGBA')

    def geo_json(self):
        bones = []
        for b in self.bones:
            px, py, pz = b['pivot']
            jb = {'name': b['name']}
            if b['parent']:
                jb['parent'] = b['parent']
            jb['pivot'] = [r4(-px), r4(py), r4(pz)]
            cubes = []
            for c in self.cubes:
                if c['bone'] != b['name']:
                    continue
                lo, hi = c['lo'], c['hi']
                jc = {'origin': [r4(-hi[0]), r4(lo[1]), r4(lo[2])],
                      'size': [r4(hi[0] - lo[0]), r4(hi[1] - lo[1]), r4(hi[2] - lo[2])]}
                if c['rot']:
                    cpx, cpy, cpz = c['pivot']
                    rx, ry, rz = c['rot']
                    jc['pivot'] = [r4(-cpx), r4(cpy), r4(cpz)]
                    jc['rotation'] = [r4(-rx), r4(-ry), r4(rz)]
                uv = {}
                for face, ri in c['uv'].items():
                    r = self.regions[ri]
                    w, h = r['w'], r['h']
                    if face in ('up', 'down'):
                        uv[face] = {'uv': [r['u'] + w, r['v'] + h], 'uv_size': [-w, -h]}
                    else:
                        uv[face] = {'uv': [r['u'], r['v']], 'uv_size': [w, h]}
                jc['uv'] = uv
                cubes.append(jc)
            if cubes:
                jb['cubes'] = cubes
            bones.append(jb)
        bw, bh, boff = self.bounds
        return {
            'format_version': '1.12.0',
            'minecraft:geometry': [{
                'description': {
                    'identifier': f'geometry.{self.name}',
                    'texture_width': self.tw,
                    'texture_height': self.th,
                    'visible_bounds_width': bw,
                    'visible_bounds_height': bh,
                    'visible_bounds_offset': [0, boff, 0],
                },
                'bones': bones,
            }],
        }


def keyed(points):
    """[(t, [x, y, z], easing)] -> GeckoLib keyframe dict."""
    out = {}
    for t, v, e in points:
        out[f'{t:.4f}'.rstrip('0').rstrip('.') if t % 1 else f'{t:.1f}'] = {'vector': [r4(x) for x in v], 'easing': e}
    return out


def animation_json(name, clips):
    anims = {}
    for clip, (length, bones) in clips.items():
        anims[f'animation.{name}.{clip}'] = {'loop': True, 'animation_length': length, 'bones': bones}
    return {'format_version': '1.8.0', 'animations': anims, 'geckolib_format_version': 2}


BOLT_Y = 4.0        # centre of the 0.5 x 0.5 hitbox
BOLT_BOXES = (      # (name, half cross-section, z from, z to): a stepped spindle, nose toward -Z
    ('tip', 0.5, -4.5, 3.5),
    ('mid', 1.0, -3.5, 2.5),
    ('body', 1.5, -2.5, 1.5),
)


def bolt_painter(half):
    def p(face, x, y, z):
        if face in ('north', 'south'):          # end caps: a lit point / a violet ring
            r = max(abs(x), abs(y - BOLT_Y)) / half
            if half <= 0.5:
                return (VIOLET[4] if face == 'north' else VIOLET[3]), True
            return (VIOLET[3] if r < 0.6 else VIOLET[2]), r < 0.6
        across = (y - BOLT_Y) / half if face in ('east', 'west') else x / half   # -1..1 across the facet
        if half >= 1.5:
            if abs(across) < 1 / 3:
                tip = abs(z + 0.5) > 1.0          # the core fades toward both ends
                return (SOUL[1] if tip else SOUL[2]), True
            return (VIOLET[3] if across > 0 else VIOLET[2]), True
        if half >= 1.0:
            if abs(z + 0.5) < 1.0 and across > 0:
                return SOUL[1], True              # the core showing between the outer facets
            return (VIOLET[4] if across > 0 else VIOLET[2]), True
        return (VIOLET[4] if abs(z + 0.5) < 2.5 else VIOLET[3]), True
    return p


def memory_bolt():
    m = Model('memory_bolt', 32, 16, bounds=(1, 1, 0.25))
    m.bone('root', (0, 0, 0))
    m.bone('shard', (0, BOLT_Y, 0))
    for name, h, z0, z1 in BOLT_BOXES:
        m.box('shard', (-h, BOLT_Y - h, z0), (h, BOLT_Y + h, z1), bolt_painter(h), rot=(0, 0, 45),
              pivot=(0, BOLT_Y, 0))
    spin = keyed([(0.0, [0, 0, 0], 'linear'), (0.125, [0, 0, 90], 'linear'), (0.25, [0, 0, 180], 'linear'),
                  (0.375, [0, 0, 270], 'linear'), (0.5, [0, 0, 360], 'linear')])
    pulse = keyed([(0.0, [1, 1, 1], 'linear'), (0.25, [1.08, 1.08, 1], 'easeInOutSine'), (0.5, [1, 1, 1], 'easeInOutSine')])
    clips = {'spin': (0.5, {'shard': {'rotation': spin, 'scale': pulse}})}
    return m, clips


WISP_C = (0.0, 3.2, 0.0)    # orb centre (0.4 x 0.4 hitbox)


def orb_painter(face, x, y, z):
    """Soul flame ball: pale cyan heart in every view, cyan rim, teal underside, white-hot crown."""
    cx, cy, cz = WISP_C
    dx, dy, dz = x - cx, y - cy, z - cz
    d = {'north': math.hypot(dx, dy), 'south': math.hypot(dx, dy), 'east': math.hypot(dz, dy),
         'west': math.hypot(dz, dy), 'up': math.hypot(dx, dz), 'down': math.hypot(dx, dz)}[face]
    if face == 'down':
        return (SOUL[0] if d < 1.0 else SOUL_DEEP[1]), True
    if face == 'up':
        return (SOUL[3] if d < 0.8 else SOUL[2]), True
    if dy < -1.6:
        return (SOUL[0] if d < 2.0 else SOUL_DEEP[1]), True
    if d < 1.6 or dy > 1.6:
        return SOUL[2], True
    return SOUL[1], True


def flame_painter(face, x, y, z):
    return (SOUL[3] if face == 'up' or y > WISP_C[1] + 3.0 else SOUL[2]), True


def wisp_painter(face, x, y, z):
    t = (z - 2.0) / 6.0      # 0 at the orb, 1 at the tip
    if face == 'down':
        t += 0.25
    if t < 0.2:
        return SOUL[2], True
    if t < 0.5:
        return SOUL[1], True
    if t < 0.8:
        return SOUL[0], True
    return SOUL_DEEP[1], True


def soul_wisp():
    m = Model('soul_wisp', 32, 16, bounds=(1, 1, 0.25))
    cx, cy, cz = WISP_C
    m.bone('root', (0, 0, 0))
    m.bone('orb', WISP_C)
    m.bone('tail', (0, cy, 1.5))
    # a 5 px voxel ball: three crossed boxes (their coplanar overlaps are buried inside the third box)
    m.box('orb', (cx - 2.5, cy - 1.5, cz - 1.5), (cx + 2.5, cy + 1.5, cz + 1.5), orb_painter)
    m.box('orb', (cx - 1.5, cy - 2.5, cz - 1.5), (cx + 1.5, cy + 2.5, cz + 1.5), orb_painter)
    m.box('orb', (cx - 1.5, cy - 1.5, cz - 2.5), (cx + 1.5, cy + 1.5, cz + 2.5), orb_painter)
    # the flame's tip, swept back as the wisp flies
    m.box('orb', (cx - 0.5, cy + 2.0, cz - 0.5), (cx + 0.5, cy + 4.0, cz + 0.5), flame_painter,
          rot=(28, 0, 0), pivot=(cx, cy + 2.0, cz), skip=('down',))
    # two trailing wisps: a flat ribbon and a thin tip each, swept back (+Z) and apart
    for s, rise, yaw, pitch in ((1, 0.3, 16, 10), (-1, -0.4, -20, -6)):
        piv = (0.8 * s, cy + rise, 1.5)
        rot = (pitch, yaw, 0)
        m.box('tail', (0.8 * s - 1, cy + rise - 0.5, 1.5), (0.8 * s + 1, cy + rise + 0.5, 5.5), wisp_painter,
              rot=rot, pivot=piv, skip=('north',))
        m.box('tail', (0.8 * s - 0.5, cy + rise - 0.5, 5.5), (0.8 * s + 0.5, cy + rise + 0.5, 8.5), wisp_painter,
              rot=rot, pivot=piv, skip=('north',))
    bob = [(0.0, [0, 0, 0], 'linear'), (0.5, [0, 0.6, 0], 'easeInOutSine'), (1.0, [0, 0, 0], 'easeInOutSine')]
    clips = {'float': (1.0, {
        'orb': {'position': keyed(bob),
                'scale': keyed([(0.0, [1, 1, 1], 'linear'), (0.25, [1.08, 1.08, 1.08], 'easeInOutSine'),
                                (0.5, [1, 1, 1], 'easeInOutSine'), (0.75, [0.95, 1.06, 0.95], 'easeInOutSine'),
                                (1.0, [1, 1, 1], 'easeInOutSine')])},
        'tail': {'position': keyed(bob),
                 'rotation': keyed([(0.0, [0, 0, 0], 'linear'), (0.25, [-6, 14, 0], 'easeInOutSine'),
                                    (0.5, [0, 0, 0], 'easeInOutSine'), (0.75, [6, -14, 0], 'easeInOutSine'),
                                    (1.0, [0, 0, 0], 'easeInOutSine')])},
    })}
    return m, clips


HELPERS = {'memory_bolt': (memory_bolt, 'spin', 0.25), 'soul_wisp': (soul_wisp, 'float', 0.25)}


def build_helpers():
    lines = []
    for name, (fn, clip, t) in HELPERS.items():
        model, clips = fn()
        base, glow = model.build()
        out = DOCS / name
        out.mkdir(parents=True, exist_ok=True)
        geo = model.geo_json()
        anim = animation_json(name, clips)
        write_json(out / f'{name}.geo.json', geo)
        write_json(out / f'{name}.animation.json', anim)
        base.save(out / f'{name}.png')
        glow.save(out / f'{name}_glowmask.png')
        tex = R.Tex(base, glow)
        views = {'front': (0, -10), 'side': (90, -10)}
        for label, (yaw, pitch) in views.items():
            im = R.render(R.geo_quads(geo, tex), yaw=yaw, pitch=pitch, scale=22, size=(256, 256),
                          center=(0, 4), bg=BG)
            im.save(out / f'preview_{label}.png')
        pose = R.sample_pose(anim['animations'][f'animation.{name}.{clip}'], t)
        im = R.render(R.geo_quads(geo, tex, pose), yaw=35, pitch=-20, scale=22, size=(256, 256), center=(0, 4), bg=BG)
        im.save(out / f'pose_{clip}_{t:.2f}.png')
        lines.append(f'{name}: {len(model.cubes)} cubes, {len(model.regions)} atlas regions, '
                     f'{model.used[1]} px in {model.used[0]} rows')
    return '\n'.join(lines)


def hide_bones(geo, hidden):
    """Copy of geo with the cubes of `hidden` bones (and their descendants) removed."""
    geo = copy.deepcopy(geo)
    bones = geo['minecraft:geometry'][0]['bones']
    parent = {b['name']: b.get('parent') for b in bones}

    def is_hidden(name):
        while name:
            if name in hidden:
                return True
            name = parent.get(name)
        return False
    for b in bones:
        if is_hidden(b['name']):
            b.pop('cubes', None)
    return geo


def watcher_assets(texture='forgotten_watcher.png'):
    geo = json.loads((WATCHER / 'forgotten_watcher.geo.json').read_text(encoding='utf-8'))
    anim = json.loads((WATCHER / 'forgotten_watcher.animation.json').read_text(encoding='utf-8'))
    glow_path = WATCHER / 'forgotten_watcher_glowmask.png'
    glow = Image.open(glow_path) if texture == 'forgotten_watcher.png' and glow_path.exists() else None
    return geo, anim, R.Tex(Image.open(WATCHER / texture), glow)


WATCHER_VIEWS = {'front': (0, -12), 'side': (90, -12), 'back': (180, -12)}


def render_watcher(geo, tex, pose=None, yaw=0, pitch=-12):
    return R.render(R.geo_quads(geo, tex, pose), yaw=yaw, pitch=pitch, scale=8, size=(400, 400),
                    center=(0, 34), bg=BG)


def watcher_previews(out=WATCHER):
    geo, anim, tex = watcher_assets()
    plain = hide_bones(geo, {'aegis'})          # Java hides the aegis outside the aegis state
    for label, (yaw, pitch) in WATCHER_VIEWS.items():
        render_watcher(plain, tex, yaw=yaw, pitch=pitch).save(out / f'preview_{label}.png')
    # shielded, as the game draws it: aegis shown, plate 0 (the open segment) hidden
    shielded = hide_bones(geo, {'aegis_0'})
    pose = R.sample_pose(anim['animations']['animation.forgotten_watcher.shielded'], 1.0)
    render_watcher(shielded, tex, pose, yaw=150, pitch=-14).save(out / 'pose_shielded_1.00.png')
    # the illusion: illusion atlas, no aegis, drawn at alpha 200
    _, _, itex = watcher_assets('forgotten_watcher_illusion.png')
    pose = R.sample_pose(anim['animations']['animation.forgotten_watcher.idle'], 0.6)
    im = R.render(R.geo_quads(plain, itex, pose), yaw=20, pitch=-12, scale=8, size=(400, 400), center=(0, 34))
    a = np.asarray(im).astype(np.float32)
    a[..., 3] *= 200 / 255
    layer = Image.fromarray(a.astype(np.uint8), 'RGBA')
    bg = Image.new('RGBA', layer.size, BG)
    Image.alpha_composite(bg, layer).save(out / 'pose_illusion_idle_0.60.png')
    return 'watcher previews written'


if __name__ == '__main__':
    steps = sys.argv[1:] or ['aegis', 'illusion', 'helpers', 'previews']
    for step in steps:
        if step == 'aegis':
            print(split_aegis())
        elif step == 'illusion':
            print(illusion_texture())
        elif step == 'helpers':
            print(build_helpers())
        elif step == 'previews':
            print(watcher_previews())
        else:
            raise SystemExit(f'unknown step {step}')
