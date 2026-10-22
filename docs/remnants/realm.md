# The Eclipsed Realm

Remnants 3.0 moves its world into a dimension of its own. Every boss, creature family and arena
from the design bible lives in **the Eclipsed Realm**, an open world under a sun that never came
back. The overworld keeps only the way in: a **Waygate** that the player builds and kindles.

This file is the implementation contract for the dimension, its blocks, the entry mechanic and
the Elden-Ring-style systems that sit on top (graces, fog gates, banners). It follows the
conventions of [design-bible.md](design-bible.md): damage in half-hearts, time in ticks, every
value marked *(cfg)* is a Jauml default under `config/remnant/realm/`.

Namespaces: blocks, items, sounds, particles, worldgen and the dimension use `remnant_bosses:`;
entity types use `remnants:`.

## 1. Lore

On the night of the Last Eclipse the Oracle called every soul to itself. When Vael's champion
shattered it mid-ritual, the call did not stop; it *tore*. Everything that had answered as a
vessel (knights bound in their armor, catacombs full of caught souls, furnaces, crystals) fell
with the Oracle into the instant of the eclipse. That instant is the Eclipsed Realm: the old
kingdoms, still standing, under a black sun ringed with gold.

The souls the Oracle dropped rose as light and caught in the branches of the Sanctum's sacred
tree. It has burned gold on the horizon ever since: **the Gracetree**. Each **Grace Ember** in
the Realm is one of its embers. Graces guide those who still have their own soul, the
*Unhollowed*, toward what remains.

In the overworld only the Waygates remember the way: old arches of carved stone that open onto
the eclipse when an **Eclipse Shard** is set into them.

## 2. Player journey

1. Craft an **Eclipse Shard** (amethyst shard + ender pearl + gold ingot) and a **Waygate**.
2. Place the Waygate: it raises a 3-wide, 4-tall stone arch (dormant: grey, runes dark).
3. Use an Eclipse Shard on it. The arch **kindles** for 6 seconds: runes light one by one, the
   rings spin up, pillars of soul light rise, the ground shakes. Then it **opens** with a flash:
   a swirling eclipse fills the arch. It stays open.
4. Walk into the arch. The **crossing** takes 2 seconds: you rise, the screen closes into a
   golden eclipse ring, and you arrive in the Realm at an **Arrival Shrine** with its own open
   Waygate (the way back) and a Grace Ember. The area title fades in:
   **THE ECLIPSED REALM**, with the biome name underneath.
5. Touch the Grace (**LOST GRACE DISCOVERED**). Resting at a grace heals you, clears harmful
   effects, sets your respawn point there and shows the **guidance of grace**: a trail of golden
   motes toward the nearest Remnant arena. From a grace you can travel to any grace you have
   discovered, or depart the Realm.
6. Explore. Biomes host the creature families; arenas sit behind **fog gates**. A felled boss
   shows **REMNANT FELLED**.
7. Walk back through the shrine's Waygate (or *Depart* from any grace) to return to the
   overworld Waygate you came from.

## 3. The dimension

| Field | Value |
|---|---|
| Dimension ID | `remnant_bosses:eclipsed_realm` |
| Dimension type | `remnant_bosses:eclipsed_realm` |
| Effects ID (client sky) | `remnant_bosses:eclipsed_realm` |
| Height | min_y −64, height 384, logical height 384 (catacombs sit at Y −20 to 20) |
| Time | `fixed_time` 12000: a permanent golden dusk. The client sky brightness is about 0.7 |
| Skylight / ceiling | yes / no |
| Beds / respawn anchors | do not work (explode). Rest at a grace instead |
| Building | none: the Realm does not change (section 3, *Protection*) |
| `natural` | false (compasses and clocks spin) |
| Ambient light | 0.0 |
| Monster spawn light | 0–7; Remnant mobs use their own rule in the Realm (section 8) |
| Infiniburn | `#minecraft:infiniburn_overworld` |
| Generator | `minecraft:noise` with `remnant_bosses:eclipsed_realm` settings, multi-noise biome source |

