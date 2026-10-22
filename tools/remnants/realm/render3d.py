"""Tiny orthographic software renderer for asset previews (no Blockbench needed).

Reads the files exactly as the game does:
- GeckoLib .geo.json (format 1.12.0): bedrock cube origins/pivots are X-mirrored into render space
  the way GeckoLib bakes them (x' = -(origin.x + size.x), pivot.x' = -pivot.x, rotation (-x, -y, z)),
  bone transforms are T(anim pos) T(pivot) Rz Ry Rx S T(-pivot), and up/down face UVs are un-flipped
  the way Blockbench exports them (uv = rect end, uv_size negative).
- Java block-model elements (from/to, rotation origin/axis/angle, per-face uv in 0-16 space).
Faces are mapped like vanilla FaceBakery: seen from outside, u runs to the viewer's right and v down;
up faces have u -> +X, v -> +Z; down faces u -> +X, v -> -Z.

Pixels: z-buffered opaque pass, then translucent faces back-to-front, then additive glowmask.
"""
import math

import numpy as np
from PIL import Image


def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def Sc(x, y, z):
    return np.diag([x, y, z, 1.0])


def Rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1.0]])


def Ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1.0]])


def Rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1.0]])


def Rzyx(rx, ry, rz):
    return Rz(rz) @ Ry(ry) @ Rx(rx)


class Tex:
    def __init__(self, image, glow=None):
        self.rgba = np.asarray(image.convert('RGBA'), dtype=np.float32) / 255.0
        self.h, self.w = self.rgba.shape[:2]
        self.glow = None if glow is None else np.asarray(glow.convert('RGBA'), dtype=np.float32) / 255.0


class Quad:
    __slots__ = ('tl', 'tr', 'bl', 'uv', 'tex', 'normal', 'shade', 'emissive', 'uv_scale')

    def __init__(self, tl, tr, bl, uv, tex, normal, shade=True, emissive=False, uv_scale=1.0):
        self.tl, self.tr, self.bl = tl, tr, bl
        self.uv, self.tex, self.normal = uv, tex, normal
        self.shade, self.emissive, self.uv_scale = shade, emissive, uv_scale


FACE_CORNERS = {
    # face: (TL, TR, BL) as indices into (x0|x1, y0|y1, z0|z1), u to the right / v down seen from outside
    'north': ((1, 1, 0), (0, 1, 0), (1, 0, 0)),
    'south': ((0, 1, 1), (1, 1, 1), (0, 0, 1)),
    'east': ((1, 1, 1), (1, 1, 0), (1, 0, 1)),
    'west': ((0, 1, 0), (0, 1, 1), (0, 0, 0)),
    'up': ((0, 1, 0), (1, 1, 0), (0, 1, 1)),
    'down': ((0, 0, 1), (1, 0, 1), (0, 0, 0)),
}
FACE_NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0),
                'up': (0, 1, 0), 'down': (0, -1, 0)}


def box_quads(lo, hi, faces, matrix, shade=True, emissive=False, uv_scale=1.0):
    """faces: name -> (uv rect (u1, v1, u2, v2), Tex). Returns transformed quads."""
    out = []
    xs, ys, zs = (lo[0], hi[0]), (lo[1], hi[1]), (lo[2], hi[2])
    nmat = matrix[:3, :3]
    for name, (uv, tex) in faces.items():
        pts = []
        for ix, iy, iz in FACE_CORNERS[name]:
            p = matrix @ np.array([xs[ix], ys[iy], zs[iz], 1.0])
            pts.append(p[:3])
        n = nmat @ np.array(FACE_NORMALS[name], dtype=float)
        ln = np.linalg.norm(n)
        n = n / ln if ln > 1e-9 else n
        out.append(Quad(pts[0], pts[1], pts[2], uv, tex, n, shade, emissive, uv_scale))
    return out


def geo_quads(geo, tex, pose=None):
    """geo: parsed .geo.json; pose: bone -> {'rotation': [x,y,z] deg, 'position': [...], 'scale': [...]}
    with values in the JSON (bedrock) convention, as animation files store them."""
    pose = pose or {}
    g = geo['minecraft:geometry'][0]
    bones = {b['name']: b for b in g['bones']}
    world = {}

    def bone_matrix(name):
        if name in world:
            return world[name]
        b = bones[name]
        parent = bone_matrix(b['parent']) if b.get('parent') else np.eye(4)
        px, py, pz = b.get('pivot', [0, 0, 0])
        p = (-px, py, pz)
        rx, ry, rz = b.get('rotation', [0, 0, 0])
        anim = pose.get(name, {})
        ax, ay, az = anim.get('rotation', (0, 0, 0))
        tx, ty, tz = anim.get('position', (0, 0, 0))
        sx, sy, sz = anim.get('scale', (1, 1, 1))
        rot = Rzyx(math.radians(-(rx + ax)), math.radians(-(ry + ay)), math.radians(rz + az))
        m = parent @ T(-tx, ty, tz) @ T(*p) @ rot @ Sc(sx, sy, sz) @ T(-p[0], -p[1], -p[2])
        world[name] = m
        return m

    quads = []
    for b in g['bones']:
        m = bone_matrix(b['name'])
        for c in b.get('cubes', []):
            ox, oy, oz = c['origin']
            sx, sy, sz = c['size']
            inf = c.get('inflate', 0.0)
            lo = (-(ox + sx) - inf, oy - inf, oz - inf)
            hi = (-ox + inf, oy + sy + inf, oz + sz + inf)
            cm = m
            if any(c.get('rotation', [0, 0, 0])):
                cx, cy, cz = c['pivot']
                cp = (-cx, cy, cz)
                r = c['rotation']
                cm = m @ T(*cp) @ Rzyx(math.radians(-r[0]), math.radians(-r[1]), math.radians(r[2])) @ T(-cp[0], -cp[1], -cp[2])
            faces = {}
            for fname, fuv in c['uv'].items():
                u, v = fuv['uv']
                w, h = fuv['uv_size']
                if fname in ('up', 'down'):
                    rect = (u + w, v + h, u, v)
                else:
                    rect = (u, v, u + w, v + h)
                faces[fname] = (rect, tex)
            quads += box_quads(lo, hi, faces, cm)
    return quads


