"""docs/models/remnants/realm/contact_sheet.png: every realm texture at x8 with labels, the sky
textures over a dusk gradient, in-world renders of the custom block models, and the waygate atlas."""
from PIL import Image, ImageDraw

import preview
from common import DOCS, TREES

INK = (236, 222, 180, 255)
SUB = (170, 174, 190, 255)
BG = (22, 24, 32, 255)


def _load(rel):
    return Image.open(TREES[0] / rel).convert('RGBA')


def _checker(w, h, a=(52, 56, 70, 255), b=(44, 48, 60, 255), cell=16):
    im = Image.new('RGBA', (w, h), a)
    d = ImageDraw.Draw(im)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            if (x // cell + y // cell) % 2:
                d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=b)
    return im


def _dusk(w, h):
    im = Image.new('RGBA', (w, h))
    top, bottom = (40, 52, 84), (150, 120, 72)
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t ** 1.6) for i in range(3))
        ImageDraw.Draw(im).line([(0, y), (w, y)], fill=(*c, 255))
    return im


def _tile(im, scale, label, sub=None):
    w, h = im.width * scale, im.height * scale
    cell = _checker(w, h)
    cell.alpha_composite(im.resize((w, h), Image.NEAREST))
    probe = ImageDraw.Draw(Image.new('RGBA', (1, 1)))
    tw = max(probe.textlength(label, font=preview.font(14)), probe.textlength(sub or '', font=preview.font(12)))
    out = Image.new('RGBA', (max(w, int(tw) + 6), h + 38), BG)
    out.paste(cell, (0, 38))
    d = ImageDraw.Draw(out)
    d.text((2, 2), label, fill=INK, font=preview.font(14))
    if sub:
        d.text((2, 20), sub, fill=SUB, font=preview.font(12))
    return out


class Flow:
    def __init__(self, width):
        self.width, self.items, self.x, self.y, self.row_h = width, [], 16, 0, 0

    def heading(self, text):
        if self.x > 16:
            self.newline()
        self.items.append(('text', text, (16, self.y + 10)))
        self.y += 44

    def add(self, im, gap=18):
        if self.x + im.width > self.width - 16:
            self.newline()
        self.items.append(('img', im, (self.x, self.y)))
        self.x += im.width + gap
        self.row_h = max(self.row_h, im.height)

    def newline(self):
        self.y += self.row_h + 22
        self.x, self.row_h = 16, 0

    def render(self):
        self.newline()
        sheet = Image.new('RGBA', (self.width, self.y + 10), BG)
        d = ImageDraw.Draw(sheet)
        for kind, obj, pos in self.items:
            if kind == 'text':
                d.text(pos, obj, fill=(250, 214, 120, 255), font=preview.font(24))
            else:
                sheet.alpha_composite(obj, pos)
        return sheet


BLOCKS = ['eclipse_stone', 'cobbled_eclipse_stone', 'eclipse_stone_bricks', 'cracked_eclipse_stone_bricks',
          'chiseled_eclipse_stone_bricks', 'graceroot_log', 'graceroot_log_top', 'graceroot_planks', 'gilded_leaves',
          'weathered_gravestone', 'grace_ember_stone', 'grace_ember_bed', 'eclipse_waygate_particle']


def build():
    f = Flow(1720)
    f.heading('Eclipsed Realm  -  block textures (x8)')
    for name in BLOCKS:
        f.add(_tile(_load(f'textures/block/{name}.png'), 8, name))
    strip = _load('textures/block/fog_gate.png')
    frames = strip.height // 16
    for i in (0, frames // 4, frames // 2, 3 * frames // 4):
        f.add(_tile(strip.crop((0, i * 16, 16, i * 16 + 16)), 8, f'fog_gate  frame {i}/{frames}', 'animated, interpolated'))
    f.heading('Tiled 3x3 (seams) and in-world renders')
    for name in ('eclipse_stone', 'cobbled_eclipse_stone', 'eclipse_stone_bricks', 'graceroot_log', 'gilded_leaves'):
        src = _load(f'textures/block/{name}.png')
        tiled = Image.new('RGBA', (48, 48))
        for ty in range(3):
            for tx in range(3):
                tiled.paste(src, (tx * 16, ty * 16))
        f.add(_tile(tiled, 4, f'{name} tiled'))
    renders = [
        ('eclipse_stone', preview.cube_image({k: _load('textures/block/eclipse_stone.png') for k in ('north', 'east', 'up', 'west', 'south')})),
        ('graceroot_log', preview.cube_image({'north': _load('textures/block/graceroot_log.png'),
                                              'east': _load('textures/block/graceroot_log.png'),
                                              'west': _load('textures/block/graceroot_log.png'),
                                              'south': _load('textures/block/graceroot_log.png'),
                                              'up': _load('textures/block/graceroot_log_top.png')})),
        ('gilded_leaves', preview.cube_image({k: _load('textures/block/gilded_leaves.png') for k in ('north', 'east', 'up', 'west', 'south')})),
        ('fog_gate (full-bright)', preview.block_model_image('fog_gate.json', None)),
        ('weathered_gravestone', preview.block_model_image('weathered_gravestone.json', None)),
        ('gravestone (leaning)', preview.block_model_image('weathered_gravestone_leaning.json', None)),
        ('grace_ember', preview.block_model_image('grace_ember.json', None)),
        ('grace_ember (front)', preview.block_model_image('grace_ember.json', None, yaw=0, pitch=-8)),
    ]
    for label, im in renders:
        bg = _dusk(im.width, im.height)
        bg.alpha_composite(im)
        f.add(_tile(bg, 1, label))
    f.heading('Item icons (x8)')
    for name in ('eclipse_shard', 'eclipse_waygate', 'grace_ember', 'fog_gate'):
        f.add(_tile(_load(f'textures/item/{name}.png'), 8, name))
    f.heading('Particles (x8, birth -> death)')
    for name in ('grace_mote', 'eclipse_spark'):
        for i in range(6):
            f.add(_tile(_load(f'textures/particle/{name}_{i}.png'), 8, f'{name}_{i}'), gap=8)
        f.newline()
    f.heading('Sky (over a dusk gradient)')
    ecl = _load('textures/environment/eclipse.png')
    bg = _dusk(384, 384)
    bg.alpha_composite(ecl.resize((384, 384), Image.NEAREST))
    f.add(_tile(bg, 1, 'eclipse.png  128x128 (x3)', 'normal alpha blend: black disc hides the sky'))
    tree = _load('textures/environment/gracetree.png')
    bg = _dusk(512, 512)
    bg.alpha_composite(tree.resize((512, 512), Image.NEAREST))
    f.add(_tile(bg, 1, 'gracetree.png  256x256 (x2)', 'luminous: additive or translucent'))
    f.heading('Waygate atlas and glowmask (x3)')
    atlas = _load('textures/block/eclipse_waygate.png')
    glow = _load('textures/block/eclipse_waygate_glowmask.png')
    f.add(_tile(atlas, 3, 'eclipse_waygate.png', f'{atlas.width}x{atlas.height}'))
    dark = Image.new('RGBA', glow.size, (10, 10, 14, 255))
    dark.alpha_composite(glow)
    f.add(_tile(dark, 3, 'eclipse_waygate_glowmask.png', 'emissive pixels only'))
    sheet = f.render()
    DOCS.mkdir(parents=True, exist_ok=True)
    sheet.save(DOCS / 'contact_sheet.png')
    return DOCS / 'contact_sheet.png'
