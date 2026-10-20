# Slice 4 — The Forgotten Watcher, the Forgotten Souls and the Forgotten Observatory

**Civilization:** Seyr, the Lantern Archive: scholars who stored memories inside cut crystal.
**Palette:** pale sandstone, verdigris copper, violet crystal, and cold remnant light for the souls.
**Progression tier:** III (netherite). The Watcher is a ranged, pattern-reading fight: positioning beats armor.

This file is the implementation contract. Conventions follow [02-hollow-sovereign.md](02-hollow-sovereign.md): damage in half-hearts, time in ticks, and every value marked *(cfg)* is a Jauml default.

## 1. Registry IDs

| Kind | ID | Replaces |
|---|---|---|
| Boss | `remnants:forgotten_watcher` | `remnants:umbrakar` |
| Boss helper | `remnants:watcher_illusion` | — (memory illusion decoy; never saved) |
| Projectile | `remnants:memory_bolt` | `remnants:umbrakar_orb` (dropped, not aliased) |
| Mob | `remnants:lost_pilgrim` | — |
| Mob | `remnants:memory_wraith` | `remnants:wraith` |
| Mob (elite) | `remnants:soul_collector` | `remnants:dirge_lantern` |
| Summon | `remnants:soul_wisp` | — (the Soul Collector's wisps; never saved) |
| Items | `remnant_bosses:memory_shard`, `knowledge_fragment`, `memory_lens`, `memory_staff`, `watchers_eye`, `archivist_hood`, `archivist_robe`, `archivist_leggings`, `archivist_boots` | `umbrakar_spawn_egg` → `forgotten_watcher_spawn_egg`, `wraith_spawn_egg` → `memory_wraith_spawn_egg`, `dirge_lantern_spawn_egg` → `soul_collector_spawn_egg` |
| Biome | `remnant_bosses:lantern_heights` | — (new Eclipsed Realm biome, section 6) |
| Structure | `remnant_bosses:forgotten_observatory` | — |

**Spawn eggs:** `forgotten_watcher_spawn_egg`, `lost_pilgrim_spawn_egg`, `memory_wraith_spawn_egg`, `soul_collector_spawn_egg`. Forgotten Souls: base `#22344A`, spots `#A3C4D6`. The Watcher: base `#4A4336` (sandstone), spots `#9E86E0` (violet crystal).

**Retired by this slice:** `remnants:umbrakar`, `remnants:umbrakar_orb`, `remnants:wraith`, `remnants:dirge_lantern`, their eggs, models (`umbrakar.geo.json`, `umbrakar_orb.geo.json`, `dirge_lantern.*`, the Java `Modelwraith` and `wraithAnimation`), renderers and configs (`umbrakar`, `umbrakar_summon`, `wraith_spawns`, `wraith_stats` and the `dirge_lantern` monster file stay on disk unused). `RegistryAliases` already maps every retired ID; `umbrakar_orb` is a dropped helper. The wraith's overworld spawns go away with it (realm.md section 8).

**Sizes (width × height, blocks):**

| Entity | Size | Notes |
|---|---|---|
| Forgotten Watcher | 1.6 × 3.2, eye 2.1 | Floats; its feet hover 0.6 above the ground *(cfg `hover_height`)*. |
| Watcher illusion | 1.6 × 3.2 | |
| Memory bolt | 0.5 × 0.5 | |
| Lost Pilgrim | 0.7 × 2.1 | |
| Memory Wraith | 0.8 × 2.2 | Floats 2 above the ground. |
| Soul Collector | 1.1 × 2.4 | Floats 0.5 above the ground. |
| Soul wisp | 0.4 × 0.4 | |

## 2. Assets

Models are built by `tools/remnants/models/forgotten_watcher.py` and `tools/remnants/models/forgotten_souls.py` (Blockbench MCP); sources and previews in `docs/models/remnants/<id>/`, installed with `tools/remnants/install_assets.py`. The two small helpers (`memory_bolt`, `soul_wisp`) and the illusion texture are generated without Blockbench by `tools/remnants/models/seyr_helpers.py` (GeckoLib JSON and painted atlases, previews rendered with `tools/remnants/realm/render3d.py`).

| Entity | Clips (ticks; L = loop, H = hold) and key ticks |
|---|---|
| forgotten_watcher | `idle` 80L (also while drifting), `volley` 36 (bolts released 20; phase 2 second ring 28), `beam` 88 (charge 0–30, aim locks 26, sweep 30–70, recovery 70–88), `teleport` 32 (folds 0–16, moves at 16), `shield` 24 (plates closed 20), `shielded` 80L, `summon` 40 (illusions appear 20), `awaken_memory` 60 (phase 2; knockback pulse 30), `hurt` 8 (aegis shattered), `death` 60H (last memory released 30) |
| lost_pilgrim | `idle` 80L, `walk` 40L, `disturbed` 30 (bell 12), `strike` 24 (hit 13), `hurt` 8, `death` 40H |
| memory_wraith | `idle` 60L, `drift` 24L (moving), `cast` 24 (bolt released 14), `blink` 12 (moves at 6), `hurt` 8, `death` 32H |
| soul_collector | `idle` 60L, `mark` 32 (mark placed 16), `harvest` 32 (hit 18), `summon` 40 (wisps 20), `hurt` 8, `death` 40H |
| memory_bolt | `spin` 10L |
| soul_wisp | `float` 20L |

**Bones the Java side drives:**
- Watcher: the `aegis` bone (six plates, one child bone each: `aegis_0` … `aegis_5`) is hidden outside the aegis state. While shielded, Java sets the `aegis` Y rotation so that `aegis_0`, which stays hidden, is the open segment and points at the synced world bearing (section 3, Aegis). The `shield` clip's scale-up still plays.
- Illusion: renders `forgotten_watcher.geo.json` with `forgotten_watcher_illusion.png` (same UVs, paler violet), translucent at alpha 200, no shadow; its aegis is always hidden.

**Item icons:** `memory_shard`, `memory_staff`, `watchers_eye` (already in `docs/models/remnants/items/`), plus `knowledge_fragment`, `memory_lens`, `archivist_hood`, `archivist_robe`, `archivist_leggings`, `archivist_boots` and the armor layers `textures/models/armor/archivist_layer_1.png` / `_layer_2.png` (scholar's hood and robe, verdigris trim, violet crystal clasp). The Memory Staff uses a handheld model.

## 3. The Forgotten Watcher

*Guardian of the Last Memory.* A floating sandstone obelisk inside three rune rings around a violet crystal eye.

| Stat | Value |
|---|---|
| Health | 680 *(cfg `max_health`)* |
| Armor / toughness | 8 / 2 |
| Knockback resistance | 1.0 |
| Drift speed | 0.08 blocks/tick *(cfg `drift_speed`)*; no gravity, no pathfinding |
| Follow range | 48 |
| XP | 400 *(cfg)* |
| Other | Persistent; never despawns; cannot change dimension; immune to fall damage and suffocation pushback |

**Home and lenses.** The Watcher remembers a home: the altar it was summoned over (altar + 1), or where it first spawned. The four **lenses** sit at home ± 9 blocks east, west, south and north *(cfg `lens_radius`)*. The Watcher drifts to keep 8–14 blocks from its target and never strays more than 22 blocks from home *(cfg `leash_radius`)*; past that it drifts back.

**Phases:**
1. **The Archive Awakens:** 100–50%.
2. **The Last Memory:** 50–0% *(cfg `phase_two_percent`)*, entered through `awaken_memory` (60 ticks, invulnerable; at tick 30 a knockback-only pulse pushes everything within 6 blocks away). The aegis rises as soon as the transition ends. Attack cooldowns ×0.75.

Attack selection runs when no attack is active and the cooldown *(cfg `attack_cooldown_ticks` 34)* is 0. It needs a living target in line of sight, and never repeats the previous attack when another option exists. Lens Shift takes priority when the target has stayed within 4 blocks for 30 ticks.

| Attack | Phases | Range | Effect |
|---|---|---|---|
| Ring Volley | all | ≤ 26 | Windup 0–20: the eye flares and a violet ring of light grows at the release height. At tick 20, 12 memory bolts (phase 2: 16) leave in an even horizontal ring, the first one aimed at the target, each tilted so it crosses the target's height at the target's distance. Bolts fly straight at 0.45 blocks/tick for 28 blocks: 7 damage each *(cfg `bolt_damage`)*, broken by blocks. Phase 2 releases a second ring at tick 28, turned half a step. Step between two bolts, or put a wall between you. |
| Focused Beam | all | 6–22 | Charge 0–30: a thin violet aim line runs from the eye to the target; the aim locks at tick 26. Sweep 30–70: the beam fires along the locked aim and sweeps from 0° to +30° (30–50), then to −30° (50–70). It is a 24-block ray from the eye that stops at the first block; anything within 0.7 of it takes 5 damage, at most once per 10 ticks *(cfg `beam_damage`, `beam_hit_interval`)*. Leave the 60° arc, duck behind cover, or close in under it. |
| Lens Shift | all | any | `teleport`: folds into the eye, and at tick 16 reappears over another lens (the free lens farthest from its target that still has line of sight to it; else any free lens; else a free spot 6–10 blocks from home). Used when the target hugs it (within 4 blocks for 30 ticks), and after every third attack. Deals no damage. |
| Memory Illusions | 2 | any | `summon`: at tick 20, two illusions appear over free lenses (at most 2 alive; cooldown 600 *(cfg)*). |
| Aegis | 2 | — | See below; raised right after `awaken_memory`, then every 600 ticks after the previous aegis ended *(cfg `aegis_cooldown_ticks`)*. |

**Aegis (rotating shield).** `shield` (24 ticks), then six crystal plates circle the Watcher for 200 ticks *(cfg `aegis_ticks`)*, one turn per 160 ticks. One plate is missing: the open segment. A hit counts only when the attacker (or the projectile's origin) is within ±35° of the open segment's bearing, measured from the Watcher's centre; every other hit is negated with an amethyst chime, and projectiles are deflected. The Watcher keeps attacking while shielded. Three hits through the open segment shatter the aegis: the Watcher plays `hurt`, stops attacking for 60 ticks and takes ×1.25 damage during that window (*exposed*). The open segment's bearing is `start_yaw + 360 × (game_time − start_tick) / 160`, synced as two values so the client and the server agree.

**Watcher illusion** (`remnants:watcher_illusion`): 1 HP; any damage shatters it (glass break, crystal shards; no loot, no XP). It floats over its lens for 400 ticks, then fades. Every 70–90 ticks it casts a Ring Volley of 8 bolts that deal 3 damage, with the same telegraph as the Watcher's. Tells: no boss bar, a translucent shimmer, no shadow, a flickering eye. Illusions are discarded when the Watcher dies or is removed.

**Hits** skip creative and spectator players, the Watcher, its illusions and the Forgotten Souls (`#remnants:forgotten_souls`). Bolts use `mobProjectile` damage and the beam uses `mobAttack`, so armor and difficulty scaling apply.

**Death:** the rings fall and the eye goes dark; at tick 30 the last memory is released (a violet and cyan burst, `BEACON_DEACTIVATE`). The entity is removed after 60 ticks.

**Loot** (`remnants:entities/forgotten_watcher`):

| Item | Count |
|---|---|
| `memory_staff` | 1 |
| `watchers_eye` | 1 |
| `knowledge_fragment` | 2–3 |
| `memory_shard` | 6–10 |
| `minecraft:amethyst_shard` | 4–9 |
| `minecraft:echo_shard` | 1 at 25% |

**Ritual** (`remnant/bosses/forgotten_watcher_summon`):
- **Activation item:** `remnant_bosses:memory_lens`, crafted shaped `MAM / ASA / MAM` (M = memory shard, A = amethyst shard, S = spyglass).
- **Pedestals:** amethyst block (east), calcite (west), chiseled sandstone (south), lectern (north).
- Works only in the Eclipsed Realm by default (`rituals_realm_only`, realm.md section 8). The Watcher appears 1.5 blocks above the altar. The Observatory's dome has the pedestals pre-filled.
- Player edits of the retired `umbrakar_summon` seed the new file once (`ConfigMigration.CARRIES`), as `kotsukage_summon` seeded the Warden's.

**Presentation:**
- **Boss bar:** theme 2 (Umbrakar's) now belongs to `entity.remnants.forgotten_watcher`: a single crystal eye ringed by rune arcs. Glow `0xFF9E86E0`, shadow `0xFF2A1F4A`.
- **Music:** the existing boss-music packet and theme, gated by `boss_music_enabled` *(cfg)*.
- **Sounds:** ambient `AMETHYST_BLOCK_CHIME` (rare), hurt `AMETHYST_BLOCK_HIT` at pitch 0.6, death `BEACON_DEACTIVATE` at pitch 0.6; windups use `BEACON_ACTIVATE`, `AMETHYST_BLOCK_RESONATE`, `ILLUSIONER_PREPARE_MIRROR` and `ENDERMAN_TELEPORT`.

**Codex:**
- **Name:** "The Forgotten Watcher".
- **Clue:** "An observatory crowns the Lantern Heights of the Eclipsed Realm. Its lenses still turn toward a sky that has not moved since the Last Eclipse."
- **Lore:** "Seyr built the Watcher to remember everything before the end. It saw the Oracle call every soul to itself, and it remembers whose voice the Oracle was answering. It was built to remember, not to tell."

## 4. Forgotten Souls

All Forgotten Souls:
- are in the `#remnants:forgotten_souls` tag and the undead tag;
- never hit each other;
- spawn from the Lantern Heights' spawn list (Lost Pilgrims also wander the Gracewood) and inside the Observatory.

| Mob | HP / armor / dmg / speed | XP | Behaviour | Loot |
|---|---|---|---|---|
| Lost Pilgrim | 20 / 2 / 4 / 0.2 | 3 | Neutral wanderer with a lantern staff; it walks and stops to pray. When a creature hurts it, it plays `disturbed`: the bell rings at tick 12 and every pilgrim within 12 blocks turns on the attacker too. Then `strike` (hit 13, reach 2.6): 4 damage and Weakness I for 60 ticks. It calms down 400 ticks after the last exchange of blows *(cfg `anger_ticks`)*. | `memory_shard` 0–1 (looting +1), `minecraft:candle` 0–1 |
| Memory Wraith (uncommon) | 26 / 0 / 6 / 0.3 (flying) | 8 | Evasive flyer, 2 blocks above the ground. Holds 6–12 blocks from its target. `cast`: at tick 14 a soul bolt (`memory_bolt`, 6 damage, 0.7 blocks/tick, range 24); cooldown 40. `blink`: when hurt (50%) or when a player comes within 3 blocks, at tick 6 it slips 4–7 blocks sideways to a free spot; cooldown 60. | `memory_shard` 1–2 (looting +1), `minecraft:paper` 0–2 |
| Soul Collector (elite) | 70 / 6 / 8 / 0.18 (floating) | 22 | Knockback resistance 0.5. `harvest` (reach 3, 120° arc, hit 18): 8 damage, and it heals 4 per creature struck. `mark` (range 4–14): at tick 16 a soul mark (radius 2) appears under the target and bursts 30 ticks later: 8 damage and Slowness I for 40 ticks. `summon`: at tick 20 two soul wisps rise, plus one per stored soul (at most 4 alive; cooldown 300). **Soul harvest:** when another non-player creature dies within 12 blocks, its soul streams into the cage: the Collector heals 6 and stores it (at most 3). **Spectre aura:** every 60 ticks, Forgotten Souls within 10 blocks gain Resistance I and Speed I for 80 ticks. | `memory_shard` 2–4, `knowledge_fragment` 15% (+5% per looting level), `minecraft:soul_lantern` 0–1 |
| Soul wisp (summon) | 4 / 0 / 4 / 0.32 (flying) | 0 | Homes in on the Collector's target; on contact it deals 4 damage and pops. Lasts 200 ticks. | none |

## 5. Items

- **Memory Shard:** material, stack 64. "A splinter of Seyr crystal. Someone's memory is still inside."
- **Ancient Knowledge Fragment** (`knowledge_fragment`): stack 16, rare. Repair material for the Memory Staff and Archivist armor.
- **Lens of Memory** (`memory_lens`): summon item, stack 16; its tooltip names the configured offerings.
- **Memory Staff:** a focus, durability 1200, repaired with a knowledge fragment. Hold use to charge (slowed like a bow, violet motes at the tip).
  - Released before 20 ticks: a homing memory bolt (6 damage, 0.9 blocks/tick, range 32; it turns toward the nearest hostile creature within 20° of its path, at most 8° per tick). Item cooldown 10; 1 durability.
  - Released after 20 ticks or more: a ring burst of 12 bolts around the player at chest height (5 damage each, range 16). Item cooldown 80; 4 durability.
- **Watcher's Eye:** relic, off-hand.
  - **Passive:** every 200 ticks, hostile creatures within 24 blocks glow for 60 ticks.
  - **Active:** blink 8 blocks toward where you look, stopping short of walls; you land safely. Cooldown 200 ticks.
  - **Cost:** 2 hunger points (food level − 2); it fails while the food level is below 2 (except in creative).
  - In the main hand, use shows the off-hand hint, as the other relics do.
- **Archivist armor** (`archivist_hood`, `archivist_robe`, `archivist_leggings`, `archivist_boots`): a scholar's robe over verdigris plates. Defence 3 / 7 / 6 / 3, toughness 2, enchantability 20, durability multiplier 33, repaired with knowledge fragments, equip sound `ARMOR_EQUIP_CHAIN`. Shaped recipes as Regalia: the matching diamond piece in the centre, surrounded by memory shards, with one knowledge fragment on top.
  - **2-piece, Quiet Mind:** Blindness and Darkness are cleared every second.
  - **4-piece, Archive Ward:** once every 600 ticks, the next projectile that would hit you is negated (amethyst chime and violet sparks).

## 6. The Lantern Heights and the Forgotten Observatory

**Lantern Heights** (`remnant_bosses:lantern_heights`, "The Lantern Heights"): a new Eclipsed Realm land biome that takes the inland mountains: continentalness ≥ 0.03 and erosion < −0.375 in the surface depth bands, at every temperature and humidity (realm.md section 3). Pale calcite cliffs, broken sandstone stargazing pillars and violet amethyst outcrops; Seyr's ruins sit on the ridges. Surface: calcite on steep faces, grass with calcite and coarse patches elsewhere. Spawns (monster): Lost Pilgrim 30 (1–3), Memory Wraith 12 (1–2), Soul Collector 2 (1); creatures: rabbits (vanilla goats only spawn on stone, snow and gravel). The Gracewood also gains Lost Pilgrims (weight 10, 1–2): pilgrims walk toward the Gracetree. Music: jagged peaks. It joins `#remnant_bosses:is_eclipsed_realm` and `#remnant_bosses:in_eclipsed_realm`, so the Catacombs and grace shrines generate there too.

**Forgotten Observatory** (`remnant_bosses:forgotten_observatory`):
- **Placement:** surface jigsaw structure, biome tag `#remnant_bosses:has_structure/forgotten_observatory` = Lantern Heights only. Spacing 28, separation 10, salt 902_114_517 (Lantern Heights cover about a quarter of inland land, so this keeps it as common as the Citadel). Terrain adaptation `beard_thin`; generates in new chunks only. It joins `#remnant_bosses:remnant_arenas` (grace guidance leads to it).
- **Pieces** (about 47 × 47, built on a raised plinth so it sits level on a ridge):

  | Piece | Contents |
  |---|---|
  | Approach | Stair up the slope through a broken sandstone gate under a verdigris arch, two lantern posts |
  | Reading halls (east and west wings) | Bookshelves and chiseled bookshelves, lecterns, desks, candles, cobwebs; chests `remnant_bosses:chests/forgotten_observatory_archive` |
  | Lens gallery | The antechamber before the dome: tall windows, a fallen lens; the dome door is sealed with fog gates |
  | Memory vault | A crystal-crusted room off a reading hall, guarded by one Soul Collector; chest `remnant_bosses:chests/forgotten_observatory_vault` with `memory_lens` 25%, `knowledge_fragment` 8%, `memory_shard` 2–5 |
  | Dome (arena) | Round floor of radius 12 (25 × 25 clear), dome up to 14 blocks with an open oculus to the eclipse. The altar ring sits in the centre with the pedestals pre-filled (rotation-proof offerings piece, as in the Catacombs). Four raised lens platforms at radius 9, due east, west, south and north of the altar |

- **Palette:** sandstone family (cut, smooth, chiseled), calcite, oxidized and weathered copper (verdigris), amethyst blocks and clusters, tinted glass, bookshelves, lecterns, candles, soul lanterns, chains, cobwebs.
- **Spawns:** `spawn_overrides` for `monster` in the `piece` bounding box: lost_pilgrim 10 (1–2), memory_wraith 6 (1), soul_collector 1 (1).
- **Fixed spawns:** one Soul Collector in the memory vault.
- **Generator:** `tools/remnants/structures/forgotten_observatory.py`, following the Catacombs generator; validated and previewed with the shared structure tools.
