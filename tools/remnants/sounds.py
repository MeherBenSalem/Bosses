"""Synthesised sound effects for Remnants (deterministic; numpy + ffmpeg/libvorbis).

Each effect is built from physical-ish recipes rather than samples: inharmonic bell partials with
exponential decay, metallic clang modes, band-limited noise sweeps, brass-like horn tones.
Run: python tools/remnants/sounds.py  -> writes .ogg files and updates sounds.json in both resource
trees, then prints the subtitle lang entries to merge.
"""
import json
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
SR = 44100
rng = np.random.default_rng(714)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def env(n, attack=0.005, release=0.05):
    e = np.ones(n)
    a, r = int(SR * attack), int(SR * release)
    e[:a] = np.linspace(0, 1, a) if a else 1
    if r:
        e[-r:] *= np.linspace(1, 0, r)
    return e


def lowpass(x, cutoff):
    # one-pole low-pass, cutoff may be an array (sweeps)
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a[i]) * x[i] + a[i] * acc
        y[i] = acc
    return y


def bell(fundamental, seconds, partials=((0.5, 0.6, 1.6), (1.0, 1.0, 1.0), (1.19, 0.5, 0.8), (1.56, 0.45, 0.7),
                                          (2.0, 0.35, 0.55), (2.51, 0.25, 0.4), (2.66, 0.2, 0.35), (3.01, 0.15, 0.3))):
    x = t(seconds)
    out = np.zeros_like(x)
    for ratio, amp, decay_scale in partials:
        decay = seconds * 0.35 * decay_scale
        beat = 1 + 0.002 * np.sin(2 * np.pi * 0.7 * x)  # slight shimmer
        out += amp * np.sin(2 * np.pi * fundamental * ratio * x * beat) * np.exp(-x / decay)
    strike = rng.standard_normal(len(x)) * np.exp(-x / 0.01) * 0.3
    return out + lowpass(strike, 3000)


def clang(seconds=0.9):
    x = t(seconds)
    modes = [(420, 1.0, 0.25), (1093, 0.7, 0.18), (1720, 0.5, 0.12), (2610, 0.35, 0.09), (3480, 0.25, 0.06)]
    out = sum(a * np.sin(2 * np.pi * f * x + rng.uniform(0, 6)) * np.exp(-x / d) for f, a, d in modes)
    hit = rng.standard_normal(len(x)) * np.exp(-x / 0.004)
    return out + 0.6 * hit


def whoosh(seconds=0.7, shimmer=True):
    x = t(seconds)
    noise = rng.standard_normal(len(x))
    sweep = 400 + 5200 * np.sin(np.pi * x / seconds) ** 2
    body = lowpass(noise, sweep) * np.sin(np.pi * x / seconds) ** 1.5
    if shimmer:
        ring = 0.25 * np.sin(2 * np.pi * (1200 + 900 * x / seconds) * x) * np.sin(np.pi * x / seconds) ** 3
        body += ring
    return body


def hum(seconds=1.2):
    x = t(seconds)
    swell = np.clip(x / seconds, 0, 1) ** 1.5
    tone = sum(a * np.sin(2 * np.pi * f * x * (1 + 0.04 * swell)) for f, a in ((55, 1.0), (110, 0.5), (165, 0.25), (330, 0.12)))
    trem = 0.6 + 0.4 * np.sin(2 * np.pi * (6 + 18 * swell) * x)
    return tone * trem * swell


def horn(seconds=1.8, base=146.8):
    x = t(seconds)
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * x) * np.clip(x - 0.3, 0, None)
    glide = np.where(x < 0.12, 0.94 + 0.06 * x / 0.12, 1.0)
    phase = 2 * np.cumsum(np.pi * base * vib * glide / SR)
    saw = sum(np.sin(k * phase) / k for k in range(1, 14))
    shaped = lowpass(saw, 900 + 1400 * np.clip(x / 0.25, 0, 1)) * env(len(x), 0.08, 0.5)
    return shaped + 0.4 * lowpass(rng.standard_normal(len(x)), 1200) * np.exp(-x / 0.08)


def soul_rise(seconds=1.4):
    x = t(seconds)
    f = 180 * (1 + 1.5 * x / seconds)
    phase = 2 * np.cumsum(np.pi * f / SR)
    tone = np.sin(phase) + 0.4 * np.sin(2.01 * phase) + 0.2 * np.sin(3.02 * phase)
    air = lowpass(rng.standard_normal(len(x)), 2000) * 0.4
    return (tone * 0.6 + air) * np.sin(np.pi * x / seconds) ** 2


def chord_bell():
    # the Last Command toll: a low bell plus a fifth an octave above, long tail
    return bell(98, 4.5) + 0.55 * bell(146.8, 4.5)


EFFECTS = {
    'sovereign_toll': (chord_bell, 'Last Command toll', 1.0),
    'sovereign_guard': (lambda: clang(0.9), 'Sovereign guard clang', 0.9),
    'spectral_slash': (lambda: whoosh(0.7), 'Spectral slash', 0.85),
    'pauldron_hum': (lambda: hum(1.1), 'Pauldron hums', 0.8),
    'legion_horn': (lambda: horn(1.8), 'Legion war horn', 0.9),
    'soul_release': (lambda: soul_rise(1.4), 'Soul released', 0.8),
}


def normalise(x, peak):
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    fade = min(len(x), int(SR * 0.02))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x


def write_ogg(samples, path):
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / 'x.wav'
        with wave.open(str(wav), 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', str(wav), '-c:a', 'libvorbis', '-q:a', '5', str(path)], check=True)


def main():
    entries = {}
    for name, (fn, subtitle, peak) in EFFECTS.items():
        samples = normalise(fn(), peak)
        for version in ('1.20.1', '1.21.1'):
            out = ROOT / version / 'common/src/main/resources/assets/remnant_bosses/sounds' / f'{name}.ogg'
            out.parent.mkdir(parents=True, exist_ok=True)
            write_ogg(samples, out)
        entries[name] = {'sounds': [{'name': f'remnant_bosses:{name}'}], 'subtitle': f'subtitles.remnant_bosses.{name}'}
        print(name, f'{len(samples) / SR:.2f}s', subtitle)
    for version in ('1.20.1', '1.21.1'):
        path = ROOT / version / 'common/src/main/resources/assets/remnant_bosses/sounds.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update(entries)
        path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({f'subtitles.remnant_bosses.{k}': v[1] for k, v in EFFECTS.items()}, indent=2))


if __name__ == '__main__':
    main()
