# Remnant Bosses v2.4.0

### New Features
* Added **Kotsukage, Bone Sovereign**, a summonable skeleton boss using the Amonde GeckoLib model, texture, and animations.
* Kotsukage uses idle, walk, left/right swipes, left/right stomps, poison breath, roar, and death animations.
* Roar plants **bone traps** (trap VFX model) that rise, damage, and slow players, and summons remnant skeleton minions.
* Skeleton minions now poison on hit, leap at distant players, and can plant bone traps.
* Added a unique Ancient Altar ritual for Kotsukage.

### Improvements
* Entity types now register under the `remnants` namespace with short names, e.g. `/summon remnants:ossukage`.
* Merged onto the Oni Shogun / GeckoLib Ossukage encounter from `origin/main`.
* Ossukage configs now include armor, follow range, and knockback resistance, and those values apply on spawn.

### Bug Fixes
* Entity IDs no longer use the long `remnant_bosses:remnant_ossukage` form as the primary name.

### Configuration
* `remnant/bosses/kotsukage_summon` — activation item and pedestal blocks.
  * Default item: `minecraft:wither_skeleton_skull`
  * Default pedestals: bone block, soul sand, soul soil, nether wart block
* `remnant/bosses/kotsukage` — health, damage, armor, speed, phase 2, swipe/stomp/poison/roar/trap/minion values, boss music.
* `remnant/balance/skeleton_minion_stats` — minion health, damage, speed, armor, poison, leap, and trap chance.
* `remnant/bosses/ossukage` — added `armor_phase_1`, `armor_phase_2`, `follow_range`, `knockback_resistance`.

### Compatibility
* Minecraft 1.20.1 Forge and Fabric
* Minecraft 1.21.1 NeoForge and Fabric
* Requires GeckoLib and JAuml 2.1.1
* Old entity IDs (`remnant_bosses:remnant_ossukage`, `remnant_bosses:rat`, …) are no longer the primary names. Use `remnants:*`.

### Upgrade Notes
1. Update every loader jar to 2.4.0. Both workspaces share this version.
2. Summon IDs are now `remnants:ossukage`, `remnants:kotsukage`, `remnants:umbrakar`, `remnants:rat`, and so on.
3. Place four pedestals around an Ancient Altar with bone block, soul sand, soul soil, and nether wart block, then right-click the altar with a wither skeleton skull to summon Kotsukage.
4. Ossukage still uses a nether star and skeleton skulls. Umbrakar still uses an echo shard.