EASE = {
    'linear': lambda t: t,
    'easeInSine': lambda t: 1 - math.cos(t * math.pi / 2),
    'easeOutSine': lambda t: math.sin(t * math.pi / 2),
    'easeInOutSine': lambda t: -(math.cos(math.pi * t) - 1) / 2,
    'easeInQuad': lambda t: t * t,
    'easeOutQuad': lambda t: 1 - (1 - t) ** 2,
    'easeInOutQuad': lambda t: 2 * t * t if t < 0.5 else 1 - (-2 * t + 2) ** 2 / 2,
    'easeInCubic': lambda t: t ** 3,
    'easeOutCubic': lambda t: 1 - (1 - t) ** 3,
    'easeInOutCubic': lambda t: 4 * t ** 3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2,
    'easeOutBack': lambda t: 1 + 2.70158 * (t - 1) ** 3 + 1.70158 * (t - 1) ** 2,
    'easeInBack': lambda t: 2.70158 * t ** 3 - 1.70158 * t ** 2,
}


def sample_channel(track, t):
    """track: {"0.0": {"vector": [...], "easing": ...}} or a constant vector."""
    if isinstance(track, list):
        return track
    keys = sorted((float(k), v) for k, v in track.items())

    def vec(v):
        return v['vector'] if isinstance(v, dict) else v
    if t <= keys[0][0]:
        return vec(keys[0][1])
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            ease = EASE[v1.get('easing', 'linear') if isinstance(v1, dict) else 'linear']
            f = 0.0 if t1 == t0 else ease((t - t0) / (t1 - t0))
            a, b = vec(v0), vec(v1)
            return [x + (y - x) * f for x, y in zip(a, b)]
    return vec(keys[-1][1])


def sample_pose(clip, t):
    pose = {}
    for bone, chans in clip.get('bones', {}).items():
        pose[bone] = {ch: sample_channel(track, t) for ch, track in chans.items()}
    return pose


def block_quads(model, textures, matrix=None):
    """model: dict with 'elements'; textures: '#key' resolution -> Tex. Units are block pixels (0-16)."""
    matrix = np.eye(4) if matrix is None else matrix
    quads = []
    for e in model['elements']:
        m = matrix
        rot = e.get('rotation')
        if rot:
            o = rot['origin']
            a = math.radians(rot['angle'])
            r = {'x': Rx(a), 'y': Ry(a), 'z': Rz(a)}[rot['axis']]
            m = matrix @ T(*o) @ r @ T(-o[0], -o[1], -o[2])
        faces = {}
        for fname, f in e['faces'].items():
            tex = textures[f['texture'].lstrip('#')]
            u1, v1, u2, v2 = f['uv']
            k = tex.w / 16.0
            faces[fname] = ((u1 * k, v1 * k, u2 * k, v2 * k), tex)
        emissive = 'neoforge_data' in e or 'forge_data' in e
        quads += box_quads(e['from'], e['to'], faces, m, shade=e.get('shade', True), emissive=emissive)
    return quads


def _brightness(n, shade):
    if not shade:
        return 1.0
    nx, ny, nz = n
    up = 1.0 if ny > 0 else 0.5
    return min(1.0, 0.6 * nx * nx + 0.8 * nz * nz + up * ny * ny)


