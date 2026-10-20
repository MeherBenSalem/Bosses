# Slice 2 — The Hollow Sovereign, the Hollow Legion and the Hollow Citadel

Civilization: **Vael**, the Hollow Crown. Palette: blackened iron, tarnished gold, faded royal blue, remnant-light cyan. Progression tier: II (diamond). See [design-bible.md](../design-bible.md) for lore and art rules; this file is the implementation contract.

Units: damage in half-hearts (vanilla), time in ticks (20/s). Every number marked *(cfg)* is read from Jauml with the value here as default, clamped to a sane range.

## 1. Registry IDs

| Kind | ID | Notes |
|---|---|---|
| Boss | `remnants:hollow_sovereign` | Same ID as 2.x; new class body, model and kit. |
| Boss helper | `remnants:sovereign_pauldron` | Flying armor piece, phase 2. |
| Projectile | `remnants:spectral_crescent` | Shared by the boss and Sovereign's Ruin. |
| Effect | `remnants:royal_blade_mark` | Last Command blade marker (noSave, client visual + server timer). |
| Mob | `remnants:hollow_sentinel` | Replaces `remnants:skeleton_melee` (alias exists). |
| Mob | `remnants:hollow_arbalist` | Replaces `remnants:skeleton_archer` (alias exists). |
| Mob | `remnants:fallen_standard_bearer` | New. |
| Mob | `remnants:soulbound_knight` | New, elite. |
| Summon | `remnants:spectral_royal_guard` | Sentinel variant; boss phase 2/3 and Sovereign's Sigil. |
| Items | `remnant_bosses:tarnished_regalia`, `royal_soul_fragment`, `sovereigns_seal`, `sovereigns_ruin`, `sovereigns_sigil`, `hollow_crown`, `regalia_cuirass`, `regalia_greaves`, `regalia_sabatons` | |
| Eggs | `remnant_bosses:hollow_sentinel_spawn_egg`, `hollow_arbalist_spawn_egg`, `fallen_standard_bearer_spawn_egg`, `soulbound_knight_spawn_egg` (`hollow_sovereign_spawn_egg` kept) | Egg colours: base `#22242C`, spots `#B8913F` (Legion); Sovereign base `#16171D` spots `#4FC9C1`. |

Retired by this slice: `remnants:skeleton_melee`, `remnants:skeleton_archer`, their eggs, models, textures, animations and configs. `RegistryAliases` already maps them.

Entity sizes (width × height, blocks): hollow_sovereign 1.8 × 4.0 (eye 3.5) · sovereign_pauldron 0.9 × 0.8 · spectral_crescent 1.6 × 0.4 · hollow_sentinel and spectral_royal_guard 0.8 × 2.4 · hollow_arbalist 0.75 × 2.35 · fallen_standard_bearer 0.8 × 2.45 · soulbound_knight 0.85 × 2.55. The spectral guard renders `hollow_sentinel.geo.json` with `hollow_sentinel_spectral.png` (translucent render type; same UVs).

