"""Pixel-art atlas painter for Remnants entity models.

Every cube gets a box-style UV net at exactly one texel per model unit (16 px per block), so pixel
density is identical across every entity in the mod. Faces are painted by hand-written pixel-art
rules per material: hue-shifted ramps, top-lit gradients, bevels, specular sheen streaks, lame
seams, rivets with rust runs, dents, engraved filigree, shaded cloth folds, stitched leather,
honed blade edges and soul light that bleeds onto neighbouring metal. Randomness only places
details (seeded per cube); it never fills a face with noise. Emissive pixels go to a glowmask.
"""
import math
import random
from dataclasses import dataclass, field

from PIL import Image

FACES = ('north', 'south', 'east', 'west', 'up', 'down')
SIDES = ('north', 'south', 'east', 'west')


def hex_rgb(value):
    value = value.lstrip('#')
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


@dataclass
class Material:
    """A colour ramp (dark to light, hue-shifted) plus a painting style."""
    ramp: list
    style: str = 'plate'   # plate, gold, blade, cloth, leather, chain, stone, soul, void, crystal, bone, wood, flat
    glow: bool = False     # every pixel also goes into the glowmask
    wear: float = 1.0      # scales scratches, dents, chips
    accent: str = None     # material used for weathering (rust on iron, soot on bronze, moss on stone)
    torn: bool = False     # cloth: ragged transparent hem
    alpha: int = 255
    sheen: bool = True     # plate/gold: specular streak

    def __post_init__(self):
        self.rgb = [hex_rgb(c) if isinstance(c, str) else tuple(c) for c in self.ramp]

    def tone(self, index):
        return self.rgb[max(0, min(len(self.rgb) - 1, int(round(index))))]

    @property
    def mid(self):
        return (len(self.rgb) - 1) // 2

    @property
    def top(self):
        return len(self.rgb) - 1


