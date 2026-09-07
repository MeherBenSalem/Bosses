# Remnant Bosses v2.5.0

### New Features

* Reworked **Umbrakar, Riftmaw Colossus** with a layered blade mane, broken crown, fangs, shoulder armour, dorsal crest, and tail crown.
* Added the **Rift Claw** attack: a committed lane of five advancing rift eruptions for mid-range pressure.
* Rebuilt the roar as an expanding arena shockwave with readable timing and one damage event per victim.

### Combat and Animation Improvements

* Re-authored Umbrakar's idle, walk, run, bite, front slam, tail slam, orb cast, roar, Rift Claw, and death animations.
* Attack windups now lock Umbrakar's facing and navigation, so each strike has a clear commitment and a fair dodge window.
* Ground markers are fixed when an attack begins and damage checks now respect exact radius, line of sight, and allied entities.
* Tail slam is reserved for players positioned behind Umbrakar, while Rift Claw gives the boss a deliberate medium-range option.
* Umbrakar no longer starts phase two in the middle of another attack.
* Death now holds long enough for the authored collapse and releases a final rift burst before removal.
* Reduced ambient particle volume so the silhouette, telegraphs, and impact effects stay readable.
* Preserved Umbrakar's existing entity IDs, configuration keys, texture atlas, boss bar, orb projectile, drops, and spawn ritual.

### Technical

* The shared Umbrakar rig now contains 30 bones and 218 cubes in both workspaces.
* Animation curves are baked into explicit keyframes for consistent playback across GeckoLib 4.4.9 and 4.8.4.
* Added an offline inspection viewer at `docs/umbrakar-preview.html` with orbit controls and an animation pose scrubber.
* Added asset validation and regeneration helpers under `tools/`.

### Compatibility

* Minecraft 1.20.1 Forge and Fabric
* Minecraft 1.21.1 NeoForge and Fabric
* Requires GeckoLib and JAuml 2.1.1

### Upgrade Notes

1. Update every loader jar to 2.5.0. Both workspaces share this version.
2. Existing Umbrakar summon IDs and configuration files remain compatible.
3. The Rift Claw and roar use the existing damage configuration families; tune `slam_damage` and `roar_damage` if needed.
4. Test the ground telegraphs on uneven terrain and around walls after upgrading.
