"""Synthesised sound effects for the Eclipsed Realm (deterministic; numpy + ffmpeg/libvorbis).

Recipes: inharmonic bell partials, a formant-filtered "choir", saw-stack brass, swept noise for stone
grinding and wind, sine-sweep booms, and a convolution reverb with a synthetic decaying-noise impulse.
Output: mono 44.1 kHz Ogg Vorbis in sounds/realm/<name>.ogg of both resource trees, plus targeted
insertion of the 8 events into sounds.json (existing entries are never rewritten or reordered).
"""
import json
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

from common import TREES, save_bytes

SR = 44100


def T(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(n, seed):
    return np.random.default_rng(seed).standard_normal(n)


def fft_filter(x, response):
    """Zero-phase filter: response(freqs) -> gain."""
    n = len(x)
    size = 1 << (n * 2 - 1).bit_length()
    X = np.fft.rfft(x, size)
    f = np.fft.rfftfreq(size, 1 / SR)
    return np.fft.irfft(X * response(f), size)[:n]


def lowpass(x, fc, order=2):
    return fft_filter(x, lambda f: 1 / np.sqrt(1 + (f / fc) ** (2 * order)))


def highpass(x, fc, order=2):
    return fft_filter(x, lambda f: 1 / np.sqrt(1 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bandpass(x, fc, q=2.0):
    bw = fc / q
    return fft_filter(x, lambda f: np.exp(-0.5 * ((f - fc) / bw) ** 2))


def sweep_lowpass(x, cutoff):
    """One-pole low-pass with a per-sample cutoff array (for swells and whooshes)."""
    a = np.exp(-2 * np.pi * np.asarray(cutoff, dtype=float) / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a[i]) * x[i] + a[i] * acc
        y[i] = acc
    return y


def reverb(x, decay=2.0, wet=0.35, damp=3000.0, seed=3, tail=None):
    tail = decay * 1.4 if tail is None else tail
    n_ir = int(SR * tail)
    t = np.arange(n_ir) / SR
    ir = noise(n_ir, seed) * np.exp(-t * 6.9 / decay)
    ir = lowpass(ir, damp, 1)
    ir[: int(SR * 0.012)] = 0                       # pre-delay
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    n = len(x) + n_ir
    size = 1 << (n - 1).bit_length()
    y = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:n]
    dry = np.concatenate([x, np.zeros(n_ir)])
    y *= np.max(np.abs(dry)) / (np.max(np.abs(y)) + 1e-9)
    return dry * (1 - wet) + y * wet


def fit(x, seconds):
    n = int(SR * seconds)
    if len(x) >= n:
        x = x[:n].copy()
    else:
        x = np.concatenate([x, np.zeros(n - len(x))])
    fade = int(SR * 0.08)
    x[-fade:] *= np.linspace(1, 0, fade) ** 2
    return x


def place(buf, x, at):
    i = int(SR * at)
    end = min(len(buf), i + len(x))
    buf[i:end] += x[: end - i]
    return buf


BELL_PARTIALS = ((0.5, 0.55, 1.6), (1.0, 1.0, 1.0), (1.19, 0.45, 0.8), (1.56, 0.4, 0.7), (2.0, 0.32, 0.55),
                 (2.51, 0.22, 0.4), (2.66, 0.18, 0.35), (3.01, 0.13, 0.3), (4.1, 0.07, 0.2))


def bell(f0, seconds, attack=0.002, seed=1, partials=BELL_PARTIALS, shimmer=0.002):
    x = T(seconds)
    out = np.zeros_like(x)
    rng = np.random.default_rng(seed)
    for ratio, amp, dscale in partials:
        decay = seconds * 0.32 * dscale
        beat = 1 + shimmer * np.sin(2 * np.pi * rng.uniform(0.4, 1.2) * x)
        out += amp * np.sin(2 * np.pi * f0 * ratio * x * beat + rng.uniform(0, 6)) * np.exp(-x / decay)
    env = np.clip(x / attack, 0, 1) if attack > 0 else 1
    strike = lowpass(noise(len(x), seed + 7) * np.exp(-x / 0.006), 4000) * 0.25
    tail = np.clip((seconds - x) / min(0.25, seconds * 0.2), 0, 1) ** 2     # no click when the buffer ends
    return (out * env + strike) * tail


def choir(freqs, seconds, attack=0.8, release=0.9, seed=11):
    """Formant-filtered detuned saw voices singing 'ah'."""
    x = T(seconds)
    rng = np.random.default_rng(seed)
    voices = np.zeros_like(x)
    for f in freqs:
        for d in (-0.006, 0.0, 0.007):
            vib = 1 + 0.004 * np.sin(2 * np.pi * rng.uniform(4.6, 5.6) * x + rng.uniform(0, 6))
            phase = np.cumsum(2 * np.pi * f * (1 + d) * vib / SR)
            saw = sum(np.sin(k * phase) / k for k in range(1, 18))
            voices += saw
    breath = noise(len(x), seed + 1) * 0.15
    src = voices + breath
    out = sum(g * bandpass(src, fc, q) for fc, q, g in ((800, 5, 1.0), (1150, 6, 0.6), (2900, 8, 0.25), (300, 2, 0.4)))
    env = np.clip(x / attack, 0, 1) ** 1.6 * np.clip((seconds - x) / release, 0, 1)
    return out * env


def brass(freqs, seconds, seed=21):
    x = T(seconds)
    out = np.zeros_like(x)
    rng = np.random.default_rng(seed)
    for f in freqs:
        for d in (-0.004, 0.004):
            phase = np.cumsum(2 * np.pi * f * (1 + d) * np.where(x < 0.08, 0.96 + 0.04 * x / 0.08, 1.0) / SR)
            out += sum(np.sin(k * phase + rng.uniform(0, 1)) / k ** 1.1 for k in range(1, 24))
    bright = 300 + 2200 * np.exp(-x / 0.6)
    out = sweep_lowpass(out, bright)
    env = np.clip(x / 0.04, 0, 1) * np.exp(-x / (seconds * 0.45))
    return out * env


def boom(seconds, f_start=80, f_end=32, decay=0.9, seed=31):
    x = T(seconds)
    f = f_end + (f_start - f_end) * np.exp(-x / 0.18)
    phase = np.cumsum(2 * np.pi * f / SR)
    body = np.sin(phase) * np.exp(-x / decay)
    thump = lowpass(noise(len(x), seed) * np.exp(-x / 0.05), 220, 2) * 2.5
    return body + thump


def grind(seconds, seed=41, density=9.0):
    """Stone grinding: low-passed noise in irregular grains with a few cracks."""
    x = T(seconds)
    rng = np.random.default_rng(seed)
    env = np.zeros_like(x)
    t0 = 0.0
    while t0 < seconds:
        i = int(t0 * SR)
        dur = rng.uniform(0.08, 0.35)
        n = min(len(x) - i, int(dur * SR))
        if n > 0:
            env[i:i + n] += rng.uniform(0.4, 1.0) * np.sin(np.linspace(0, np.pi, n)) ** 0.7
        t0 += rng.exponential(1 / density)
    body = lowpass(noise(len(x), seed + 1), 520, 2) * 2.2 + bandpass(noise(len(x), seed + 2), 1400, 3) * 0.5
    cracks = np.zeros_like(x)
    for _ in range(int(seconds * 3)):
        i = rng.integers(0, len(x) - 2000)
        cracks[i:i + 2000] += noise(2000, int(i)) * np.exp(-np.arange(2000) / 120) * rng.uniform(0.3, 0.8)
    return body * env + lowpass(cracks, 3000)


def waygate_kindle():
    dur = 6.0
    x = T(dur)
    f = 55 * (82.41 / 55) ** (x / dur) ** 1.4
    phase = np.cumsum(2 * np.pi * f / SR)
    drone = sum(a * np.sin(k * phase) for k, a in ((1, 1.0), (2, 0.55), (3, 0.35), (4, 0.22), (5, 0.12), (6, 0.08)))
    trem_rate = 2 + 7 * (x / dur) ** 2
    trem = 0.75 + 0.25 * np.sin(np.cumsum(2 * np.pi * trem_rate / SR))
    drone *= trem * (0.18 + 0.82 * (x / dur) ** 1.3)
    stone = grind(dur) * np.clip(1.15 - x / 5.0, 0.1, 1) * 0.55
    out = drone * 0.55 + stone
    # a bell for each rune lighting, rising (matches the kindle clip: runes at 0.35 .. 3.15 s)
    for k, (at, fr) in enumerate(((0.35, 330), (1.0, 392), (1.65, 440), (2.3, 494), (2.75, 587), (3.15, 659))):
        out = place(out, bell(fr, 2.4, seed=100 + k) * 0.32, at)
    # shimmering swell into the opening
    sw = np.clip((x - 3.2) / 2.8, 0, 1) ** 2
    shimmer = sum(np.sin(2 * np.pi * fr * x + p) for fr, p in ((1320, 0), (1760, 1.1), (1980, 2.3), (2640, 0.4)))
    shimmer *= (0.6 + 0.4 * np.sin(2 * np.pi * 9 * x)) * sw * 0.10
    air = sweep_lowpass(noise(len(x), 7), 300 + 4500 * sw) * sw * 0.6
    out += shimmer + air
    return fit(reverb(out, 2.2, 0.32), dur)


def waygate_open():
    dur = 2.5
    out = boom(dur, 85, 30, 0.9) * 1.2
    chord = sum(bell(fr, dur, attack=0.03, seed=200 + i) * a for i, (fr, a) in
                enumerate(((220, 0.9), (330, 0.7), (440, 0.6), (554.4, 0.45), (880, 0.3))))
    x = T(dur)
    chord *= 0.85 + 0.15 * np.sin(2 * np.pi * 3.5 * x) ** 2
    out += chord * 0.55
    out += highpass(noise(len(x), 9), 5000) * np.exp(-x / 0.5) * 0.06 * (1 + 0.5 * np.sin(2 * np.pi * 5 * x))
    return fit(reverb(out, 2.4, 0.42), dur)


def waygate_hum():
    dur = 4.0
    x = T(dur)
    base = (np.sin(2 * np.pi * 55 * x) + 0.8 * np.sin(2 * np.pi * 55.6 * x) + 0.5 * np.sin(2 * np.pi * 110 * x)
            + 0.25 * np.sin(2 * np.pi * 165.3 * x) + 0.12 * np.sin(2 * np.pi * 220 * x))
    pulse = 0.62 + 0.38 * np.sin(2 * np.pi * 1.0 * x - np.pi / 2) ** 2
    high = 0.05 * np.sin(2 * np.pi * 880 * x) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.5 * x))
    out = base * pulse + high + lowpass(noise(len(x), 13), 180) * 0.25 * pulse
    edge = np.clip(np.minimum(x / 0.45, (dur - x) / 0.55), 0, 1)
    return fit(reverb(out * edge, 1.2, 0.2), dur)


