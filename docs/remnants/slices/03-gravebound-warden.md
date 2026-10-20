# Slice 3 — The Gravebound Warden, the Cryptborn and the Gravebound Catacombs

**Civilization:** the Ossuary Covenant, keepers of the dead and wardens of the soul-prisons.
**Palette:** rusted black iron, old bone, grave cloth, tomb stone with moss, and cold soul light.
**Progression tier:** I (iron). This is the first boss most players meet.

This file is the implementation contract. Conventions follow [02-hollow-sovereign.md](02-hollow-sovereign.md): damage in half-hearts, time in ticks, and every value marked *(cfg)* is a Jauml default.

## 1. Registry IDs

| Kind | ID | Replaces |
|---|---|---|
| Boss | `remnants:gravebound_warden` | `remnants:kotsukage` |
| Boss helper | `remnants:warden_chain` | `remnants:kotsukage_trap` (dropped, not aliased) |
| Boss helper | `remnants:chain_anchor` | — (destructible arena anchor) |
| Mob | `remnants:gravebound_soldier` | `remnants:skeleton_minion` |
| Mob | `remnants:crypt_lurker` | `remnants:grave_skitter` |
| Mob | `remnants:catacomb_rat` | `remnants:rat` |
| Miniboss | `remnants:burial_guardian` | — |
| Items | `remnant_bosses:grave_iron`, `crypt_key`, `wardens_grasp`, `gravebound_executioner`, `crypt_sigil`, `warden_helm`, `warden_hauberk`, `warden_tassets`, `warden_boots` | `rat_fang`, `old_skeleton_bone` → `grave_iron`; `old_skeleton_head` → `minecraft:skeleton_skull`; `fang_on_a_stick` → `minecraft:stone_sword` |

**Spawn eggs:** base `#271C17`, spots `#C3B99F` (Cryptborn). The Warden egg uses spots `#4FC9C1`.

**Retired with no successor:** `remnants:kunai`, `remnants:ossukage_rune_effect` and the old `rat` skins.

**Sizes (width × height, blocks):**

| Entity | Size |
|---|---|
| Warden | 2.2 × 4.6 |
| Warden chain | 0.5 × 0.5 |
| Chain anchor | 1.0 × 2.0 |
| Soldier | 0.7 × 2.1 |
| Lurker | 1.4 × 0.8 |
| Rat | 0.6 × 0.6 |
| Burial Guardian | 1.6 × 3.3 |

## 2. Assets

Built by `tools/remnants/models/gravebound_warden.py` and `tools/remnants/models/cryptborn.py`. Sources and previews are in `docs/models/remnants/<id>/`. Install with `tools/remnants/install_assets.py`.

| Entity | Clips (ticks; L = loop, H = hold) and key ticks |
|---|---|
| gravebound_warden | `idle` 80L, `walk` 40L, `sweep` 48 (hit 25), `chop` 52 (hit 28; wedged to 38), `chain_grab` 44 (chain out 20–22, latch 22–30, pull to 38), `eruption` 56 (slam 24, eruptions 26–46), `summon` 50 (soldiers 26), `chain_wall` 60 (walls 30), `cell_break` 60 (phase 2), `stagger` 24, `death` 100H (souls freed 56) |
| gravebound_soldier | `idle` 60L, `walk` 28L, `thrust` 24 (hit 10), `sweep` 28 (hit 13), `hurt` 8, `rise` 30, `death` 32H |
| crypt_lurker | `idle` 48L, `walk` 12L, `pounce` 32 (crouch to 12, leap 12–17, hit window 17–25), `bite` 16 (hit 8), `emerge` 24, `hurt` 6, `death` 30H |
| burial_guardian | `idle` 80L, `walk` 48L, `slam` 48 (hit 26), `sweep` 36 (hit 18), `unseal` 52 (soul blast 24), `hurt` 8, `death` 48H |
| catacomb_rat | `idle` 40L, `walk` 10L, `attack` 12 (hit 6), `hurt` 6, `death` 20H |

