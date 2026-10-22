# The Ashbound Constructs and the Corrupted Relics

Two creature families that roam the Eclipsed Realm ahead of their boss slices: the **Ashbound Constructs**
(Kharum, slice 6, the Ashen Colossus) and the **Corrupted Relics** (the Sanctum, slice 8, the Shattered Oracle).
Their bosses, arenas, gear and rituals are not part of this file; when those slices land they extend the families
described here rather than replace them.

This file is the implementation contract. Conventions follow [02-hollow-sovereign.md](../slices/02-hollow-sovereign.md):
damage in half-hearts, time in ticks, and every value marked *(cfg)* is a Jauml default in
`config/remnant/monsters/<id>`.

## 1. Registry IDs

| Kind | ID | Replaces |
|---|---|---|
| Mob (common) | `remnants:ashbound_crawler` | `remnants:armored_grub` |
| Mob (common) | `remnants:ember_servitor` | — |
| Mob (elite) | `remnants:rusted_guardian` | — |
| Projectile | `remnants:ember_bolt` | — (the Servitor's shot; never saved) |
| Mob (common) | `remnants:shardling` | — |
| Mob (uncommon) | `remnants:ancient_vessel` | — |
| Mob (elite) | `remnants:relic_stalker` | `remnants:sickleghast` |
| Projectile | `remnants:crystal_bolt` | — (the Shardling's shot; never saved) |
| Items | `remnant_bosses:ashen_cog`, `remnant_bosses:relic_shard` | — |
| Spawn eggs | `<mob>_spawn_egg` for the six mobs | `armored_grub_spawn_egg` → `ashbound_crawler_spawn_egg`, `sickleghast_spawn_egg` → `relic_stalker_spawn_egg` |
| Entity tags | `#remnants:ashbound`, `#remnants:corrupted_relics` | — |

**Spawn eggs:** Ashbound base `#2A1C12` (weathered bronze), spots `#E0661C` (ember). Relics base `#5F5A63`
(chalk marble), spots `#D77BC0` (Oracle rose).

**Retired early:** `remnants:armored_grub` and `remnants:sickleghast` with their eggs, Java classes, renderers and
models, assets, the sickleghast loot table and config default, and the grub's two sounds (`armored_grub_ambient`,
`armored_grub_death`, their `.ogg` files and subtitles). `RegistryAliases` already mapped both IDs and both eggs
([migration.md](../migration.md)); `AliasRegression` now proves both resolve against the live registrations.
The old `config/remnant/monsters/sickleghast` file stays on disk unused.

**Sizes (width × height, blocks):**

| Entity | Size | Notes |
|---|---|---|
| Ashbound Crawler | 0.9 × 0.8 | fire-immune |
| Ember Servitor | 0.8 × 1.3 | fire-immune; hovers 2.5 above the ground (body 2.8–3.8 up) |
| Rusted Guardian | 1.4 × 2.8 | fire-immune; step height 1 |
| Ember bolt | 0.5 × 0.5 | fire-immune |
| Shardling | 0.8 × 1.0 | hovers 1.6 above the ground |
| Ancient Vessel | 0.7 × 1.3 | |
| Relic Stalker | 1.2 × 2.6 | step height 1 |
| Crystal bolt | 0.5 × 0.5 | |

## 2. Assets

Built by `tools/remnants/models/ashbound.py` and `tools/remnants/models/relics.py`; sources and previews in
`docs/models/remnants/<id>/`, installed with `tools/remnants/install_assets.py`. The bolts draw the Memory Bolt model
(`memory_bolt.geo.json` and its `spin` clip) with their own atlases, `ember_bolt.png` and `crystal_bolt.png` (+
glowmasks): the Memory Bolt atlas recoloured to the ember and Oracle ramps by `tools/remnants/families_data.py`, same
UVs (previews in `docs/models/remnants/ember_bolt/` and `crystal_bolt/`). Item icons `ashen_cog` and `relic_shard`
come from `docs/models/remnants/items/`.

| Entity | Clips (ticks; L = loop, H = hold) and key ticks |
|---|---|
| ashbound_crawler | `idle` 40L, `walk` 8L, `scorch` 24 (rears 0–8, flame 8–18, damage window 10–18, recovery to 24), `hurt` 6, `death` 20H (swells to 16, bursts at 16) |
| ember_servitor | `idle` 40L, `fly` 20L (moving), `shoot` 20 (lens flares 0–9, bolt released 10), `repair` 32L (heal pulses at 8, 16, 24), `hurt` 6, `death` 28H (falls, no damage) |
| rusted_guardian | `idle` 60L, `walk` 36L, `smash` 52 (hammer raised 0–22, impact 26, recovery to 52), `sweep` 40 (wind-back 0–16, hit 18, recovery to 40), `hurt` 8, `death` 48H |
| shardling | `idle` 40L (also while drifting), `shoot` 16 (bolt released 8; preceded by a 4-tick charge), `hurt` 6, `death` 12H |
| ancient_vessel | `idle` 60L, `walk` 16L, `pulse` 28 (lid lifts 0–12, ring released 14), `hurt` 6, `death` 24H (swells to 18, bursts at 18) |
| relic_stalker | `idle` 60L, `walk` 14L, `ambush` 32 (folds 0–12, springs 12, contact window 15–22), `slash` 20 (windup 0–8, hit 10), `hurt` 6, `death` 36H |
| ember_bolt, crystal_bolt | `memory_bolt.spin` 10L |

Every clip is listed in `RemnantClips` and checked by `ClipContractRegression` (names, lengths, loop modes, geo,
texture and glowmask, and the two bolt atlases).

## 3. Family rules

- **Allegiance.** Each family is a tag: `#remnants:ashbound` (crawler, servitor, guardian) and
  `#remnants:corrupted_relics` (shardling, vessel, stalker). A family never damages its own members: melee, cones,
  rings, bolts and death bursts all skip them, and damage from a family member is ignored. Creative and spectator
  players are never targets. The two families do not seek each other out.
- **Not undead.** Constructs and relics are vessels, not the dead: they are not in `#minecraft:undead` (1.21.1) and
  report `MobType.UNDEFINED` (1.20.1).
- **Fire.** The Ashbound are fire-immune (furnace hearts). Their flames and bursts set creatures burning; nothing
  either family does places fire or changes a block (the Realm is build-protected), and no burst uses a real explosion.
- **Readable combat** (design bible §2.4). Every damaging attack has a tell of at least 10 ticks (pose, sound and
  particles) and its damage lands on the clip's action beat. The Shardling's 8-tick clip windup is lengthened by a
  4-tick charge (chime and gathering motes) before the clip starts.
- **Bodies.** All six run on the Hollow Legion's action machine (`LegionEntity`): one synchronized action at a time,
  server-timed hits, held death clip, stats from `remnant/monsters/<id>`. Walkers extend `AshboundEntity` or
  `CorruptedRelicEntity`; the two floaters extend `HoveringRemnantEntity` (flying navigation, no gravity or fall
  damage, hover lift), which takes its allegiance from the family.

## 4. The Ashbound Constructs (Kharum: bronze, soot, ember)

| Mob | HP / armor / speed | XP | Behaviour | Loot |
|---|---|---|---|---|
| Ashbound Crawler (common) | 16 / 4 / 0.33 | 5 | Fast furnace bug; hostile. Closes in and **scorches**: rears up for 8 ticks with a hiss, the flame catches at 8, and between ticks 10 and 18 a cone 3 blocks long and 60° wide hits each creature once for 4 damage and sets it burning for 60 ticks. Attack cooldown 30. **Death:** the furnace swells for 16 ticks, then bursts: 3 damage within 2.5 blocks and burning for 40 ticks (ember, lava and smoke particles, a small explosion sound; no blocks change). | `ashen_cog` 0–1 (+0–1 looting), `minecraft:coal` 0–2 |
| Ember Servitor (common) | 14 / 2 / 0.30 (flying) | 5 | Maintenance drone on a spinning rotor, 2.5 above the ground. Holds 4–9 blocks from its target. **Shoot:** the lens flares (ticks 0–9) and at tick 10 an ember bolt leaves at 0.6 blocks/tick (range 18): 5 damage and burning for 30 ticks; it fizzles on blocks. Cooldown 50. **Repair:** out of combat it flies to the most damaged Ashbound construct within 10 blocks (never itself), hovers beside it and loops `repair`: 0.5 health at ticks 8, 16 and 24 of each 32-tick loop (about 1 health per second), with sparks from its welding claw. A target interrupts the weld at once. | `ashen_cog` 0–1 (+0–1 looting), `minecraft:redstone` 0–2 |
| Rusted Guardian (elite) | 70 / 14 / 0.20 | 25 | Heavy soldier: knockback resistance 0.9, turns at most 8° per tick and only attacks a target within ±40° of its facing, so flanking pays. **Smash:** the hammer rises over 22 ticks (piston hiss, an ember ring traced on the floor from tick 7), impact at 26: a 3-block ground ring for 14 damage; anyone in the air above it is spared; recovery to 52. **Sweep:** wind-back to 16, a 150° arc of reach 3.3 at tick 18 for 11 damage with knockback 1.6; recovery to 40. Cooldown 40 after either. | `ashen_cog` 2–4 (+0–1 looting), `minecraft:iron_ingot` 1–3 |

## 5. The Corrupted Relics (Sanctum: chalk marble, gilt, rose-violet soul glass)

| Mob | HP / armor / speed | XP | Behaviour | Loot |
|---|---|---|---|---|
| Shardling (common) | 8 / 0 / 0.28 (flying) | 3 | Fragile floating crystal, 1.6 above the ground; keeps 6–12 blocks from its target. **Shoot:** a 4-tick charge (amethyst chime, rose motes gather), then the `shoot` clip; at its tick 8 a crystal bolt leaves at 0.55 blocks/tick (range 20): 5 damage. 11 ticks from tell to release. Cooldown 60. **Groups take turns:** when one starts charging, every other Shardling within 12 blocks holds its shot for 16 ticks. Spawns in groups. | `relic_shard` 0–1 (+0–1 looting), `minecraft:amethyst_shard` 0–1 |
| Ancient Vessel (uncommon) | 30 / 6 / 0.16 | 10 | A living urn that shuffles after intruders; knockback resistance 1.0. **Pulse:** while its target is within 5 blocks, the lid lifts (ticks 0–12) and a rose ring is drawn on the floor at radius 5; at tick 14 the ring bursts: 6 damage and Slowness I for 40 ticks to everything within 5 blocks (2.5 up or down). One pulse every 100 ticks (28-tick clip + cooldown 72). **Death:** the cracks hum and the urn swells for 18 ticks, then bursts: 8 damage within 3 blocks with explosion particles and sound; no blocks change. | `relic_shard` 1–2 (+0–1 looting), `minecraft:gold_nugget` 2–5 |
| Relic Stalker (elite) | 50 / 8 / 0.30 | 20 | Assembled artifact limbs (candlestick legs, scepter-blade arms, a censer head); knockback resistance 0.4. **Hidden** like the Crypt Lurker: invisible, motionless, silent, untargetable and unhurtable until a survival player comes within 7 blocks. Natural, structure and spawner stalkers start hidden; eggs and commands spawn them visible (`Hidden:1b` hides one). **Ambush:** a shimmer as it appears, then it folds low for 12 ticks (the tell), springs at tick 12 toward where the target stood, and the first contact between ticks 15 and 22 deals 8 damage and Slowness I for 60 ticks. Also used as a gap-closer at 2.5–7 blocks (cooldown 160). **Slash:** windup 0–8, a 110° arc of reach 2.2 at tick 10 for 9 damage; it chains a second slash when the target is still in reach (at most 2 in a row). With no target for 600 ticks it hides again. | `relic_shard` 2–4 (+0–1 looting), `minecraft:gold_ingot` 0–2, `minecraft:candle` 0–1 |

Bolts and melee use `mobProjectile` / `mobAttack` damage, so armor, shields and difficulty scaling apply; the death
bursts use explosion damage (blast protection helps).

**Balance against the families already in the Realm.** The commons sit between the Catacomb Rat and the Crypt Lurker
(fragile but dangerous in numbers; the Shardling dies to two sword hits); the Vessel is an uncommon like the Memory
Wraith (fight it at range or step out of the ring); the two elites match the Soul Collector and the Soulbound Knight
(about 50–70 health, 9–14 damage, 20–25 XP) and keep the slow, punishable rhythm of the Burial Guardian without its
boss bar.

## 6. Items

- **Ashen Cog** (`remnant_bosses:ashen_cog`): material, stack 64. "A soot-black cog from a Kharum construct. Its
  teeth are still warm." Will feed the Colossus summon and Furnace Guardian armor (slice 6).
- **Relic Shard** (`remnant_bosses:relic_shard`): material, stack 64. "A splinter of Sanctum soul glass. The Oracle's
  light still hums inside." Will feed the Oracle summon and relic repair (slice 8).

Both are `LoreItem`s with one tooltip line, in the creative tab after the Forgotten Watcher's items.

## 7. Spawning (the Eclipsed Realm only)

Monster category, from each Realm biome's spawn list (`tools/remnants/structures/realm_data.py`; validated by
`tools/remnants/realm/validate_data.py`). The Realm light rule and 40% thinning apply (realm.md §8). Spawn placements
are `ON_GROUND` / `MOTION_BLOCKING_NO_LEAVES` with `ModEntities::canMonsterSpawn` on every loader (Forge
`Operation.REPLACE`, NeoForge `Operation.AND`, as the other families). They never spawn in the overworld.

