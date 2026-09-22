# Hollow Sovereign encounter

The Hollow Sovereign is registered as `remnants:hollow_sovereign` on Minecraft 1.20.1 (Fabric/Forge) and 1.21.1 (Fabric/NeoForge). Its spawn egg is available in the mod creative tab as `remnant_bosses:hollow_sovereign_spawn_egg`.

For a test world with commands enabled:

```mcfunction
/summon remnants:hollow_sovereign ~ ~ ~
```

The 106-cube bone model uses Kotsukage's unchanged texture atlas and the nine polished animation clips. A single full-body animation controller prioritizes death, then spells, then movement. Damage runs on the server; clients generate the dense soul-flame, green dust, and white spark patterns from synchronized attack age, target, phase, and wave radius. These are vanilla-compatible particles, not a shader-pack requirement.

## Fight

| Ability | Telegraph and effect | Damage timing |
| --- | --- | --- |
| Awakening | Four rotating soul halos and a six-block soul seal; 3-second entrance | No entrance damage |
| Twin Reaping | Two sweeping claw arcs, body twist and wrist follow-through | 0.55 s and 1.15 s |
| Grave Tide | Hands raised; converging light at the core; expanding double soul shockwave | Starts at 1.05 s, once per victim per wave |
| Hollow Judgment | Fixed aim line and charging core halo; green/white core beam wrapped in a soul-fire helix | 1.45 s; solid blocks stop the beam |
| Crown's Edict | Rotating five-point seal marks the target's original position, with a shrinking countdown ring; erupts into a spiraling soul column | 1.4 s; move outside the marked radius |
| Unbound Crown | At 45% health: rotating halos, soul seal and a close-range phase-transition pulse | 2 s into transition |
| Death | 3.5-second collapse and contracting core particles | No death damage |

Phase two increases damage by 30%, pursuit speed by 20%, and the eruption radius from 3.5 to 5 blocks. Attack selection requires a living target, line of sight, and configured spell range. Hits exclude creative/spectator players, allies, and other Sovereigns. Spells do not destroy terrain. The boss does not naturally despawn. Phase state survives saving; an interrupted attack is cancelled on reload instead of replaying its impact.

Default rewards: 150 XP, 4–8 Echo Shards, and 12–24 bones, subject to Minecraft's normal loot/XP rules.

## Summoning ritual

1. Place an **Ancient Altar** with enough clear space above it for a 1.8-block-wide, 4-block-tall entity.
2. Place four **Ancient Pedestals** at the same height, exactly three blocks east, west, north, and south of the altar.
3. Put **bone blocks** on the east and west pedestals, and **skeleton skulls** on the north and south pedestals.
4. Right-click the altar with a **Heart of the Sea** in your main hand.

```text
               Skull
             Pedestal
                 |
            (3 blocks)
                 |
Bone / Pedestal — Altar — Pedestal / Bone
                 |
            (3 blocks)
                 |
             Pedestal
               Skull
```

The four topper blocks and one activation item are consumed only after the spawn succeeds; creative players retain their item. The altar and pedestals remain. Peaceful difficulty, disabled summoning, blocked space, invalid configured blocks, and another Sovereign within 64 blocks reject the ritual with a message. It uses no delayed global task, so repeated clicks cannot queue duplicate spawns. The Heart of the Sea is distinct from Umbrakar's Echo Shard activation item.

## Configuration

JAUML generates these files in the instance's `config` directory; restart after editing. Missing values receive defaults without overwriting existing values. Without JAUML, built-in defaults apply.

| File | Controls |
| --- | --- |
| `remnant/bosses/hollow_sovereign.json` | Health, attack damage, armor, speed, phase threshold/multiplier, cooldown, individual spell damage, range/radius, XP, summoning toggle and duplicate radius |
| `remnant/bosses/hollow_sovereign_summon.json` | Activation item and four pedestal topper block IDs |
| `remnant/client/presentation.json` | `custom_boss_bars` (1/0), `vfx_density` (0–1; 0 disables decorative and telegraph particles) |

Copies of the generated default JSON files are included beside this guide. Numeric values are bounded to prevent invalid ranges and runaway particle counts. The client density control affects presentation, never server damage.

## Custom boss bars

All four boss types receive original procedural pixel ornaments: crimson Ossukage, gold/bone Kotsukage, purple rift-eye Umbrakar, and green crowned-skull Sovereign. Health fills use the server's interpolated boss progress. Name matching uses translation keys, not localized display strings. Ordinary vanilla bars keep the vanilla renderer when no custom mod boss is present. Mixed encounters retain vanilla bar drawing with adjusted row spacing. Custom rows are bounded to the upper half of the screen.

`bossbars.png` is a layout preview rendered from the actual drawing code using a Java graphics adapter; its title font is a preview substitute. The bars are inspired by the supplied visual direction; no pixels from the reference image are copied.

Other mods that replace the entire boss overlay may conflict. Set `custom_boss_bars` to 0 to restore the vanilla overlay path.

Forge also registers `CustomizeGuiOverlayEvent.BossEventProgress` on its client event bus. This ensures the custom bars render in Forge source-set development launches where the jar-manifest mixin configuration may not be discovered. The event cancels the recognized vanilla bar and supplies the 36-pixel row increment; unknown bosses are untouched.

## Validation

- Built all four loader targets; inspected the packaged resources and Forge Mixin refmap.
- 1.21.1 Fabric client startup/resource-load smoke check and generated-config readback.
- 1.20.1 Forge client startup and integrated-world load, with the mapped HUD mixin active.
- Structural checks cover rig references, animation names and endpoints, resource paths, and identical assets across versions.
- The boss-bar Java preview exercises each mod theme and leaves the vanilla Wither unidentified.

Live combat balance, multiplayer latency, terrain edge cases, and low-end GPU performance still need in-world playtesting. A build or menu startup is not a completed fight test.

Suggested playtest: trigger each attack in survival, dodge the marked seal, place a wall across the beam, cross the wave once, force phase two, save/reload below the threshold, complete the death animation, try invalid and duplicate rituals, and compare mixed vanilla/mod bars at several GUI scales. Check dedicated-server startup and multiple clients before release.
