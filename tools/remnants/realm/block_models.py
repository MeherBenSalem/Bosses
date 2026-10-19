"""Blockstates, block models and item models for the Eclipsed Realm blocks.

Vanilla parents are used wherever vanilla has a shape (cube_all, cube_column, stairs, slab, wall).
Custom element models (gravestone, grace ember) are written as Java block-model elements.
Forge / NeoForge read `render_type` from the model; Fabric registers render layers in code.
"""
from common import rl, save_json

B = 'block/'
GLOW_DATA = {'block_light': 15, 'sky_light': 15}


def blockstate(name, variants=None, multipart=None):
    data = {'variants': variants} if variants is not None else {'multipart': multipart}
    save_json(data, f'blockstates/{name}.json')


def model(name, data):
    save_json(data, f'models/block/{name}.json')


def item(name, parent=None, layer=None):
    if layer:
        data = {'parent': 'minecraft:item/generated', 'textures': {'layer0': rl(f'item/{layer}')}}
    else:
        data = {'parent': rl(f'block/{parent or name}')}
    save_json(data, f'models/item/{name}.json')


def cube_all(name, texture=None, render_type=None):
    data = {'parent': 'minecraft:block/cube_all', 'textures': {'all': rl(B + (texture or name))}}
    if render_type:
        data['render_type'] = render_type
    model(name, data)


def simple_block(name):
    cube_all(name)
    blockstate(name, {'': {'model': rl(B + name)}})
    item(name)


def eclipse_stone():
    cube_all('eclipse_stone')
    model('eclipse_stone_mirrored', {'parent': 'minecraft:block/cube_mirrored_all',
                                     'textures': {'all': rl('block/eclipse_stone')}})
    m, mm = rl('block/eclipse_stone'), rl('block/eclipse_stone_mirrored')
    blockstate('eclipse_stone', {'': [{'model': m}, {'model': mm}, {'model': m, 'y': 180}, {'model': mm, 'y': 180}]})
    item('eclipse_stone')


def stairs(name, texture):
    tex = {'bottom': rl(B + texture), 'top': rl(B + texture), 'side': rl(B + texture)}
    for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        model(name + suffix, {'parent': f'minecraft:block/{parent}', 'textures': tex})
    base_y = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    files = {'straight': '', 'inner_left': '_inner', 'inner_right': '_inner', 'outer_left': '_outer', 'outer_right': '_outer'}
    variants = {}
    for facing in ('east', 'north', 'south', 'west'):
        for half in ('bottom', 'top'):
            for shape in ('inner_left', 'inner_right', 'outer_left', 'outer_right', 'straight'):
                y = base_y[facing]
                if half == 'bottom' and shape.endswith('_left'):
                    y -= 90
                if half == 'top' and shape.endswith('_right'):
                    y += 90
                v = {'model': rl(B + name + files[shape])}
                if half == 'top':
                    v['x'] = 180
                if y % 360:
                    v['y'] = y % 360
                if 'x' in v or 'y' in v:
                    v['uvlock'] = True
                variants[f'facing={facing},half={half},shape={shape}'] = v
    blockstate(name, variants)
    item(name)


def slab(name, texture, double_model):
    tex = {'bottom': rl(B + texture), 'top': rl(B + texture), 'side': rl(B + texture)}
    model(name, {'parent': 'minecraft:block/slab', 'textures': tex})
    model(name + '_top', {'parent': 'minecraft:block/slab_top', 'textures': tex})
    blockstate(name, {
        'type=bottom': {'model': rl(B + name)},
        'type=double': {'model': rl(B + double_model)},
        'type=top': {'model': rl(B + name + '_top')},
    })
    item(name)


def wall(name, texture):
    tex = {'wall': rl(B + texture)}
    model(name + '_post', {'parent': 'minecraft:block/template_wall_post', 'textures': tex})
    model(name + '_side', {'parent': 'minecraft:block/template_wall_side', 'textures': tex})
    model(name + '_side_tall', {'parent': 'minecraft:block/template_wall_side_tall', 'textures': tex})
    model(name + '_inventory', {'parent': 'minecraft:block/wall_inventory', 'textures': tex})
    parts = [{'when': {'up': 'true'}, 'apply': {'model': rl(B + name + '_post')}}]
    for height, suffix in (('low', '_side'), ('tall', '_side_tall')):
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = {'model': rl(B + name + suffix), 'uvlock': True}
            if y:
                apply['y'] = y
            parts.append({'when': {side: height}, 'apply': apply})
    blockstate(name, multipart=parts)
    item(name, parent=name + '_inventory')