def waygate_cross():
    dur = 2.0
    x = T(dur)
    rise = np.clip(x / 1.65, 0, 1)
    src = noise(len(x), 17)
    whoosh = sweep_lowpass(src, 250 + 5200 * rise ** 2) * rise ** 2.2
    whoosh += bandpass(src, 2200, 2) * rise ** 3 * 0.4
    glide = np.sin(np.cumsum(2 * np.pi * (180 + 620 * rise ** 2) / SR)) * rise ** 2 * 0.25
    out = whoosh * 2.0 + glide
    cut = int(SR * 1.68)
    out[cut:cut + 220] *= np.linspace(1, 0, 220)
    out[cut + 220:] = 0
    out = reverb(out, 0.6, 0.18, tail=0.4)
    return fit(out, dur)


def grace_discover():
    dur = 3.0
    out = choir((293.66, 369.99, 440.0, 587.33), dur, attack=0.9, release=1.0) * 0.5
    out = place(out, bell(587.33, 2.6, attack=0.01, seed=300) * 0.4, 0.05)
    out = place(out, bell(880.0, 2.2, attack=0.01, seed=301) * 0.3, 0.55)
    out = place(out, bell(1174.7, 1.8, attack=0.01, seed=302) * 0.2, 1.05)
    return fit(reverb(out, 2.8, 0.45), dur)


