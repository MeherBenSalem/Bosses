"""Seyr item icons and the Archivist armor layers.

Same conventions as items.py / items_later.py: 16x16 character grids painted from the Seyr ramps,
then a selective dark outline where the shape meets transparency.
The inks below are local to this file so they never collide with items.INK.
Run: python tools/remnants/items_seyr.py  (writes PNGs into docs/models/remnants/items/ and
regenerates the items preview sheet: items_later icons, the Seyr icons, and the armor layers)
     python tools/remnants/items_seyr.py --install  (copies the nine Seyr item textures and the
     archivist layers into both resource trees, writes their item models and the four spawn egg models)
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import items  # noqa: E402
import items_later  # noqa: E402
from paint import Atlas, Face, Material, STYLES, apply_decal  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/models/remnants/items'


def hx(value):
    value = value.lstrip('#')
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


SANDSTONE = ['#4A4336', '#6E6450', '#958A6E', '#BDB394', '#DDD5B8']
VERDIGRIS = ['#1F3B36', '#2F5C52', '#4A8574', '#73AE98']
VIOLET = ['#2A1F4A', '#45337A', '#6A52B0', '#9E86E0', '#D2C6FF']
ROBE = ['#121626', '#1A2337', '#22344A', '#30495F', '#456478']      # scholar's cloth, cool shadows
LEATHER = ['#22160E', '#332216', '#46301F', '#5C412A']
SOUL = ['#2E8C8C', '#4FC9C1', '#9CF2E6', '#E0FFFA']

# sandstone a-e, verdigris g-j, violet p-t, robe u-y, leather L-O, remnant light 6-9 (dark -> light)
INK = {}
for chars, ramp in (('abcde', SANDSTONE), ('ghij', VERDIGRIS), ('pqrst', VIOLET), ('uvwxy', ROBE),
                    ('LMNO', LEATHER), ('6789', SOUL)):
    for ch, c in zip(chars, ramp):
        INK[ch] = hx(c)


def grid(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), 'icons are 16x16'
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                im.putpixel((x, y), (*INK[ch], 255))
    return items.outlined(im)


ICONS = {
    # A broken Seyr tablet: bevelled sandstone, a carved frame, a glowing violet glyph.
    'knowledge_fragment': grid([
        '................',
        '................',
        '..eeeeee........',
        '..eddddde.......',
        '..edcccccdee....',
        '..eddddddbdc....',
        '..edddddtddcde..',
        '..eddddsdsdddb..',
        '..edddtrtrtddb..',
        '..eddddsdsdddb..',
        '..edddddtddddb..',
        '..edccccccccdb..',
        '..eddddddddddb..',
        '..cbbbbbbbbbba..',
        '....aaaaaaaaa...',
        '................',
    ]),
    # Lens of Memory: a verdigris ring around violet crystal glass, a short handle with a sandstone grip.
    'memory_lens': grid([
        '................',
        '....hijjjih.....',
        '...ijtsrrrhi....',
        '..ijtsrrrqqhh...',
        '..jtsrrrqqqqh...',
        '.hjsrrrqqqqpgh..',
        '.ijrrrqqqqqpgh..',
        '.ijrrqqqqqppgh..',
        '.hirqqqqqpppgh..',
        '..hiqqqqpppgh...',
        '..hhiqpppppgh...',
        '...hhggpggghi...',
        '.....hhgghhjih..',
        '...........iddc.',
        '............dcb.',
        '.............ba.',
    ]),
    # Archivist hood: a peaked cowl, verdigris-trimmed opening, deep shadow, violet clasp.
    'archivist_hood': grid([
        '................',
        '.......xw.......',
        '......xxwv......',
        '.....xyxwwv.....',
        '....xyxwwwwv....',
        '...xyxjjjjjwv...',
        '...xxjuuuuujv...',
        '..xyiuuuuuuuiv..',
        '..xxiuuuuuuuiv..',
        '..xwiuuuuuuuiv..',
        '..xwwiuuuuuiwv..',
        '..xwwwiiriiwwv..',
        '..wwwwwstwwwwv..',
        '...vvvvvrvvvv...',
        '................',
        '................',
    ]),
    # Archivist robe: wide sleeves, verdigris collar and front trim, a violet crystal clasp.
    'archivist_robe': grid([
        '................',
        '...xwwiijiiwwv..',
        '..xyxwwjtiwwxwv.',
        '.xyxwwwistwwwxwv',
        '.xxwwwwiriwwwwwv',
        '.ijwwwwwjiwwwwji',
        '.iiwvwwwjiwwvwii',
        '...xwwwwjiwwwv..',
        '...xwwwwjiwwwv..',
        '...iiiiijiiiii..',
        '...xwwwwjiwwwv..',
        '...xwwwwjiwwwv..',
        '...xwwvwjiwvwv..',
        '...jjiiijiiiih..',
        '................',
        '................',
    ]),
    # Archivist leggings: a verdigris sash with a violet buckle over a split robe skirt.
    'archivist_leggings': grid([
        '................',
        '...jjiiirriiih..',
        '...iiiiistiiih..',
        '...xwwwwvwwwwv..',
        '...xwwwv.xwwwv..',
        '...xwwwv.xwwwv..',
        '..xwwwwv.xwwwwv.',
        '..xwwvwv.xwvwwv.',
        '..xwwwwv.xwwwwv.',
        '..xwwvwv.xwvwwv.',
        '..xwwwwv.xwwwwv.',
        '..jjiiih.jjiiih.',
        '..hhhhgg.hhhhgg.',
        '................',
        '................',
        '................',
    ]),
    # Archivist boots: soft cloth boots with verdigris cuffs and toe caps, leather soles.
    'archivist_boots': grid([
        '................',
        '................',
        '................',
        '................',
        '...jjii....jjii.',
        '...xwwv....xwwv.',
        '...xwwv....xwwv.',
        '...xwvv....xwvv.',
        '...xwwv....xwwv.',
        '..xwwwv...xwwwv.',
        '.jiwwwv..jiwwwv.',
        '.jiiiih..jiiiih.',
        '.NNMMML..NNMMML.',
        '................',
        '................',
        '................',
    ]),
}


# Archivist armor layers use the vanilla humanoid armor UV layout (64x32).
PAL = {
    'robe': Material(ROBE, 'cloth'),
    'robe_lining': Material(['#0C0F1A', '#121626', '#1A2337', '#22344A'], 'cloth'),
    'verdigris': Material(VERDIGRIS, 'plate', sheen=False, wear=0.6),
    'crystal': Material(VIOLET, 'crystal'),
    'leather': Material(LEATHER, 'leather'),
}


def archivist_layers():
    layer1, layer2 = Atlas(64, 32), Atlas(64, 32)

    def paint_box(atlas, u, v, w, h, d, mat, decals=(), seed=0):
        rects = {}
        for name, (x, y, fw, fh) in items.box_faces(u, v, w, h, d).items():
            key = {'top': 'up', 'bottom': 'down', 'front': 'north', 'back': 'south', 'right': 'east', 'left': 'west'}[name]
            rects[key] = (x, y, x + fw, y + fh)
        rng = random.Random(seed)
        for key, rect in rects.items():
            STYLES[PAL[mat].style](Face(atlas, rect, PAL[mat], key), rng, PAL)
        for decal in decals:
            apply_decal(atlas, rects, decal, PAL, rng)
        return rects

    def clear_rows(atlas, rects, keys, y0, y1):
        for key in keys:
            u1, v1, u2, v2 = rects[key]
            for y in range(v1 + y0, min(v2, v1 + y1)):
                for x in range(u1, u2):
                    atlas.clear(x, y)

    def clear_face(atlas, rect):
        u1, v1, u2, v2 = rect
        for y in range(v1, v2):
            for x in range(u1, u2):
                atlas.clear(x, y)

    trim = ('band', {'row': 0, 'size': 1, 'mat': 'verdigris'})
    hem = ('band', {'row': -1, 'size': 1, 'mat': 'verdigris'})
    sides = ('north', 'south', 'east', 'west')

    # Layer 1 -- head (0,0): the hood's dark lining; the face stays open under a shadowed brow
    head = paint_box(layer1, 0, 0, 8, 8, 8, 'robe_lining', seed=41)
    u1, v1, u2, v2 = head['north']
    for y in range(v1 + 2, v2):
        for x in range(u1 + 1, u2 - 1):
            layer1.clear(x, y)
    # hat (32,0): the deep outer hood with a verdigris-trimmed opening
    hood = paint_box(layer1, 32, 0, 8, 8, 8, 'robe', [hem], seed=42)
    fh = Face(layer1, hood['north'], PAL['verdigris'], 'north')
    for x in range(1, 7):
        fh.put(x, 1, 2 if x % 3 else 3)          # the hood's front hem over the brow
    for y in range(1, 8):
        fh.put(0, y, 2 if y % 3 else 3)
        fh.put(7, y, 1)
    u1, v1, u2, v2 = hood['north']
    for y in range(v1 + 2, v2):
        for x in range(u1 + 1, u2 - 1):
            layer1.clear(x, y)
    fb = Face(layer1, hood['south'], PAL['robe'], 'south')
    for y in range(1, 7):                          # a stitched seam down the back of the cowl
        fb.put(3, y, 0)
        fb.put(4, y, 3 if y % 2 else 2)
    ft = Face(layer1, hood['up'], PAL['robe'], 'up')
    for y in range(8):
        ft.put(3, y, 1)
        ft.put(4, y, 3)
    clear_face(layer1, head['down'])               # open underneath, like vanilla helmets
    clear_face(layer1, hood['down'])
    # body (16,16): robe with a verdigris collar, front trim, belt and a violet crystal clasp
    chest = paint_box(layer1, 16, 16, 8, 12, 4, 'robe', [trim, ('band', {'row': 8, 'size': 1, 'mat': 'verdigris'})], seed=43)
    fv = Face(layer1, chest['north'], PAL['verdigris'], 'north')
    for y in range(1, 12):
        if y != 8:
            fv.put(3, y, 2)
            fv.put(4, y, 1)
    fc = Face(layer1, chest['north'], PAL['crystal'], 'north')
    for (x, y), t in (((3, 2), 4), ((4, 2), 3), ((3, 3), 3), ((4, 3), 2), ((3, 9), 3), ((4, 9), 2)):
        fc.put(x, y, t)                           # clasp at the collar, a smaller gem on the belt
    fs = Face(layer1, chest['south'], PAL['verdigris'], 'south')
    for y, row in enumerate(['.#.#.', '..#..', '.#.#.']):   # Seyr rune between the shoulders
        for x, ch in enumerate(row):
            if ch == '#':
                fs.put(1 + x, 3 + y, 3)
    # arms (40,16): sleeves under verdigris shoulder plates, verdigris cuffs
    arm = paint_box(layer1, 40, 16, 4, 12, 4, 'robe', [('band', {'row': 9, 'size': 2, 'mat': 'verdigris'})], seed=44)
    for key in ('up',) + sides:
        f = Face(layer1, arm[key], PAL['verdigris'], key)
        rows = range(f.h) if key == 'up' else range(3)
        for y in rows:
            for x in range(f.w):
                f.put(x, y, 3 if y == 0 else (1 if y == 2 else 2))
    # boots (0,16): soft cloth wraps from the shin down, verdigris cuff and toe, leather sole
    boot = paint_box(layer1, 0, 16, 4, 12, 4, 'robe', [('band', {'row': 6, 'size': 1, 'mat': 'verdigris'})], seed=45)
    clear_rows(layer1, boot, sides, 0, 6)
    clear_face(layer1, boot['up'])
    for key in sides:
        f = Face(layer1, boot[key], PAL['robe'], key)
        for y in range(7, 11):
            f.put((y + (1 if key in ('east', 'south') else 0)) % 4, y, 4)   # crossed wraps
        fl = Face(layer1, boot[key], PAL['leather'], key)
        for x in range(4):
            fl.put(x, 11, 1)
    ftoe = Face(layer1, boot['north'], PAL['verdigris'], 'north')
    for x in range(4):
        ftoe.put(x, 10, 2)
    fsole = Face(layer1, boot['down'], PAL['leather'], 'down')
    for y in range(4):
        for x in range(4):
            fsole.put(x, y, 1 if (x + y) % 3 else 0)

    # Layer 2 -- waist (16,16): only the sash band and the skirt's top show below a chestplate
    waist = paint_box(layer2, 16, 16, 8, 12, 4, 'robe', [('band', {'row': 7, 'size': 2, 'mat': 'verdigris'})], seed=46)
    clear_rows(layer2, waist, sides, 0, 7)
    clear_face(layer2, waist['up'])
    fw = Face(layer2, waist['north'], PAL['crystal'], 'north')
    fw.put(3, 7, 4)
    fw.put(4, 7, 3)
    fw.put(3, 8, 3)
    fw.put(4, 8, 2)
    # legs (0,16): the robe skirt, split per leg, with a verdigris hem
    paint_box(layer2, 0, 16, 4, 12, 4, 'robe', [('band', {'row': 10, 'size': 1, 'mat': 'verdigris'}), hem], seed=47)
    return layer1.image, layer2.image


def write():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, im in ICONS.items():
        im.save(OUT / f'{name}.png')
    l1, l2 = archivist_layers()
    l1.save(OUT / 'archivist_layer_1.png')
    l2.save(OUT / 'archivist_layer_2.png')
    # preview sheet: every icon (items_later + Seyr) on top, the archivist layers below
    icons = list(items_later.ICONS.values()) + list(ICONS.values())
    scale, cols = 6, 10
    rows = (len(icons) + cols - 1) // cols
    lw, lh = 64 * scale, 32 * scale
    sheet = Image.new('RGBA', (cols * 17 * scale, rows * 17 * scale + lh + 2 * scale), (46, 46, 54, 255))
    for i, im in enumerate(icons):
        big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
        sheet.paste(big, ((i % cols) * 17 * scale, (i // cols) * 17 * scale), big)
    for j, layer in enumerate((l1, l2)):
        big = layer.resize((lw, lh), Image.NEAREST)
        sheet.paste(big, (j * (lw + 4 * scale), rows * 17 * scale + scale), big)
    sheet.save(OUT / 'preview.png')
    return OUT / 'preview.png'


SLICE_ITEMS = ('memory_shard', 'memory_staff', 'watchers_eye', 'knowledge_fragment', 'memory_lens',
               'archivist_hood', 'archivist_robe', 'archivist_leggings', 'archivist_boots')
SPAWN_EGGS = ('forgotten_watcher_spawn_egg', 'lost_pilgrim_spawn_egg', 'memory_wraith_spawn_egg',
              'soul_collector_spawn_egg')
STAFF_DISPLAY = {   # handheld like sovereigns_ruin.json, but a slimmer staff
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.5, 1], 'scale': [1.1, 1.1, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.5, 1], 'scale': [1.1, 1.1, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.4, 1.13], 'scale': [0.8, 0.8, 0.8]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.4, 1.13], 'scale': [0.8, 0.8, 0.8]},
}


def install():
    """Copy the Seyr item textures and armor layers into both resource trees and write their item
    models (the spawn eggs copy the form of an existing *_spawn_egg.json in the same tree)."""
    import json
    import shutil
    written = []
    for version in ('1.20.1', '1.21.1'):
        assets = ROOT / version / 'common/src/main/resources/assets/remnant_bosses'
        egg = (assets / 'models/item/gravebound_warden_spawn_egg.json').read_bytes()
        for sub in ('textures/item', 'models/item', 'textures/models/armor'):
            (assets / sub).mkdir(parents=True, exist_ok=True)
        for name in SLICE_ITEMS:
            shutil.copyfile(OUT / f'{name}.png', assets / 'textures/item' / f'{name}.png')
            model = {'parent': 'minecraft:item/handheld' if name == 'memory_staff' else 'minecraft:item/generated',
                     'textures': {'layer0': f'remnant_bosses:item/{name}'}}
            if name == 'memory_staff':
                model['display'] = STAFF_DISPLAY
            (assets / 'models/item' / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
            written += [f'{version}: textures/item/{name}.png', f'{version}: models/item/{name}.json']
        for layer in ('archivist_layer_1', 'archivist_layer_2'):
            shutil.copyfile(OUT / f'{layer}.png', assets / 'textures/models/armor' / f'{layer}.png')
            written.append(f'{version}: textures/models/armor/{layer}.png')
        for name in SPAWN_EGGS:
            (assets / 'models/item' / f'{name}.json').write_bytes(egg)
            written.append(f'{version}: models/item/{name}.json')
    return written


if __name__ == '__main__':
    if sys.argv[1:] == ['--install']:
        print('\n'.join(install()))
    else:
        print(write())