PALETTE = {
    'iron': Material(['#0C0D12', '#15171E', '#1F222B', '#2B2F3A', '#3A3F4D', '#525866', '#737B8B'], 'plate', accent='rust', wear=0.7),
    'iron_dark': Material(['#08090C', '#0F1015', '#16181F', '#1F222B', '#2B2F3A', '#3A3F4D'], 'plate', wear=0.5),
    'steel': Material(['#1B1E25', '#2A2F39', '#3E4552', '#56606F', '#7A8494', '#A6AEBD', '#D6DCE6'], 'plate', wear=0.5),
    'blade': Material(['#1E2128', '#2D323C', '#434A57', '#5E6676', '#828B9B', '#ADB5C3', '#E2E7EF'], 'blade'),
    'gold': Material(['#2A1E0C', '#4A3516', '#6E5021', '#95702E', '#BC9440', '#DDBA5E', '#F6E19A'], 'gold'),
    'royal': Material(['#070A15', '#0E1325', '#161E3D', '#212D57', '#2D3F71', '#3D538C'], 'cloth', torn=True),
    'royal_lining': Material(['#1A080C', '#2C0F15', '#42171F', '#59222A', '#712F35'], 'cloth'),
    'leather': Material(['#140D08', '#22160E', '#332216', '#46301F', '#5C412A', '#765737'], 'leather'),
    'chain': Material(['#0E0F13', '#1C1E25', '#2E323C', '#474C58', '#676D7B'], 'chain'),
    'soul': Material(['#0E3A3C', '#1C5E60', '#2E8C8C', '#4FC9C1', '#8FEDE0', '#D4FFF8', '#FFFFFF'], 'soul', glow=True),
    'soul_dim': Material(['#0B2A2D', '#12454A', '#1C5E60', '#2E8C8C', '#4FC9C1', '#8FEDE0'], 'soul', glow=True),
    'void': Material(['#030406', '#06070B', '#0A0C12', '#10131A', '#171B25'], 'void'),
    'rust': Material(['#24100A', '#3D1C0E', '#5A2C15', '#7A4020', '#9A5A32', '#B8784A'], 'plate', wear=1.2),
    'bronze': Material(['#21150C', '#3A2615', '#57391F', '#7A512B', '#9E6E3B', '#C2914F', '#E0B676'], 'plate', accent='soot'),
    'soot': Material(['#0E0C0A', '#171411', '#211D19', '#2C2722', '#38322B'], 'flat'),
    'ember': Material(['#4A1206', '#86280C', '#C84A14', '#F07A22', '#FFAE4A', '#FFE08A', '#FFF7D6'], 'soul', glow=True),
    'sandstone': Material(['#3E382C', '#5A5241', '#7C7259', '#A09574', '#C2B894', '#DED6B9'], 'stone'),
    'verdigris': Material(['#16302B', '#22463F', '#336657', '#4B8B73', '#6FAE95', '#9CCDB6'], 'plate'),
    'crystal': Material(['#1E1438', '#33255E', '#4D3A8E', '#6E58BA', '#9D86E0', '#CDBFFF', '#F1EBFF'], 'crystal', glow=True),
    'oracle': Material(['#3D1834', '#5B2A4F', '#86386F', '#B4559A', '#D77BC0', '#F0B0DE', '#FFE6F7'], 'crystal', glow=True),
    'bone': Material(['#433C31', '#5F5646', '#7F7461', '#A2967D', '#C3B99F', '#DFD7C1', '#F4EFE2'], 'bone'),
    'marble': Material(['#4E4A53', '#6B6672', '#8C8794', '#AEA9B6', '#CDC9D4', '#E6E3EB', '#F8F7FA'], 'polished'),
    'stone': Material(['#23271F', '#343A2E', '#474F3F', '#5C6551', '#727D65', '#8C977C'], 'stone', accent='moss'),
    'moss': Material(['#1A2B10', '#263F17', '#345A1F', '#477A28', '#5E9A35', '#7DB84A'], 'flat'),
    'heartwood': Material(['#1C110A', '#2B1A10', '#3E2717', '#553821', '#6E4C2E', '#8A633E'], 'wood'),
    'soul_cloth': Material(['#0F3236', '#1C5E60', '#2E8C8C', '#4FC9C1', '#9CF2E6'], 'cloth', glow=True, torn=True, alpha=210),
    'heraldry': Material(['#0A0E1C', '#121A33', '#1B2850', '#263A6C', '#344C86'], 'enamel', wear=1.0),
    'grave_cloth': Material(['#0E0F0C', '#161813', '#20231B', '#2B2F24', '#383D2F', '#474D3B'], 'cloth', torn=True),
    'tomb': Material(['#141317', '#1E1D22', '#2A292F', '#38373F', '#4A4952', '#5F5E69', '#7A7884'], 'stone', accent='moss'),
    'fur': Material(['#120F0D', '#1E1915', '#2C251F', '#3B322A', '#4C4136', '#5F5345'], 'fur'),
    'wax': Material(['#5E5444', '#857A63', '#AFA386', '#D4CAAD', '#EEE7D2'], 'bone'),
    'verdigris_grille': Material(['#16302B', '#22463F', '#336657', '#4B8B73', '#6FAE95', '#9CCDB6'], 'grille'),
    'leaves': Material(['#132A0E', '#1D3D14', '#2A561C', '#3A7226', '#4E8F33', '#6DAE45'], 'leaves'),
    'bark': Material(['#160F0A', '#24180F', '#332317', '#45301F', '#5A4029'], 'wood'),
    'amber': Material(['#8A420A', '#C46A10', '#F2A23A', '#FFD98A', '#FFF3CF'], 'soul', glow=True),
}


