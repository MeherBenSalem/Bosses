"""Rig + animation description that is built inside Blockbench through the MCP plugin.

A Rig is authored in Python (bones, cubes, locators, clips), painted into a pixel-art atlas
by paint.py, then constructed in a fresh GeckoLib project in Blockbench, which exports the
.bbmodel source, the GeckoLib .geo.json and .animation.json, and preview renders.
"""
import base64
import io
import json
import math
import zlib
from dataclasses import dataclass, field
from pathlib import Path

from paint import PALETTE, Atlas, mirrored_rects, paint_cube_net, texel_dims

EASINGS = {'linear', 'step', 'easeInSine', 'easeOutSine', 'easeInOutSine', 'easeInQuad', 'easeOutQuad',
           'easeInOutQuad', 'easeInCubic', 'easeOutCubic', 'easeInOutCubic', 'easeInQuart', 'easeOutQuart',
           'easeInOutQuart', 'easeInExpo', 'easeOutExpo', 'easeInOutExpo', 'easeInBack', 'easeOutBack',
           'easeInOutBack', 'easeInElastic', 'easeOutElastic', 'easeInBounce', 'easeOutBounce'}


@dataclass
class Bone:
    name: str
    pivot: tuple
    parent: str = None
    rotation: tuple = (0, 0, 0)


@dataclass
class Cube:
    bone: str
    name: str
    frm: tuple
    to: tuple
    mat: str
    rotation: tuple = (0, 0, 0)
    origin: tuple = None
    inflate: float = 0.0
    decals: tuple = ()
    share: str = None      # paint key of a cube whose UV region this cube reuses
    mirror: bool = False   # reuse the shared region mirrored on X
    rects: dict = None

    @property
    def size(self):
        return tuple(b - a for a, b in zip(self.frm, self.to))

    @property
    def key(self):
        return f'{self.bone}/{self.name}'


@dataclass
class Clip:
    name: str
    length: float
    loop: object = False          # True, False or 'hold'
    channels: dict = field(default_factory=dict)  # bone -> channel -> [(t, value, easing)]
    seam_period: dict = field(default_factory=dict)  # bone -> rotational symmetry (degrees) for loop seams

    def _add(self, bone, channel, keys):
        track = self.channels.setdefault(bone, {}).setdefault(channel, [])
        for k in keys:
            t, value = k[0], k[1]
            easing = k[2] if len(k) > 2 else 'easeInOutSine'
            if easing not in EASINGS:
                raise ValueError(f'{self.name}: unknown easing {easing}')
            if t > self.length + 1e-6:
                raise ValueError(f'{self.name}: key at {t}s beyond length {self.length}s')
            if isinstance(value, (int, float)):
                value = [value, value, value]
            track.append((round(t, 4), [float(v) for v in value], easing))
        track.sort(key=lambda k: k[0])
        return self

    def rot(self, bone, *keys):
        return self._add(bone, 'rotation', keys)

    def pos(self, bone, *keys):
        return self._add(bone, 'position', keys)

    def scale(self, bone, *keys):
        return self._add(bone, 'scale', keys)

    def poses(self, frames, easing='easeInOutSine'):
        """Author a clip as key poses.

        frames: [(time, {bone: {'rot': [...], 'pos': [...], 'scale': [...]}}, optional easing), ...]
        Every bone/channel mentioned anywhere is keyed at every frame time; a frame that omits it holds
        the previous value (the neutral pose before the first mention). This keeps blends predictable.
        """
        neutral = {'rot': [0, 0, 0], 'pos': [0, 0, 0], 'scale': [1, 1, 1]}
        channels = {}
        for _, pose, *_ in frames:
            for bone, chans in pose.items():
                for chan in chans:
                    channels.setdefault((bone, chan), None)
        for (bone, chan) in channels:
            current = list(neutral[chan])
            keys = []
            for frame in frames:
                t, pose = frame[0], frame[1]
                ease = frame[2] if len(frame) > 2 else easing
                if bone in pose and chan in pose[bone]:
                    v = pose[bone][chan]
                    current = [v, v, v] if isinstance(v, (int, float)) else list(v)
                keys.append((t, list(current), ease))
            name = {'rot': 'rotation', 'pos': 'position', 'scale': 'scale'}[chan]
            self._add(bone, name, keys)
        return self