| Biome | Creature | Weight | Group |
|---|---|---|---|
| `ashen_wastes` | ashbound_crawler | 25 | 2–4 |
| | ember_servitor | 12 | 1–2 |
| | rusted_guardian | 4 | 1 |
| `ossuary_depths` | ashbound_crawler | 10 | 1–3 |
| | relic_stalker | 6 | 1 |
| | ancient_vessel | 6 | 1 |
| `gracewood` | shardling | 12 | 3–4 |
| `tarnished_fields` | shardling | 6 | 2–3 |
| | ancient_vessel | 4 | 1 |
| | rusted_guardian | 2 | 1 |
| `gravefield_barrows` | relic_stalker | 5 | 1 |
| | ancient_vessel | 6 | 1 |

**Spawn costs** (`energy_budget` / `charge`), as the other families: crawler and servitor 0.15 / 0.7 (patrol), guardian,
vessel and stalker 0.12 / 1.0 (elite), shardling 0.2 / 0.5 (swarm, like the Catacomb Rat).

## 8. Config (`config/remnant/monsters/<id>`)

Every mob reads `max_health`, `armor`, `attack_damage`, `movement_speed`, `xp_reward` and `attack_cooldown_ticks`, plus:

| File | Keys (defaults) |
|---|---|
| `ashbound_crawler` | `scorch_damage` 4, `scorch_range` 3, `scorch_arc_degrees` 60, `scorch_burn_ticks` 60, `burst_damage` 3, `burst_radius` 2.5, `burst_burn_ticks` 40 |
| `ember_servitor` | `bolt_damage` 5, `bolt_speed` 0.6, `bolt_range` 18, `bolt_burn_ticks` 30, `hold_min_distance` 4, `hold_max_distance` 9, `repair_radius` 10, `repair_reach` 3, `repair_heal` 0.5 |
| `rusted_guardian` | `smash_damage` 14, `smash_radius` 3, `sweep_damage` 11, `sweep_reach` 2.6, `sweep_knockback` 1.6, `turn_degrees` 8, `knockback_resistance` 0.9 |
| `shardling` | `bolt_damage` 5, `bolt_speed` 0.55, `bolt_range` 20, `hold_min_distance` 6, `hold_max_distance` 12, `group_radius` 12, `group_stagger_ticks` 16 |
| `ancient_vessel` | `pulse_damage` 6, `pulse_radius` 5, `pulse_slow_ticks` 40, `burst_damage` 8, `burst_radius` 3, `knockback_resistance` 1 |
| `relic_stalker` | `ambush_radius` 7, `ambush_damage` 8, `ambush_slow_ticks` 60, `ambush_cooldown_ticks` 160, `slash_damage` 9, `slash_reach` 2.2, `max_combo` 2, `rehide_ticks` 600, `knockback_resistance` 0.4 |