def graceroot():
    tex = {'end': rl('block/graceroot_log_top'), 'side': rl('block/graceroot_log')}
    model('graceroot_log', {'parent': 'minecraft:block/cube_column', 'textures': tex})
    model('graceroot_log_horizontal', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': tex})
    blockstate('graceroot_log', {
        'axis=x': {'model': rl('block/graceroot_log_horizontal'), 'x': 90, 'y': 90},
        'axis=y': {'model': rl('block/graceroot_log')},
        'axis=z': {'model': rl('block/graceroot_log_horizontal'), 'x': 90},
    })
    item('graceroot_log')
    simple_block('graceroot_planks')
    # untinted leaves: cube_all has no tintindex, so the gold texture shows as painted
    cube_all('gilded_leaves', render_type='minecraft:cutout_mipped')
    blockstate('gilded_leaves', {'': {'model': rl('block/gilded_leaves')}})
    item('gilded_leaves')


def face(uv, tex='#0', cull=None):
    f = {'uv': [round(v, 3) for v in uv], 'texture': tex}
    if cull:
        f['cullface'] = cull
    return f


def element(frm, to, faces, rotation=None, shade=True, extra=None):
    e = {'from': [round(v, 4) for v in frm], 'to': [round(v, 4) for v in to], 'faces': faces}
    if rotation:
        e['rotation'] = rotation
    if not shade:
        e['shade'] = False
    if extra:
        e.update(extra)
    return e


def box_faces(frm, to, uv_side, uv_top=None, uv_bottom=None, tex='#0', skip=()):
    """Faces for a box, sampling one shared side rect (offset per face so sides don't repeat exactly)."""
    w, h, d = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    u, v = uv_side
    faces = {
        'north': face((u, v, u + w, v + h), tex),
        'south': face((u + 1, v, u + 1 + w, v + h), tex),
        'east': face((u + 2, v, u + 2 + d, v + h), tex),
        'west': face((u + 3, v, u + 3 + d, v + h), tex),
    }
    tu, tv = uv_top or (u, v)
    faces['up'] = face((tu, tv, tu + w, tv + d), tex)
    bu, bv = uv_bottom or (u, v + 1)
    faces['down'] = face((bu, bv, bu + w, bv + d), tex)
    for s in skip:
        faces.pop(s, None)
    return faces


def gravestone_elements(lean=False):
    """A rounded-top headstone (stepped top) on a plinth. Front (engraving) faces north."""
    T = '#stone'
    plinth = element((3, 0, 5.5), (13, 2, 10.5), {
        'north': face((0, 14, 10, 16), T), 'south': face((0, 14, 10, 16), T),
        'east': face((10, 11, 15, 13), T), 'west': face((10, 11, 15, 13), T),
        'up': face((0, 11, 10, 16), T), 'down': face((0, 11, 10, 16), T, 'down'),
    })
    sink = 1 if lean else 0
    rot = {'origin': [8, 2, 8], 'axis': 'z', 'angle': 22.5} if lean else None
    parts = [
        ((4, 2 - sink, 7), (12, 11 - sink, 9), (0, 2, 8, 11)),
        ((5, 11 - sink, 7), (11, 12 - sink, 9), (1, 1, 7, 2)),
        ((6, 12 - sink, 7), (10, 13 - sink, 9), (2, 0, 6, 1)),
    ]
    els = [plinth]
    for frm, to, (u1, v1, u2, v2) in parts:
        w, h = to[0] - frm[0], to[1] - frm[1]
        els.append(element(frm, to, {
            'north': face((u1, v1, u2, v2), T),
            'south': face((8 + u1, v1, 8 + u2, v2), T),
            'east': face((8, v1, 10, v2), T),
            'west': face((14, v1, 16, v2), T),
            'up': face((8, 0, 8 + w, 2), T),
            'down': face((8, 9, 8 + w, 11), T),
        }, rotation=rot))
    return els


def gravestone():
    tex = {'stone': rl('block/weathered_gravestone'), 'particle': rl('block/weathered_gravestone')}
    for name, lean in (('weathered_gravestone', False), ('weathered_gravestone_leaning', True)):
        model(name, {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex,
                     'elements': gravestone_elements(lean)})
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        up = {'model': rl('block/weathered_gravestone'), 'weight': 3}
        lean = {'model': rl('block/weathered_gravestone_leaning')}
        if y:
            up['y'] = y
            lean['y'] = y
        variants[f'facing={facing}'] = [up, lean]
    blockstate('weathered_gravestone', variants)
    item('weathered_gravestone')


RING_STONES = [
    # (centre x, centre z, width, depth, height, y-rotation)
    (13.3, 9.4, 2.5, 2.0, 5.0, 22.5),
    (9.4, 13.3, 2.0, 2.0, 3.0, -22.5),
    (4.1, 11.9, 3.0, 2.0, 6.0, 45),
    (2.7, 6.6, 2.0, 2.0, 3.5, 22.5),
    (6.6, 2.7, 2.5, 2.0, 5.0, -45),
    (11.9, 4.1, 2.0, 2.5, 2.5, 0),
]


def grace_ember_elements():
    S, W, G = '#stone', '#sword', '#glow'
    els = [
        element((4, 0, 4), (12, 1, 12), {'north': face((0, 8, 8, 9), S), 'south': face((4, 8, 12, 9), S),
                                         'east': face((2, 8, 10, 9), S), 'west': face((6, 8, 14, 9), S),
                                         'up': face((0, 8, 8, 16), S), 'down': face((8, 8, 16, 16), S, 'down')}),
        element((5, 1, 5), (11, 2, 11), {'north': face((1, 8, 7, 9), S), 'south': face((5, 8, 11, 9), S),
                                         'east': face((3, 8, 9, 9), S), 'west': face((7, 8, 13, 9), S),
                                         'up': face((5, 9, 11, 15), S)}),
    ]
    for i, (cx, cz, w, d, h, ry) in enumerate(RING_STONES):
        frm = (cx - w / 2, 0, cz - d / 2)
        to = (cx + w / 2, h, cz + d / 2)
        u = (i * 5) % 12
        faces = {
            'north': face((u, 0, u + w, h), S), 'south': face((u + 2, 0, u + 2 + w, h), S),
            'east': face((u + 1, 0, u + 1 + d, h), S), 'west': face((u + 3, 0, u + 3 + d, h), S),
            'up': face((u, 0, u + w, d), S),
        }
        rot = {'origin': [cx, 0, cz], 'axis': 'y', 'angle': ry} if ry else None
        els.append(element(frm, to, faces, rotation=rot))
    for frm, to in (((10.5, 0, 11.5), (11.5, 1, 12.5)), ((3.5, 0, 9), (4.5, 0.75, 10))):
        els.append(element(frm, to, box_faces(frm, to, (6, 1), (6, 0), tex=S, skip=('down',))))
    # the sword, driven point-down, turned 45 degrees so both flats catch the light
    rot = {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}
    sword = [
        # blade: 2 wide (x), 1 thick (z)
        ((7, 1, 7.5), (9, 10, 8.5), {'north': face((0, 2, 2, 11), W), 'south': face((0, 4, 2, 13), W),
                                     'east': face((2, 2, 3, 11), W), 'west': face((3, 2, 4, 11), W)}),
        ((5.5, 10, 7.25), (10.5, 11, 8.75), {'north': face((4, 0, 9, 1), W), 'south': face((4, 1, 9, 2), W),
                                             'east': face((4, 2, 5.5, 3), W), 'west': face((7, 2, 8.5, 3), W),
                                             'up': face((4, 0, 9, 1.5), W), 'down': face((4, 1.5, 9, 3), W)}),
        ((7.5, 11, 7.5), (8.5, 13.5, 8.5), {'north': face((10, 0, 11, 2.5), W), 'south': face((10, 2, 11, 4.5), W),
                                            'east': face((11, 0, 12, 2.5), W), 'west': face((10, 3, 11, 5.5), W)}),
        ((7.25, 13.5, 7.25), (8.75, 14.5, 8.75), {'north': face((12, 0, 13.5, 1), W), 'south': face((12, 1, 13.5, 2), W),
                                                  'east': face((13, 0, 14.5, 1), W), 'west': face((12, 2, 13.5, 3), W),
                                                  'up': face((12, 0, 13.5, 1.5), W)}),
    ]
    for frm, to, faces in sword:
        els.append(element(frm, to, faces, rotation=rot))
    # the ember: crossed flame planes around the hilt plus a burning collar on the guard (fullbright)
    glow = {'forge_data': GLOW_DATA, 'neoforge_data': GLOW_DATA}
    flame_rot = {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45}
    els.append(element((5, 9.5, 8), (11, 16.5, 8), {'north': face((0, 0, 6, 7), G), 'south': face((0, 0, 6, 7), G)},
                       rotation=flame_rot, shade=False, extra=glow))
    els.append(element((8, 9.5, 5), (8, 16.5, 11), {'east': face((8, 0, 14, 7), G), 'west': face((8, 0, 14, 7), G)},
                       rotation=flame_rot, shade=False, extra=glow))
    els.append(element((7, 10.5, 7), (9, 12, 9), {
        'north': face((4, 10, 6, 11.5), G), 'south': face((6, 10, 8, 11.5), G), 'east': face((8, 10, 10, 11.5), G),
        'west': face((10, 10, 12, 11.5), G), 'up': face((12, 10, 14, 12), G)}, rotation=rot, shade=False, extra=glow))
    return els


def grace_ember():
    tex = {'stone': rl('block/grace_ember_stone'), 'sword': rl('block/grace_ember_sword'),
           'glow': rl('block/grace_ember_glow'), 'particle': rl('block/grace_ember_stone')}
    model('grace_ember', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex,
                          'elements': grace_ember_elements()})
    blockstate('grace_ember', {'': {'model': rl('block/grace_ember')}})
    item('grace_ember', layer='grace_ember')


