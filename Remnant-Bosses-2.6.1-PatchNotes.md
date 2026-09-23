# Remnant Bosses v2.6.1

### Bug Fixes
* Fixed a Fabric **1.21.1** client crash on game start when custom boss bars were packaged. The common mixin config was missing its `refmap`, so Mixin could not resolve `BossHealthOverlay.render` on intermediary mappings (`No refMap loaded` / failed inject on `class_337`).

### Compatibility
* Minecraft 1.20.1: Fabric and Forge
* Minecraft 1.21.1: Fabric and NeoForge
* Requires GeckoLib and JAuml 2.1.1

### Upgrade Notes
1. Replace every loader jar with the matching **2.6.1** build.
2. Keep client and server mod versions aligned.
3. No configuration changes required.