def grace_rest():
    dur = 1.5
    out = bell(659.25, dur, attack=0.004, seed=400) * 0.7 + bell(987.77, dur, attack=0.004, seed=401) * 0.45
    out = place(out, bell(1318.5, 1.2, attack=0.004, seed=402) * 0.2, 0.12)
    out = lowpass(out, 5000, 1)
    return fit(reverb(out, 1.4, 0.35), dur)


def remnant_felled():
    dur = 3.5
    out = brass((73.42, 110.0, 146.83, 174.61), dur) * 0.55
    out += bell(73.42, dur, seed=500) * 0.6 + bell(146.83, dur, seed=501) * 0.35
    out += boom(dur, 70, 40, 1.1, seed=502) * 0.9
    return fit(reverb(out, 3.2, 0.48), dur)


def fog_gate_pass():
    dur = 1.0
    x = T(dur)
    env = np.sin(np.pi * np.clip(x / 0.85, 0, 1)) ** 1.5
    air = sweep_lowpass(noise(len(x), 23), 300 + 700 * env) * env * 2.2
    hiss = bandpass(noise(len(x), 24), 2800, 1.5) * env ** 2 * 0.12
    thump = np.sin(2 * np.pi * 70 * x) * np.exp(-x / 0.12) * 0.35
    return fit(reverb(lowpass(air + hiss, 1300, 2) + thump, 0.8, 0.25), dur)