**Bone visibility:**
- Warden: `cage_door` and `souls` stay visible. After `cell_break`, the `souls` bone is scaled to 0.2 for the rest of the fight; the game keeps it small.
- Burial Guardian: the `lid` bone stays open after its `unseal` clip in phase 2.

**Item icons:** in `docs/models/remnants/items/`: `grave_iron`, `wardens_grasp`, `gravebound_executioner`, `crypt_sigil`. Install them with the slice.

## 3. The Gravebound Warden

| Stat | Value |
|---|---|
| Health | 520 *(cfg)* |
| Armor / toughness | 10 / 2 |
| Knockback resistance | 1.0 |
| Speed | 0.22 (phase 2 ×1.25) |
| XP | 200 |
| Other | Persistent; never despawns |

**Phases:**
1. **The Keeper:** 100–50%.
2. **The Open Cell:** 50–0%, entered through `cell_break`. Freed souls swirl around it. Attack cooldowns ×0.75, and chain walls become available.

| Attack | Effect |
|---|---|
| Executioner's Sweep | 110° frontal arc, radius 4.5: 12 damage plus Slowness I for 40 ticks. |
| Execution Chop | 2-block-wide line, 6 long, hit at tick 28: 18 damage. Ducking out of the line avoids it. The blade stays wedged until tick 38. Hitting the Warden during that window deals ×1.25 damage. |
| Chain Grab | Spawns a `warden_chain` projectile at speed 1.4, range 14. On hit it latches for 8 ticks and then pulls the target 70% of the distance toward the Warden, without damage. A follow-up Sweep is queued at the end of the pull. Blocking with a shield or breaking line of sight cancels the latch. |
| Grave Eruption | Fist slam at tick 24. Then a row of 6 eruptions, 1.5 blocks apart, toward the target, one every 3 ticks: 9 damage each. Each is preceded by 12 ticks of cracking-ground particles. |
| Raise the Condemned | 2 Gravebound Soldiers (3 in phase 2) rise with the `rise` clip. At most 4 alive. Cooldown 400 ticks. |
| Chain Wall | Phase 2 only. Places 4 `chain_anchor` entities around the arena (radius 9, 30 HP each). Chains strung between neighbouring anchors deal 6 damage and knockback on contact, and split the arena into cells. Destroying all 4 anchors staggers the Warden for 60 ticks. Cooldown 700 ticks; anchors expire after 400 ticks. |

**Death:** the cell door swings open and the trapped souls are released, with `soul_release` sound and soul particles. This is the narrative payoff.

**Loot:**

| Item | Count |
|---|---|
| `crypt_sigil` | 1 |
| `wardens_grasp` | 1 |
| `gravebound_executioner` | 1 |
| `grave_iron` | 6–10 |
| `minecraft:bone` | 6–12 |
| `minecraft:wither_skeleton_skull` | 1 at 15% |

**Ritual** (`remnant/bosses/gravebound_warden_summon`):
- **Activation item:** `remnant_bosses:crypt_key`, crafted shaped `G G / GIG / G G` (G = grave iron, I = iron block).
- **Pedestals:** bone block (east), soul soil (west), chain (south), iron bars (north).
- The ritual works only in the Eclipsed Realm by default (`rituals_realm_only`, realm.md §8). The Catacombs chapel has the pedestals pre-filled.

**Codex:**
- **Name:** "The Gravebound Warden".
- **Clue:** "Chains rattle beneath old cemeteries. The Covenant's catacombs still keep their prisoners."
- **Lore:** "When the Oracle shattered, the souls it had taken fell like rain. The Ossuary Covenant caught them and locked them away. Its Warden still keeps the cells, long after there was anyone left to report to."

**Boss bar:** theme with glow `0xFFA6623B` and shadow `0xFF20231B`.

## 4. Cryptborn

All Cryptborn:
- are in the `#remnants:cryptborn` tag and the undead tag;
- never hit each other.

