# Remnant Bosses v2.2.0

### New Features
* Added **Umbrakar, Riftmaw Colossus**, a summonable remnant colossus with GeckoLib idle, walk, run, bite, slam, roar, tail-orb, and death animations.
* Added the **Umbrakar Orb**, a homing rift projectile spawned by Umbrakar's tail-orb attack.
* Added a new altar summon ritual for Umbrakar, separate from Ossukage.

### Improvements
* Shared MultiLoader content for Umbrakar on 1.20.1 (Forge + Fabric) and 1.21.1 (NeoForge + Fabric).

### Bug Fixes
* None.

### Configuration
* `remnant/bosses/umbrakar_summon` — activation item and pedestal blocks.
  * Default item: `minecraft:echo_shard`
  * Default pedestals: amethyst block, crying obsidian, end stone, sculk
* `remnant/bosses/umbrakar` — health, damage, armor, speed, phase 2, slam/bite/roar/orb values.

### Compatibility
* Minecraft 1.20.1 Forge and Fabric
* Minecraft 1.21.1 NeoForge and Fabric
* Requires GeckoLib and JAuml

### Upgrade Notes
1. Update every loader jar to 2.2.0. All workspaces share this version.
2. Place four pedestals around an Ancient Altar, put the configured blocks on top, and right-click the altar with an echo shard to summon Umbrakar.
3. Ossukage still uses a nether star and skeleton skulls.