### Protection

The Realm does not change. Survival and adventure players there cannot:

- break blocks (the adventure-mode check, on both client and server, so blocks do not even crack);
- place blocks or use items on blocks (flint and steel, hoes, bone meal, and so on). Items in
  `#remnant_bosses:realm_usable` (empty by default) are let through;
- empty or fill buckets;
- damage terrain with explosions, from any source. Entities are still hurt.

Blocks can still be *used*: graces, Waygates, fog gates, doors, levers and chests work as usual. A
refused action shows *"The Realm will not change."* on the action bar (at most every two
seconds). Creative and spectator players are exempt. Config `remnant/realm/realm` →
`build_protection` (1 = on, the default; 0 = off).

Code: `realm/RealmProtection`, `mixin/RealmBuildMixin` (`Player.blockActionRestricted`),
`RealmItemUseMixin` (`ItemStack.useOn`), `RealmBucketMixin` (`BucketItem.use`) and
`RealmExplosionMixin` (`Explosion.interactsWithBlocks`).

### Terrain

Noise settings start from the vanilla overworld router (continents, erosion, rivers, caves,
aquifers), so the Realm has overworld-scale landforms. Changes:

- **Default block:** `remnant_bosses:eclipse_stone` (deepslate below Y 0, as vanilla).
- **Surface rule:** grass block (tinted per biome) over dirt over eclipse stone in the fields and
  the Gracewood; coarse dirt, gravel and eclipse stone patches in the Ashen Wastes; podzol and
  rooted dirt in the Gravefield Barrows; sand and gravel at the shores of the Stilled Sea.
- Vanilla ores generate in eclipse stone (it joins `#minecraft:stone_ore_replaceables`).

### Biomes

| Biome | Climate slot | Look | Spawns (monster) | Structures |
|---|---|---|---|---|
| `tarnished_fields` | mid continent, temperate, low weirdness | Pale gold grass, lone graceroot trees, broken pillars, gravestones | Hollow Legion; a few Shardlings, Ancient Vessels and Rusted Guardians | Hollow Citadel, grace shrines |
| `gracewood` | humid | Dense graceroot forest, glowing gilded canopy, grace motes in the air | few; Catacomb Rats, Crypt Lurkers, Lost Pilgrims, Shardling groups | grace shrines |
| `gravefield_barrows` | cold, dry | Grey-green grass, gravestone fields, bone piles, dead trees, ash | Cryptborn; Relic Stalkers, Ancient Vessels | grace shrines |
| `ashen_wastes` | hot, dry, high erosion | Grey ash and gravel, eclipse stone outcrops, dead trees, falling ash | Legion elites, Gravebound Soldiers, Ashbound Constructs | Hollow Citadel, grace shrines |
| `lantern_heights` | inland mountains: continentalness ≥ 0.03, erosion < −0.375, any temperature and humidity | Pale calcite cliffs, sage grass, violet amethyst outcrops, broken sandstone stargazing pillars | Forgotten Souls | Forgotten Observatory, grace shrines |
| `stilled_sea` | ocean continentalness | Dark still water, sand and gravel shores | none (squid, cod) | — |
| `ossuary_depths` | underground (depth 0.2–0.9) | Cave biome: bone and cobweb clutter | Cryptborn; Ashbound Crawlers, Relic Stalkers, Ancient Vessels | Gravebound Catacombs (in every land biome) |

The Ashbound Constructs and the Corrupted Relics roam ahead of their boss slices ([families/ashbound-and-relics.md](families/ashbound-and-relics.md) section 7 has their weights).
The Lantern Heights arrived with slice 4 ([slices/04-forgotten-watcher.md](slices/04-forgotten-watcher.md), section 6).
All land biomes are in the tag `#remnant_bosses:is_eclipsed_realm`; all seven in
`#remnant_bosses:in_eclipsed_realm`. Biome effects use dusk-friendly colours (fog brighter than it
looks, because the dusk fog multiplier darkens it). No precipitation anywhere.