class Rig:
    def __init__(self, name, texture_size=(256, 256), palette=None, root_pivot=(0, 0, 0)):
        self.name = name
        self.tw, self.th = texture_size
        self.palette = dict(PALETTE, **(palette or {}))
        self.bones = {}
        self.cubes = []
        self.locators = []
        self.clips = {}
        self.upright = set()
        self.preview_hidden = set()   # bones hidden in previews (shown by game logic only)
        self.bone('root', root_pivot, None)

    def bone(self, name, pivot, parent='root', rotation=(0, 0, 0)):
        if name in self.bones:
            raise ValueError(f'duplicate bone {name}')
        if parent is not None and parent not in self.bones:
            raise ValueError(f'bone {name}: unknown parent {parent}')
        self.bones[name] = Bone(name, tuple(pivot), parent, tuple(rotation))
        return name

    def cube(self, bone, name, frm, to, mat, rot=(0, 0, 0), origin=None, decals=(), inflate=0.0,
             share=None, mirror=False):
        if bone not in self.bones:
            raise ValueError(f'cube {name}: unknown bone {bone}')
        if bone == 'root':
            raise ValueError(f'cube {name}: attach cubes to a child bone, not root '
                             '(place_cube treats group "root" as the project root)')
        if mat not in self.palette:
            raise ValueError(f'cube {name}: unknown material {mat}')
        frm, to = tuple(frm), tuple(to)
        if origin is None:
            origin = tuple((a + b) / 2 for a, b in zip(frm, to)) if any(rot) else self.bones[bone].pivot
        c = Cube(bone, name, frm, to, mat, tuple(rot), tuple(origin), inflate, tuple(decals), share, mirror)
        if any(c2.key == c.key for c2 in self.cubes):
            raise ValueError(f'duplicate cube {c.key}')
        self.cubes.append(c)
        return c

    def box(self, bone, name, center, size, mat, rot=(0, 0, 0), origin=None, **kw):
        frm = [c - s / 2 for c, s in zip(center, size)]
        to = [c + s / 2 for c, s in zip(center, size)]
        if origin is None and any(rot):
            origin = tuple(center)
        return self.cube(bone, name, frm, to, mat, rot, origin, **kw)

    def pair(self, bone_fmt, name_fmt, center, size, mat, rot=(0, 0, 0), origin=None, **kw):
        """Left (+X) cube and its mirrored right (-X) twin sharing one UV region."""
        left = self.box(bone_fmt.format(side='left'), name_fmt.format(side='left'), center, size, mat, rot,
                        origin, **kw)
        mc = (-center[0], center[1], center[2])
        mrot = (rot[0], -rot[1], -rot[2])
        morigin = None if origin is None else (-origin[0], origin[1], origin[2])
        right = self.box(bone_fmt.format(side='right'), name_fmt.format(side='right'), mc, size, mat, mrot,
                         morigin, share=left.key, mirror=True, **{k: v for k, v in kw.items() if k != 'share'})
        return left, right

    def bone_pair(self, name_fmt, pivot, parent_fmt='root', rotation=(0, 0, 0)):
        for side, s in (('left', 1), ('right', -1)):
            parent = parent_fmt.format(side=side)
            self.bone(name_fmt.format(side=side), (pivot[0] * s, pivot[1], pivot[2]), parent,
                      (rotation[0], rotation[1] * s, rotation[2] * s))

    def locator(self, bone, name, position):
        self.locators.append((bone, name, tuple(position)))

    def mark_upright(self, *bones):
        """Bones whose geometry rises above their pivot (torso, head...). For these, authored X rotations
        mean "positive pitches forward" (a bow); Blockbench's X+ swings a bone's lower end forward, which
        tips upward geometry backward, so X is negated for these bones at build time."""
        self.upright.update(bones)

    def _editor_rot(self, bone, value):
        return [-value[0], value[1], value[2]] if bone in self.upright else list(value)

    def clip(self, name, length, loop=False):
        c = Clip(name, length, loop)
        self.clips[name] = c
        return c

    def paint(self):
        atlas = Atlas(self.tw, self.th)
        by_key = {}
        order = sorted((c for c in self.cubes if not c.share), key=lambda c: -(texel_dims(c.size)[1] + texel_dims(c.size)[2]))
        for c in order:
            w, h, d = texel_dims(c.size)
            net = atlas.allocate(w, h, d)
            paint_cube_net(atlas, net, c.mat, self.palette, c.decals, seed=zlib.crc32(c.key.encode()))
            c.rects = dict(net.rects)
            by_key[c.key] = c
        for c in self.cubes:
            if c.share:
                src = by_key.get(c.share)
                if src is None:
                    raise ValueError(f'{c.key} shares unknown {c.share}')
                if texel_dims(src.size) != texel_dims(c.size):
                    raise ValueError(f'{c.key} shares {c.share} with a different size')
                c.rects = mirrored_rects(src.rects) if c.mirror else dict(src.rects)
        self.atlas = atlas
        return atlas

    def build(self, bb, out_dir, preview_clips=(), views=None):
        out = Path(out_dir)
        out.mkdir(parents=True, exist_ok=True)
        atlas = self.paint()
        tex_path = out / f'{self.name}.png'
        glow_path = out / f'{self.name}_glowmask.png'
        atlas.save(tex_path, glow_path)
        has_glow = atlas.glow.getbbox() is not None
        if not has_glow:
            glow_path.unlink()

        bb.call('create_project', name=self.name, format='geckolib_model')
        bb.eval(f"Project.texture_width={self.tw}; Project.texture_height={self.th}; "
                f"Project.geometry_name='{self.name}'; Project.box_uv=false; 'ok'")
        buf = io.BytesIO()
        atlas.image.save(buf, 'PNG')
        bb.call('create_texture', name=f'{self.name}.png', width=self.tw, height=self.th,
                data='data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode())

        # Groups in parent-first order
        for b in self._bones_topological():
            if b.name == 'root' and b.parent is None:
                bb.call('add_group', name='root', origin=list(b.pivot))
                continue
            bb.call('add_group', name=b.name, origin=list(b.pivot), parent=b.parent or 'root',
                    rotation=self._editor_rot(b.name, b.rotation))

        # Cubes and their per-face UVs in one transaction per bone
        by_bone = {}
        for c in self.cubes:
            by_bone.setdefault(c.bone, []).append(c)
        for bone, cubes in by_bone.items():
            elements = [{'name': c.name, 'from': list(c.frm), 'to': list(c.to), 'origin': list(c.origin),
                         'rotation': list(c.rotation), **({'inflate': c.inflate} if c.inflate else {})}
                        for c in cubes]
            bb.call('place_cube', elements=elements, group=bone, texture=f'{self.name}.png')
        assignments = [{'name': c.name, 'group': c.bone,
                        'faces': {f: list(r) for f, r in c.rects.items()}} for c in self.cubes]
        res = bb.eval(
            "const A=" + json.dumps(assignments) + ";"
            "const tex=Texture.all.find(t=>t.name==='" + self.name + ".png');"
            "Undo.initEdit({elements:Cube.all});let n=0,miss=[];"
            "for(const a of A){const c=Cube.all.find(c=>c.name===a.name&&c.parent&&c.parent.name===a.group);"
            "if(!c){miss.push(a.group+'/'+a.name);continue;}c.box_uv=false;"
            "for(const [f,uv] of Object.entries(a.faces)){c.faces[f].uv=uv;c.faces[f].texture=tex.uuid;}n++;}"
            "Canvas.updateAll();Undo.finishEdit('Remnants per-face UV');JSON.stringify({mapped:n,missing:miss})")
        if res.get('missing'):
            raise RuntimeError(f'cubes missing after placement: {res["missing"]}')

        for bone, name, pos in self.locators:
            bb.call('add_locator', name=name, parent=bone, position=list(pos))

        # Animations: keyframes, then GeckoLib easing per keyframe
        clip_meta = {}
        for clip in self.clips.values():
            bones = {}
            for bone, chans in clip.channels.items():
                if bone not in self.bones:
                    raise ValueError(f'clip {clip.name}: unknown bone {bone}')
                merged = {}
                for chan, keys in chans.items():
                    for t, v, _ in keys:
                        merged.setdefault(t, {'time': t})[chan] = self._editor_rot(bone, v) if chan == 'rotation' else v
                bones[bone] = [merged[t] for t in sorted(merged)]
            bb.call('create_animation', name=f'animation.{self.name}.{clip.name}', loop=clip.loop is True,
                    animation_length=clip.length, bones=bones)
            easing = {bone: {chan: [[t, e] for t, _, e in keys] for chan, keys in chans.items()}
                      for bone, chans in clip.channels.items()}
            loop_mode = 'loop' if clip.loop is True else ('hold' if clip.loop == 'hold' else 'once')
            bb.eval(
                "const E=" + json.dumps(easing) + ";"
                f"const anim=Animation.all.find(a=>a.name==='animation.{self.name}.{clip.name}');"
                f"anim.loop='{loop_mode}';anim.snapping=24;let n=0;"
                "for(const [bone,chans] of Object.entries(E)){const ga=Object.values(anim.animators).find(x=>x.name===bone);"
                "if(!ga)continue;for(const [chan,keys] of Object.entries(chans)){for(const [t,e] of keys){"
                "const kf=ga[chan].find(k=>Math.abs(k.time-t)<1e-3);if(kf){kf.interpolation='linear';kf.easing=e;n++;}}}}"
                "JSON.stringify(n)")
            clip_meta[clip.name] = {'length': clip.length, 'loop': loop_mode}

        anim_json = bb.eval("JSON.stringify(Animator.buildFile(null, Animation.all.map(a=>a.name)))")
        (out / f'{self.name}.animation.json').write_text(json.dumps(anim_json, indent=2), encoding='utf-8')
        (out / 'clips.json').write_text(json.dumps(clip_meta, indent=2), encoding='utf-8')
        data = bb.json('export_model', codec_id='project', max_content_length=2000000)
        if len(data['content']) >= 2000000:
            raise RuntimeError('export truncated')
        (out / f'{self.name}.bbmodel').write_text(data['content'], encoding='utf-8')
        # GeckoLib projects compile geometry through the Bedrock codec, whose export action is gated
        # to Bedrock formats; compile it directly so the .geo.json matches the GeckoLib plugin output.
        geo = bb.eval("Codecs.bedrock.compile({raw:true})")
        if not isinstance(geo, dict) or 'minecraft:geometry' not in geo:
            raise RuntimeError(f'unexpected geometry export: {str(geo)[:200]}')
        (out / f'{self.name}.geo.json').write_text(json.dumps(geo, indent=2), encoding='utf-8')
        self.render_previews(bb, out, preview_clips, views)
        return {'cubes': len(self.cubes), 'bones': len(self.bones), 'clips': list(self.clips),
                'atlas_rows_used': atlas.used_height, 'glow': has_glow}

    def retexture(self, bb, out_dir, preview_clips=(), views=None):
        """Repaint the atlas and swap it into the open Blockbench project (same cubes, same UVs)."""
        out = Path(out_dir)
        atlas = self.paint()
        glow_path = out / f'{self.name}_glowmask.png'
        atlas.save(out / f'{self.name}.png', glow_path)
        if atlas.glow.getbbox() is None:
            glow_path.unlink()
        buf = io.BytesIO()
        atlas.image.save(buf, 'PNG')
        url = 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()
        bb.eval("const t=Texture.all.find(t=>t.name==='" + self.name + ".png');t.fromDataURL('" + url + "');"
                "t.updateMaterial?.();Canvas.updateAll();'ok'")
        data = bb.json('export_model', codec_id='project', max_content_length=2000000)
        (out / f'{self.name}.bbmodel').write_text(data['content'], encoding='utf-8')
        self.render_previews(bb, out, preview_clips, views)

    def render_previews(self, bb, out, preview_clips=(), views=None):
        height = max(c.to[1] for c in self.cubes)
        span = max(max(abs(c.frm[0]), abs(c.to[0]), abs(c.frm[2]), abs(c.to[2])) for c in self.cubes)
        dist = max(height, span * 2) * 1.6 + 20
        mid = height * 0.5
        views = views or {
            'front': [dist * 0.55, mid + dist * 0.25, -dist],
            'back': [-dist * 0.55, mid + dist * 0.2, dist],
            'side': [dist * 1.1, mid + dist * 0.1, 0.01],
        }
        try:
            bb.call('delete_offscreen_view', view='remnants_preview')
        except Exception:
            pass
        bb.call('create_offscreen_view', id='remnants_preview', width=900, height=900, antialias=False,
                copy_view='none')
        def visibility(clip=None):
            hide = [b for b in self.preview_hidden if not (clip and clip.startswith(('shield', 'reconstruct')))]
            bb.eval("const H=" + json.dumps(hide) + ";const setv=(g,v)=>{g.visibility=v;g.children.forEach(c=>{"
                    "if(c instanceof Group)setv(c,v);else c.visibility=v;});};"
                    "Group.all.forEach(g=>setv(g,true));Group.all.filter(g=>H.includes(g.name)).forEach(g=>setv(g,false));"
                    "Canvas.updateVisibility();'ok'")
        bb.call('set_mode', mode_id='edit')
        bb.eval("Animation.all.forEach(x=>{x.playing=false;});Animator.showDefaultPose(true);'ok'")
        visibility()
        for label, pos in views.items():
            res = bb.call('set_camera_angle', view='remnants_preview', position=pos, target=[0, mid, 0],
                          projection='perspective', fov=40)
            img = bb.images(bb.call('capture_screenshot', view='remnants_preview'))
            if img:
                (out / f'preview_{label}.png').write_bytes(img[0])
        if preview_clips:
            bb.call('set_mode', mode_id='animate')  # Blockbench only applies clip poses in Animate mode
        for clip, t in preview_clips:
            visibility(clip)
            bb.eval(f"Animation.all.forEach(x=>{{x.playing=false;}});"
                    f"const a=Animation.all.find(a=>a.name==='animation.{self.name}.{clip}');a.select();a.playing=true;"
                    f"Animator.showDefaultPose(true);Timeline.setTime({t});Animator.preview();'ok'")
            bb.call('set_camera_angle', view='remnants_preview', position=views['front'], target=[0, mid, 0],
                    projection='perspective', fov=40)
            img = bb.images(bb.call('capture_screenshot', view='remnants_preview'))
            if img:
                (out / f'pose_{clip}_{t:.2f}.png').write_bytes(img[0])
        bb.eval("Animation.all.forEach(x=>{x.playing=false;});Animator.showDefaultPose(true);"
                "Group.all.forEach(g=>{g.visibility=true;});Cube.all.forEach(c=>{c.visibility=true;});Canvas.updateVisibility();'ok'")
        bb.call('set_mode', mode_id='edit')
        try:
            bb.call('delete_offscreen_view', view='remnants_preview')
        except Exception:
            pass  # plugin 1.7 throws 'onContextMenu is not defined' after disposing; the view is gone

    def _bones_topological(self):
        done, order = set(), []

        def visit(b):
            if b.name in done:
                return
            if b.parent:
                visit(self.bones[b.parent])
            done.add(b.name)
            order.append(b)
        for b in self.bones.values():
            visit(b)
        return order

    def validate(self):
        problems = []
        for c in self.cubes:
            if any(s <= 0 for s in c.size):
                problems.append(f'{c.key}: non-positive size {c.size}')
        for clip in self.clips.values():
            for bone, chans in clip.channels.items():
                for chan, keys in chans.items():
                    for t, v, _ in keys:
                        if any(math.isnan(x) or math.isinf(x) for x in v):
                            problems.append(f'{clip.name}/{bone}/{chan}@{t}: non-finite')
                    if clip.loop is True and keys and keys[0][0] == 0 and keys[-1][0] == clip.length:
                        def seam_ok(a, b):
                            d = abs(a - b)
                            period = clip.seam_period.get(bone, 360)
                            return d < 1e-3 or (chan == 'rotation' and abs(d / period - round(d / period)) < 1e-6)
                        if not all(seam_ok(a, b) for a, b in zip(keys[0][1], keys[-1][1])):
                            problems.append(f'{clip.name}/{bone}/{chan}: loop seam mismatch')
        return problems
