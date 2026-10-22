"""Hand-drawn 16x16 item icons and armor layers for the Vael Remnants items.

Icons are character grids painted from the shared material ramps (paint.PALETTE), then given a
selective dark outline (only where the shape meets transparency) like vanilla item sprites.
Run: python tools/remnants/items.py [--preview out.png]
"""
import json
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paint import PALETTE  # noqa: E402

ROOT = Path(__file__).resolve().parents[1].parent
T = PALETTE
OUTLINE = (10, 10, 14, 255)

# Character -> colour. Lowercase iron/steel, uppercase gold/royal, digits soul.
INK = {
    'a': T['iron'].tone(0), 'b': T['iron'].tone(1), 'c': T['iron'].tone(2), 'd': T['iron'].tone(3),
    'e': T['iron'].tone(4), 'f': T['iron'].tone(5), 'g': T['iron'].tone(6),
    'm': T['blade'].tone(1), 'n': T['blade'].tone(2), 'o': T['blade'].tone(3), 'p': T['blade'].tone(4),
    'q': T['blade'].tone(5), 'r': T['blade'].tone(6),
    'G': T['gold'].tone(1), 'H': T['gold'].tone(2), 'I': T['gold'].tone(3), 'J': T['gold'].tone(4),
    'K': T['gold'].tone(5), 'L': T['gold'].tone(6),
    'R': T['royal'].tone(1), 'S': T['royal'].tone(2), 'U': T['royal'].tone(3), 'V': T['royal'].tone(4),
    'x': T['leather'].tone(1), 'y': T['leather'].tone(3), 'z': T['leather'].tone(5),
    '1': T['soul'].tone(1), '2': T['soul'].tone(2), '3': T['soul'].tone(3), '4': T['soul'].tone(4),
    '5': T['soul'].tone(5), '6': T['soul'].tone(6),
    'k': OUTLINE[:3],
}


def grid(rows, outline=True):
    h, w = len(rows), max(len(r) for r in rows)
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in INK:
                im.putpixel((x, y), (*INK[ch], 255))
    if outline:
        im = outlined(im)
    return im


def outlined(im):
    out = im.copy()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if im.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and im.getpixel((nx, ny))[3] and im.getpixel((nx, ny))[:3] != OUTLINE[:3]:
                    out.putpixel((x, y), OUTLINE)
                    break
    return out