## 9. Code and data map

| Area | Where |
|---|---|
| Family rules | `entity/Ashbound`, `entity/CorruptedRelics`, `entity/AshboundEntity`, `entity/CorruptedRelicEntity`, `entity/HoveringRemnantEntity` |
| Mobs | `entity/AshboundCrawlerEntity`, `EmberServitorEntity`, `RustedGuardianEntity`, `ShardlingEntity`, `AncientVesselEntity`, `RelicStalkerEntity` |
| Bolts | `entity/RemnantBoltEntity` (base), `EmberBoltEntity`, `CrystalBoltEntity`; `client/renderer/RemnantBoltRenderer` |
| Registration | `init/ModEntities`, `init/ModItems`, `init/ModTabs`, `config/BoneMonsterConfig`; per loader: entity types, eggs, attributes, spawn placements, renderers (`RemnantGeoRenderer` with the glowmask layer) |
| Data | `tools/remnants/families_data.py` writes loot tables, family tags (`entity_types/` on 1.20.1, `entity_type/` on 1.21.1), item models and textures, egg models, bolt atlases and lang entries into both trees, and removes the retired keys and loot; idempotent |
| Spawns | `tools/remnants/structures/realm_data.py` (`ASHBOUND`, `RELICS`, `COSTS`, the five biome spawn lists) |
| Checks | `ClipContractRegression`, `AliasRegression` (`constructsAndRelics`), `tools/remnants/realm/validate_data.py`, `tools/prod-boot/families_commands.txt` (summons each mob in the Realm, counts both tags, rolls every loot table, kills them), and the `CreatureShowcase` beat of the 1.21.1 realm runtime walkthrough (`tools/realm-runtime`): a portrait of each creature and a line-up, each creature fighting a stationary villager (captured on the action beat, health before and after reported), a Servitor welding a damaged Crawler, and a hidden Stalker that waits unseen at 10 blocks and ambushes at 5.5. Captures land in `1.21.1/fabric/run/screenshots/realm-12-*` to `realm-15-*`; a reviewed set is kept in `docs/remnants/families/screenshots/` |