| Mob | HP / armor / dmg / speed | XP | Behaviour | Spawns | Loot |
|---|---|---|---|---|---|
| Catacomb Rat | 8 / 0 / 2 / 0.32 | 1 | Neutral. Flees light levels ≥ 10. Swarms in groups of 3–6 when one is hit. The candle flame is purely visual. | Realm biome spawn lists (realm.md §3); and in Catacombs | `grave_iron` at 10% |
| Crypt Lurker | 22 / 3 / 5 / 0.3 | 6 | Ambush: hides buried and invisible (only the `emerge` clip) until a player comes within 6 blocks, then emerges and pounces: 6 damage, plus Slowness I for 40 ticks on a hit. | Realm biome spawn lists (realm.md §3); and in Catacombs | `minecraft:bone` 1–3, `grave_iron` 0–1 |
| Gravebound Soldier | 24 / 5 / 5 / 0.26 | 7 | Group fighter. Soldiers within 8 blocks share a target. `thrust` has reach 3.2; the guisarme `sweep` hooks and pulls 1 block. Raised by the Warden; spawns in Catacombs. | Structure only | `grave_iron` 0–1, `minecraft:bone` 0–2 |
| Burial Guardian (miniboss) | 140 / 14 / 14 / 0.18 | 60 | Boss bar, no ritual. `slam` is a 4-radius ring. `sweep` has knockback 1.4. In phase 2 (at 50%) it uses `unseal`: a soul blast cone, 10 damage and Wither I for 60 ticks. Very slow turning (max 6° per tick), so flanking pays. | One per Catacombs, guarding the vault | `grave_iron` 3–5, `crypt_key` at 35% |

## 5. Items

- **Grave Iron:** material, stack 64.
- **Crypt Key:** summon item, stack 16.
- **Warden's Grasp:** chain weapon. 7 damage, speed 1.4. Use throws a hook that pulls the target toward the player (cooldown 40), or pulls the player to a block. Sweeping hits apply Slowness I.
- **Gravebound Executioner:** axe. 10 damage, speed 0.9. +40% damage against slowed or chained targets.
- **Crypt Sigil:** relic, off-hand.
  - **Passive:** undead deal 10% less damage to you.
  - **Active:** the nearest enemy within 12 blocks is chained in place for 60 ticks (no movement; it can still attack).
  - **Cost:** Wither I for 60 ticks on yourself.
  - **Cooldown:** 900 ticks.
- **Warden armor:** rusted plate over grave cloth, iron-to-diamond tier: defence 2 / 6 / 5 / 2, toughness 1.
  - **2-piece:** +10% damage against undead.
  - **4-piece, Jailer's Hold:** a melee hit has a 15% chance to chain the target for 20 ticks (cooldown 100).

## 6. Gravebound Catacombs (structure)

**Placement:** underground jigsaw structure at Y −20 to 20 under every land biome of the Eclipsed Realm (`#remnant_bosses:is_eclipsed_realm`, see [realm.md](../realm.md)). Spacing 34, separation 12. It generates in new chunks only. Its chapel door is sealed with fog gates (realm.md §7).

**Pieces:**

| Piece | Contents |
|---|---|
| Entry stair | Leads down from a ruined surface mausoleum |
| Ossuary corridors | Bone-block niches, soul lanterns, cobwebs |
| Cell blocks | Iron-bar cells; some hold soul lanterns, a few hold chests |
| Burial vault | The Burial Guardian, plus a chest with `crypt_key` at 35% |
| Chain chapel | The arena: 23 × 23, with the altar ring pre-filled with bone block, soul soil, chain and iron bars |

**Palette:** deepslate tiles and bricks, polished blackstone, bone blocks, chains, iron bars, soul lanterns and soul fire, moss and cobwebs.

**Spawns:**

| Mob | Weight |
|---|---|
| gravebound_soldier | 10 |
| crypt_lurker | 6 |
| catacomb_rat | 8 |

**Generator:** `tools/remnants/structures/` following the Hollow Citadel generator.
