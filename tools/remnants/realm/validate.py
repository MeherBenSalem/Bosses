"""Consistency checks over the written realm assets (both trees). Returns a list of problems."""
import json

from PIL import Image

from common import NS, TREES, WRITTEN

REQUIRED_BONES = ['root', 'pillar_left', 'pillar_right', 'lintel', 'eclipse_disc', 'ring_outer', 'ring_inner',
                  'runes_left', 'runes_right', 'runes_lintel', 'vortex', 'vortex_a', 'vortex_b', 'vortex_c', 'shards']
CLIPS = {'dormant': (True, None), 'kindle': ('hold_on_last_frame', 6.0), 'open': (True, None)}
BLOCKSTATES = ['eclipse_stone', 'cobbled_eclipse_stone', 'eclipse_stone_bricks', 'cracked_eclipse_stone_bricks',
               'chiseled_eclipse_stone_bricks', 'eclipse_stone_brick_stairs', 'eclipse_stone_brick_slab',
               'eclipse_stone_brick_wall', 'graceroot_log', 'graceroot_planks', 'gilded_leaves', 'weathered_gravestone',
               'grace_ember', 'fog_gate', 'eclipse_waygate', 'waygate_frame', 'waygate_veil']
ITEMS = ['eclipse_stone', 'cobbled_eclipse_stone', 'eclipse_stone_bricks', 'cracked_eclipse_stone_bricks',
         'chiseled_eclipse_stone_bricks', 'eclipse_stone_brick_stairs', 'eclipse_stone_brick_slab',
         'eclipse_stone_brick_wall', 'graceroot_log', 'graceroot_planks', 'gilded_leaves', 'weathered_gravestone',
         'grace_ember', 'fog_gate', 'eclipse_waygate', 'eclipse_shard']
SOUNDS = ['waygate_kindle', 'waygate_open', 'waygate_hum', 'waygate_cross', 'grace_discover', 'grace_rest',
          'remnant_felled', 'fog_gate_pass']


def _rl_path(tree, rl, folder, ext):
    ns, path = rl.split(':', 1) if ':' in rl else ('minecraft', rl)
    if ns != NS:
        return None
    return tree / folder / (path + ext)