### Sky (client)

Drawn by `EclipseSkyRenderer`, registered per loader:

- A gradient dome: slate blue at the zenith to ash gold at the horizon.
- **The Eclipse:** a black disc with a pulsing gold-and-cyan corona, fixed high in the southern
  sky (`textures/environment/eclipse.png`).
- **The Gracetree:** a huge golden tree on the northern horizon, always visible, gently
  shimmering (`textures/environment/gracetree.png`).
- Sparse faint stars. No clouds.

## 4. Blocks

| ID | Kind | Notes |
|---|---|---|
| `eclipse_stone` | stone | The Realm's stone. Drops `cobbled_eclipse_stone` (silk touch: itself). Hardness 1.5 |
| `cobbled_eclipse_stone` | stone | Smelts into `eclipse_stone` |
| `eclipse_stone_bricks` | stone | |
| `cracked_eclipse_stone_bricks` | stone | Smelt bricks |
| `chiseled_eclipse_stone_bricks` | stone | Gold eclipse rune. From two slabs |
| `eclipse_stone_brick_stairs`, `_slab`, `_wall` | stone | Crafting and stonecutter |
| `graceroot_log` | log (axis) | Pale bark with gold veins |
| `graceroot_planks` | planks | 4 per log |
| `gilded_leaves` | leaves | Golden, light 6, cutout mipped, untinted. Drops sticks (shears/silk: itself) |
| `weathered_gravestone` | decoration (horizontal facing) | Small headstone model, no full collision |
| `grace_ember` | mechanic | Site of grace. Light 13, unbreakable in survival, cutout model plus a glow renderer (block entity `remnant_bosses:grace_ember`). Section 6 |
| `eclipse_waygate` | mechanic, block entity | The Waygate core. Section 5 |
| `waygate_frame` | mechanic, invisible | Pillar and lintel filler; full collision; no item |
| `waygate_veil` | mechanic, invisible | Doorway filler; no collision; no item |
| `fog_gate` | mechanic | Translucent animated fog. Players pass, mobs do not. Unbreakable in survival. Section 7 |

Blockstate properties:

- `eclipse_waygate`: `facing` (north/south/west/east), `phase` (`dormant`, `kindling`, `open`).
  Light 4 / 10 / 15.
- `waygate_frame`: none. `waygate_veil`: `open` (bool; light 12 when true).
- `weathered_gravestone`: `facing`.
- `graceroot_log`: `axis`. Stairs, slab and wall: vanilla properties.

Render layers: `gilded_leaves` cutout mipped; `grace_ember`, `weathered_gravestone` cutout;
`fog_gate` translucent. Forge and NeoForge read `render_type` from the block model JSON; Fabric
registers them in code.

## 5. The Waygate

### Shape

The item places a 3 × 4 arch, centred on the clicked position, its front facing the player.
`right` is `facing.getClockWise()`.

```
 y=3  F F F        F = waygate_frame (collision)
 y=2  F V F        V = waygate_veil (no collision)
 y=1  F V F        C = eclipse_waygate core (block entity; 3 px plinth collision)
 y=0  F C F
     -1 0 +1   (along `right`)
```

All 12 positions must be air or replaceable, or placement fails with a message. Breaking any
part removes the whole arch; the core drops the Waygate item (and the shard is lost).

### Phases

| Phase | Enter | Visual (GeckoLib clip) | Sound |
|---|---|---|---|
| `dormant` | placed | `dormant` loop: grey, faint drifting motes | — |
| `kindling` | use an Eclipse Shard on any part (consumed) | `kindle`, 120 ticks: runes light bottom to top, the rings spin up, the lintel eclipse cracks with light; particle pillars rise at both pillars; motes spiral inward; nearby cameras shake | `waygate_kindle` |
| `open` | kindle ends | `open` loop: rings turn, the vortex swirls, the eclipse disc glows; a flash for players within 24 blocks | `waygate_open`, then `waygate_hum` every 80 ticks |