def fog_gate():
    # A full cube drawn unshaded and full-bright (forge_data / neoforge_data light; ignored elsewhere),
    # so the fog reads as luminous gold-white haze instead of dark smoked glass.
    faces = {d: {'uv': [0, 0, 16, 16], 'texture': '#fog', 'cullface': d}
             for d in ('down', 'up', 'north', 'south', 'west', 'east')}
    model('fog_gate', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
        'textures': {'fog': rl('block/fog_gate'), 'particle': rl('block/fog_gate')},
        'elements': [element((0, 0, 0), (16, 16, 16), faces, shade=False,
                             extra={'forge_data': GLOW_DATA, 'neoforge_data': GLOW_DATA})],
    })
    blockstate('fog_gate', {'': {'model': rl('block/fog_gate')}})
    item('fog_gate', layer='fog_gate')


def waygate():
    particle = {'textures': {'particle': rl('block/eclipse_waygate_particle')}}
    for name in ('eclipse_waygate', 'waygate_frame', 'waygate_veil'):
        model(name, dict(particle))
    variants = {}
    for facing in ('north', 'south', 'west', 'east'):
        for phase in ('dormant', 'kindling', 'open'):
            variants[f'facing={facing},phase={phase}'] = {'model': rl('block/eclipse_waygate')}
    blockstate('eclipse_waygate', variants)
    blockstate('waygate_frame', {'': {'model': rl('block/waygate_frame')}})
    blockstate('waygate_veil', {'open=false': {'model': rl('block/waygate_veil')},
                                'open=true': {'model': rl('block/waygate_veil')}})
    item('eclipse_waygate', layer='eclipse_waygate')
    item('eclipse_shard', layer='eclipse_shard')


def build():
    eclipse_stone()
    for name in ('cobbled_eclipse_stone', 'eclipse_stone_bricks', 'cracked_eclipse_stone_bricks',
                 'chiseled_eclipse_stone_bricks'):
        simple_block(name)
    stairs('eclipse_stone_brick_stairs', 'eclipse_stone_bricks')
    slab('eclipse_stone_brick_slab', 'eclipse_stone_bricks', 'eclipse_stone_bricks')
    wall('eclipse_stone_brick_wall', 'eclipse_stone_bricks')
    graceroot()
    gravestone()
    grace_ember()
    fog_gate()
    waygate()