@dataclass
class Net:
    """UV rectangles for one painted cube: face -> (u1, v1, u2, v2) in texels."""
    u: int
    v: int
    w: int
    h: int
    d: int
    rects: dict = field(default_factory=dict)

    @classmethod
    def at(cls, u, v, w, h, d):
        net = cls(u, v, w, h, d)
        net.rects = {
            'up': (u + d, v, u + d + w, v + d),
            'down': (u + d + w, v, u + d + 2 * w, v + d),
            'east': (u, v + d, u + d, v + d + h),
            'north': (u + d, v + d, u + d + w, v + d + h),
            'west': (u + d + w, v + d, u + 2 * d + w, v + d + h),
            'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        }
        return net

    @staticmethod
    def footprint(w, h, d):
        return 2 * (w + d), d + h


def texel_dims(size):
    return tuple(max(1, int(math.ceil(s - 1e-6))) for s in size)


class Atlas:
    def __init__(self, width, height):
        self.width, self.height = width, height
        self.image = Image.new('RGBA', (width, height), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (width, height), (0, 0, 0, 0))
        self._shelves = []
        self._next_y = 0

    def allocate(self, w, h, d):
        fw, fh = Net.footprint(w, h, d)
        if fw > self.width:
            raise ValueError(f'cube net {fw}x{fh} wider than atlas {self.width}')
        for shelf in self._shelves:
            y, sh, x = shelf
            if fh <= sh and x + fw <= self.width:
                shelf[2] = x + fw
                return Net.at(x, y, w, h, d)
        if self._next_y + fh > self.height:
            raise ValueError(f'atlas {self.width}x{self.height} full; enlarge it')
        self._shelves.append([self._next_y, fh, fw])
        net = Net.at(0, self._next_y, w, h, d)
        self._next_y += fh
        return net

    def put(self, x, y, rgb, glow=False, alpha=255):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.image.putpixel((x, y), (*rgb, alpha))
            if glow:
                self.glow.putpixel((x, y), (*rgb, 255))
            elif self.glow.getpixel((x, y))[3]:
                self.glow.putpixel((x, y), (0, 0, 0, 0))

    def get(self, x, y):
        return self.image.getpixel((x, y))

    def clear(self, x, y):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.image.putpixel((x, y), (0, 0, 0, 0))
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def save(self, path, glow_path=None):
        self.image.save(path)
        if glow_path:
            self.glow.save(glow_path)

    @property
    def used_height(self):
        return self._next_y


class Face:
    """Pixel access to one face rectangle in face-local coordinates (x right, y down)."""

    def __init__(self, atlas, rect, mat, face):
        self.atlas, self.mat, self.face = atlas, mat, face
        self.u1, self.v1, u2, v2 = rect
        self.w, self.h = u2 - self.u1, v2 - self.v1

    def put(self, x, y, idx, mat=None, glow=None):
        mat = mat or self.mat
        if 0 <= x < self.w and 0 <= y < self.h:
            self.atlas.put(self.u1 + x, self.v1 + y, mat.tone(idx), mat.glow if glow is None else glow, mat.alpha)

    def clear(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.atlas.clear(self.u1 + x, self.v1 + y)

    def tint(self, x, y, rgb, t):
        if 0 <= x < self.w and 0 <= y < self.h:
            r, g, b, a = self.atlas.get(self.u1 + x, self.v1 + y)
            if a:
                self.atlas.put(self.u1 + x, self.v1 + y, mix((r, g, b), rgb, t), False, a)


def _light(face):
    return {'up': 1, 'down': -1}.get(face, 0)


def paint_plate(f, rng, palette, gold=False):
    m, w, h, mat = f.mat.mid + _light(f.face), f.w, f.h, f.mat
    side = f.face in SIDES
    if not side:
        # caps: lit centre, darker rim, a highlight row along the leading edge
        for y in range(h):
            for x in range(w):
                edge = x in (0, w - 1) or y in (0, h - 1)
                f.put(x, y, m - 1 if edge and w > 2 and h > 2 else m)
        if w >= 4 and h >= 4:
            for x in range(1, w - 1):
                f.put(x, 1, m + 1)
        return
    # 1. top-lit gradient: upper fifth light, lower quarter shadowed
    for y in range(h):
        band = m
        if h >= 4:
            if y < max(1, round(h * 0.2)):
                band = m + 1
            elif y >= h - max(1, round(h * 0.25)):
                band = m - 1
        for x in range(w):
            f.put(x, y, band)
    # 2. overlapping lames on tall faces: shadow line under a lit lip
    if h >= 10 and w >= 3:
        for seam in range(6, h - 3, 6):
            for x in range(w):
                f.put(x, seam, m - 2)
                f.put(x, seam + 1, m + 1)
    # 3. specular sheen streak a third of the way across
    if mat.sheen and w >= 5 and h >= 5:
        sx = max(1, int(w * 0.3))
        for y in range(1, h - 1):
            core = h * 0.35 <= y <= h * 0.65
            f.put(sx, y, m + 2 if core else m + 1)
            if w >= 8 and core:
                f.put(sx + 1, y, m + 1)
    # 4. gold engraving: a running scroll along the middle row
    if gold and h >= 3 and w >= 4:
        c = h // 2
        for x in range(w):
            dy = (0, -1, 0, 1)[x % 4] if h >= 5 else 0
            f.put(x, c + dy, m - 1)
            if h >= 5 and x % 4 == 1:
                f.put(x, c + dy - 1, m + 2)
    # 5. bevel: bright top edge, dark bottom edge, darker side edges
    for x in range(w):
        f.put(x, 0, m + 2 if h > 2 else m + 1)
        if h > 2:
            f.put(x, h - 1, m - 2)
    if w >= 6 and h > 2:
        for y in range(1, h - 1):
            f.put(0, y, m - 1)
            f.put(w - 1, y, m - 1)
    # 6. rivets at the upper corners, sometimes weeping rust
    accent = palette.get(mat.accent) if mat.accent else None
    if w >= 6 and h >= 6 and not gold:
        for rx in (1, w - 2):
            f.put(rx, 1, mat.top)
            f.put(rx, 2, m - 2)
            if accent and rng.random() < 0.2 * mat.wear:
                for ry in range(3, 3 + rng.choice((1, 2))):
                    if ry < h - 1:
                        f.put(rx, ry, 1, accent)
    # 7. scratches and dents
    for _ in range(int((w * h // 45) * mat.wear)):
        sx, sy = rng.randrange(1, max(2, w - 1)), rng.randrange(1, max(2, h - 1))
        for i in range(rng.choice((2, 3))):
            if 0 < sx + i < w - 1 and 0 < sy + i < h - 1:
                f.put(sx + i, sy + i, m + 1)
    for _ in range(int((w * h // 110) * mat.wear)):
        dx, dy = rng.randrange(1, max(2, w - 2)), rng.randrange(1, max(2, h - 2))
        f.put(dx, dy, m - 2)
        f.put(dx + 1, dy + 1, m + 1)
    # 8. chips on the bevel and weathering along the bottom
    for _ in range(int(((w + h) // 12) * mat.wear)):
        f.put(rng.randrange(w), rng.choice((0, h - 1)), m - 1)
    if accent and h >= 6 and w >= 4:
        run = rng.randrange(1, w - 2) if rng.random() < 0.6 * mat.wear else None
        if run is not None:
            for x in range(run, min(w - 1, run + rng.choice((2, 3)))):
                f.put(x, h - 2, 1, accent)
    if gold:
        for _ in range(max(1, w * h // 30)):
            f.put(rng.randrange(w), rng.randrange(h), mat.top)


def paint_blade(f, rng, palette):
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            f.put(x, y, m)
    if h >= w and w >= 3:
        for y in range(h):
            f.put(0, y, m + 2)            # honed edges
            f.put(w - 1, y, m + 2)
            f.put(w // 2, y, m - 1)       # spine shadow
            if w >= 4:
                f.put(1, y, m + 1)
        for y in range(0, h, 7):          # diagonal light glints
            for i in range(min(3, w - 2)):
                if y + i < h:
                    f.put(1 + i, y + i, m + 2)
        for _ in range(max(1, h // 9)):   # nicks in the edge
            f.put(rng.choice((0, w - 1)), rng.randrange(h), m - 1)
    else:
        for x in range(w):
            f.put(x, 0, m + 2)
            if h > 1:
                f.put(x, h - 1, m - 1)


def paint_cloth(f, rng, palette):
    m, w, h, mat = f.mat.mid + _light(f.face), f.w, f.h, f.mat
    if f.face not in SIDES:
        for y in range(h):
            for x in range(w):
                f.put(x, y, m - 1)
        return
    x = 0
    while x < w:
        fold = rng.choice((3, 3, 4, 4, 5))
        for i in range(fold):
            if x + i >= w:
                break
            t = i / max(1, fold - 1)
            idx = m + 1 if t < 0.25 else (m if t < 0.6 else (m - 1 if t < 0.9 else m - 2))
            for y in range(h):
                f.put(x + i, y, idx)
        x += fold
    for xx in range(w):
        f.put(xx, 0, m + 1)
    for _ in range(max(0, w * h // 70)):         # faded, sun-bleached patches
        px, py = rng.randrange(w), rng.randrange(h)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            f.tint(px + dx, py + dy, mat.tone(mat.top), 0.35)
    if mat.torn and h >= 4:
        for xx in range(w):
            depth = rng.choice((0, 0, 1, 1, 2, 3)) if h > 6 else rng.choice((0, 1))
            for y in range(h - depth, h):
                f.clear(xx, y)
            if depth < h:
                f.put(xx, h - depth - 1, m - 2)
    else:
        for xx in range(w):
            f.put(xx, h - 1, m - 2)


def paint_leather(f, rng, palette):
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            f.put(x, y, m)
    for _ in range(max(1, w * h // 8)):
        f.put(rng.randrange(w), rng.randrange(h), m - 1)
    if f.face in SIDES:
        for x in range(w):
            f.put(x, 0, m + 1)
            f.put(x, h - 1, m - 2)
        if h >= 4:
            for x in range(1, w - 1, 2):
                f.put(x, 1, m + 2)        # stitching
                if h >= 6:
                    f.put(x, h - 2, m + 1)
        for _ in range(max(0, w * h // 40)):
            f.put(rng.randrange(w), rng.randrange(1, max(2, h - 1)), m + 1)


def paint_chain(f, rng, palette):
    # Rows of riveted rings: a lit ring top every other pixel, a soft shadow row beneath
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            if y % 2 == 0:
                f.put(x, y, m + 1 if (x + y // 2) % 2 == 0 else m)
            else:
                f.put(x, y, m - 1)
    for x in range(w):
        f.put(x, h - 1, m - 2)


def paint_enamel(f, rng, palette):
    # Painted heraldic field: flat colour, lighter brush strokes, flaked edges showing dark metal
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            f.put(x, y, m)
    for y in range(1, h - 1, 3):
        for x in range(rng.randrange(0, 2), w - 1, 4):
            f.put(x, y, m + 1)
    for _ in range(int(((w + h) // 3) * f.mat.wear)):
        x, y = rng.randrange(w), rng.choice((0, h - 1, rng.randrange(h)))
        f.put(x, y, 0)
    for x in range(w):
        f.put(x, 0, m + 1)


def paint_stone(f, rng, palette):
    m, w, h, mat = f.mat.mid + _light(f.face), f.w, f.h, f.mat
    course = 4 if h >= 8 else max(2, h)
    for y in range(h):
        row = y // course
        for x in range(w):
            f.put(x, y, m + (1 if y % course == 0 else 0))
        if y % course == course - 1:
            for x in range(w):
                f.put(x, y, m - 2)
        else:
            for x in range((row * 3 + 1) % 6, w, 6):
                f.put(x, y, m - 2)
    for _ in range(max(0, w * h // 50)):          # hairline cracks
        cx, cy = rng.randrange(w), rng.randrange(h)
        for i in range(rng.choice((2, 3, 4))):
            f.put(cx, cy + i, m - 1)
            cx += rng.choice((-1, 0, 1))
    accent = palette.get(mat.accent) if mat.accent else None
    if accent:
        if f.face == 'up':
            for y in range(h):
                for x in range(w):
                    if rng.random() < 0.4:
                        f.put(x, y, 2 + (x + y) % 3, accent)
        elif f.face in SIDES and h > 3:
            for x in range(w):
                for y in range(rng.choice((0, 0, 1, 1, 2, 3))):
                    f.put(x, y, 3 - y, accent)


def paint_soul(f, rng, palette):
    mat, w, h = f.mat, f.w, f.h
    if w * h <= 3:  # eyes and pin-lights: keep the hue, never wash out to white
        for y in range(h):
            for x in range(w):
                f.put(x, y, mat.mid + 1)
        return
    cx, cy = (w - 1) / 2, (h - 1) / 2
    rmax = max(1.0, math.hypot(cx, cy))
    for y in range(h):
        for x in range(w):
            t = 1 - math.hypot(x - cx, y - cy) / rmax
            f.put(x, y, mat.mid - 1 + t * (mat.top - mat.mid + 1.4))
    if h >= 4:                                     # rising flicker streaks
        for x in range(0, w, 2):
            top = rng.randrange(0, max(1, h // 2))
            for y in range(top, top + 2):
                f.put(x, y, mat.top - 1)


def paint_void(f, rng, palette):
    m, w, h = f.mat.mid, f.w, f.h
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            f.put(x, y, m + 1 if edge and w > 2 and h > 2 else (m - 1 if (x + y) % 3 else m))
    if w * h >= 12:
        soul = palette['soul_dim']
        for _ in range(max(1, w * h // 30)):
            f.put(rng.randrange(1, max(2, w - 1)), rng.randrange(1, max(2, h - 1)), 3, soul, True)


def paint_crystal(f, rng, palette):
    m, w, h, mat = f.mat.mid + _light(f.face), f.w, f.h, f.mat
    for y in range(h):
        for x in range(w):
            facet = ((x + y) // 2) % 3
            f.put(x, y, m + (1 if facet == 0 else 0) - (1 if facet == 2 else 0))
    for i in range(min(w, h)):
        f.put(i, i, mat.top)
    for x in range(w):
        f.put(x, h - 1, m - 2)


def paint_bone(f, rng, palette):
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            f.put(x, y, m)
    for _ in range(max(1, w * h // 14)):
        f.put(rng.randrange(w), rng.randrange(h), m - 1)
    if f.face in SIDES:
        for x in range(w):
            f.put(x, 0, m + 2)
            f.put(x, h - 1, m - 2)
        for _ in range(max(0, h // 6)):
            cx, cy = rng.randrange(w), rng.randrange(h)
            for i in range(3):
                f.put(cx, cy + i, m - 2)
                cx += rng.choice((-1, 0, 1))


def paint_wood(f, rng, palette):
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    vertical = h >= w
    for y in range(h):
        for x in range(w):
            f.put(x, y, m)
    for i in range(w if vertical else h):
        if rng.random() < 0.35:
            for j in range(h if vertical else w):
                if rng.random() < 0.9:
                    if vertical:
                        f.put(i, j, m - 1)
                    else:
                        f.put(j, i, m - 1)
    for _ in range(max(1, w * h // 70)):
        kx, ky = rng.randrange(w), rng.randrange(h)
        f.put(kx, ky, m - 2)
        f.put(kx + 1, ky, m - 1)
    if f.face in SIDES:
        for x in range(w):
            f.put(x, 0, m + 1)


def paint_fur(f, rng, palette):
    # Short strands combed toward the back: staggered dark/light dashes, darker belly rows
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            f.put(x, y, m - (1 if y >= h - max(1, h // 3) and f.face in SIDES else 0))
    for y in range(h):
        off = (y * 2) % 3
        for x in range(off, w, 3):
            f.put(x, y, m - 1)
            if x + 1 < w and rng.random() < 0.5:
                f.put(x + 1, y, m + 1)
    if f.face in SIDES:
        for x in range(w):
            f.put(x, 0, m + 1)


def paint_grille(f, rng, palette):
    # Cage bars: a lit frame top and bottom, vertical bars with see-through gaps (render cutout)
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            frame = y in (0, h - 1) or x in (0, w - 1)
            if frame or x % 2 == 0 or f.face not in SIDES:
                f.put(x, y, m + (1 if y == 0 else (-1 if y == h - 1 else 0)) + (1 if x % 4 == 0 and not frame else 0))
            else:
                f.clear(x, y)


def paint_leaves(f, rng, palette):
    # Minecraft-style foliage: clustered light/dark leaf pixels with see-through gaps
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            roll = rng.random()
            if roll < 0.12 and (x in (0, w - 1) or y in (0, h - 1) or roll < 0.05):
                f.clear(x, y)
            elif roll < 0.35:
                f.put(x, y, m + 1)
            elif roll < 0.55:
                f.put(x, y, m - 1)
            else:
                f.put(x, y, m)
    for _ in range(max(1, w * h // 20)):
        f.put(rng.randrange(w), rng.randrange(h), m + 2)


def paint_polished(f, rng, palette):
    # Polished marble: smooth top-lit face, fine diagonal veins, crisp bevel
    m, w, h = f.mat.mid + _light(f.face), f.w, f.h
    for y in range(h):
        for x in range(w):
            f.put(x, y, m + (1 if y < max(1, h // 4) and f.face in SIDES else 0))
    for _ in range(max(1, (w + h) // 8)):
        vx, vy = rng.randrange(w), rng.randrange(h)
        for i in range(rng.randrange(3, 7)):
            f.put(vx, vy, m - 1)
            vx += rng.choice((0, 1))
            vy += 1
    for x in range(w):
        f.put(x, 0, m + 2)
        if h > 2:
            f.put(x, h - 1, m - 1)
    if w > 3:
        for y in range(1, h - 1):
            f.put(0, y, m + 1)
            f.put(w - 1, y, m - 1)


def paint_flat(f, rng, palette):
    m = f.mat.mid + _light(f.face)
    for y in range(f.h):
        for x in range(f.w):
            f.put(x, y, m)


STYLES = {
    'plate': paint_plate,
    'gold': lambda f, rng, pal: paint_plate(f, rng, pal, gold=True),
    'blade': paint_blade,
    'cloth': paint_cloth,
    'leather': paint_leather,
    'chain': paint_chain,
    'enamel': paint_enamel,
    'stone': paint_stone,
    'soul': paint_soul,
    'void': paint_void,
    'crystal': paint_crystal,
    'bone': paint_bone,
    'wood': paint_wood,
    'flat': paint_flat,
    'fur': paint_fur,
    'grille': paint_grille,
    'leaves': paint_leaves,
    'polished': paint_polished,
}


def paint_face(atlas, rect, face, mat, rng, palette):
    f = Face(atlas, rect, mat, face)
    if f.w > 0 and f.h > 0:
        STYLES[mat.style](f, rng, palette)


def _face_iter(rects, faces):
    if faces in (None, 'sides'):
        faces = SIDES
    elif faces == 'all':
        faces = FACES
    elif isinstance(faces, str):
        faces = (faces,)
    for name in faces:
        if name in rects:
            yield name, rects[name]


def apply_decal(atlas, rects, decal, palette, rng):
    kind = decal[0]
    opts = decal[1] if len(decal) > 1 else {}
    mat = palette[opts.get('mat', 'gold')]
    for name, rect in _face_iter(rects, opts.get('faces', 'sides')):
        f = Face(atlas, rect, mat, name)
        if f.w > 0 and f.h > 0:
            DECALS[kind](f, opts, palette, rng)


def d_band(f, opts, palette, rng):
    """Inlaid horizontal trim. row counts from the top (negative: from the bottom)."""
    row, size, m = opts.get('row', 0), opts.get('size', 1), f.mat.mid
    start = row if row >= 0 else f.h + row
    for y in range(start, min(f.h, start + size)):
        for x in range(f.w):
            if size == 1:
                idx = m + 1 if x % 3 else m + 2
            else:
                idx = m + 2 if y == start else (m - 1 if y == start + size - 1 else m + (1 if x % 3 == 0 else 0))
            f.put(x, y, idx)
    if opts.get('studs'):
        for x in range(1, f.w - 1, opts['studs']):
            f.put(x, start, f.mat.top)


def d_vband(f, opts, palette, rng):
    col, size, m = opts.get('col'), opts.get('size', 1), f.mat.mid
    start = (f.w - size) // 2 if col is None else (col if col >= 0 else f.w + col)
    for x in range(start, min(f.w, start + size)):
        for y in range(f.h):
            f.put(x, y, m + (2 if y == 0 else (1 if x == start else 0)) - (1 if y == f.h - 1 else 0))


def d_rim(f, opts, palette, rng):
    m = f.mat.mid
    for x in range(f.w):
        f.put(x, 0, m + 2)
        f.put(x, f.h - 1, m - 1)
    for y in range(1, f.h - 1):
        f.put(0, y, m + 1)
        f.put(f.w - 1, y, m)


def d_rivets(f, opts, palette, rng):
    for x in range(1, f.w - 1, opts.get('spacing', 3)):
        f.put(x, 1 if f.h > 2 else 0, f.mat.top)
        if f.h > 4:
            f.put(x, f.h - 2, f.mat.top - 1)


def d_crack(f, opts, palette, rng):
    x = opts.get('x', f.w // 2)
    for y in range(opts.get('from', 0), f.h - opts.get('stop', 0)):
        f.put(max(0, min(f.w - 1, x)), y, f.mat.top - (y % 2))
        if rng.random() < 0.45:
            x += rng.choice((-1, 1))
            f.put(max(0, min(f.w - 1, x)), y, f.mat.mid)


def d_glyph(f, opts, palette, rng):
    bitmap = opts['bitmap']
    gh, gw = len(bitmap), max(len(r) for r in bitmap)
    ox = (f.w - gw) // 2 + opts.get('dx', 0)
    oy = (f.h - gh) // 2 + opts.get('dy', 0)
    for y, row in enumerate(bitmap):
        for x, ch in enumerate(row):
            idx = {'#': f.mat.top, '+': f.mat.mid + 1, '-': f.mat.mid - 1, '*': f.mat.top - 1}.get(ch)
            if idx is not None:
                f.put(ox + x, oy + y, idx)


def d_fill(f, opts, palette, rng):
    STYLES[f.mat.style](f, rng, palette)


def d_gradient(f, opts, palette, rng):
    # soot/grime stain rising from the bottom edge: solid rows that fade upward, with a ragged top
    size = opts.get('size', 3)
    for i in range(size):
        y = f.h - 1 - i
        if y < 0:
            break
        for x in range(f.w):
            if i == size - 1 and rng.random() < 0.5:
                continue
            f.tint(x, y, f.mat.tone(f.mat.mid - 1), 0.75 - 0.5 * i / max(1, size - 1))


def d_glow_edge(f, opts, palette, rng):
    """Remnant light bleeding onto metal next to a soul source: tints an edge strip."""
    edge, width, strength = opts.get('edge', 'left'), opts.get('width', 2), opts.get('strength', 0.55)
    light = f.mat.tone(f.mat.mid)
    strength = opts.get('strength', 0.4)
    for i in range(width):
        t = strength * (1 - i / width)
        for j in range(f.h if edge in ('left', 'right') else f.w):
            if edge == 'left':
                f.tint(i, j, light, t)
            elif edge == 'right':
                f.tint(f.w - 1 - i, j, light, t)
            elif edge == 'top':
                f.tint(j, i, light, t)
            else:
                f.tint(j, f.h - 1 - i, light, t)


def d_filigree(f, opts, palette, rng):
    """Engraved scrollwork filling a band of rows (row/size like band)."""
    row, size, m = opts.get('row', 1), opts.get('size', 3), f.mat.mid
    start = row if row >= 0 else f.h + row
    for y in range(start, min(f.h, start + size)):
        for x in range(f.w):
            phase = (x + (y - start) * 2) % 6
            f.put(x, y, m + 2 if phase == 0 else (m - 1 if phase in (2, 3) else m))


def d_stitch(f, opts, palette, rng):
    row = opts.get('row', 1)
    y = row if row >= 0 else f.h + row
    for x in range(0, f.w, 2):
        f.put(x, y, f.mat.top)


DECALS = {
    'band': d_band, 'vband': d_vband, 'rim': d_rim, 'rivets': d_rivets, 'crack': d_crack, 'glyph': d_glyph,
    'fill': d_fill, 'gradient': d_gradient, 'glow_edge': d_glow_edge, 'filigree': d_filigree, 'stitch': d_stitch,
}

GLYPHS = {
    'vael_crest': ['..#..', '.#+#.', '#+#+#', '.#+#.', '..#..'],
    'crown': ['#.#.#', '#####', '+###+'],
    'eye': ['.##.', '#++#', '.##.'],
    'chain': ['+#+', '#.#', '+#+'],
}


def paint_cube_net(atlas, net, mat_key, palette, decals, seed):
    rng = random.Random(seed)
    mat = palette[mat_key]
    for face in FACES:
        paint_face(atlas, net.rects[face], face, mat, rng, palette)
    for decal in decals:
        apply_decal(atlas, net.rects, decal, palette, rng)


def mirrored_rects(rects):
    """UV rects for the X-mirrored twin of a cube: swap east/west, reverse u on every face."""
    out = {}
    for face, (u1, v1, u2, v2) in rects.items():
        target = {'east': 'west', 'west': 'east'}.get(face, face)
        out[target] = (u2, v1, u1, v2)
    return out
