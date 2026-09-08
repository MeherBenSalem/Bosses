# Remnant Bosses v2.5.1

### Bug Fixes
* Restored a clean `skeleton_fight.ogg` (Ossukage/boss fight theme). The 2.4.0+ packaged stream had corrupt Ogg Vorbis page CRCs and threw `IllegalStateException: Corrupt or missing data in bitstream` / failed stream reads on playback ([#1](https://github.com/MeherBenSalem/Bosses/issues/1)).

### Compatibility
* Minecraft 1.20.1 Forge and Fabric
* Minecraft 1.21.1 NeoForge and Fabric
* Requires GeckoLib and JAuml 2.1.1

### Upgrade Notes
1. Update every loader jar to **2.5.1**. Both workspaces share this version.
2. No config changes required.
3. If you used a resource-pack override of `skeleton_fight.ogg` as a workaround, you can remove it after upgrading.