Item assets are already in place: `textures/item/<id>.png` + `models/item/<id>.json` for the nine items above (Sovereign's Ruin uses a handheld model with enlarged third-person display), and `textures/models/armor/regalia_layer_1.png` / `regalia_layer_2.png` (crowned half-helm painted into the helmet). Sources: `tools/remnants/items.py`.

## 2. Assets (all under `assets/remnant_bosses/`)

Built by `tools/remnants/models/<id>.py` through Blockbench MCP; sources (`.bbmodel`, previews) in `docs/models/remnants/<id>/`. Every entity uses `geo/entity/<id>.geo.json`, `animations/entity/<id>.animation.json`, `textures/entities/<id>.png`, plus `textures/entities/<id>_glowmask.png` when it has emissive pixels (render with GeckoLib `AutoGlowingGeoLayer`). Clip names are `animation.<id>.<clip>`.

### Hollow Sovereign clips (Java contract)

| Clip | Length | Key ticks | Notes |
|---|---|---|---|
| `idle` | loop 60 | — | |
| `walk` | loop 32 | — | phase 1 movement |
| `run` | loop 20 | — | phase 2/3 pursuit |
| `awaken` | 60 | — | summon intro, invulnerable, no damage |
| `combo` | 48 | hits 14, 26, 38 | Royal Combo |
| `throne_slam` | 44 | hit 22 (ring starts) | sword stuck until ~32 = punish window |
| `guard` | 40 | guard window 4–32 | melee hit inside window → `riposte` |
| `riposte` | 18 | hit 6 | |
| `rift_cleave` | 40 | wave launched 18 | |
| `summon` | 40 | guards spawn 24 | |
| `spectral_slash` | 32 | crescents released 16 | phase 3 |
| `soul_lunge` | 30 | dash 12–18, hit 16 | phase 3 |
| `last_command` | 120 | marks 30/50/70, strikes 60/80/100 | once, ≤10% HP |
| `phase_shatter` | 60 | pauldrons + guards spawn 36 | → phase 2; knockback-only pulse at 30 |
| `phase_soul` | 60 | — | → phase 3 |
| `stagger` | 20 | — | guard broken / pauldron destroyed |
| `death` | 80, hold | — | remove entity after 80 |

Bones the renderer hides by phase (`GeoModel#setCustomAnimations`): `pauldron_left`, `pauldron_right` from phase 2; `breastplate` from phase 3. The soul bone and the eyes are emissive.

### Legion clips

| Entity | Clips (ticks) |
|---|---|
| hollow_sentinel | `idle` loop 60, `walk` loop 30, `block` loop 20, `bash` 20 (hit 10), `strike` 36 (hit 18), `hurt` 8, `death` 40 hold, `rise` 30 (spectral guard spawn) |
| hollow_arbalist | `idle` loop 60, `walk` loop 30, `aim` 16 (fires on 16), `fire` 10, `reload` 40, `kick` 16 (hit 8), `hurt` 8, `death` 40 hold |
| fallen_standard_bearer | `idle` loop 60, `walk` loop 32, `sweep` 24 (hit 12), `rally` 50 (buff on 30), `hurt` 8, `death` 50 hold |
| soulbound_knight | `idle` loop 60, `walk` loop 28, `run` loop 18, `combo` 34 (hits 12, 24), `lunge` 30 (dash 14–20, hit 18), `counter` 24 (stance 0–14, hit 18), `hurt` 8, `death` 46 hold |
| sovereign_pauldron | `idle` loop 40, `telegraph` 20, `dive` loop 10 |
| spectral_crescent | `spin` loop 10 |

## 3. The Hollow Sovereign

| Stat | Value |
|---|---|
| Max health | 720 *(cfg `max_health`)* |
| Armor / toughness | 12 / 4 |
| Knockback resistance | 1.0 |
| Speed | 0.24 walk; ×1.35 run in phase 2/3 |
| Follow range | 48 |
| Size | 1.8 × 4.0, eye 3.5 |
| XP | 300 *(cfg)* |
| Persistence | always; never despawns; cannot change dimension |

Phases: **1 Fallen Monarch** (100–66%), **2 Shattered Throne** (66–33%), **3 The Last Command** (33–0%). Thresholds *(cfg `phase_two_percent` 66, `phase_three_percent` 33, `last_command_percent` 10)*. A threshold crossed mid-attack waits for that attack to finish. Transitions are invulnerable and never deal damage.

Attack selection runs when no attack is active and the cooldown is 0 *(cfg `attack_cooldown_ticks` 30 / phase 2: ×0.75 / phase 3: ×0.6)*. It requires a living target with line of sight. The same attack is never chosen twice in a row.

| Attack | Phases | Range (blocks) | Effect |
|---|---|---|---|
| Royal Combo | all | ≤ 4.5 | Three 100° frontal arcs, radius 4: 9 / 9 / 13 *(cfg `combo_damage` 9; last hit ×1.45)*. Each hit is checked separately. |
| Throne Slam | all | ≤ 7 | Ground ring expanding 0 → 9 blocks over ticks 22–34. Hits once per entity when \|dist − radius\| < 1.0 and the victim is within 1.2 blocks of the ground: 14 *(cfg)*. Jumping avoids it. |
| Guard → Riposte | all | ≤ 4 | Picked when the target has hit the boss at least 3 times in the last 60 ticks, or 20% of the time at close range. During the window, melee hits are negated (anvil clang, gold sparks) and trigger `riposte`: a 16-damage thrust, 3-block line. Projectiles are deflected. An **axe** hit at ≥ 0.9 attack strength breaks the guard instead: `stagger`, and the boss takes ×1.25 damage for 40 ticks. |
| Rift Cleave | all | 5–14 | Ground wave travelling 12 blocks in a straight line from tick 18 at 1 block/tick, 1.5 blocks wide: 12 *(cfg)*. Sidestep. |
| Summon Guards | 2, 3 | any | 2 spectral royal guards when fewer than 2 are alive; cooldown 500 ticks. |
| Pauldron dive | 2 | — | Run by the pauldron entities (§3.1), independently of the boss's own attacks. |
| Spectral Slash | 3 | 5–18 | Three crescents in a 30° fan, speed 1.1 blocks/tick, 10 damage each, pierce. |
| Soul Lunge | 3 | 6–14 | Dash toward the target position locked at tick 12, max 9 blocks, stopping at walls: 15 damage in a 2-block radius at tick 16. Afterimage particles along the path. |
| The Last Command | 3, once | — | Triggered at ≤ 10%. The boss kneels and takes ×1.5 damage while kneeling. At ticks 30/50/70 a mark (radius 2.5) appears under every player within 32 blocks, plus 2 random marks within 6 blocks of them. Each mark fills with a shrinking ring for 30 ticks, then a spectral blade falls: 12 damage inside the radius. |

Melee hits use `mobAttack`, so difficulty scaling applies. Hits skip creative/spectator players, the Sovereign, its pauldrons and Legion allies (`#remnants:hollow_legion` entity tag).

Readability: every attack plays a windup sound at its start. Rings, lines and marks are drawn with vanilla particles (`SOUL_FIRE_FLAME`, `SOUL`, `END_ROD`, cyan `DustParticleOptions`), scaled by `vfx_density`.

### 3.1 Sovereign pauldrons (phase 2)

Two pauldron entities spawn at tick 36 of `phase_shatter`.

- **Stats:** 40 HP, armor 6, no gravity, fire immune. Owner UUID is saved.
- **Orbit:** radius 3.5 around the boss at height +3.2, 1 revolution per 160 ticks, opposite phases.
- **Dive:** every 140–180 ticks, staggered so only one is ever diving. It telegraphs for 20 ticks (`telegraph` clip, shakes, soul particles, `ITEM_TRIDENT_RETURN` at pitch 0.6), locks the target position, then dives in a straight line at 1.2 blocks/tick. 8 damage on contact. It stops at blocks or after 14 blocks, then returns.
- **Destruction:** staggers the boss for 20 ticks. Pauldrons are discarded when the boss enters phase 3, dies or is removed.
- **No loot.**

### 3.2 Spectral royal guard

A `HollowSentinelEntity` subclass.

- **Stats:** 20 HP, 4 damage, rendered translucent with `hollow_sentinel_spectral.png`. No loot, no XP. Despawns after 600 ticks or when its owner is gone.
- **Boss guards:** spawn with `rise` and target the boss's target.
- **Sigil guards (Sovereign's Sigil):** allied to the player who summoned them. They target whatever that player attacks or is attacked by, and never attack players or tamed animals.

### 3.3 Summoning

The ritual config `remnant/bosses/hollow_sovereign_summon` keeps its keys but changes its defaults:

| Key | Default |
|---|---|
| `portal_activation_item` | `remnant_bosses:sovereigns_seal` |
| East pedestal | `minecraft:gold_block` |
| West pedestal | `minecraft:gold_block` |
| South pedestal | `minecraft:soul_lantern` |
| North pedestal | `minecraft:soul_lantern` |

The existing `SovereignRitual` flow stays as is: immediate spawn at altar +1, duplicate check, messages. Worlds whose config file already exists keep every value the player changed; values still at their 2.x default (this ritual, and `max_health`, `armor`, `movement_speed`, `phase_two_percent`, `attack_cooldown_ticks`, `slam_damage`, `xp_reward` in `hollow_sovereign`) are rewritten once to the 3.0 defaults by `ConfigMigration`, which stamps each file with `config_version: 3`.

### 3.4 Presentation

- **Boss bar:** themed. The display name is `entity.remnants.hollow_sovereign`. `RemnantBossBars` theme 3 must accept that key as well as the legacy `entity.remnant_bosses.hollow_sovereign`. Colours: glow `0xFFD9B860`, shadow `0xFF2A3560`.
- **Music:** uses the existing boss-music packet with the existing theme, gated by `boss_music_enabled` *(cfg)*.
- **Sounds:** ambient `ENTITY_IRON_GOLEM_STEP` (rare), hurt `ENTITY_IRON_GOLEM_HURT` at pitch 0.6, death `ENTITY_WITHER_DEATH` at pitch 0.7.
- **Loot** (`remnants:entities/hollow_sovereign`):

  | Item | Count |
  |---|---|
  | `sovereigns_ruin` | 1 |
  | `hollow_crown` | 1 |
  | `sovereigns_sigil` | 1 |
  | `royal_soul_fragment` | 2–3 |
  | `minecraft:gold_ingot` | 4–9 |

## 4. Hollow Legion

All Legion mobs:
- are in the `#remnants:hollow_legion` entity-type tag and the undead tag;
- never damage each other;
- use iron-golem/skeleton-family vanilla sounds at lowered pitch;
- spawn only inside the Hollow Citadel, through structure `spawn_overrides` (no biome spawns).

| Mob | HP / armor / dmg / speed | XP | Behaviour | Loot |
|---|---|---|---|---|
| Hollow Sentinel | 30 / 8 / 7 / 0.23 | 8 | Tower shield. When not attacking, hits from the front 140° are reduced 60% (melee) or blocked (projectiles); axes disable the shield for 100 ticks. `bash`: 4 damage, knockback 1.0, reach 2.5. `strike`: 9 damage, reach 3. Cooldown 30. | `tarnished_regalia` 0–1 (looting +1), iron nugget 1–4 |
| Hollow Arbalist | 24 / 4 / 3 / 0.25 | 9 | Holds 8–14 blocks, backs off when closer than 6. `aim` (16-tick telegraph, crossbow raised, `CROSSBOW_LOADING_MIDDLE`), then fires one bolt (vanilla arrow, 8 damage, low spread), then `reload` 40. When cornered within 2.5 blocks: `kick` for 3 damage and knockback 1.2. | `tarnished_regalia` 0–1, arrows 0–3 |
| Fallen Standard Bearer | 34 / 6 / 6 / 0.24 | 14 | Stays 4–8 blocks behind the nearest ally. **Aura** (radius 10, refreshed every 40 ticks): Legion allies get Resistance I and Speed I for 60 ticks, shown by soul particle links. When hurt, at most once per 300 ticks: `rally` — plants the banner; at tick 30 allies in 10 blocks gain Strength I for 160 ticks. `sweep`: 6 damage arc, reach 3. | `tarnished_regalia` 1–2, `minecraft:white_banner` 0–1 |
| Soulbound Knight (elite) | 60 / 12 / 10 / 0.27 | 25 | `combo`: 10 / 10, 110° arc, reach 3.5. `lunge`: from 5–10 blocks, dash locked at tick 14, 12 damage. `counter`: 25% chance when struck; for 14 ticks melee is negated and answered with a 12-damage cut. Knockback resistance 0.6. | `tarnished_regalia` 1–3, `royal_soul_fragment` 0–1 (12%, looting +4% per level) |

## 5. Items

| Item | Type | Details |
|---|---|---|
| Tarnished Regalia | material | Stack 64. Tooltip: "Gilt scraps from the armor of Vael's legion." |
| Royal Soul Fragment | material | Stack 16, rare rarity, foil off. Repair material for Sovereign's Ruin and Regalia armor. |
| Sovereign's Seal | summon | Stack 16. Shaped `RGR / GEG / RGR` (R regalia, G gold ingot, E echo shard). Altar activation item. Tooltip names the pedestal offerings. |
| Sovereign's Ruin | greatsword | 11 attack damage, 0.8 attack speed, durability 1800, enchantable like a sword, repair with a royal soul fragment, fire resistant. **Charged slash:** hold use; after at least 15 ticks, release to fire one `spectral_crescent` (10 damage, pierces 3, range 20). 60-tick item cooldown; costs 3 durability. While charging: slowed like a bow, with soul particles at the blade. |
| Hollow Crown | helmet | Regalia set. Unique model on the armor layer: crown points rise above the helm. |
| Regalia Cuirass / Greaves / Sabatons | armor | Shaped recipes: the matching diamond piece in the centre, surrounded by tarnished regalia, with 1 royal soul fragment on top. |
| Sovereign's Sigil | relic | Off-hand relic. **Passive:** tamed allies within 16 blocks deal +10% damage. **Active (use):** summon two allied spectral royal guards for 400 ticks; costs 4 max health (attribute modifier `remnant_bosses:sigil_toll`) until they fade or die; cooldown 1800 ticks. Works only from the off-hand. In the main hand, use shows "The Sigil answers only an empty off-hand… hold it in your off-hand." |

### Regalia armor set

| Stat | Helmet | Chest | Legs | Boots |
|---|---|---|---|---|
| Defense | 3 | 8 | 6 | 3 |

Toughness 2.5 per piece. Knockback resistance 0.05 per piece. Enchantability 15. Durability multiplier 35. Equip sound: `ARMOR_EQUIP_NETHERITE`. Repair item: royal soul fragment.

Set bonuses, checked server-side each second:

- **2 pieces, Royal Bearing:** +0.1 knockback resistance.
- **4 pieces, Unbowed:** a hit dealing ≥ 6 damage grants Resistance I for 60 ticks. Cooldown 400 ticks. Gold particle burst.

The armor texture layers are `textures/models/armor/regalia_layer_1.png` / `_layer_2.png`. Hollow Crown points are painted into the helmet overlay region of layer 1.

## 6. Hollow Citadel (structure)

- **Generation:** datapack jigsaw structure `remnant_bosses:hollow_citadel`. Biome tag `#remnant_bosses:has_structure/hollow_citadel` covers the Eclipsed Realm's Tarnished Fields and Ashen Wastes ([realm.md](../realm.md); it originally covered overworld plains, meadow, taiga and similar). The throne hall door is sealed with fog gates (realm.md §7). Structure set spacing 44 / separation 18 / salt 714_309_221. Surface terrain adaptation `beard_thin`. Generates only in new chunks.
- **Layout** (about 48 × 48, 3 levels; all blocks vanilla plus the mod altar and pedestals):

  | Piece | Contents |
  |---|---|
  | Gatehouse | Broken portcullis (iron bars), two sentinel posts |
  | Courtyard | Collapsed fountain, banner poles, arbalist walkways on the walls |
  | Barracks wing | Bunks, weapon racks (armor stands with chain/iron), loot chest `remnant_bosses:chests/hollow_citadel_barracks` |
  | Royal vault | Hidden behind a breakable wall of cracked deepslate bricks; loot chest `remnant_bosses:chests/hollow_citadel_vault`, which can contain `sovereigns_seal` (20%), `tarnished_regalia`, gold, `royal_soul_fragment` (5%) |
  | Throne hall (arena) | 21 × 21 clear floor, 9-block ceiling. Throne at the north end. The Ancient Altar ring sits in the centre with pedestals already holding gold blocks and soul lanterns, so a Seal is all that's needed. |

- **Palette:** deepslate bricks/tiles, cracked and chiseled deepslate, polished blackstone, gold blocks used sparingly (throne, vault), blue/black wool and carpets for faded banners, soul lanterns, chains, cobwebs, moss. Weathering applied by processor lists (cracked, mossy, random air gaps on the upper walls).
- **Spawns:** `spawn_overrides` for `monster` in `full` bounding box:

  | Mob | Weight | Group size |
  |---|---|---|
  | hollow_sentinel | 10 | 1–2 |
  | hollow_arbalist | 6 | 1 |
  | fallen_standard_bearer | 2 | 1 |
  | soulbound_knight | 1 | 1 |

- **Fixed spawns:** placed with structure-block data or jigsaw-placed entities — 2 sentinels at the gatehouse, 1 knight in the throne hall antechamber.

## 7. Codex (Boss Hunts) text

- **Name:** "The Hollow Sovereign".
- **Clue:** "A ruined citadel under a faded blue banner. Its legion still guards the gate."
- **Defeated lore:** "The Last King of Vael bound his soul into his armor so the Oracle could not take it. His command is still being obeyed."
