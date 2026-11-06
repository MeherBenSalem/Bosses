#!/usr/bin/env python3
"""Compositor for the reel: cuts the captured shots together on the beat, grades them, draws the titles and
captions, mixes the audio and encodes a 1080x1920 H.264/AAC file for Instagram Reels and TikTok.

The edit itself (which shot, which seconds, which effect, which words) lives in reel.py; this file is the machinery.
"""
from __future__ import annotations

import math
import subprocess
from dataclasses import dataclass, field
from functools import lru_cache
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H, FPS = 1080, 1920, 30
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "1.21.1" / "fabric" / "runs" / "reel" / "reel" / "out"
FONTS = {
    "cinzel": str(ROOT / "marketing" / "source" / "fonts" / "cinzel.ttf"),
    "garamond": str(ROOT / "marketing" / "source" / "fonts" / "eb-garamond.ttf"),
    "black": "/usr/share/fonts/opentype/inter/Inter-Black.otf",
    "bold": "/usr/share/fonts/opentype/inter/Inter-Bold.otf",
}
# The grade, applied by the decoding ffmpeg (multi-threaded C) before frames reach Python: a filmic S-curve with lifted
# blacks, gold highlights and teal shadows, a little more saturation, and a light sharpen.
GRADE = ("curves=r='0/0.049 0.06/0.086 0.15/0.154 0.3/0.301 0.45/0.48 0.6/0.665 0.75/0.83 0.9/0.954 1/1'"
         ":g='0/0.065 0.06/0.099 0.15/0.162 0.3/0.304 0.45/0.48 0.6/0.663 0.75/0.821 0.9/0.937 1/0.992'"
         ":b='0/0.089 0.06/0.118 0.15/0.176 0.3/0.309 0.45/0.48 0.6/0.659 0.75/0.802 0.9/0.899 1/0.94',"
         "eq=saturation=1.16,unsharp=5:5:0.55:5:5:0")
# Applied by the encoder: vignette and fine film grain.
FINISH = "vignette=angle=0.62:mode=forward,noise=c0s=3:c0f=t+u"
GOLD = (232, 199, 122)
PALE_GOLD = (243, 227, 181)
WHITE = (255, 255, 255)


# ---------------------------------------------------------------- the edit model

@dataclass
class Clip:
    shot: str
    src: float          # source in-point, seconds
    dur: float          # duration in the reel, seconds
    speed: float = 1.0  # source seconds per reel second
    zoom: tuple = (1.0, 1.0)      # scale at start and end (>= 1 crops in)
    center: tuple = (0.5, 0.5)    # zoom centre, in frame fractions
    punch: float = 0.0            # extra zoom at the cut that settles over ~8 frames
    flash: float = 0.0            # white flash strength at the cut
    whip: int = 0                 # whip-pan into this clip: -1 from the left, 1 from the right
    fade_in: float = 0.0          # from black, seconds
    fade_out: float = 0.0         # to black, seconds
    shake: float = 0.0            # extra hand-held shake in pixels
    image: str | None = None      # a still image instead of a shot (end card backdrop)
    blur: float = 0.0             # gaussian blur radius (end card backdrop)
    dim: float = 0.0              # darkening ramped in over the clip (0..1), for text over footage
    exposure: float = 0.0         # midtone lift before the grade (gamma 1 + exposure), for dark arenas


@dataclass(frozen=True)
class Text:
    text: str
    start: float
    end: float
    y: int                        # vertical centre, pixels
    font: str = "black"
    size: int = 92
    color: tuple = WHITE
    accent: tuple | None = None   # colour for words wrapped in *asterisks*
    tracking: float = 0.0         # extra letter spacing, in em
    stroke: int = 0
    glow: tuple | None = None     # glow colour behind the text
    shadow: bool = True
    anim: str = "pop"             # pop, rise, fade, type, slam
    rule: bool = False            # gold rules above and below (title cards)
    align: str = "center"
    x: int = W // 2
    max_width: int = 900
    line_gap: float = 1.08
    gradient: tuple | None = None  # (top colour, bottom colour) instead of a flat fill
    band: bool = False             # a translucent dark band behind the text