def sovereigns_ruin():
    """Greatsword, tip top-right: honed edges, a soul-lit fuller, gilt guard, wrapped grip."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    def put(x, y, ch):
        if 0 <= x < 16 and 0 <= y < 16:
            im.putpixel((x, y), (*INK[ch], 255))
    # blade: three diagonals around x + y = 15, from the tip (14,1) down to (6,9)
    for i in range(1, 10):
        cx, cy = 15 - i, i
        put(cx, cy, '4' if i % 3 else '5')        # soul fuller
        put(cx - 1, cy, 'q' if i < 8 else 'p')    # lit edge
        put(cx, cy - 1, 'r' if i < 4 else 'q')
        put(cx, cy + 1, 'n')                      # shadowed edge
        put(cx + 1, cy, 'o')
    put(15, 0, 'r')
    put(14, 0, 'q')
    put(15, 1, 'p')
    # crossguard perpendicular to the blade, with a soul gem
    for (x, y), ch in zip([(3, 9), (4, 10), (5, 11), (6, 12), (7, 13), (8, 14)], 'GIJKJI'):
        put(x, y, ch)
    for (x, y), ch in zip([(2, 8), (9, 15)], 'HH'):
        put(x, y, ch)
    put(5, 10, 'J')
    put(6, 11, '5')
    put(4, 9, 'K')
    # grip and pommel
    for (x, y), ch in zip([(4, 12), (3, 13), (2, 14)], 'yzy'):
        put(x, y, ch)
    put(5, 12, 'x')
    put(3, 12, 'x')
    put(1, 15, 'J')
    put(0, 15, 'K')
    put(1, 14, 'L')
    return outlined(im)


def hollow_crown():
    return grid([
        '................',
        '................',
        '.......L........',
        '...K...K...K....',
        '...J..KJK..J..L.',
        '..KJK.JIJ.KJK.K.',
        '..JIJ.JIJ.JIJ...',
        '..JIJJJIJJJIJJ..',
        '..JIIIIIIIIIIJ..',
        '..KLKKK5KKKLKK..',
        '..JII4I6I4IIIJ..',
        '..IHGGG5GGGGHI..',
        '...GGGGGGGGGGG..',
        '................',
        '................',
        '................',
    ])


def sovereigns_sigil():
    """A signet disc: gilt rim, a crowned soul-gem, hung from a blue ribbon."""
    return grid([
        '......U..U......',
        '.....UVUUVU.....',
        '......UVVU......',
        '.....KJJJJK.....',
        '....KLJIIJLK....',
        '...KJIL.L.IJK...',
        '...JIKLKLKIIJ...',
        '..KJI.3443.IJK..',
        '..JIIJ4664JIIJ..',
        '..JIIJ4664JIIJ..',
        '..KJI.3443.IJK..',
        '...JIIJ33JIIJ...',
        '...KJIIIIIIJK...',
        '....KJIIIIJK....',
        '.....KGGGGK.....',
        '................',
    ])


def sovereigns_seal():
    """A wax-and-gold royal seal stamped with the Vael crest."""
    return grid([
        '................',
        '.....GIIIIG.....',
        '...GIJKKKKJIG...',
        '..GJKSSSSSSKJG..',
        '..IKSSUKKUSSKI..',
        '.GJSSUKLLKUSSJG.',
        '.IKSSKL55LKSSKI.',
        '.IKSSKL55LKSSKI.',
        '.GJSSUKLLKUSSJG.',
        '..IKSSUKKUSSKI..',
        '..GJKSSSSSSKJG..',
        '...GIJKKKKJIG...',
        '.....GIIIIG.....',
        '......VU.UV.....',
        '.....UV...VU....',
        '................',
    ])


def tarnished_regalia():
    """A torn shard of blackened plate with a run of gilt trim along its top edge."""
    return grid([
        '................',
        '................',
        '...JKKJ.........',
        '..cJIIJKJ.......',
        '..cddeefJK......',
        '.cdeegffeJK.....',
        '.cdefgfeedIJ....',
        '.cdeefeeeddIJ...',
        '..cdeeeeedcbJ...',
        '..cdedeeedc.I...',
        '...cdddedcb.....',
        '...bcddcb.......',
        '....bcb.........',
        '.....b..........',
        '................',
        '................',
    ])


def royal_soul_fragment():
    """A shard of the king's soul: a cold flame caught in a sliver of crystal."""
    return grid([
        '................',
        '........4.......',
        '.......45.......',
        '.......46.......',
        '......3564......',
        '......4664......',
        '.....346653.....',
        '.....456654.....',
        '....34566543....',
        '....24666542....',
        '.....345543.....',
        '.....234432.....',
        '......2332......',
        '.......11.......',
        '................',
        '................',
    ])


def regalia_cuirass():
    return grid([
        '................',
        '..dfe......efd..',
        '.cJKe......eKJc.',
        '.cdJeddddddeJdc.',
        '..cdIedeedeIdc..',
        '...cde4ff4edc...',
        '...cde5665edc...',
        '...cdee55eedc...',
        '...cdeeeeeedc...',
        '...cJKJJJJKJc...',
        '...cdeeeeeedc...',
        '...bcdeeeedcb...',
        '....bcdddddb....',
        '....GIJJJJIG....',
        '................',
        '................',
    ])


def regalia_greaves():
    return grid([
        '................',
        '...JKJJJJJKJ....',
        '...cdeeeeeedc...',
        '...cdeedeedcd...',
        '...cdeec.cedc...',
        '...cdec...edc...',
        '...JKJ....JKJ...',
        '...cdc....cdc...',
        '...cec....ced...',
        '...cec....cec...',
        '...cec....cec...',
        '...cdc....cdc...',
        '...IJI....IJI...',
        '...bcb....bcb...',
        '................',
        '................',
    ])


