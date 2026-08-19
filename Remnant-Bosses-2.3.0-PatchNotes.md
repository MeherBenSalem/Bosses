# Remnant Bosses v2.3.0

### New Features
* Converted the project to NightBeam MultiLoader: 1.20.1 Forge + Fabric, and 1.21.1 NeoForge + Fabric. There is no Forge build for 1.21.1.
* Added **Umbrakar, Riftmaw Colossus**, a summonable remnant boss with GeckoLib idle, walk, run, bite, slam, roar, tail-orb, and death animations.
* Added the **Umbrakar Orb**, a homing rift projectile from Umbrakar's tail-orb attack.
* Added a separate Ancient Altar ritual for Umbrakar (echo shard). Ossukage still uses a nether star and skeleton skulls.

### Improvements
* Re-exported Umbrakar from Blockbench as a GeckoLib model so geo UVs and animation keyframes load on GeckoLib 4.4.9 and 4.8.4.
* Smoothed Umbrakar and orb animations (Catmull-Rom, denser keyframes).
* Added ambient, walk, attack, roar, phase 2, and orb particle bursts.
* Umbrakar now chases the player instead of standing still when the player looks in range of the old tiny box.
* Hitbox enlarged to 4.2 × 3.6 to cover the body, with a larger shadow and cull box for the tail.
* Shared version across both workspaces.

### Bug Fixes
* Fixed GeckoLib crash on load from missing cube UVs and duplicate start keyframes in the first Umbrakar export.
* Fixed Fabric Umbrakar not walking: melee AI treated the player as already in range because the collision box did not match the long model.
* Fixed 1.21.1 world-save crash when a kunai was in the world (`Cannot encode empty ItemStack`).
* Completed Umbrakar config keys on 1.21.1 so movement speed and combat values always exist.

### Configuration
* `remnant/bosses/umbrakar_summon` — activation item and pedestal blocks.
  * Default item: `minecraft:echo_shard`
  * Default pedestals: amethyst block, crying obsidian, end stone, sculk
* `remnant/bosses/umbrakar` — health, damage, armor, speed, phase 2, slam/bite/roar/orb values.
  * Default movement speed: `0.32` (phase 2: `0.38`)

### Compatibility
* Minecraft 1.20.1 Forge and Fabric
* Minecraft 1.21.1 NeoForge and Fabric
* Requires GeckoLib and JAuml 2.1.1

### Upgrade Notes
1. Update every loader jar to 2.3.0. Both workspaces share this version.
2. Respawn Umbrakar after updating so the new hitbox and chase AI apply.
3. Place four pedestals around an Ancient Altar, put the configured blocks on top, and right-click the altar with an echo shard to summon Umbrakar.
4. Ossukage still uses a nether star and skeleton skulls.
