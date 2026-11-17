# Promotional reel: "4 Soulslike Bosses in Minecraft"

| File | Use |
| --- | --- |
| `remnant_bosses_reel.mp4` | The reel: 1080×1920, 30 fps, H.264 High + AAC 48 kHz, 41.5 s. Upload as-is to Instagram Reels, TikTok and YouTube Shorts. |
| `cover.jpg` | 1080×1920 cover frame (the Gracetree end card). |

Every shot is real gameplay from the 1.21.1 Fabric build (Remnant Bosses 3.1.0, default configuration), rendered frame
by frame by the game. Bosses are staged with commands and their own attacks; nothing is animated outside the game. The
HUD elements on screen (boss bars, GRACE DISCOVERED, REMNANT FELLED, the Boss Hunts journal) are the mod's own.
How it was made and how to re-render it: [`tools/reel/README.md`](../../tools/reel/README.md).

## Running order

| Time | Shot | On screen |
| --- | --- | --- |
| 0.0 | Hollow Sovereign's throne slam (slow motion on impact; the music drops on it) | 4 SOULSLIKE BOSSES / IN MINECRAFT |
| 2.3 | Pauldrons tear loose (phase two) | |
| 3.7 | A Waygate rises, kindles and opens | KINDLE THE WAYGATE |
| 5.7 | The black eclipse, the Gracetree over the Hollow Citadel, the Tarnished Fields | CROSS INTO THE ECLIPSED REALM |
| 9.9 | A grace shrine and its beam | REST AT GRACE |
| 11.3 | Lightning on every offering at the Ancient Altar | RAISE THE RITE |
| 12.7 | Soul burst, Last Command blade rain | HOLLOW SOVEREIGN · LAST KING OF VAEL |
| 15.4 | Chain grab, grave eruptions, chain walls | GRAVEBOUND WARDEN · KEEPER OF THE FORGOTTEN DEAD |
| 19.6 | Memory volley, focused beam, crystal aegis | FORGOTTEN WATCHER · GUARDIAN OF THE LAST MEMORY |
| 23.7 | Ossukage rises with his soldiers, then fights | OSSUKAGE · THE ONI SHOGUN |
| 26.5 | Hollow Legion, Cryptborn, Forgotten Souls, Ashbound Constructs, Corrupted Relics | 5 CREATURE FAMILIES |
| 30.0 | Regalia, Warden and Archivist sets; a Sovereign's Ruin crescent | CLAIM THEIR GEAR |
| 32.7 | The final blow: REMNANT FELLED | |
| 35.5 | The Boss Hunts journal (Gravebound Warden: Defeated) | EVERY HUNT, RECORDED |
| 36.9 | End card | REMNANT BOSSES · FREE ON CURSEFORGE & MODRINTH · WHICH BOSS FALLS FIRST? |

Music: the mod's own boss fight theme (`assets/remnant_bosses/sounds/skeleton_fight.ogg`). Sound effects: the game's
and the mod's own sounds, placed at the frames they played, plus synthesized hits and whooshes.

## Posting copy

**Caption (TikTok / Reels / Shorts)**

> 4 soulslike bosses, a realm under a black sun, and their gear for the taking 👑
> Remnant Bosses is free on CurseForge & Modrinth (Fabric, Forge, NeoForge · 1.20.1 to 26.3).
> Which boss falls first? 👇

**Hashtags:** #minecraft #minecraftmods #moddedminecraft #soulslike #bossfight #minecraftjava #gaming

**Pinned comment:** Download: search "Remnant Bosses" on CurseForge or Modrinth. Hollow Sovereign, Gravebound Warden,
Forgotten Watcher or Ossukage: who's the hardest?

## Voice-over (optional)

The reel is cut to work without narration. To add the planned ElevenLabs narration (voice "Obsidian - A Dark Fantasy
Storyteller", model Eleven v3), generate the lines below, place each at its time in one 48 kHz WAV, save it as
`1.21.1/fabric/runs/reel/reel/out/voiceover.wav` and run `python3 tools/reel/reel.py render`; the music ducks under it.

| At | Line |
| --- | --- |
| 0.2 | They outlived their kingdoms. |
| 3.7 | Kindle the Waygate… |
| 5.8 | …and cross into the Eclipsed Realm. |
| 9.9 | Rest at grace. |
| 11.3 | Raise the rite. |
| 12.7 | The Hollow Sovereign. |
| 15.5 | The Gravebound Warden. |
| 19.6 | The Forgotten Watcher. |
| 23.8 | And Ossukage. |
| 26.6 | Five creature families. |
| 30.0 | Claim their gear. |
| 35.6 | Every hunt, recorded. |
| 37.2 | Remnant Bosses. Free on CurseForge and Modrinth. |
| 40.0 | Which one falls first? |
