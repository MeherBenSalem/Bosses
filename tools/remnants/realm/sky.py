"""Sky textures (textures/environment/): the Eclipse and the Gracetree.

Sky art may use smooth alpha. The eclipse must be drawn with normal alpha blending so its black
disc hides the sky behind it; the gracetree reads with either additive or alpha blending (alpha
follows brightness, RGB is gold everywhere it is visible)."""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from common import save_png


def eclipse(size=128):
    c = (size - 1) / 2
    ys, xs = np.mgrid[0:size, 0:size].astype(float)
    dx, dy = xs - c, ys - c
    r = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    rgba = np.zeros((size, size, 4))
    R0 = 34.0
    # corona: gold streamers whose strength varies with angle, fading out by r ~ 62
    stream = (1 + 0.30 * np.cos(5 * th + 0.7) + 0.22 * np.cos(7 * th + 2.1) + 0.16 * np.cos(11 * th + 4.0)
              + 0.12 * np.cos(13 * th + 1.3))
    fall = np.clip(np.exp(-(r - R0 - 2) / (6.5 * stream.clip(0.4))), 0, 1)
    fall *= np.clip((63 - r) / 8, 0, 1)
    inner = np.array([255, 236, 170], dtype=float)
    outer = np.array([214, 150, 58], dtype=float)
    t = np.clip((r - R0) / 26, 0, 1)[..., None]
    col = inner * (1 - t) + outer * t
    alpha = 235 * fall
    # faint cyan streaks (Remnant light) radiating out of the corona
    rng = random.Random(5)
    cyan = np.zeros_like(r)
    for _ in range(7):
        ang = rng.uniform(-math.pi, math.pi)
        width = rng.uniform(0.015, 0.03)
        length = rng.uniform(46, 60)
        d = np.abs((th - ang + math.pi) % (2 * math.pi) - math.pi)
        streak = np.clip(1 - d / width, 0, 1) * np.clip((length - r) / 10, 0, 1) * (r > R0 + 2)
        cyan = np.maximum(cyan, streak)
    cyan_col = np.array([156, 242, 230], dtype=float)
    a_cyan = 110 * cyan
    col = (col * alpha[..., None] + cyan_col * a_cyan[..., None]) / np.maximum(alpha + a_cyan, 1e-6)[..., None]
    alpha = np.clip(alpha + a_cyan * (1 - alpha / 255), 0, 255)
    rgba[..., :3] = col
    rgba[..., 3] = alpha
    # thin bright rim, gold-white, brightest on the upper left
    rim = (r >= R0) & (r < R0 + 2.2)
    bright = np.clip(0.75 + 0.25 * np.cos(th + 2.3), 0, 1)
    rim_col = np.stack([255 * np.ones_like(r), 236 + 18 * bright, 168 + 80 * bright], -1)
    rgba[rim, :3] = rim_col[rim]
    rgba[rim, 3] = 255
    soft = (r >= R0 + 2.2) & (r < R0 + 3.2)
    rgba[soft, :3] = np.array([250, 214, 120])
    rgba[soft, 3] = np.maximum(rgba[soft, 3], 220)
    # the black disc, with the faintest violet inner limb
    disc = r < R0
    rgba[disc, :3] = np.array([6, 4, 10])
    limb = (r >= R0 - 2.5) & (r < R0)
    rgba[limb, :3] = np.array([18, 12, 30])
    rgba[disc, 3] = 255
    return Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), 'RGBA')