Waygates generated in the Realm (Arrival Shrines) are placed already `open`.

### Crossing

- Trigger: a `ServerPlayer` whose box touches the core or a veil while the phase is `open`, not
  riding, not already crossing, and not on cooldown (100 ticks after any crossing).
- Duration 40 ticks. The player gets Levitation 0 and Resistance 4 for 40 ticks, Darkness for
  50 ticks; the client overlay closes the screen into a gold eclipse ring (`waygate_cross`).
- At tick 40 the server teleports. Then Slow Falling for 100 ticks; the client fades back in and
  shows the area title.

### Links

`RealmLinks` (saved data on the overworld) pairs each overworld Waygate core with a Realm
Waygate core.

- **Overworld → Realm:** use the linked Realm Waygate if it still exists. Otherwise reuse any
  Realm Waygate within 48 blocks of the same X/Z. Otherwise build an **Arrival Shrine** at that
  X/Z on the Realm surface (chunk generated on demand) and link it.
- **Realm → overworld:** to the linked overworld Waygate if it still exists. Otherwise to the
  player's overworld respawn point, or the world spawn.
- The player always lands 2 blocks in front of the destination Waygate, facing away from it.
- Each player's last overworld Waygate is stored for *Depart*.

### Arrival Shrine

Template `remnant_bosses:realm/arrival_shrine` (11 × 9 × 11, generated by
`tools/remnants/structures/`). Local coordinates:

| What | Local position |
|---|---|
| Floor level | y = 1 (y = 0 is the foundation layer) |
| Waygate core (`facing=south`, `phase=open`), with frame and veil | (5, 1, 2) |
| Grace Ember | (5, 1, 7) |
| Player landing spot (facing south) | (5, 1, 4) |

Java places the template with local (5, 1, 5) on the chosen surface point and fills any gap under
the 11 × 11 foundation with eclipse stone (up to 16 blocks deep). If the template is missing, a
programmatic platform is built instead.

## 6. Grace Embers

- Look: a low ring of seven weathered standing stones, one fallen, around a cairn on an ember bed
  of glowing ash (block model, 5 px tall; the ember bed renders full-bright). Above it a block-entity renderer
  (`GraceEmberRenderer`) draws additive light: a pulsing golden halo on the ground, a living flame
  (8-frame strip), a bright core, four orbiting motes and a thin beam of light rising about 7
  blocks, visible from afar. Golden motes drift up from it (`grace_mote` particle). Textures
  `textures/block/grace_ember_{stone,bed}.png`, effects `textures/effect/grace_{halo,core,beam,flame}.png`,
  all from `tools/remnants/realm/grace.py`. Graces saved before the renderer existed have no block
  entity: the server adds it on a random tick (saved with the chunk), and a client adds its own and redraws the
  section when the grace's animation ticks nearby or when the player rests there
  (`GraceEmberBlock.ensureBlockEntity`).
- Placed by Arrival Shrines and **grace shrines** (a small worldgen ruin, section 9). Creative
  players can place them.
