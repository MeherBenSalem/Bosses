#!/usr/bin/env python3
"""The Remnant Bosses reel: the edit (shots, beats, words) and the audio mix. Renders with edit.py.

  python3 tools/reel/reel.py audio            # mix only -> out/reel_audio.wav
  python3 tools/reel/reel.py render [start end] [--draft]   # -> out/remnant_bosses_reel.mp4

Music is the mod's own fight theme (assets/remnant_bosses/sounds/skeleton_fight.ogg, 173.35 BPM). The track starts at
14.21 s so its drop (15.78 s) lands on the Hollow Sovereign's throne slam at 1.57 s; every later cut sits on a half-bar.
If an ElevenLabs voice-over is present (out/voiceover.wav), it is mixed in and the music ducks under it.
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import audio  # noqa: E402
from edit import GOLD, PALE_GOLD, WHITE, Clip, Edit, Image_, Text, render, H  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "1.21.1" / "fabric" / "runs" / "reel" / "reel" / "out"
MUSIC = ROOT / "1.21.1" / "common" / "src" / "main" / "resources" / "assets" / "remnant_bosses" / "sounds" / "skeleton_fight.ogg"
LOGO = ROOT / "marketing" / "source" / "logo.png"

IMPACT = 1.57             # the throne slam, in reel seconds
MUSIC_AT_ZERO = 14.21     # track position at reel 0 (drop at 15.78)
HALF = 4 * 60 / 173.35 / 2  # half a bar: 0.6922 s
INK = (16, 12, 8)
TITLE_GLOW = (110, 80, 25)


def bars(n: float) -> float:
    return n * HALF


# ---------------------------------------------------------------- picture

def build() -> Edit:
    c: list[Clip] = []
    mark: dict[str, int] = {}

    def add(label: str | None, clip: Clip) -> None:
        if label:
            mark[label] = len(c)
        c.append(clip)

    # Hook: the Hollow Sovereign's throne slam; the music drops on the impact.
    add("hook", Clip("sov1_slam", 0.0, IMPACT + bars(1), zoom=(1.0, 1.06), center=(0.5, 0.45)))
    add(None, Clip("sov2_shatter", 1.70 - bars(1), bars(2), zoom=(1.04, 1.10), punch=0.10, whip=1))
    # The Waygate kindles and opens (sped up), then the eclipse and the Gracetree.
    add("gate", Clip("gate1_kindle", 1.0, bars(3), speed=3.15, zoom=(1.0, 1.08), whip=-1))
    add("realm", Clip("realm2_eclipse", 1.4, bars(3), speed=1.45, zoom=(1.0, 1.05), flash=0.9))
    add(None, Clip("realm1_gracetree", 3.0, bars(2), speed=1.4, zoom=(1.0, 1.04)))
    add(None, Clip("realm3_flyover", 1.6, bars(1), speed=1.5, punch=0.06))
    add("grace", Clip("grace1_rest", 0.4, bars(2), zoom=(1.02, 1.08)))
    # The rite: lightning on every skull.
    add("rite", Clip("oss1_ritual", 0.15, bars(2), speed=1.25, zoom=(1.0, 1.06), flash=0.35))
    # The Hollow Sovereign.
    add("sovereign", Clip("sov3_soul", 0.95, bars(2), zoom=(1.02, 1.08), punch=0.08, whip=1))
    add(None, Clip("sov4_last_command", 2.2, bars(2), zoom=(1.0, 1.05)))
    # The Gravebound Warden.
    add("warden", Clip("war1_grab", 0.3, bars(2), zoom=(1.0, 1.06), punch=0.08, whip=-1, exposure=0.4))
    add(None, Clip("war2_eruption", 1.4, bars(2), zoom=(1.03, 1.08), exposure=0.35))
    add(None, Clip("war4_walls", 4.73 - bars(1), bars(2), zoom=(1.0, 1.05), punch=0.05, exposure=0.3))
    # The Forgotten Watcher.
    add("watcher", Clip("wat1_volley", 0.4, bars(2), zoom=(1.0, 1.06), punch=0.08, whip=1))
    add(None, Clip("wat2_beam", 1.6, bars(2), zoom=(1.02, 1.07)))
    add(None, Clip("wat3_aegis", 0.5, bars(2), zoom=(1.0, 1.05)))
    # Ossukage, the Oni Shogun: risen from the rite, then the fight.
    add("ossukage", Clip("oss1_ritual", 6.0, bars(2), zoom=(1.02, 1.08), punch=0.08, whip=-1))
    add(None, Clip("oss2_fight", 2.4, bars(2), zoom=(1.04, 1.10)))
    # The bestiary, one creature family per half bar.
    for i, name in enumerate(["fam1_legion", "fam2_cryptborn", "fam3_souls", "fam4_ashbound", "fam5_relics"]):
        add("bestiary" if i == 0 else None, Clip(name, 0.8, bars(1), zoom=(1.03, 1.09), punch=0.06, flash=0.25 if i == 0 else 0.0))
    # The gear.
    add("gear", Clip("gear1_sets", 0.6, bars(2), zoom=(1.0, 1.05), whip=1))
    add(None, Clip("gear2_crescent", 1.1, bars(2), zoom=(1.0, 1.04)))
    # The end of a hunt: REMNANT FELLED, then the journal.
    add("felled", Clip("felled1_warden", 0.4, bars(4), zoom=(1.0, 1.06)))
    add("journal", Clip("journal1", 1.4, bars(2), zoom=(1.03, 1.08), center=(0.5, 0.36)))
    # End card over the Gracetree.
    add("end", Clip("realm1_gracetree", 5.4, 4.6, speed=0.15, zoom=(1.06, 1.12), dim=0.45, fade_out=0.6))

    starts = np.cumsum([0] + [x.dur for x in c])

    def at(label: str) -> float:
        return float(starts[mark[label]])

    t: list[Text] = []
    s, e = 0.0, at("gate") - 0.05
    t.append(Text("4 SOULSLIKE BOSSES", s, e, y=470, size=82, color=WHITE, anim="punch", max_width=960))
    t.append(Text("IN MINECRAFT", s, e, y=572, size=82, color=GOLD, anim="punch", max_width=960))
    caption = dict(y=1310, size=74, color=WHITE, accent=GOLD, anim="pop", max_width=880)
    t.append(Text("KINDLE THE *WAYGATE*", at("gate") + 0.1, at("realm") - 0.06, **caption))
    t.append(Text("CROSS INTO THE *ECLIPSED REALM*", at("realm") + 0.18, at("grace") - 0.06, **caption))
    t.append(Text("REST AT *GRACE*", at("grace") + 0.1, at("rite") - 0.06, **caption))
    t.append(Text("RAISE THE *RITE*", at("rite") + 0.1, at("sovereign") - 0.06, **caption))

    def title(name: str, sub: str, label: str, until: str) -> None:
        a, b = at(label), at(until)
        t.append(Text(name, a + 0.12, b - 0.08, y=1270, font="cinzel", size=66, color=PALE_GOLD, tracking=0.05, stroke=1,
                      glow=TITLE_GLOW, rule=True, anim="rise", gradient=((255, 240, 200), (214, 168, 84))))
        t.append(Text(sub, a + 0.3, b - 0.08, y=1422, font="cinzel", size=38, color=PALE_GOLD, tracking=0.18, anim="rise"))

    title("HOLLOW SOVEREIGN", "LAST KING OF VAEL", "sovereign", "warden")
    title("GRAVEBOUND WARDEN", "KEEPER OF THE FORGOTTEN DEAD", "warden", "watcher")
    title("FORGOTTEN WATCHER", "GUARDIAN OF THE LAST MEMORY", "watcher", "ossukage")
    title("OSSUKAGE", "THE ONI SHOGUN", "ossukage", "bestiary")
    t.append(Text("*5* CREATURE FAMILIES", at("bestiary") + 0.06, at("gear") - 0.06, **caption))
    t.append(Text("CLAIM THEIR *GEAR*", at("gear") + 0.1, at("felled") - 0.06, **caption))
    t.append(Text("EVERY HUNT, *RECORDED*", at("journal") + 0.1, at("end") - 0.06, **{**caption, "y": 1455, "band": True}))
    end = at("end")
    t.append(Text("REMNANT\nBOSSES", end + 0.35, end + 4.6, y=925, font="cinzel", size=128, color=PALE_GOLD, tracking=0.04,
                  stroke=2, glow=TITLE_GLOW, anim="rise", gradient=((255, 242, 205), (205, 158, 72)), line_gap=1.0))
    t.append(Text("Cross into the eclipse. Rest at grace.\nFell the Remnants.", end + 0.8, end + 4.6, y=1160, font="garamond",
                  size=46, color=PALE_GOLD, anim="rise", max_width=960))
    t.append(Text("FREE ON *CURSEFORGE*\n& *MODRINTH*", end + 1.2, end + 4.6, y=1322, size=60, color=WHITE, accent=GOLD,
                  anim="pop", max_width=960, line_gap=1.0))
    t.append(Text("FABRIC  ·  FORGE  ·  NEOFORGE", end + 1.5, end + 4.6, y=1446, font="bold", size=36, color=PALE_GOLD,
                  tracking=0.06, anim="fade"))
    t.append(Text("1.20.1  ·  1.21.1  ·  26.2  ·  26.3", end + 1.5, end + 4.6, y=1494, font="bold", size=32, color=PALE_GOLD,
                  tracking=0.06, anim="fade"))
    t.append(Text("WHICH BOSS FALLS FIRST? ↓", end + 2.0, end + 4.6, y=400, size=60, color=WHITE, anim="pop", max_width=980))
    images = [Image_(str(LOGO), end + 0.15, end + 4.6, y=668, width=250, glow=(255, 205, 110))]
    flashes = [(IMPACT, 0.55, None)]
    edit = Edit(clips=c, texts=t, images=images, flashes=flashes)
    edit.marks = {k: at(k) for k in mark}
    return edit


# ---------------------------------------------------------------- sound

def mix(edit: Edit) -> np.ndarray:
    total = edit.duration + 0.5
    n = int(total * audio.SR)
    music = np.zeros((n, 2), np.float32)
    game = np.zeros((n, 2), np.float32)
    fx = np.zeros((n, 2), np.float32)

    track = audio.load(str(MUSIC))
    seg = track[int(MUSIC_AT_ZERO * audio.SR):int(MUSIC_AT_ZERO * audio.SR) + n]
    music[:len(seg)] = seg
    # The breakdown before the drop sits low, the drop and everything after it full.
    env = np.ones(n, np.float32)
    k0 = int(IMPACT * audio.SR)
    env[:k0] = np.linspace(0.35, 0.55, k0)
    end = int((edit.duration - 1.6) * audio.SR)
    env[end:] = np.linspace(1, 0, n - end) ** 1.5
    music *= env[:, None] * 0.82

    # The game's own sounds, from every clip's sound log.
    t0 = 0.0
    for clip in edit.clips:
        if clip.image is None:
            audio.game_sounds(game, clip.shot, clip.src, clip.src + clip.dur * clip.speed, t0, clip.speed, gain=1.0,
                              boost={"explode": 1.6, "sonic_boom": 1.3, "chain": 1.3, "lightning": 1.2, "toll": 1.4})
        t0 += clip.dur

    # Accents: a sub drop on the slam and on each boss title, whooshes on whip cuts, a riser into the drop.
    starts = np.cumsum([0] + [c.dur for c in edit.clips])
    audio.place(fx, audio.load(str(audio.event_file("remnant_bosses:sovereign_toll"))), 0.0, 0.9)
    audio.place(fx, audio.riser(IMPACT - 0.05), 0.05, 0.35)
    audio.place(fx, audio.boom(), IMPACT, 0.95)
    for i, clip in enumerate(edit.clips):
        if clip.whip:
            audio.place(fx, audio.whoosh(0.5), starts[i] - 0.32, 0.55, pan=0.4 * clip.whip)
        if clip.flash and i > 0:
            audio.place(fx, audio.boom(1.6, 55, 30, 0.4), starts[i], 0.5)
    marks = edit.marks
    for label in ("sovereign", "warden", "watcher", "ossukage"):  # boss title cards
        audio.place(fx, audio.boom(2.0, 58, 26, 0.5), marks[label], 0.75)
    audio.place(fx, audio.load(str(audio.event_file("remnant_bosses:legion_horn"))), marks["bestiary"], 0.6)
    audio.place(fx, audio.load(str(audio.event_file("remnant_bosses:waygate_open"))), marks["realm"] - 0.15, 0.7)
    end_card = marks["end"]
    audio.place(fx, audio.boom(3.0, 50, 24, 0.7), end_card, 1.0)
    audio.place(fx, audio.load(str(audio.event_file("remnant_bosses:remnant_felled"))), end_card + 0.1, 0.6)

    mixdown = music + game * 0.9 + fx * 0.85
    vo_path = OUT / "voiceover.wav"
    if vo_path.exists():
        vo = audio.load(str(vo_path))[:n]
        vo_bus = np.zeros_like(mixdown)
        vo_bus[:len(vo)] = vo
        level = np.convolve(np.abs(vo_bus[:, 0]), np.ones(4800) / 4800, mode="same")
        duck = 1 - 0.45 * np.clip(level / (level.max() + 1e-9) * 6, 0, 1)
        mixdown = (music * duck[:, None]) + game * 0.75 + fx * 0.8 + vo_bus * 1.1
    peak = np.abs(mixdown).max()
    mixdown = mixdown / max(1.0, peak / 0.98)
    return audio.limiter(mixdown * 1.25, 0.95)


def main() -> None:
    edit = build()
    wav = OUT / "reel_audio.wav"
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args or args[0] in ("audio", "render"):
        audio.write_wav(wav, mix(edit))
        print(f"audio -> {wav} ({edit.duration:.2f} s)")
    if args and args[0] == "render":
        start = float(args[1]) if len(args) > 1 else 0.0
        stop = float(args[2]) if len(args) > 2 else None
        target = OUT / ("reel_draft.mp4" if "--draft" in sys.argv else "remnant_bosses_reel.mp4")
        draft = "--draft" in sys.argv
        render(edit, target, wav, start, stop, crf=24 if draft else 18, preset="veryfast" if draft else "slow")
        print(f"video -> {target}")


if __name__ == "__main__":
    main()
