# Reel tooling

Everything that produced the promotional reel (`marketing/video/remnant_bosses_reel.mp4`): a development-only capture
client, the shot list, and the editor. Nothing here ships in a mod jar.

## How the footage is made

Every frame is real 1.21.1 Fabric gameplay, rendered by the game itself. `src/v1211` is a dev-only Fabric mod (the
`reelDirector` source set in `1.21.1/fabric/build.gradle`, enabled with `-PreelDirector`) that turns the dev client into
an offline renderer:

- **Virtual time.** The client timer advances by exactly one frame's worth of game ticks per rendered frame, so a slow
  software renderer still produces perfectly even 30 fps motion, including in-engine slow motion (`speed` keys).
- **Lockstep.** The integrated server runs a tick only when the client asks for it, so bosses, projectiles and
  particles stay in step with the frames.
- **Complete frames.** Visible chunk sections are meshed synchronously and the visibility graph is rebuilt in the same
  frame, so nothing pops in.
- **Scripted camera.** Keyframed position, look target (a point or a tagged entity), roll and field of view on
  Hermite splines, applied before the mod's own camera shake so boss impacts still shake the shot.
- **Capture.** The framebuffer streams to ffmpeg (H.264 4:4:4, CRF 10). Every sound the game starts is logged with its
  frame, position and pitch (`*.sounds.jsonl`), so the mix can place the game's own audio exactly.
- **Staging.** Server commands, forced boss attacks (`startAttack` by name), health thresholds for phase changes,
  the player's inputs (attack, use, walk), real item use on blocks (Waygate, Eclipse Shard, Nether Star on the altar,
  resting at a grace), the mod's realm banners, and screens (the Boss Hunts journal).

## Running it

```
tools/reel/client.sh start            # Xvfb + software GL, 1080x1920 window, world "ReelWorld" (seed 20261019)
python3 tools/reel/shots.py list      # every shot
python3 tools/reel/shots.py queue 'sov*' gate1_kindle   # queue jobs; the client renders them in order
tools/reel/sheet.sh sov1_slam 0.5     # contact sheet of a finished shot
python3 tools/reel/reel.py render     # cut, grade, titles, mix -> remnant_bosses_reel.mp4
tools/reel/client.sh stop
```

Shots render into `1.21.1/fabric/runs/reel/reel/out/` (git-ignored). `previs.py` checks where subjects land in the 9:16
frame before a render and can solve a two-subject composition.

## The edit

`reel.py` holds the cut: 41.5 s, cut to the mod's own fight theme (`skeleton_fight.ogg`, 173.35 BPM). The track starts
14.21 s in, so its drop lands on the Hollow Sovereign's throne slam at 1.57 s, and every cut after it falls on a
half bar. `edit.py` decodes each shot through the grade (filmic curve, gold highlights and teal shadows, saturation,
sharpening), adds zoom, punch-in, flash and whip-pan accents, draws the titles (Cinzel and EB Garamond from
`marketing/source/fonts`, Inter), and finishes with vignette and grain. `audio.py` mixes the music, the game's logged
sounds and the synthesized accents (sub drops, whooshes, riser).

To add a voice-over, put it at `1.21.1/fabric/runs/reel/reel/out/voiceover.wav` (48 kHz, timed to the reel) and render
again. The music ducks under it. The script is in `marketing/video/README.md`.
