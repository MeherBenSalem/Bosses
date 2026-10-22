# Remnant Bosses v2.6.0

## The Hollow Sovereign

- Added the **Hollow Sovereign**, a towering bone monarch with a suspended crown and volatile soul core.
- Added a complete combat kit with claw combinations, ground slams, core blasts, shockwaves, a roar, and an enraged second phase.
- Added high-impact, synchronized visual effects and smooth authored animations for movement, attacks, phase changes, summoning, and death.
- Added a configurable Ancient Altar ritual. By default, use a Heart of the Sea with two Bone Blocks and two Skeleton Skulls on the four pedestals.
- Added dedicated balance and summon configuration files, loot, spawn egg, renderer, model, texture, and animation resources.

## Three New Monsters

- Added the **Grave Skitter**, a six-legged bone crawler with a telegraphed pounce.
- Added the **Sickleghast**, a scythe-armed ghoul with paired strikes and a wide frontal cleave.
- Added the **Dirge Lantern**, a flying ribcage spirit that marks an area before releasing a soul burst.
- Added spawn eggs, loot tables, combat animation sets, particle tells, and individual balance configuration for all three monsters.
- The new monsters are available through spawn eggs and summon commands; natural biome spawning is not included in this release.

## Boss Presentation

- Added custom themed boss bars for Remnant bosses on Fabric, Forge, and NeoForge clients.
- Added the client presentation configuration at `config/remnant/client/presentation.json`.
- `custom_boss_bars` enables or disables custom frames; `vfx_density` scales Hollow Sovereign and new-monster visual effects.

## Rituals and Configuration

- Added Hollow Sovereign ritual validation for peaceful difficulty, available spawn space, duplicate radius, altar layout, activation item, and all pedestal offerings.
- Offerings are consumed only after the boss successfully spawns. Creative players retain the activation item.
- Added safe defaults for the Hollow Sovereign, its ritual, all three new monsters, custom boss bars, and visual-effect density.
- Existing JAuml configuration values remain intact when the new defaults are created.

## Assets and Cleanup

- Removed the unused Marrowmaw model, texture, animation, and documentation assets from the packaged mod and codex.
- Updated the interactive documentation with the full playable bestiary and textured 3D ritual layouts showing default recipes and exact cardinal placement.

## Compatibility

- Minecraft 1.20.1: Fabric and Forge
- Minecraft 1.21.1: Fabric and NeoForge
- Requires GeckoLib and JAuml 2.1.1

## Upgrade Notes

1. Replace every loader jar with the matching **2.6.0** build.
2. Keep client and server mod versions aligned.
3. Start the game once to generate the new Hollow Sovereign, monster, and client presentation configuration files.
4. Existing boss IDs and existing configuration values remain compatible.