def check_tree(tree):
    errs = []
    models = {}

    def model(rl):
        p = _rl_path(tree, rl, 'models', '.json')
        if p is None:
            return None
        if not p.exists():
            errs.append(f'missing model {rl}')
            return None
        if rl not in models:
            models[rl] = json.loads(p.read_text(encoding='utf-8'))
        return models[rl]

    for name in BLOCKSTATES:
        p = tree / 'blockstates' / f'{name}.json'
        if not p.exists():
            errs.append(f'missing blockstate {name}')
            continue
        bs = json.loads(p.read_text(encoding='utf-8'))
        refs = []
        for v in bs.get('variants', {}).values():
            refs += [x['model'] for x in (v if isinstance(v, list) else [v])]
        for part in bs.get('multipart', []):
            a = part['apply']
            refs += [x['model'] for x in (a if isinstance(a, list) else [a])]
        for r in refs:
            model(r)
    for name in ITEMS:
        p = tree / 'models/item' / f'{name}.json'
        if not p.exists():
            errs.append(f'missing item model {name}')
            continue
        m = json.loads(p.read_text(encoding='utf-8'))
        if m['parent'].startswith(NS + ':'):
            model(m['parent'])
        for t in m.get('textures', {}).values():
            tp = _rl_path(tree, t, 'textures', '.png')
            if tp and not tp.exists():
                errs.append(f'item {name}: missing texture {t}')
    for rl, m in models.items():
        for k, t in m.get('textures', {}).items():
            if t.startswith('#'):
                continue
            tp = _rl_path(tree, t, 'textures', '.png')
            if tp and not tp.exists():
                errs.append(f'{rl}: missing texture {t}')
        for e in m.get('elements', []):
            for v in e['from'] + e['to']:
                if not -16 <= v <= 32:
                    errs.append(f'{rl}: element bound {v} outside [-16, 32]')
            rot = e.get('rotation')
            if rot and (rot['angle'] not in (-45, -22.5, 0, 22.5, 45) or rot['axis'] not in 'xyz'):
                errs.append(f'{rl}: illegal rotation {rot}')
            for fname, f in e['faces'].items():
                if any(not 0 <= u <= 16 for u in f['uv']):
                    errs.append(f'{rl}: {fname} uv {f["uv"]} outside 0-16')
                if f['texture'].lstrip('#') not in m['textures']:
                    errs.append(f'{rl}: face texture {f["texture"]} unresolved')
    # textures
    for rel in [w for w in WRITTEN if w.startswith('textures/block/') and w.endswith('.png')]:
        im = Image.open(tree / rel)
        name = rel.rsplit('/', 1)[1]
        if name == 'fog_gate.png':
            if im.width != 16 or im.height % 16:
                errs.append('fog_gate strip size')
        elif not name.startswith('eclipse_waygate.') and not name.startswith('eclipse_waygate_glowmask') and im.size != (16, 16):
            errs.append(f'{rel} is {im.size}, expected 16x16')
    mc = json.loads((tree / 'textures/block/fog_gate.png.mcmeta').read_text(encoding='utf-8'))
    if not mc['animation'].get('interpolate'):
        errs.append('fog_gate mcmeta interpolate')
    # geckolib
    geo = json.loads((tree / 'geo/block/eclipse_waygate.geo.json').read_text(encoding='utf-8'))
    g = geo['minecraft:geometry'][0]
    if g['description']['identifier'] != 'geometry.eclipse_waygate' or geo['format_version'] != '1.12.0':
        errs.append('geo identifier/format')
    tw, th = g['description']['texture_width'], g['description']['texture_height']
    tex = Image.open(tree / 'textures/block/eclipse_waygate.png')
    glow = Image.open(tree / 'textures/block/eclipse_waygate_glowmask.png')
    if tex.size != (tw, th) or glow.size != (tw, th):
        errs.append(f'waygate texture size {tex.size}/{glow.size} != {tw}x{th}')
    names = {b['name'] for b in g['bones']}
    for b in REQUIRED_BONES:
        if b not in names:
            errs.append(f'missing bone {b}')
    for b in g['bones']:
        if b.get('parent') and b['parent'] not in names:
            errs.append(f'bone {b["name"]} has unknown parent')
        for c in b.get('cubes', []):
            for f, uv in c['uv'].items():
                (u, v), (w, h) = uv['uv'], uv['uv_size']
                for uu in (u, u + w):
                    if not -1e-6 <= uu <= tw + 1e-6:
                        errs.append(f'{b["name"]} {f} u {uu} out of atlas')
                for vv in (v, v + h):
                    if not -1e-6 <= vv <= th + 1e-6:
                        errs.append(f'{b["name"]} {f} v {vv} out of atlas')
            lo = [c['origin'][0], c['origin'][1], c['origin'][2]]
            hi = [lo[i] + c['size'][i] for i in range(3)]
            if lo[0] < -24.01 or hi[0] > 24.01 or lo[1] < -0.01 or hi[1] > 66 or lo[2] < -11 or hi[2] > 11:
                errs.append(f'{b["name"]} cube outside the arch envelope: {lo} {hi}')
    anim = json.loads((tree / 'animations/block/eclipse_waygate.animation.json').read_text(encoding='utf-8'))
    for clip, (loop, length) in CLIPS.items():
        for key in (f'animation.eclipse_waygate.{clip}', clip):
            a = anim['animations'].get(key)
            if not a:
                errs.append(f'missing clip {key}')
                continue
            if a.get('loop') != loop:
                errs.append(f'{key} loop {a.get("loop")} != {loop}')
            if length and abs(a['animation_length'] - length) > 1e-6:
                errs.append(f'{key} length {a["animation_length"]}')
            for bone, chans in a['bones'].items():
                if bone not in names:
                    errs.append(f'{key} animates unknown bone {bone}')
                for ch, track in chans.items():
                    for t in track:
                        if float(t) > a['animation_length'] + 1e-6:
                            errs.append(f'{key}/{bone}/{ch} key {t} past the end')
    # particles, sounds
    for name in ('grace_mote', 'eclipse_spark'):
        pj = json.loads((tree / f'particles/{name}.json').read_text(encoding='utf-8'))
        if not 4 <= len(pj['textures']) <= 8:
            errs.append(f'{name}: {len(pj["textures"])} frames')
        for t in pj['textures']:
            if not _rl_path(tree, t, 'textures/particle', '.png').exists():
                errs.append(f'{name}: missing frame {t}')
    sj = json.loads((tree / 'sounds.json').read_text(encoding='utf-8'))
    for s in SOUNDS:
        if s not in sj:
            errs.append(f'sounds.json lacks {s}')
            continue
        if sj[s].get('subtitle') != f'subtitles.{NS}.{s}':
            errs.append(f'{s} subtitle')
        for snd in sj[s]['sounds']:
            nm = snd['name'] if isinstance(snd, dict) else snd
            if not _rl_path(tree, nm, 'sounds', '.ogg').exists():
                errs.append(f'{s}: missing {nm}.ogg')
    for rel in ('textures/environment/eclipse.png', 'textures/environment/gracetree.png'):
        im = Image.open(tree / rel)
        want = (128, 128) if 'eclipse' in rel else (256, 256)
        if im.size != want or im.mode != 'RGBA':
            errs.append(f'{rel} {im.size} {im.mode}')
    return errs


def check():
    errs = []
    for tree in TREES:
        errs += [f'[{tree.parts[-7]}] {e}' for e in check_tree(tree)]
    a, b = TREES
    for rel in WRITTEN:
        if (a / rel).read_bytes() != (b / rel).read_bytes():
            errs.append(f'trees differ: {rel}')
    return errs
