#!/usr/bin/env python3
"""Audio for the reel: the game's own sounds (from the director's sound logs), mixed at the frames they played.

Sound files resolve to the Minecraft asset store Loom downloaded (vanilla) or to the mod's resources (remnant_bosses).
Everything is decoded to 48 kHz stereo float32 and cached in memory.
"""
from __future__ import annotations

import json
import math
import subprocess
from functools import lru_cache
from pathlib import Path

import numpy as np

SR = 48000
ROOT = Path(__file__).resolve().parents[2]
ASSETS = Path.home() / ".gradle" / "caches" / "fabric-loom" / "assets"
MOD_ASSETS = ROOT / "1.21.1" / "common" / "src" / "main" / "resources" / "assets"
OUT = ROOT / "1.21.1" / "fabric" / "runs" / "reel" / "reel" / "out"


@lru_cache(maxsize=1)
def _index() -> dict:
    return json.loads((ASSETS / "indexes" / "1.21.1-17.json").read_text())["objects"]


def sound_file(ref: str) -> Path:
    """'minecraft:sounds/mob/irongolem/throw.ogg' or 'remnant_bosses:sounds/x.ogg' -> a file on disk."""
    ns, path = ref.split(":", 1)
    if ns == "minecraft":
        h = _index()["minecraft/" + path]["hash"]
        return ASSETS / "objects" / h[:2] / h
    return MOD_ASSETS / ns / path


def event_file(event: str, variant: int = 0) -> Path:
    """A sound event id (e.g. 'minecraft:entity.generic.explode') -> one of its files, via the sounds.json files."""
    ns, name = event.split(":", 1)
    if ns == "minecraft":
        sounds = json.loads(Path(_vanilla_sounds_json()).read_text())
    else:
        sounds = json.loads((MOD_ASSETS / ns / "sounds.json").read_text())
    entries = sounds[name]["sounds"]
    entry = entries[variant % len(entries)]
    sname = entry if isinstance(entry, str) else entry["name"]
    sns, spath = sname.split(":", 1) if ":" in sname else (ns, sname)
    return sound_file(f"{sns}:sounds/{spath}.ogg")


@lru_cache(maxsize=1)
def _vanilla_sounds_json() -> str:
    h = _index()["minecraft/sounds.json"]["hash"]
    return str(ASSETS / "objects" / h[:2] / h)


@lru_cache(maxsize=512)
def load(path: str) -> np.ndarray:
    """Decode any audio file to (n, 2) float32 at 48 kHz."""
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "f32le", "-ac", "2", "-ar", str(SR), "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).copy()


def repitch(x: np.ndarray, pitch: float) -> np.ndarray:
    """Play faster/slower like OpenAL's pitch (changes length)."""
    if abs(pitch - 1) < 1e-3:
        return x
    n = int(len(x) / pitch)
    src = np.arange(n) * pitch
    i = np.minimum(src.astype(int), len(x) - 2)
    f = (src - i)[:, None]
    return x[i] * (1 - f) + x[i + 1] * f


def place(bus: np.ndarray, x: np.ndarray, at: float, gain: float = 1.0, pan: float = 0.0) -> None:
    """Adds x into bus starting at time `at` (seconds); pan -1..1 (equal power)."""
    start = int(round(at * SR))
    if start >= len(bus) or gain <= 0:
        return
    if start < 0:
        x = x[-start:]
        start = 0
    n = min(len(x), len(bus) - start)
    if n <= 0:
        return
    a = (pan + 1) * math.pi / 4
    bus[start:start + n, 0] += x[:n, 0] * gain * math.cos(a) * math.sqrt(2)
    bus[start:start + n, 1] += x[:n, 1] * gain * math.sin(a) * math.sqrt(2)


def sound_log(name: str) -> list[dict]:
    path = OUT / f"{name}.sounds.jsonl"
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text().splitlines() if line.strip()]


# Sources and events that would only clutter a trailer mix.
SKIP_SOURCES = {"music", "ambient", "record"}
SKIP_EVENTS = ("step", "ambient", "swim", "splash", "armor.equip_generic")