def gracetree(size=256, ss=4):
    """An immense golden tree on the horizon: tall trunk from the bottom centre, a broad dome of
    luminous gold strands; darker gold limbs show through the light."""
    S = size * ss
    rng = random.Random(1717)
    glow = Image.new('L', (S, S), 0)
    limbs = Image.new('L', (S, S), 0)
    leaves = Image.new('L', (S, S), 0)
    trunk = Image.new('L', (S, S), 0)
    gd, ld, fd, td = ImageDraw.Draw(glow), ImageDraw.Draw(limbs), ImageDraw.Draw(leaves), ImageDraw.Draw(trunk)
    base_x, base_y = S / 2, S - 1
    top_y = S * 0.58
    # trunk: tapered, with a root flare
    for i in range(200):
        f = i / 199
        y = base_y - (base_y - top_y) * f
        w = (0.074 - 0.040 * f) * S + (0.06 * S * (1 - f) ** 6)
        sway = math.sin(f * 2.4) * 0.006 * S
        td.line([(base_x - w / 2 + sway, y), (base_x + w / 2 + sway, y)], fill=255, width=ss + 1)
    tips = []

    def branch(x, y, ang, length, width, depth):
        nx, ny = x + math.cos(ang) * length, y + math.sin(ang) * length
        ld.line([(x, y), (nx, ny)], fill=255, width=max(1, int(width)))
        gd.line([(x, y), (nx, ny)], fill=255, width=max(2, int(width * 2.2)))
        if depth == 0 or length < S * 0.012:
            tips.append((nx, ny, ang))
            return
        kids = 3 if depth > 3 else 2
        for k in range(kids):
            spread = (k - (kids - 1) / 2) * rng.uniform(0.38, 0.62)
            # pull every limb outward and slightly down at the rim: a broad, dome-like crown
            outward = (0.18 if math.cos(ang) > 0 else -0.18) * (1 - depth / 7)
            na = ang + spread + outward + rng.uniform(-0.08, 0.08)
            na = min(-0.22, max(-math.pi + 0.22, na))      # never curl under: a broad rising dome
            branch(nx, ny, na, length * rng.uniform(0.68, 0.80), width * 0.66, depth - 1)

    top = (base_x + math.sin(2.4) * 0.006 * S, top_y)
    for a, l, w in ((-math.pi / 2, 0.15, 0.022), (-math.pi / 2 - 0.62, 0.16, 0.018), (-math.pi / 2 + 0.62, 0.16, 0.018),
                    (-math.pi / 2 - 1.15, 0.13, 0.014), (-math.pi / 2 + 1.15, 0.13, 0.014)):
        branch(top[0], top[1], a, l * S * 0.86, w * S, 6)
    # leaves: luminous clusters and hanging strands at every tip
    for x, y, ang in tips:
        for _ in range(5):
            px, py = x + rng.gauss(0, S * 0.012), y + rng.gauss(0, S * 0.010)
            rad = rng.uniform(0.6, 1.8) * ss
            fd.ellipse([px - rad, py - rad, px + rad, py + rad], fill=rng.randint(180, 255))
            gd.ellipse([px - rad * 3, py - rad * 3, px + rad * 3, py + rad * 3], fill=200)
        if rng.random() < 0.35:
            length = rng.uniform(0.02, 0.06) * S
            fd.line([(x, y), (x + rng.uniform(-3, 3) * ss, y + length)], fill=150, width=max(1, ss // 2))
    glow_b = glow.filter(ImageFilter.GaussianBlur(S * 0.022))
    halo = Image.new('L', (S, S), 0)
    hd = ImageDraw.Draw(halo)
    hd.ellipse([S * 0.10, S * 0.04, S * 0.90, S * 0.66], fill=90)
    halo = halo.filter(ImageFilter.GaussianBlur(S * 0.07))
    g = np.asarray(glow_b, dtype=float) / 255
    h = np.asarray(halo, dtype=float) / 255
    lmb = np.asarray(limbs, dtype=float) / 255
    lf = np.asarray(leaves, dtype=float) / 255
    tr = np.asarray(trunk, dtype=float) / 255
    # intensity: halo + glow + bright leaves; limbs and trunk core are a darker, denser gold
    inten = np.clip(0.30 * h + 0.75 * g + 1.0 * lf, 0, 1)
    gold_hi = np.array([255, 232, 150])
    gold_mid = np.array([240, 184, 70])
    gold_dark = np.array([178, 112, 34])
    col = gold_mid * (1 - lf[..., None]) + gold_hi * lf[..., None]
    structure = np.clip(lmb * 0.8 + tr, 0, 1)
    col = col * (1 - structure[..., None] * 0.6) + gold_dark * (structure[..., None] * 0.6)
    alpha = np.clip(inten * 255 + structure * 170, 0, 255)
    # luminous seams running up the trunk (the souls rising through it)
    seams = Image.new('L', (S, S), 0)
    sd = ImageDraw.Draw(seams)
    for k, off in enumerate((-0.30, -0.08, 0.12, 0.33)):
        pts = []
        for i in range(60):
            f = i / 59
            y = base_y - (base_y - top_y) * f
            w = (0.074 - 0.040 * f) * S
            pts.append((base_x + off * w + math.sin(f * 7 + k) * 0.004 * S + math.sin(f * 2.4) * 0.006 * S, y))
        sd.line(pts, fill=255, width=max(1, ss // 2 + 1))
    sm = np.asarray(seams, dtype=float) / 255 * tr
    col = col * (1 - sm[..., None]) + gold_hi * sm[..., None]
    alpha = np.maximum(alpha, sm * 255)
    # rim light on the trunk edges
    tr_edge = np.asarray(trunk.filter(ImageFilter.FIND_EDGES), dtype=float) / 255
    col = col * (1 - tr_edge[..., None]) + gold_hi * tr_edge[..., None]
    alpha = np.maximum(alpha, tr_edge * 255)
    rgba = np.dstack([np.clip(col, 0, 255), alpha])
    img = Image.fromarray(rgba.astype(np.uint8), 'RGBA')
    # premultiplied downsample so transparent pixels don't darken edges
    arr = np.asarray(img, dtype=float)
    pm = arr.copy()
    pm[..., :3] *= arr[..., 3:4] / 255
    small = pm.reshape(size, ss, size, ss, 4).mean(axis=(1, 3))
    a = small[..., 3:4]
    small[..., :3] = np.where(a > 0, small[..., :3] * 255 / np.maximum(a, 1e-6), 0)
    return Image.fromarray(np.clip(small, 0, 255).astype(np.uint8), 'RGBA')


def build():
    save_png(eclipse(), 'textures/environment/eclipse.png')
    save_png(gracetree(), 'textures/environment/gracetree.png')