EFFECTS = {
    'waygate_kindle': (waygate_kindle, 0.9),
    'waygate_open': (waygate_open, 0.95),
    'waygate_hum': (waygate_hum, 0.7),
    'waygate_cross': (waygate_cross, 0.85),
    'grace_discover': (grace_discover, 0.85),
    'grace_rest': (grace_rest, 0.75),
    'remnant_felled': (remnant_felled, 0.95),
    'fog_gate_pass': (fog_gate_pass, 0.8),
}


def normalise(x, peak):
    x = x - np.mean(x)
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def encode_ogg(samples):
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / 'x.wav'
        ogg = Path(tmp) / 'x.ogg'
        with wave.open(str(wav), 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        # -fflags +bitexact / -map_metadata -1 keep the output byte-stable between runs
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', str(wav), '-map_metadata', '-1',
                        '-fflags', '+bitexact', '-flags:a', '+bitexact', '-c:a', 'libvorbis', '-q:a', '5',
                        '-ac', '1', '-ar', str(SR), str(ogg)], check=True)
        return ogg.read_bytes()


def entry(name):
    return {'sounds': [{'name': f'remnant_bosses:realm/{name}'}], 'subtitle': f'subtitles.remnant_bosses.{name}'}


def ensure_sound_entries(path):
    """Append any missing realm events just before the closing brace of sounds.json.
    Re-reads the file immediately before writing and leaves every existing byte in place."""
    text = path.read_text(encoding='utf-8')
    data = json.loads(text)
    missing = [k for k in EFFECTS if k not in data]
    if not missing:
        return []
    nl = '\r\n' if '\r\n' in text else '\n'
    end = text.rstrip().rfind('}')
    head = text[:end].rstrip()
    add = ''
    for k in missing:
        body = json.dumps(entry(k), indent=2).replace('\n', nl + '  ')
        add += ',' + nl + '  ' + json.dumps(k) + ': ' + body
    new = head + add + nl + '}' + nl
    merged = json.loads(new)
    assert all(merged[k] == data[k] for k in data), 'existing entries changed'
    path.write_text(new, encoding='utf-8', newline='')
    return missing


def build(samples_out=None):
    lengths = {}
    for name, (fn, peak) in EFFECTS.items():
        x = normalise(fn(), peak)
        lengths[name] = len(x) / SR
        save_bytes(encode_ogg(x), f'sounds/realm/{name}.ogg')
        if samples_out is not None:
            samples_out[name] = x
    added = {}
    for tree in TREES:
        added[str(tree)] = ensure_sound_entries(tree / 'sounds.json')
    return lengths, added