def game_sounds(bus: np.ndarray, name: str, src_in: float, src_out: float, at: float, speed: float = 1.0,
                gain: float = 1.0, boost: dict | None = None) -> None:
    """Mixes the sounds a clip's source played between src_in and src_out (source seconds) into bus, the clip
    starting at `at` (output seconds). Linear attenuation by distance from the camera, as the game does."""
    boost = boost or {}
    for e in sound_log(name):
        if not (src_in - 0.05 <= e["t"] < src_out):
            continue
        if e["source"] in SKIP_SOURCES or any(s in e["event"] for s in SKIP_EVENTS):
            continue
        g = e["volume"] * gain
        for k, v in boost.items():
            if k in e["event"]:
                g *= v
        if not e["relative"] and e["attenuation"] == "LINEAR":
            d = math.dist((e["x"], e["y"], e["z"]), (e["camX"], e["camY"], e["camZ"]))
            reach = max(e["volume"], 1.0) * e["range"]
            g *= max(0.0, 1.0 - d / reach) ** 0.7
        g = min(g, 1.6)
        if g < 0.02:
            continue
        try:
            clip = load(str(sound_file(e["file"])))
        except (KeyError, subprocess.CalledProcessError):
            continue
        place(bus, repitch(clip, e["pitch"]), at + (e["t"] - src_in) / speed, g * 0.55)


# ---------------------------------------------------------------- synthesized trailer accents

def _onepole(x: np.ndarray, cutoff: np.ndarray) -> np.ndarray:
    """Time-varying one-pole low-pass (cutoff in Hz per sample), mono."""
    a = np.exp(-2 * np.pi * np.asarray(cutoff, np.float64) / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = a[i] * acc + (1 - a[i]) * x[i]
        y[i] = acc
    return y


def boom(length: float = 2.2, f0: float = 62.0, f1: float = 28.0, click: float = 0.6) -> np.ndarray:
    """A cinematic sub drop: a falling sine with a noisy transient on top."""
    n = int(length * SR)
    t = np.arange(n) / SR
    freq = f1 + (f0 - f1) * np.exp(-t * 3.2)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    body = np.sin(phase) * np.exp(-t * 1.6) * 0.95
    rng = np.random.default_rng(3)
    noise = rng.normal(0, 1, n) * np.exp(-t * 28) * click
    noise = _onepole(noise, np.full(n, 2400.0))
    x = np.tanh((body + noise) * 1.4) * 0.8
    return np.stack([x, x], axis=1).astype(np.float32)


def whoosh(length: float = 0.55, rise: bool = True) -> np.ndarray:
    """A filtered-noise swish for whip cuts."""
    n = int(length * SR)
    u = np.linspace(0, 1, n)
    env = np.sin(np.pi * np.clip(u * (1.3 if rise else 1.0), 0, 1)) ** 1.6
    cutoff = 400 + 5200 * (u if rise else 1 - u) ** 1.4
    rng = np.random.default_rng(11)
    l = _onepole(rng.normal(0, 1, n), cutoff) * env
    r = _onepole(rng.normal(0, 1, n), cutoff) * env
    x = np.stack([l, r], axis=1)
    return (x / (np.abs(x).max() + 1e-9) * 0.7).astype(np.float32)


def riser(length: float = 2.0) -> np.ndarray:
    """Rising noise plus a climbing tone, ending abruptly (for a build into a hit)."""
    n = int(length * SR)
    u = np.linspace(0, 1, n)
    rng = np.random.default_rng(5)
    noise = _onepole(rng.normal(0, 1, n), 300 + 7000 * u ** 2) * u ** 2.2
    freq = 110 * 2 ** (u * 2.5)
    tone = np.sin(2 * np.pi * np.cumsum(freq) / SR) * u ** 3 * 0.25
    x = noise / (np.abs(noise).max() + 1e-9) * 0.55 + tone
    return np.stack([x, x * 0.97], axis=1).astype(np.float32)


def lowpass(x: np.ndarray, cutoff: float) -> np.ndarray:
    """Static low-pass via FFT (both channels)."""
    spec = np.fft.rfft(x, axis=0)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= (1 / np.sqrt(1 + (f / cutoff) ** 8))[:, None]
    return np.fft.irfft(spec, n=len(x), axis=0).astype(np.float32)


def limiter(x: np.ndarray, ceiling: float = 0.94) -> np.ndarray:
    """Soft-knee limiting to the ceiling."""
    return (np.tanh(x / ceiling) * ceiling).astype(np.float32)


def write_wav(path: Path, x: np.ndarray) -> None:
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    import wave
    with wave.open(str(path), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