def regalia_sabatons():
    return grid([
        '................',
        '................',
        '................',
        '................',
        '................',
        '..JKJ......JKJ..',
        '..cec......cec..',
        '..cec......cec..',
        '..cde......cde..',
        '.cdeed....cdeed.',
        'cdeeed...cdeeed.',
        'IJJJJI...IJJJJI.',
        'bccccb...bccccb.',
        '................',
        '................',
        '................',
    ])


ICONS = {
    'sovereigns_ruin': sovereigns_ruin, 'hollow_crown': hollow_crown, 'sovereigns_sigil': sovereigns_sigil,
    'sovereigns_seal': sovereigns_seal, 'tarnished_regalia': tarnished_regalia,
    'royal_soul_fragment': royal_soul_fragment, 'regalia_cuirass': regalia_cuirass,
    'regalia_greaves': regalia_greaves, 'regalia_sabatons': regalia_sabatons,
}
HANDHELD = {'sovereigns_ruin'}


# Armor layers use the vanilla humanoid armor UV layout (64x32).

def box_faces(u, v, w, h, d):
    """Vanilla box-UV face rectangles for a cuboid whose UV origin is (u, v)."""
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h),
    }


def regalia_layers():
    from paint import Face, STYLES, Atlas, apply_decal
    import random
    pal = PALETTE
    layer1, layer2 = Atlas(64, 32), Atlas(64, 32)

    def paint_box(atlas, u, v, w, h, d, mat, decals=(), seed=0, faces=None):
        rects = {}
        for name, (x, y, fw, fh) in box_faces(u, v, w, h, d).items():
            key = {'top': 'up', 'bottom': 'down', 'front': 'north', 'back': 'south', 'right': 'east', 'left': 'west'}[name]
            rects[key] = (x, y, x + fw, y + fh)
        rng = random.Random(seed)
        for key, rect in rects.items():
            if faces and key not in faces:
                continue
            STYLES[pal[mat].style](Face(atlas, rect, pal[mat], key), rng, pal)
        for decal in decals:
            apply_decal(atlas, rects, decal, pal, rng)
        return rects

    gold_rim = ('band', {'row': 0, 'size': 1, 'mat': 'gold'})
    gold_hem = ('band', {'row': -1, 'size': 1, 'mat': 'gold'})
    # Layer 1: helmet (head box 8x8x8 at 0,0), chest (20,16 8x12x4), arms (40,16 4x12x4), boots (0,16 4x12x4)
    head = paint_box(layer1, 0, 0, 8, 8, 8, 'iron', [gold_hem], seed=1)
    # Crowned half-helm: the top two rows of the sides become gilt crown points over an open crown
    for key in ('north', 'south', 'east', 'west'):
        u1, v1, u2, v2 = head[key]
        f = Face(layer1, head[key], pal['gold'], key)
        for x in range(u2 - u1):
            point = x % 3 == 1 or (key == 'north' and x in (3, 4))
            f.put(x, 0, pal['gold'].top if point else 0) if point else f.clear(x, 0)
            f.put(x, 1, 5 if point else 4)
            f.put(x, 2, 3 if x % 2 else 4)
        if key == 'north':  # visor slit with soul eyes, and the crown gem
            fn = Face(layer1, head[key], pal['iron'], key)
            for x in range(1, 7):
                fn.put(x, 5, 0)
            fs = Face(layer1, head[key], pal['soul'], key)
            fs.put(2, 5, 4)
            fs.put(5, 5, 4)
            fs.put(3, 2, 5)
            fs.put(4, 2, 5)
    u1, v1, u2, v2 = head['up']
    for y in range(v1 + 1, v2 - 1):
        for x in range(u1 + 1, u2 - 1):
            layer1.clear(x, y)
    # Hat overlay layer (32,0) left empty: the crown is painted into the helmet itself.
    paint_box(layer1, 16, 16, 8, 12, 4, 'iron', [gold_rim, ('filigree', {'mat': 'gold', 'row': 2, 'size': 2, 'faces': 'north'}),
                                                  ('band', {'row': 8, 'size': 1, 'mat': 'gold'})], seed=2)
    chest = box_faces(16, 16, 8, 12, 4)['front']
    fs = Face(layer1, (chest[0], chest[1], chest[0] + chest[2], chest[1] + chest[3]), pal['soul'], 'north')
    for (x, y), t in [((3, 5), 5), ((4, 5), 5), ((3, 6), 4), ((4, 6), 4), ((3, 4), 3), ((4, 7), 3)]:
        fs.put(x, y, t)
    paint_box(layer1, 40, 16, 4, 12, 4, 'iron', [gold_rim, ('band', {'row': 6, 'size': 1, 'mat': 'gold'})], seed=3)
    paint_box(layer1, 0, 16, 4, 12, 4, 'iron', [('band', {'row': 7, 'size': 1, 'mat': 'gold'})], seed=4)
    # Layer 2: leggings (body 16,16 waist + legs 0,16)
    paint_box(layer2, 16, 16, 8, 12, 4, 'iron', [('band', {'row': 7, 'size': 2, 'mat': 'gold'})], seed=5)
    tabard = box_faces(16, 16, 8, 12, 4)['front']
    ft = Face(layer2, (tabard[0] + 2, tabard[1] + 9, tabard[0] + 6, tabard[1] + 12), pal['royal'], 'north')
    STYLES['cloth'](ft, random.Random(9), pal)
    paint_box(layer2, 0, 16, 4, 12, 4, 'iron', [('band', {'row': 4, 'size': 1, 'mat': 'gold'})], seed=6)
    return layer1.image, layer2.image


