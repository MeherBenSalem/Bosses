# Pre-overhaul inventory (Remnant Bosses 2.7.0)

A snapshot of everything the mod contained before the Remnants 3.0 overhaul, with the decision taken for each element. Decisions point to the slice that carries them out (see [design-bible.md §9](design-bible.md#9-delivery-order)) and to the ID mapping in [migration.md](migration.md).

Namespaces: entity types use `remnants:`; items, blocks, sounds, assets, networking and the creative tab use `remnant_bosses:`.

## Platforms and pipeline

| Area | State | Decision |
|---|---|---|
| Workspaces | `1.20.1/` (Fabric, Forge, Java 17) and `1.21.1/` (Fabric, NeoForge, Java 21); hand-maintained `common/` copies with API differences | Keep. Every slice lands in both workspaces. |
| Rendering | GeckoLib for bosses and newer mobs; Blockbench-Java `EntityModel`s for rat, skeleton minion, wraith, kunai | Keep GeckoLib as the only animation framework; retire the Java models with their mobs. |
| Config | Jauml JSON files under `config/remnant/...`, read by reflection with hard-coded fallbacks | Keep. New entities get new files; old files are left on disk, unused. |
| Asset authoring | Python scripts driving the Blockbench MCP plugin on port 3000 (`tools/build_*.py`) | Keep and generalise: `tools/remnants/` (client, rig DSL, atlas painter, exporter). |
| Tests | `RecipeRegression` and `JournalRegression` main-method harnesses (`gradle check`), `scripts/*.test.mjs`, `scripts/verify_release_jars.py`, manual `tools/verify_*.py` | Keep; add alias, loot-path and asset-contract checks. |
| Worldgen | None, apart from rat/wraith spawn biome modifiers | Add datapack structures per slice. |

## Bosses

| Boss | ID | Kit (summary) | Ritual (item; pedestal tops E/W/S/N) | Decision |
|---|---|---|---|---|
| Ossukage | `remnants:ossukage` | 800 HP samurai skeleton: 3-hit melee chain, kunai throw (≈100+ dmg, owner-less arrow), phase-2 dash with random explosion/darkness/minions; rune VFX entity; phase-2 texture swap | Nether Star; 4× skeleton skull | **Replace** with the Eclipsed Knight (slice 5). |
| Kotsukage | `remnants:kotsukage` | 700 HP bone colossus: swipes, stomps, poison breath, roar that drops traps and minions; no persistence; full heal on reload (bug) | Wither Skeleton Skull; bone block, soul sand, soul soil, nether wart block | **Replace** with the Gravebound Warden (slice 3). |
| Umbrakar | `remnants:umbrakar` | 750 HP rift beast: marching riftclaw, bite, front/tail slam, homing orb, expanding roar ring; no music | Echo Shard; amethyst block, crying obsidian, end stone, sculk | **Replace** with the Forgotten Watcher (slice 4). |
| Hollow Sovereign | `remnants:hollow_sovereign` | 950 HP bone soul-caster: claw combo, ground-slam ring, core beam, eruption seal, enrage at 45%; shares Kotsukage's atlas | Heart of the Sea; bone block ×2, skeleton skull ×2 | **Rebuild** under the same ID as the armored Last King of Vael (slice 2). |

Helper entities: `remnants:kunai`, `remnants:ossukage_rune_effect` (noSave), `remnants:kotsukage_trap`, `remnants:umbrakar_orb` — **removed** with their bosses; transient, never aliased.

## Creatures

| Creature | ID | Notes | Decision |
|---|---|---|---|
| Rat | `remnants:rat` | 30 HP melee monster, 4 skins (re-rolled on every load), double-registered spawns on Forge/NeoForge | **Replace** with Catacomb Rat (slice 3). |
| Skeleton minion | `remnants:skeleton_minion` | Leaps, places Kotsukage traps, poison hits; summoned by Ossukage, Kotsukage and the ritual | **Replace** with Gravebound Soldier (slice 3). |
| Wraith | `remnants:wraith` | 20 HP melee floater; Fabric-only natural spawns | **Replace** with Memory Wraith (slice 4). |
| Armored grub | `remnants:armored_grub` | Neutral flying grub, custom sounds | **Replaced** by the Ashbound Crawler (shipped ahead of slice 6). |
| Skeleton Warrior | `remnants:skeleton_melee` | Smash/slash melee; AoE hits every entity (no ally filter) | **Replace** with Hollow Sentinel (slice 2). |
| Skeleton archer | `remnants:skeleton_archer` | Kiting archer, draw and volley | **Replace** with Hollow Arbalist (slice 2). |
| Grave Skitter | `remnants:grave_skitter` | Bone crawler, pounce | **Replace** with Crypt Lurker (slice 3). |
| Sickleghast | `remnants:sickleghast` | Tall scythe ghoul | **Replaced** by the Relic Stalker (shipped ahead of slice 8). |
| Dirge Lantern | `remnants:dirge_lantern` | Flying ribcage, marked burst | **Replace** with Soul Collector (slice 4). |

`BoneMonsterEntity` (shared base with config-driven stats, cone attacks and windups) is a sound pattern and is **kept** as the basis for new common mobs.

## Items and blocks

| Item | Decision |
|---|---|
| `boss_journal` (Boss Hunts Journal) | **Keep ID**; becomes the Codex of Remnants. |
| `ancient_altar`, `ancient_pedestal` | **Keep IDs**; restyled, used inside arenas and anywhere. |
| `ossukage_sword` (kunai thrower; 11 tooltip lines describe unimplemented effects) | **Replace** with Eclipsed Blade (slice 5). |
| `rat_fang`, `fang_on_a_stick` (weak sword; no riding code) | **Replace** with Cryptborn materials/weapon (slice 3). |
| `old_skeleton_bone`, `old_skeleton_head` | **Replace** with Grave Iron and a Cryptborn trophy (slice 3). |
| 13 spawn eggs | Each replaced with its successor's egg in the slice that replaces the entity. |

## Known defects found during inventory

Fixed in the foundation step unless marked otherwise:

- `EntityAliases` was never wired into any loader, so pre-2.4.1 `remnant_bosses:*` entities were silently dropped. Replaced by the alias layer in [migration.md](migration.md).
- 1.21.1 loot for `rat`, `skeleton_minion`, `kotsukage` and `umbrakar` sits in the ignored `loot_tables/` folder, so those mobs drop nothing on 1.21.1.
- Unused stale loot copies under `data/remnant_bosses/loot_tables/entities/`.
- Altar ritual consumes one activation item per nearby player (shared path). Fixed when the shared ritual path is rewritten (slice 3).
- Rats are spawn-registered twice on Forge/NeoForge, once ignoring config (slice 3, when rats are replaced).
- Kotsukage/Umbrakar never set persistence; Kotsukage fully heals on reload; Ossukage music can stick (resolved by replacement).