- **Use (right-click):**
  - First use by a player: **LOST GRACE DISCOVERED** banner and `grace_discover`.
  - Rest: full heal, fire out, harmful effects cleared, respawn point set here (forced), sound
    `grace_rest`.
  - Guidance of grace: a trail of golden motes from the grace toward the nearest Remnant arena
    (Hollow Citadel, Gravebound Catacombs or Forgotten Observatory) within 64 chunks (search on
    demand, once per rest).
  - Opens the **Grace menu**: rest done; travel to any discovered grace (crossing-style fade);
    *Depart the Realm* (to the player's last overworld Waygate). The current grace is listed first
    (*You are here*), then the others nearest first, each with its distance in meters
    (*1,284 m*); graces in another dimension show *Another world*. Seven per page.
- A name tag used on a grace renames it. Default name: the biome name, numbered when repeated.
- `GraceRegistry` (saved data on the Realm level) stores every grace that has been used and each
  player's discovered set. A grace that no longer exists is dropped from the list on access.

## 7. Fog gates

`fog_gate` fills arena doorways in Realm structures. It renders as translucent swirling gold-white
fog with drifting motes. It has collision for every entity except players, so bosses and their
summons stay in their arena. A player walking in hears `fog_gate_pass` and is slowed briefly.

## 8. Spawning

- Remnant creatures spawn in the Realm from the biome spawners above. They never spawn naturally
  in the overworld (the old rat and wraith overworld spawns go away with their mobs).
- In the Realm the light check is replaced: a spawn needs block light ≤ 7 (graces make safe
  ground; players cannot place torches there, see section 3) and passes a 40% thinning roll *(cfg)*. Biomes use `spawn_costs` to keep
  patrols sparse.
- Arena structures keep their `spawn_overrides`.
- Rituals: the Hollow Sovereign, Gravebound Warden and Forgotten Watcher rituals only work in the
  Realm *(cfg: `rituals_realm_only`, default true)*. Older bosses are unaffected until their slices land.

## 9. Structures in the Realm

| Structure | Biomes | Placement |
|---|---|---|
| Hollow Citadel | `tarnished_fields`, `ashen_wastes` | as before, Realm only |
| Gravebound Catacombs | every Realm land biome, underground | as before, Realm only |
| Forgotten Observatory | `lantern_heights` | spacing 28, separation 10; surface, on a raised plinth (slice 4) |
| `grace_shrine` | every Realm land biome | spacing 24, separation 8; a 7 × 7 ruin of broken pillars around a Grace Ember |
| Arrival Shrine | — | placed by the Waygate (section 5) |

Arena doorways (Citadel throne hall, Catacombs chapel, Observatory dome) get fog gates. Every arena is in
`#remnant_bosses:remnant_arenas`, which the guidance of grace searches.

## 10. Banners and titles (client overlay)

Drawn by `RealmOverlay` over the HUD, in the style of a cinematic title: a dark translucent band
across the screen, large spaced gold capitals, thin gold rules fading at both ends.

| Banner | When | Colour |
|---|---|---|
| Area title (large: realm name; small: biome) | arriving through a Waygate | gold |
| Region title (small) | entering a Realm biome for the first time in 10 minutes | pale gold |
| LOST GRACE DISCOVERED | first use of a grace | gold |
| REMNANT FELLED | a boss dies within 64 blocks | gold, with a flash |
| GRACE RESTORED | resting | soft gold, short |

The crossing overlay (section 5) and the open flash use the same renderer.

## 11. Assets

| Kind | Files |
|---|---|
| Block textures (16 px) | `textures/block/<id>.png` for every cube block; `graceroot_log_top`, `chiseled_eclipse_stone_bricks`; `fog_gate` (animated strip + `.mcmeta`); `grace_ember_stone`, `grace_ember_bed`; `weathered_gravestone` |
| Waygate | `geo/block/eclipse_waygate.geo.json`, `animations/block/eclipse_waygate.animation.json`, `textures/block/eclipse_waygate.png`, `textures/block/eclipse_waygate_glowmask.png` |
| Waygate bones | `root`, `pillar_left`, `pillar_right`, `lintel`, `eclipse_disc`, `ring_outer`, `ring_inner`, `runes_left`, `runes_right`, `runes_lintel`, `vortex` (`vortex_a`, `vortex_b`, `vortex_c`), `shards` |
| Waygate clips | `dormant` (loop), `kindle` (6.0 s, hold last frame), `open` (loop) |
| Item icons | `eclipse_shard`, `eclipse_waygate`, `grace_ember`, `fog_gate` |
| Particles | `particles/grace_mote.json`, `particles/eclipse_spark.json`, frames in `textures/particle/` |
| Grace effects | `textures/effect/grace_halo.png` (64²), `grace_core.png` (32²), `grace_beam.png` (16×128), `grace_flame.png` (8 frames, 256×64); premultiplied on black, drawn additively |
| Sky | `textures/environment/eclipse.png` (128²), `textures/environment/gracetree.png` (256², alpha) |
| Sounds | `waygate_kindle`, `waygate_open`, `waygate_hum`, `waygate_cross`, `grace_discover`, `grace_rest`, `remnant_felled`, `fog_gate_pass` |

The waygate geometry is 3 blocks wide and 4 tall (48 × 64 px), its origin at the core block's
bottom centre, front toward −Z in model space (the renderer rotates by `facing`).

## 12. Code map

| Area | Classes (common unless noted) |
|---|---|
| Registration | `init/ModBlocks`, `init/ModItems`, `init/ModBlockEntities`, `init/ModParticles`, `init/ModSounds` |
| Blocks | `block/realm/*`: `EclipseWaygateBlock`, `WaygateFrameBlock`, `WaygateVeilBlock`, `GraceEmberBlock`, `FogGateBlock`, `GravestoneBlock` |
| Block entity | `block/realm/EclipseWaygateBlockEntity` (GeckoLib) |
| Travel | `realm/RealmTravel` (crossing state, teleport), `realm/RealmLinks`, `realm/ArrivalShrine`, `realm/GraceRegistry`, `realm/GraceGuidance`, `realm/EclipsedRealm` (keys, helpers) |
| Network | `network/ClientboundRealmFxPacket`, `ClientboundGraceMenuPacket`, `ServerboundGraceActionPacket` |
| Protection | `realm/RealmProtection`, `mixin/RealmBuildMixin`, `RealmItemUseMixin`, `RealmBucketMixin`, `RealmExplosionMixin` |
| Client | `client/realm/RealmOverlay`, `ScreenShake` (+ `mixin/CameraShakeMixin`), `GraceScreen`, `GraceEmberRenderer`, `EclipseSkyRenderer`, `EclipsedRealmEffects`, `WaygateRenderer`, particle providers |
| Loader glue | sky/effects registration, render layers, particle providers, HUD hook, packet wiring |

## 13. Tooling and verification

| What | Where |
|---|---|
| Realm worldgen JSON (dimension, noise, biomes, features, tags, loot, recipes) | `tools/remnants/structures/realm_data.py` (writes both trees; `--dry-run`) |
| Grace shrine and Arrival Shrine templates | `tools/remnants/structures/grace_shrine.py`, `arrival_shrine.py` (`--install`, `--preview`) |
| Static data validation (biome climate coverage, IDs, formats, shrine contract) | `python tools/remnants/realm/validate_data.py` |
| Art: block textures, models, grace, Waygate GeckoLib model, particles, sky, sounds | `python tools/remnants/realm/build_assets.py`; review images in `docs/models/remnants/realm/` |
| In-game walkthrough (build, kindle, cross, rest, grace menu distances, protection, tour the biomes, depart; on 1.21.1 also the Ashbound and Relic creature showcase, [families/ashbound-and-relics.md](families/ashbound-and-relics.md) section 9) with screenshots | `tools/realm-runtime/`; run `gradlew :fabric:runRealmRuntimeTestClient -PrealmRuntimeTest` in `1.21.1/` or `1.20.1/` (needs a disposable world named `RealmValidation` on 1.20.1; 1.21.1 creates it) |

Implementation notes that refine the sections above:

- Biome music: Tarnished Fields old-growth taiga, Gracewood cherry grove, Barrows swamp, Wastes soul sand valley, Lantern Heights jagged peaks, Stilled Sea frozen peaks, Ossuary Depths deep dark.
- Vanilla springs, glow lichen and lava lakes are copied into `remnant_bosses:` so they accept eclipse stone; granite, diorite and andesite blobs are left out to keep the Realm's stone readable. Fossils appear in the Ossuary Depths.
- The Waygate's vortex discs sit in the visible opening (centred 23.5 px up), behind two recessed rings; the controller uses no transition, because the kindle clip ends where the open loop begins.
