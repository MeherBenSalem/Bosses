"""Review renders for docs/models/remnants/realm/ (read back from the written asset files)."""
import json

from PIL import Image, ImageDraw, ImageFont

import render3d as R
from common import DOCS, TREES

BG = (46, 50, 64, 255)


def font(size):
    for name in ('arialbd.ttf', 'arial.ttf', 'DejaVuSans-Bold.ttf'):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def load_waygate(tree=None):
    tree = tree or TREES[0]
    geo = json.loads((tree / 'geo/block/eclipse_waygate.geo.json').read_text(encoding='utf-8'))
    anim = json.loads((tree / 'animations/block/eclipse_waygate.animation.json').read_text(encoding='utf-8'))
    base = Image.open(tree / 'textures/block/eclipse_waygate.png')
    glow = Image.open(tree / 'textures/block/eclipse_waygate_glowmask.png')
    return geo, anim, R.Tex(base, glow)


VIEWS = (('front', 0, 0), ('side', 90, 0), ('3/4', 32, -18), ('back 3/4', 212, -18))


def waygate_pose(geo, anim, tex, clip, t, views=VIEWS, scale=6, ground=True):
    pose = R.sample_pose(anim['animations'][f'animation.eclipse_waygate.{clip}'], t)
    quads = R.geo_quads(geo, tex, pose)
    if ground:
        quads += ground_quads()
    out = []
    for label, yaw, pitch in views:
        im = R.render(quads, yaw=yaw, pitch=pitch, scale=scale, size=(int(68 * scale), int(78 * scale)),
                      center=(0, 31), bg=BG)
        out.append((label, im))
    return out


_GROUND = None


def ground_quads():
    """A 5x3 patch of eclipse stone under the arch for scale (drawn 0.01 below y = 0)."""
    global _GROUND
    if _GROUND is None:
        tex = R.Tex(Image.open(TREES[0] / 'textures/block/eclipse_stone.png'))
        quads = []
        for bx in range(-2, 3):
            for bz in range(-1, 2):
                lo = (bx * 16 - 8, -16.01, bz * 16 - 8)
                hi = (lo[0] + 16, -0.01, lo[2] + 16)
                quads += R.box_quads(lo, hi, {'up': ((0, 0, 16, 16), tex)}, R.T(0, 0, 0))
        _GROUND = quads
    return _GROUND


POSES = (('dormant', 1.0, 'dormant  t=1.0'), ('kindle', 1.5, 'kindle  t=1.5  runes lighting'),
         ('kindle', 2.6, 'kindle  t=2.6  eclipse cracks'), ('kindle', 5.0, 'kindle  t=5.0  vortex opening'),
         ('kindle', 5.5, 'kindle  t=5.5  overshoot'), ('open', 1.0, 'open  t=1.0'), ('open', 3.0, 'open  t=3.0'))


def waygate_sheet(path_name='waygate_poses.png'):
    geo, anim, tex = load_waygate()
    rows = []
    for clip, t, label in POSES:
        rows.append((label, waygate_pose(geo, anim, tex, clip, t)))
    cw, ch = rows[0][1][0][1].size
    head = 30
    sheet = Image.new('RGBA', (cw * len(VIEWS), (ch + head) * len(rows)), (24, 26, 34, 255))
    d = ImageDraw.Draw(sheet)
    f = font(18)
    for r, (label, ims) in enumerate(rows):
        y = r * (ch + head)
        d.text((8, y + 5), label, fill=(240, 220, 160, 255), font=f)
        for c, (vl, im) in enumerate(ims):
            sheet.paste(im, (c * cw, y + head))
            d.text((c * cw + 8, y + head + 6), vl, fill=(200, 200, 210, 255), font=font(14))
    DOCS.mkdir(parents=True, exist_ok=True)
    sheet.save(DOCS / path_name)
    return DOCS / path_name


def waygate_key_poses():
    """One image per clip key pose (front + side + 3/4) for quick review."""
    geo, anim, tex = load_waygate()
    paths = []
    for clip, t in (('dormant', 1.0), ('kindle', 2.6), ('kindle', 5.5), ('open', 1.0)):
        ims = waygate_pose(geo, anim, tex, clip, t, VIEWS[:3])
        cw, ch = ims[0][1].size
        sheet = Image.new('RGBA', (cw * 3, ch), BG)
        for i, (_, im) in enumerate(ims):
            sheet.paste(im, (i * cw, 0))
        p = DOCS / f'waygate_{clip}_{t:.1f}.png'
        sheet.save(p)
        paths.append(p)
    return paths


def waygate_gif(path_name='waygate_kindle_open.gif', fps=12):
    geo, anim, tex = load_waygate()
    frames = []
    seq = [('dormant', i / fps) for i in range(int(1.0 * fps))]
    seq += [('kindle', i / fps) for i in range(int(6.0 * fps) + 1)]
    seq += [('open', i / fps) for i in range(int(4.0 * fps))]
    for clip, t in seq:
        im = waygate_pose(geo, anim, tex, clip, t, (('3/4', 28, -14),), scale=4)[0][1]
        frames.append(im.convert('RGB').quantize(colors=255, method=Image.Quantize.MEDIANCUT))
    p = DOCS / path_name
    frames[0].save(p, save_all=True, append_images=frames[1:], duration=int(1000 / fps), loop=0, optimize=False)
    return p


def block_model_image(model_rel, textures, yaw=35, pitch=-25, scale=10, size=(200, 200), centre_y=7):
    tree = TREES[0]
    model = json.loads((tree / 'models/block' / model_rel).read_text(encoding='utf-8'))
    tex = {k: R.Tex(Image.open(tree / 'textures' / (v.split(':', 1)[1] + '.png'))) for k, v in model['textures'].items()}
    quads = R.block_quads(model, tex, R.T(-8, 0, -8))
    return R.render(quads, yaw=yaw, pitch=pitch, scale=scale, size=size, center=(0, centre_y), bg=(0, 0, 0, 0))


def cube_image(textures, yaw=35, pitch=-25, scale=10, size=(200, 200)):
    """textures: dict face -> PIL image (16x16 or a strip; first frame used)."""
    faces = {}
    for f, im in textures.items():
        faces[f] = ((0, 0, 16, 16), R.Tex(im.crop((0, 0, 16, 16))))
    quads = R.box_quads((-8, 0, -8), (8, 16, 8), faces, R.T(0, 0, 0))
    return R.render(quads, yaw=yaw, pitch=pitch, scale=scale, size=size, center=(0, 7), bg=(0, 0, 0, 0))
