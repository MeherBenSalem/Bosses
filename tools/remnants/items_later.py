"""Item icons for the Warden, Watcher, Knight, Colossus, Titan and Oracle bosses, written to
docs/models/remnants/items/. Same ink conventions as items.py, plus the civilization-specific inks
below.
Run: python tools/remnants/items_later.py  (writes PNGs + a preview sheet)
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import items  # noqa: E402
from paint import PALETTE, hex_rgb  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/models/remnants/items'

# extra inks: rust (r-family uses '7','8','9'), bone ('0'), crystal violet ('V','W'), ember ('E','F'),
# bronze ('B','C'), moss/leaf ('M','N'), heartwood ('h','i'), oracle rose ('P','Q'), umbra ('X')
EXTRA = {
    '7': hex_rgb('#33170C'), '8': hex_rgb('#6A361D'), '9': hex_rgb('#A6623B'),
    '0': PALETTE['bone'].tone(4), ')': PALETTE['bone'].tone(2),
    'V': PALETTE['crystal'].tone(3), 'W': PALETTE['crystal'].tone(5), 'w': PALETTE['crystal'].tone(1),
    'E': PALETTE['ember'].tone(3), 'F': PALETTE['ember'].tone(5),
    'B': PALETTE['bronze'].tone(3), 'C': PALETTE['bronze'].tone(5), 'D': PALETTE['bronze'].tone(1),
    'M': PALETTE['leaves'].tone(2), 'N': PALETTE['leaves'].tone(4),
    'h': PALETTE['heartwood'].tone(2), 'i': PALETTE['heartwood'].tone(4),
    'P': PALETTE['oracle'].tone(2), 'Q': PALETTE['oracle'].tone(4), 'O': PALETTE['oracle'].tone(6),
    'X': (6, 6, 9), 'Y': PALETTE['marble'].tone(4), 'Z': PALETTE['marble'].tone(6),
    'A': PALETTE['amber'].tone(1), 'T': PALETTE['amber'].tone(3),
}
items.INK.update(EXTRA)
g = items.grid

ICONS = {
    # Gravebound Warden (Ossuary)
    'grave_iron': g(['................', '................', '................', '....889889......', '...8988998......',
                     '..79988899987...', '..7898888898777.', '..78888888887...', '...7788888877...', '....77777777....',
                     '................', '................', '................', '................', '................', '................']),
    'wardens_grasp': g(['............dd..', '...........deed.', '..........de..ed', '..........de..ed', '...........deed.',
                        '............dd..', '..........cc....', '.........cc.....', '........dd......', '.......cc.......',
                        '......dd........', '....898.........', '...89998........', '..899998........', '..7899987.......', '...77777........']),
    'gravebound_executioner': g(['.......8998.....', '......899998....', '.....89999998...', '.....8988899....', '.....89888998...',
                                 '......8888998...', '.......7889c....', '........yc......', '.......yc.......', '......yc........',
                                 '.....yc.........', '....yc..........', '...yc...........', '..dc............', '..d.............', '................']),
    'crypt_sigil': g(['................', '.....aaaaaa.....', '....a888888a....', '...a89000098a...', '..a8900330098a..',
                      '..a8903443098a..', '..a8900330098a..', '..a8909009098a..', '...a89000098a...', '....a888888a....',
                      '.....aa88aa.....', '.......dd.......', '......d..d......', '.......dd.......', '................', '................']),
    # Forgotten Watcher (Seyr)
    'memory_shard': g(['................', '.......W........', '......WVw.......', '......VWV.......', '.....VWVVw......',
                       '.....VWWVw......', '....wVWWVVw.....', '....VWWWWVw.....', '.....VWWVw......', '.....wVVw.......',
                       '......ww........', '................', '................', '................', '................', '................']),
    'memory_staff': g(['...........WW...', '..........WVVW..', '..........VWWV..', '...........VV...', '..........JJ....',
                       '.........JKJ....', '........hi......', '.......hi.......', '......hi........', '.....hi.........',
                       '....hi..........', '...hi...........', '..hi............', '.JJ.............', '.J..............', '................']),
    'watchers_eye': g(['................', '.....JJJJJ......', '...JJKKKKKJJ....', '..JK.......KJ...', '.JK..wwww...KJ..',
                       '.JK.wVWWVw..KJ..', 'JK.wVWXXWVw..KJ.', 'JK.wVWXXWVw..KJ.', '.JK.wVWWVw..KJ..', '.JK..wwww...KJ..',
                       '..JK.......KJ...', '...JJKKKKKJJ....', '.....JJJJJ......', '................', '................', '................']),
    # Eclipsed Knight (Vael)
    'eclipsed_blade': g(['..............r.', '.............rq.', '............rqX.', '...........rqX..', '..........rqX...',
                         '.........rqX....', '........rqX.....', '.......rqX......', '......rqX.......', '...F.rqX........',
                         '....FqX.........', '.....yF.........', '....yyEF........', '...yy..F........', '..dd............', '.d..............']),
    'eclipsed_oath': g(['................', '....FEEEEEF.....', '...FE.....EF....', '..FE.XXXXX.EF...', '..E.XXXXXXX.E...',
                        '.FE.XXXXXXX.EF..', '.E..XXXXXXX..E..', '.FE.XXXXXXX.EF..', '..E.XXXXXXX.E...', '..FE.XXXXX.EF...',
                        '...FE.....EF....', '....FEEEEEF.....', '................', '................', '................', '................']),
    # Ashen Colossus (Kharum)
    'ashen_cog': g(['................', '......BB........', '...B.BCCB.B.....', '...BBCCCCBB.....', '....BCDDCB......',
                    '.BBBCD..DCBBB...', '.BCCD....DCCB...', '.BCCD....DCCB...', '.BBBCD..DCBBB...', '....BCDDCB......',
                    '...BBCCCCBB.....', '...B.BCCB.B.....', '......BB........', '................', '................', '................']),
    'ashen_warhammer': g(['...DBBBBBBD.....', '..DBCCCCCCBD....', '..DBEEFFEEBD....', '..DBCCCCCCBD....', '...DBBBBBBD.....',
                          '......dd........', '......dc........', '......dc........', '......dc........', '......dc........',
                          '......dc........', '......yz........', '......yz........', '......yz........', '.....BCCB.......', '................']),
    'colossus_core': g(['................', '.....DBBBD......', '....DBCCCBD.....', '...DBEEEEEBD....', '...BEEFFFEEB....',
                        '...BEFFFFFEB....', '...BEFFFFFEB....', '...BEEFFFEEB....', '...DBEEEEEBD....', '....DBCCCBD.....',
                        '.....DBBBD......', '................', '................', '................', '................', '................']),
    # Rootbound Titan (Thornwood)
    'heartwood_sliver': g(['................', '..........i.....', '.........ih.....', '........ihh.....', '.......ihhA.....',
                           '......ihhT......', '.....ihhA.......', '....ihhh........', '...ihhh.........', '..ihh...........',
                           '..hh............', '................', '................', '................', '................', '................']),
    'rootbound_greatshield': g(['..NMMMMMMMMMN...', '.NMhhhhhhhhhMN..', '.Mhiihhhhiihh M.', '.Mhhh.TTT.hhhM..', '.Mhh.TAAAT.hhM..',
                                '.Mhh.TAAAT.hhM..', '.Mhhh.TTT.hhhM..', '.Mhihhhhhhihh M.', '..Mhhhhhhhhhh...', '..Mhhihhhhihh...',
                                '...Mhhhhhhhh....', '....Mhhhhhh.....', '.....Mhhhh......', '......Mhh.......', '.......M........', '................']),
    'titan_heart': g(['................', '....MM...MM.....', '...MNNM.MNNM....', '..MhhhhMhhhhM...', '..MhTTTAATTThM..',
                      '..MhTAAAAAAThM..', '...MhTAAAAThM...', '....MhTAAThM....', '.....MhTThM.....', '......MhhM......',
                      '.......MM.......', '................', '................', '................', '................', '................']),
    # Shattered Oracle (Sanctum)
    'relic_shard': g(['................', '................', '.......Z........', '......ZYP.......', '.....ZYYPP......',
                      '....ZYYYPQP.....', '....YYYPQOQ.....', '.....YYPQQ......', '......YPQ.......', '.......P........',
                      '................', '................', '................', '................', '................', '................']),
    'oracles_staff': g(['....JKJ.KJK.....', '...JK.QQQ.KJ....', '...K.QOOOQ.K....', '...J.QOOOQ.J....', '...JK.QQQ.KJ....',
                        '....JKJJJKJ.....', '.......YZ.......', '.......YZ.......', '.......YZ.......', '.......YZ.......',
                        '.......JK.......', '.......YZ.......', '.......YZ.......', '.......YZ.......', '......JKKJ......', '................']),
    'shattered_halo': g(['................', '....JKK..KJ.....', '..JK.......KJ...', '.JK.........J...', '.K...........K..',
                         'J............J..', 'K...............', 'J............J..', '.K...........K..', '.JK.........JK..',
                         '..JK.......K....', '....JK..KKJ.....', '................', '................', '................', '................']),
    'divine_remnant': g(['................', '.......OO.......', '......OQQO......', '.....OQPPQO.....', '....OQPZZPQO....',
                         '....OQPZZPQO....', '.....OQPPQO.....', '......OQQO......', '.......OO.......', '......J..J......',
                         '.....J....J.....', '................', '................', '................', '................', '................']),
}


def write():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, im in ICONS.items():
        im.save(OUT / f'{name}.png')
    scale = 6
    cols = 10
    rows = (len(ICONS) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * 17 * scale, rows * 17 * scale), (46, 46, 54, 255))
    for i, im in enumerate(ICONS.values()):
        big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
        sheet.paste(big, ((i % cols) * 17 * scale, (i // cols) * 17 * scale), big)
    sheet.save(OUT / 'preview.png')
    return OUT / 'preview.png'


if __name__ == '__main__':
    print(write())


# Warden armor layers use the vanilla humanoid armor UV layout (64x32).
def warden_layers():
    import random
    sys.path.insert(0, str(Path(__file__).resolve().parent / 'models'))
    from gravebound_warden import PALETTE as WARDEN
    from paint import Atlas, Face, STYLES, apply_decal
    pal = dict(PALETTE, **WARDEN)
    layer1, layer2 = Atlas(64, 32), Atlas(64, 32)

    def paint_box(atlas, u, v, w, h, d, mat, decals=(), seed=0):
        rects = {}
        for name, (x, y, fw, fh) in items.box_faces(u, v, w, h, d).items():
            key = {'top': 'up', 'bottom': 'down', 'front': 'north', 'back': 'south', 'right': 'east', 'left': 'west'}[name]
            rects[key] = (x, y, x + fw, y + fh)
        rng = random.Random(seed)
        for key, rect in rects.items():
            STYLES[pal[mat].style](Face(atlas, rect, pal[mat], key), rng, pal)
        for decal in decals:
            apply_decal(atlas, rects, decal, pal, rng)
        return rects

    iron_rim = ('band', {'row': 0, 'size': 1, 'mat': 'iron_dark'})
    iron_hem = ('band', {'row': -1, 'size': 1, 'mat': 'iron_dark'})
    head = paint_box(layer1, 0, 0, 8, 8, 8, 'rust', [iron_hem, ('rivets', {'mat': 'iron', 'spacing': 2})], seed=11)
    fn = Face(layer1, head['north'], pal['void'], 'north')
    for x in range(1, 7):
        fn.put(x, 4, 1)          # the single executioner's slit
    fs = Face(layer1, head['north'], pal['soul_dim'], 'north')
    fs.put(2, 4, 3)
    fs.put(5, 4, 3)
    hood = paint_box(layer1, 32, 0, 8, 8, 8, 'grave_cloth', seed=12)   # hat layer: the grave hood over the helm
    for key in ('north',):
        u1, v1, u2, v2 = hood[key]
        for y in range(v1 + 2, v2):
            for x in range(u1 + 1, u2 - 1):
                layer1.clear(x, y)   # open face under the hood
    chest = paint_box(layer1, 16, 16, 8, 12, 4, 'rust', [iron_rim, ('band', {'row': 8, 'size': 1, 'mat': 'iron_dark'})], seed=13)
    fc = Face(layer1, chest['north'], pal['chain'], 'north')
    for i in range(8):      # chains crossing the chest
        fc.put(i, 1 + i, 3 if i % 2 else 1)
        fc.put(7 - i, 1 + i, 3 if i % 2 else 1)
    fb = Face(layer1, chest['north'], pal['bone'], 'north')
    for x, y in ((3, 4), (4, 4), (3, 5), (4, 5)):
        fb.put(x, y, 4)      # bone clasp
    paint_box(layer1, 40, 16, 4, 12, 4, 'rust', [iron_rim, ('band', {'row': 6, 'size': 1, 'mat': 'chain'})], seed=14)
    paint_box(layer1, 0, 16, 4, 12, 4, 'rust', [('band', {'row': 7, 'size': 1, 'mat': 'iron_dark'})], seed=15)
    paint_box(layer2, 16, 16, 8, 12, 4, 'grave_cloth', [('band', {'row': 7, 'size': 2, 'mat': 'rust'})], seed=16)
    paint_box(layer2, 0, 16, 4, 12, 4, 'grave_cloth', [('band', {'row': 4, 'size': 1, 'mat': 'rust'}), ('band', {'row': 8, 'size': 1, 'mat': 'chain'})], seed=17)
    return layer1.image, layer2.image


if __name__ == '__main__' and len(sys.argv) > 1 and sys.argv[1] == '--warden-armor':
    a, b = warden_layers()
    a.save(OUT / 'warden_layer_1.png')
    b.save(OUT / 'warden_layer_2.png')
    print('warden layers written')