def write_all():
    for version in ('1.20.1', '1.21.1'):
        assets = ROOT / version / 'common/src/main/resources/assets/remnant_bosses'
        (assets / 'textures/item').mkdir(parents=True, exist_ok=True)
        (assets / 'models/item').mkdir(parents=True, exist_ok=True)
        (assets / 'textures/models/armor').mkdir(parents=True, exist_ok=True)
        for name, fn in ICONS.items():
            fn().save(assets / 'textures/item' / f'{name}.png')
            model = {'parent': 'minecraft:item/handheld' if name in HANDHELD else 'minecraft:item/generated',
                     'textures': {'layer0': f'remnant_bosses:item/{name}'}}
            if name in HANDHELD:  # greatsword: carried larger than a vanilla sword
                model['display'] = {
                    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 5, 1.25], 'scale': [1.25, 1.25, 0.85]},
                    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 5, 1.25], 'scale': [1.25, 1.25, 0.85]},
                    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.6, 1.13], 'scale': [0.9, 0.9, 0.9]},
                    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.6, 1.13], 'scale': [0.9, 0.9, 0.9]},
                }
            (assets / 'models/item' / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
        l1, l2 = regalia_layers()
        l1.save(assets / 'textures/models/armor/regalia_layer_1.png')
        l2.save(assets / 'textures/models/armor/regalia_layer_2.png')


def preview(path):
    icons = [fn() for fn in ICONS.values()]
    l1, l2 = regalia_layers()
    scale = 8
    sheet = Image.new('RGBA', (len(icons) * 17 * scale, 16 * scale + 34 * scale), (60, 60, 66, 255))
    for i, ic in enumerate(icons):
        sheet.paste(ic.resize((16 * scale, 16 * scale), Image.NEAREST), (i * 17 * scale, 0), ic.resize((16 * scale, 16 * scale), Image.NEAREST))
    for j, layer in enumerate((l1, l2)):
        big = layer.resize((64 * scale // 2, 32 * scale // 2), Image.NEAREST)
        sheet.paste(big, (j * (64 * scale // 2 + 10), 17 * scale), big)
    sheet.save(path)


if __name__ == '__main__':
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    else:
        write_all()
        print('wrote', len(ICONS), 'icons and regalia armor layers into both resource trees')