@dataclass
class Image_:
    path: str
    start: float
    end: float
    y: int
    width: int
    anim: str = "fade"
    glow: tuple | None = None


@dataclass
class Edit:
    clips: list
    texts: list = field(default_factory=list)
    images: list = field(default_factory=list)
    flashes: list = field(default_factory=list)   # (time, strength, colour)

    @property
    def duration(self) -> float:
        return sum(c.dur for c in self.clips)


# ---------------------------------------------------------------- frames

def _source_path(shot: str) -> Path:
    return OUT / f"{shot}.mp4"


def clip_frames(clip: Clip, n: int):
    """Yields n RGB frames for a clip (uint8 HxWx3), sampling the source at clip.speed with frame blending."""
    if clip.image:
        img = Image.open(clip.image).convert("RGB")
        img = _cover(img, int(W * 1.12), int(H * 1.12))
        if clip.blur:
            img = img.filter(ImageFilter.GaussianBlur(clip.blur))
        arr = np.asarray(img)
        for _ in range(n):
            yield arr
        return
    need = clip.dur * clip.speed + 2 / FPS
    proc = subprocess.Popen(["ffmpeg", "-v", "error", "-ss", f"{clip.src:.4f}", "-i", str(_source_path(clip.shot)),
                             "-t", f"{need:.4f}", "-vf", (f"eq=gamma={1 + clip.exposure:.3f}," if clip.exposure else "") + GRADE,
                             "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
                            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    size = W * H * 3
    buf: list[np.ndarray] = []
    read = 0

    def get(i: int) -> np.ndarray:
        nonlocal read
        while read <= i:
            data = proc.stdout.read(size)
            if len(data) < size:
                return buf[-1] if buf else np.zeros((H, W, 3), np.uint8)
            buf.append(np.frombuffer(data, np.uint8).reshape(H, W, 3))
            read += 1
            if len(buf) > 4:
                buf.pop(0)
        return buf[i - (read - len(buf))] if i >= read - len(buf) else buf[0]

    for k in range(n):
        pos = k * clip.speed
        i = int(pos)
        f = pos - i
        a = get(i)
        if f > 0.15 and clip.speed < 0.999:
            b = get(i + 1)
            yield (a.astype(np.uint16) * int(256 * (1 - f)) + b.astype(np.uint16) * int(256 * f) >> 8).astype(np.uint8)
        else:
            yield a
    proc.stdout.close()
    proc.wait()


def _cover(img: Image.Image, w: int, h: int) -> Image.Image:
    s = max(w / img.width, h / img.height)
    img = img.resize((int(img.width * s + 0.5), int(img.height * s + 0.5)), Image.LANCZOS)
    x, y = (img.width - w) // 2, (img.height - h) // 2
    return img.crop((x, y, x + w, y + h)).resize((W, H), Image.LANCZOS) if (w, h) != (W, H) else img.crop((x, y, x + w, y + h))


def ease(u: float) -> float:
    u = min(1.0, max(0.0, u))
    return u * u * (3 - 2 * u)


def transform(frame: np.ndarray, scale: float, cx: float, cy: float, dx: float = 0, dy: float = 0, angle: float = 0) -> np.ndarray:
    if abs(scale - 1) < 1e-4 and dx == 0 and dy == 0 and angle == 0:
        return frame
    m = cv2.getRotationMatrix2D((cx * W, cy * H), angle, scale)
    m[0, 2] += dx
    m[1, 2] += dy
    return cv2.warpAffine(frame, m, (W, H), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REFLECT)


def motion_blur(frame: np.ndarray, px: int) -> np.ndarray:
    """Horizontal box blur of px pixels (whip pans)."""
    if px < 2:
        return frame
    return cv2.blur(frame, (px, 1))


# ---------------------------------------------------------------- text

@lru_cache(maxsize=64)
def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONTS[name], size)


def _wrap(t: Text, f: ImageFont.FreeTypeFont) -> list[str]:
    lines = []
    for para in t.text.split("\n"):
        words, line = para.split(" "), ""
        for w_ in words:
            trial = (line + " " + w_).strip()
            if _width(trial.replace("*", ""), f, t.tracking) > t.max_width and line:
                lines.append(line)
                line = w_
            else:
                line = trial
        lines.append(line)
    return lines


def _width(s: str, f: ImageFont.FreeTypeFont, tracking: float) -> float:
    return f.getlength(s) + tracking * f.size * max(0, len(s) - 1)


@lru_cache(maxsize=128)
def render_text(t: Text) -> tuple[np.ndarray, int, int]:
    """Renders a text block once: RGBA array plus its top-left position."""
    f = font(t.font, t.size)
    lines = _wrap(t, f)
    asc, desc = f.getmetrics()
    lh = int((asc + desc) * t.line_gap)
    pad = int(t.size * 0.9)
    widths = [_width(l.replace("*", ""), f, t.tracking) for l in lines]
    bw = int(max(widths)) + 2 * pad
    bh = lh * len(lines) + 2 * pad + (int(t.size * 0.5) if t.rule else 0)
    layer = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
    mask = Image.new("L", (bw, bh), 0)
    draw = ImageDraw.Draw(layer)
    mdraw = ImageDraw.Draw(mask)
    y = pad + (int(t.size * 0.25) if t.rule else 0)
    for line, lw in zip(lines, widths):
        x = pad + (bw - 2 * pad - lw) / 2 if t.align == "center" else pad
        accent = False
        for part in _segments(line):
            if part == "*":
                accent = not accent
                continue
            colour = (t.accent if accent and t.accent else t.color)
            for ch in part:
                draw.text((x, y), ch, font=f, fill=colour + (255,), stroke_width=t.stroke, stroke_fill=colour + (255,))
                mdraw.text((x, y), ch, font=f, fill=255, stroke_width=t.stroke + 2)
                x += f.getlength(ch) + t.tracking * t.size
        y += lh
    if t.gradient:
        top, bottom = np.array(t.gradient[0], np.float32), np.array(t.gradient[1], np.float32)
        ramp = np.linspace(0, 1, bh, dtype=np.float32)[:, None, None]
        grad = (top * (1 - ramp) + bottom * ramp).repeat(bw, axis=1)
        arr = np.array(layer)
        arr[..., :3] = grad.astype(np.uint8)
        layer = Image.fromarray(arr)
        draw = ImageDraw.Draw(layer)
    if t.rule:
        ry0, ry1 = int(pad * 0.45), bh - int(pad * 0.45)
        cx0, cx1 = pad - int(t.size * 0.6), bw - pad + int(t.size * 0.6)
        for ry in (ry0, ry1):
            draw.line([(cx0, ry), (cx1, ry)], fill=GOLD + (230,), width=3)
            mdraw.line([(cx0, ry), (cx1, ry)], fill=255, width=5)
    out = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
    if t.band:
        ImageDraw.Draw(out).rounded_rectangle([pad * 0.45, pad * 0.55, bw - pad * 0.45, bh - pad * 0.55], radius=int(t.size * 0.35),
                                              fill=(8, 6, 4, 165))
    if t.shadow:
        sh = mask.filter(ImageFilter.GaussianBlur(t.size * 0.12))
        shadow = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
        shadow.putalpha(sh.point(lambda v: int(v * 0.85)))
        out = Image.alpha_composite(out, shadow)
    if t.glow:
        gl = mask.filter(ImageFilter.GaussianBlur(t.size * 0.35))
        glow = Image.new("RGBA", (bw, bh), t.glow + (0,))
        glow.putalpha(gl.point(lambda v: int(v * 0.75)))
        out = Image.alpha_composite(out, glow)
    out = Image.alpha_composite(out, layer)
    arr = np.asarray(out)
    left = t.x - bw // 2 if t.align == "center" else t.x - pad
    return arr, left, t.y - bh // 2


def _segments(line: str) -> list[str]:
    parts, cur = [], ""
    for ch in line:
        if ch == "*":
            if cur:
                parts.append(cur)
            parts.append("*")
            cur = ""
        else:
            cur += ch
    if cur:
        parts.append(cur)
    return parts


def text_state(t: Text, now: float) -> tuple[float, float, float]:
    """(alpha, scale, dy) for a text at time now."""
    if now < t.start or now > t.end:
        return 0.0, 1.0, 0.0
    a_in = (now - t.start) / 0.22
    a_out = (t.end - now) / 0.25
    alpha = min(1.0, a_in, a_out)
    scale, dy = 1.0, 0.0
    u = ease((now - t.start) / 0.28)
    if t.anim == "pop":
        scale = 1.18 - 0.18 * u if now - t.start < 0.28 else 1.0
        scale *= 1 - 0.04 * max(0.0, 1 - a_out)
    elif t.anim == "slam":
        scale = 1.3 - 0.3 * ease((now - t.start) / 0.16)
        alpha = min(1.0, 0.35 + (now - t.start) / 0.08, a_out)
    elif t.anim == "punch":
        # Fully legible from its first frame (a thumbnail), settling from slightly large.
        scale = 1.08 - 0.08 * ease((now - t.start) / 0.3)
        alpha = min(1.0, a_out)
    elif t.anim == "rise":
        dy = 40 * (1 - u)
    elif t.anim == "drift":
        scale = 1.0 + 0.05 * (now - t.start) / max(0.1, t.end - t.start)
    return max(0.0, alpha), scale, dy


def composite(frame: np.ndarray, layer: np.ndarray, left: int, top: int, alpha: float, scale: float = 1.0, dy: float = 0.0):
    if alpha <= 0.003:
        return frame
    if abs(scale - 1) > 1e-3:
        h0, w0 = layer.shape[:2]
        img = Image.fromarray(layer).resize((max(1, int(w0 * scale)), max(1, int(h0 * scale))), Image.BICUBIC)
        layer2 = np.asarray(img)
        left = int(left + (w0 - layer2.shape[1]) / 2)
        top = int(top + (h0 - layer2.shape[0]) / 2)
        layer = layer2
    top = int(top + dy)
    h, w = layer.shape[:2]
    x0, y0 = max(0, left), max(0, top)
    x1, y1 = min(W, left + w), min(H, top + h)
    if x1 <= x0 or y1 <= y0:
        return frame
    sub = layer[y0 - top:y1 - top, x0 - left:x1 - left]
    a = (sub[..., 3:4].astype(np.uint16) * int(alpha * 256)) >> 8
    region = frame[y0:y1, x0:x1].astype(np.uint16)
    frame[y0:y1, x0:x1] = ((region * (255 - a) + sub[..., :3].astype(np.uint16) * a) // 255).astype(np.uint8)
    return frame


@lru_cache(maxsize=16)
def render_image(path: str, width: int, glow: tuple | None) -> np.ndarray:
    img = Image.open(path).convert("RGBA")
    img = img.resize((width, int(img.height * width / img.width)), Image.LANCZOS)
    if glow:
        pad = width // 5
        canvas = Image.new("RGBA", (img.width + 2 * pad, img.height + 2 * pad), (0, 0, 0, 0))
        a = Image.new("L", canvas.size, 0)
        a.paste(img.getchannel("A"), (pad, pad))
        g = Image.new("RGBA", canvas.size, glow + (0,))
        g.putalpha(a.filter(ImageFilter.GaussianBlur(width // 14)).point(lambda v: int(v * 0.9)))
        canvas = Image.alpha_composite(canvas, g)
        canvas.alpha_composite(img, (pad, pad))
        img = canvas
    return np.asarray(img)


# ---------------------------------------------------------------- render

def render(edit: Edit, out_path: Path, audio_path: Path | None = None, start: float = 0.0, end: float | None = None,
           crf: int = 18, preview_every: int = 0, preview_dir: Path | None = None, preset: str = "slow") -> None:
    total = edit.duration
    end = total if end is None else min(end, total)
    cmd = ["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-"]
    if audio_path:
        cmd += ["-ss", f"{start:.3f}", "-i", str(audio_path)]
    cmd += ["-t", f"{end - start:.3f}", "-vf", FINISH, "-c:v", "libx264", "-preset", preset, "-crf", str(crf), "-maxrate", "9M", "-bufsize", "18M", "-pix_fmt", "yuv420p",
            "-profile:v", "high", "-level", "4.2", "-r", str(FPS), "-g", "60", "-bf", "2", "-colorspace", "bt709",
            "-color_primaries", "bt709", "-color_trc", "bt709"]
    if audio_path:
        cmd += ["-c:a", "aac", "-b:a", "256k", "-ar", "48000"]
    cmd += ["-movflags", "+faststart", str(out_path)]
    enc = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    texts = [(t, *render_text(t)) for t in edit.texts]
    images = [(im, render_image(im.path, im.width, im.glow)) for im in edit.images]
    t0 = 0.0
    k = 0
    for ci, clip in enumerate(edit.clips):
        n = int(round((t0 + clip.dur) * FPS)) - int(round(t0 * FPS))
        first = int(round(t0 * FPS))
        if (first + n) / FPS <= start or first / FPS >= end:
            t0 += clip.dur
            continue
        for j, frame in enumerate(clip_frames(clip, n)):
            now = (first + j) / FPS
            if now < start - 1e-6 or now >= end - 1e-6:
                continue
            u = j / max(1, n - 1)
            z = clip.zoom[0] + (clip.zoom[1] - clip.zoom[0]) * ease(u)
            if clip.punch:
                z *= 1 + clip.punch * max(0.0, 1 - j / 8) ** 2
            dx = dy = ang = 0.0
            if clip.shake:
                dx = clip.shake * (math.sin(now * 37) + 0.5 * math.sin(now * 71))
                dy = clip.shake * (math.cos(now * 41) + 0.5 * math.sin(now * 59))
            if clip.whip and j < 5:
                dx += clip.whip * W * 0.25 * (1 - j / 5) ** 2
            f = transform(frame, z, clip.center[0], clip.center[1], dx, dy, ang)
            if clip.whip and j < 5:
                f = motion_blur(f, int(160 * (1 - j / 5)))
            nxt = edit.clips[ci + 1] if ci + 1 < len(edit.clips) else None
            if nxt is not None and nxt.whip and j >= n - 4:
                f = motion_blur(transform(f, 1.0, 0.5, 0.5, -nxt.whip * W * 0.12 * (j - (n - 4)) / 4), int(140 * (j - (n - 5)) / 4))
            f = np.array(f)
            flash = 0.0
            if clip.flash:
                flash = max(flash, clip.flash * max(0.0, 1 - j / 6))
            for ft, fs, fc in edit.flashes:
                if 0 <= now - ft < 0.25:
                    flash = max(flash, fs * (1 - (now - ft) / 0.25))
            if flash > 0:
                f = (f.astype(np.float32) * (1 - flash) + np.array([255, 246, 225], np.float32) * flash).astype(np.uint8)
            if clip.dim:
                d = clip.dim * ease(j / max(1, min(n - 1, 18)))
                f = (f.astype(np.uint16) * int(256 * (1 - d)) >> 8).astype(np.uint8)
            fade = 1.0
            if clip.fade_in:
                fade = min(fade, j / FPS / clip.fade_in)
            if clip.fade_out:
                fade = min(fade, (n - 1 - j) / FPS / clip.fade_out)
            if fade < 1:
                f = (f.astype(np.float32) * max(0.0, fade)).astype(np.uint8)
            for im, layer in images:
                if im.start <= now <= im.end:
                    a = min(1.0, (now - im.start) / 0.35, (im.end - now) / 0.3)
                    sc = 1.0 + 0.04 * (1 - ease((now - im.start) / 0.6))
                    composite(f, layer, W // 2 - layer.shape[1] // 2, im.y - layer.shape[0] // 2, a, sc)
            for t, layer, left, top in texts:
                alpha, sc, ddy = text_state(t, now)
                composite(f, layer, left, top, alpha, sc, ddy)
            enc.stdin.write(f.tobytes())
            if preview_every and preview_dir and k % preview_every == 0:
                Image.fromarray(f).resize((270, 480)).save(preview_dir / f"p{k:05d}.jpg", quality=80)
            k += 1
        t0 += clip.dur
    enc.stdin.close()
    enc.wait()
