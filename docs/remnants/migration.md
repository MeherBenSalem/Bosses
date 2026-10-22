# Content migration plan (Remnant Bosses 2.7 → Remnants 3.0)

Goal: a world created on 2.x loads on 3.0 without losing chunks, inventories or Boss Hunts progress. Retired creatures and items become their Remnants successors instead of vanishing.

## Principles

1. **New content, new IDs.** A redesigned creature gets an ID that matches its new identity. The only exception is `remnants:hollow_sovereign`, whose name survives the redesign.
2. **Every retired ID has a successor.** Saved entities and item stacks are rewritten from the old ID to the successor ID as they load. Transient helper entities (projectiles and effects) are dropped instead.
3. **Aliases only fire for unregistered IDs.** An old entity keeps working until the slice that replaces it lands. The alias layer ignores any source ID that is still registered, so the full table can ship early without hijacking live content.
4. **Saved data stays readable both ways.** The Boss Hunts store keeps schema 1 and canonicalises retired boss keys on every load (see below), so older builds can still read and write it; they preserve keys they do not know.
5. **Old config files are left alone.** New bosses read new Jauml files. Retired files stay on disk, unused, so downgrading does not lose settings.

## Entity map (namespace `remnants`)

| Old ID | New ID | Slice | Note |
|---|---|---|---|
| `hollow_sovereign` | `hollow_sovereign` (kept) | 2 | Old NBT `SovereignEnraged=true` loads as phase 2. |
| `skeleton_melee` | `hollow_sentinel` | 2 | |
| `skeleton_archer` | `hollow_arbalist` | 2 | |
| `kotsukage` | `gravebound_warden` | 3 | |
| `skeleton_minion` | `gravebound_soldier` | 3 | |
| `grave_skitter` | `crypt_lurker` | 3 | |
| `rat` | `catacomb_rat` | 3 | |
| `umbrakar` | `forgotten_watcher` | 4 | |
| `wraith` | `memory_wraith` | 4 | |
| `dirge_lantern` | `soul_collector` | 4 | |
| `ossukage` | `eclipsed_knight` | 5 | |
| `armored_grub` | `ashbound_crawler` | 6 | Retired early, with the Ashbound Constructs ([families/ashbound-and-relics.md](families/ashbound-and-relics.md)). |
| `sickleghast` | `relic_stalker` | 8 | Retired early, with the Corrupted Relics. |
| `kunai`, `ossukage_rune_effect`, `kotsukage_trap`, `umbrakar_orb` | *(dropped)* | 3–5 | Short-lived projectiles and effects. |

Pre-2.4.1 IDs from the old `remnant_bosses` entity namespace (`remnant_bosses:rat`, `remnant_bosses:remnant_ossukage`, and so on) resolve through the same table. For example, `remnant_bosses:remnant_ossukage` becomes `remnants:ossukage`, which then becomes `remnants:eclipsed_knight` once slice 5 lands. This also fixes the 2.4.1 aliases, which were declared but never wired into any loader.

## Item map (namespace `remnant_bosses`)

| Old ID | New ID | Slice |
|---|---|---|
| `skeleton_melee_spawn_egg` | `hollow_sentinel_spawn_egg` | 2 |
| `skeleton_archer_spawn_egg` | `hollow_arbalist_spawn_egg` | 2 |
| `kotsukage_spawn_egg` | `gravebound_warden_spawn_egg` | 3 |
| `skeleton_minion_spawn_egg` | `gravebound_soldier_spawn_egg` | 3 |
| `grave_skitter_spawn_egg` | `crypt_lurker_spawn_egg` | 3 |
| `rat_spawn_egg` | `catacomb_rat_spawn_egg` | 3 |
| `rat_fang`, `old_skeleton_bone` | `grave_iron` | 3 |
| `old_skeleton_head` | `minecraft:skeleton_skull` | 3 |
| `fang_on_a_stick` | `minecraft:stone_sword` (same damage; durability carries over) | 3 |
| `umbrakar_spawn_egg` | `forgotten_watcher_spawn_egg` | 4 |
| `wraith_spawn_egg` | `memory_wraith_spawn_egg` | 4 |
| `dirge_lantern_spawn_egg` | `soul_collector_spawn_egg` | 4 |
| `remnant_ossukage_spawn_egg` | `eclipsed_knight_spawn_egg` | 5 |
| `ossukage_sword` | `eclipsed_blade` | 5 |
| `armored_grub_spawn_egg` | `ashbound_crawler_spawn_egg` | 6 |
| `sickleghast_spawn_egg` | `relic_stalker_spawn_egg` | 8 |
| `boss_journal`, `hollow_sovereign_spawn_egg`, `ancient_altar`, `ancient_pedestal` | kept | — |

## How the alias layer works

`com.nightbeam.remnants.init.RegistryAliases` replaces the unused `EntityAliases` class. It holds both maps, resolves chains, and is consulted only when the source ID is absent from the live registry.

| Path | 1.20.1 Fabric | 1.20.1 Forge | 1.21.1 Fabric | 1.21.1 NeoForge |
|---|---|---|---|---|
| Entities in chunks, spawners, passengers (`EntityType.by(CompoundTag)`) | common mixin rewrites `id` | common mixin | common mixin | common mixin |
| Top-level item stacks: inventories, containers, item entities | mixin on `ItemStack.of` | mixin on `ItemStack.of` | mixin on `ItemStack.parse` / `parseOptional` | same mixin |
| Registry lookups by name (nested item components, commands, loot) | mixin on vanilla `MappedRegistry` lookup | `MissingMappingsEvent` remap (Forge wraps the vanilla registries) | mixin on vanilla `MappedRegistry` lookup | native `Registry#addAlias` |

Regression checks (`AliasRegression`, part of `gradle check` in both workspaces):

- No ID maps to itself.
- Chains terminate.
- No successor is a retired ID.
- Every entity successor has a loot table in the folder that version actually loads.

## Boss Hunts store

The store lives in `data/remnant_bosses_hunts.dat`, under `Players → <uuid> → <bossId> → state`.

- **Key canonicalisation, on every load:** each `bossId` key of a schema-1 store is renamed through the entity map with the alias layer's guard: a key moves only when it is not a current `BossHunt` ID and its resolved successor is. When both keys exist, the furthest-advanced state wins (an out-of-range value counts as undiscovered). Non-integer values, unknown keys and stores with any other schema are left untouched. The pass is idempotent and marks the store for saving only when it renamed something.
- **Schema stays 1:** builds older than 3.0 keep reading and writing the store and preserve keys they do not know. After a downgrade, a renamed boss shows as undiscovered there, but its progress stays on disk and merges back on the next 3.0 load.
- **Journal packet:** its entry limit rises from 4 to 7, to cover the full boss roster.
- **Old clients:** they cannot join a 3.0 server anyway, because the mod version is checked at join.

## Arenas and new chunks

Structures are datapack worldgen and only generate in chunks that have never been generated. Existing worlds keep their terrain and reach the new structures by exploring further out. Altar rituals remain available everywhere, so no boss is locked behind new chunks.