def render(quads, yaw=0.0, pitch=0.0, scale=8.0, size=None, center=None, bg=(0, 0, 0, 0), ambient=1.0,
           glow_strength=1.0, emissive_boost=1.0):
    """Orthographic render. yaw/pitch in degrees; at yaw 0 the camera looks from -Z toward +Z."""
    view = Rx(math.radians(pitch)) @ Ry(math.radians(yaw))
    proj = []
    allpts = []
    for q in quads:
        pts = [(view @ np.append(p, 1.0))[:3] for p in (q.tl, q.tr, q.bl)]
        proj.append(pts)
        tl, tr, bl = pts
        allpts += [tl, tr, bl, tr + bl - tl]
    allpts = np.array(allpts)
    if center is None:
        center = (allpts[:, 0].min() + allpts[:, 0].max()) / 2, (allpts[:, 1].min() + allpts[:, 1].max()) / 2
    if size is None:
        w = int((allpts[:, 0].max() - allpts[:, 0].min()) * scale) + 16
        h = int((allpts[:, 1].max() - allpts[:, 1].min()) * scale) + 16
        size = (w, h)
    W, H = size
    color = np.zeros((H, W, 3), dtype=np.float32)
    color[:] = np.array(bg[:3], dtype=np.float32) / 255.0
    alpha = np.full((H, W), bg[3] / 255.0, dtype=np.float32)
    zbuf = np.full((H, W), -1e9, dtype=np.float32)
    glow = np.zeros((H, W, 3), dtype=np.float32)

    def screen(p):
        return np.array([W / 2 - (p[0] - center[0]) * scale, H / 2 - (p[1] - center[1]) * scale, -p[2]])

    translucent = []
    for q, pts in zip(quads, proj):
        tl, tr, bl = (screen(p) for p in pts)
        item = (q, tl, tr, bl)
        if q.tex.rgba[..., 3].min() < 0.99 and _has_partial_alpha(q):
            translucent.append(item)
        else:
            _raster(item, color, alpha, zbuf, glow, ambient, False, emissive_boost)
    translucent.sort(key=lambda it: (it[1][2] + it[2][2] + it[3][2]) / 3)
    for item in translucent:
        _raster(item, color, alpha, zbuf, glow, ambient, True, emissive_boost)
    out = np.clip(color + glow * glow_strength, 0, 1)
    img = np.dstack([out, alpha[..., None]])
    return Image.fromarray((img * 255).astype(np.uint8), 'RGBA')


def _has_partial_alpha(q):
    u1, v1, u2, v2 = q.uv
    x0, x1 = int(math.floor(min(u1, u2))), int(math.ceil(max(u1, u2)))
    y0, y1 = int(math.floor(min(v1, v2))), int(math.ceil(max(v1, v2)))
    region = q.tex.rgba[max(0, y0):y1, max(0, x0):x1, 3]
    return region.size and ((region > 0.02) & (region < 0.98)).any()


def _raster(item, color, alpha, zbuf, glow, ambient, blend, emissive_boost):
    q, tl, tr, bl = item
    eu, ev = tr - tl, bl - tl
    det = eu[0] * ev[1] - eu[1] * ev[0]
    if abs(det) < 1e-6:
        return
    H, W = zbuf.shape
    corners = np.array([tl, tr, bl, tr + bl - tl])
    x0, x1 = max(0, int(math.floor(corners[:, 0].min()))), min(W, int(math.ceil(corners[:, 0].max())) + 1)
    y0, y1 = max(0, int(math.floor(corners[:, 1].min()))), min(H, int(math.ceil(corners[:, 1].max())) + 1)
    if x0 >= x1 or y0 >= y1:
        return
    ys, xs = np.mgrid[y0:y1, x0:x1].astype(np.float32)
    px, py = xs + 0.5 - tl[0], ys + 0.5 - tl[1]
    s = (px * ev[1] - py * ev[0]) / det
    t = (eu[0] * py - eu[1] * px) / det
    inside = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
    if not inside.any():
        return
    depth = tl[2] + s * eu[2] + t * ev[2]
    u1, v1, u2, v2 = q.uv
    tex = q.tex
    uu = u1 + s * (u2 - u1)
    vv = v1 + t * (v2 - v1)
    iu = np.where(u2 >= u1, np.floor(uu), np.ceil(uu) - 1).astype(int)
    iv = np.where(v2 >= v1, np.floor(vv), np.ceil(vv) - 1).astype(int)
    iu = np.clip(iu, 0, tex.w - 1)
    iv = np.clip(iv, 0, tex.h - 1)
    texel = tex.rgba[iv, iu]
    a = texel[..., 3]
    zb = zbuf[y0:y1, x0:x1]
    mask = inside & (a > 0.1) & (depth > zb + 1e-4)
    if not mask.any():
        return
    light = 1.0 if q.emissive else _brightness(q.normal, q.shade) * ambient
    rgb = texel[..., :3] * light
    if q.emissive:
        rgb = np.clip(rgb * emissive_boost, 0, 1)
    if tex.glow is not None:
        # GeckoLib's glow layer redraws glowmask pixels full-bright (eyes shader, alpha blended) over the base
        gl = tex.glow[iv, iu]
        ga = gl[..., 3:4]
        rgb = rgb * (1 - ga) + gl[..., :3] * ga
    col = color[y0:y1, x0:x1]
    al = alpha[y0:y1, x0:x1]
    if blend:
        aa = a[..., None]
        col[mask] = (rgb * aa + col * (1 - aa))[mask]
        al[mask] = np.maximum(al[mask], a[mask])
    else:
        col[mask] = rgb[mask]
        al[mask] = 1.0
    zb[mask] = depth[mask]
