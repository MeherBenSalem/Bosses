# Hollow Sovereign

Original boss model authored through the running Blockbench MCP plugin, using the GeckoLib model format. The editable source is `hollow_sovereign.bbmodel`, with its texture embedded. Separate geometry, animation, and PNG exports are included here and copied into both Minecraft version resource trees.

106 cubes, 24 bones, 512 × 512 atlas. The PNG is a byte-for-byte copy of the mod's existing Kotsukage texture in both supported versions. Per-face UVs reuse its opaque, authored bone patches, with existing green pixels for the eyes and recessed core. No generated noise or repainted texture is used. Green pixels are color accents; an emissive rendering layer is not implemented.

The revised silhouette has slimmer bone shoulder plates and forearms, smaller horns, two shoulder spines per side, and five floating crown fragments. The large violet gem, gold trim, and obsidian palette have been replaced with weathered bone and recessed joints. The original nine animation names and durations are preserved.

The polished animation pass uses shape-preserving cubic curves sampled at 24 FPS, with a 120 FPS export timing grid to retain clip endpoints. Added motion includes forearm and wrist follow-through, counter-motion in the head, bent-knee walk swings, staggered floating crown fragments, slam compression, and blast recoil. The core-blast arms now open outward. Idle and walk loop endpoints match; death exports with `hold_on_last_frame`. The editable project contains 7,539 channel keyframes. More keys approximate the curves; deliberate attack timing and recovery poses provide the weight.

`animation_preview.mp4` is a 12 FPS viewport recording of idle, claw combo, ground slam, and core blast. The underlying clips have 24 FPS samples and interpolate during playback.

Runtime asset paths: `remnant_bosses:geo/entity/hollow_sovereign.geo.json`, `remnant_bosses:animations/entity/hollow_sovereign.animation.json`, and `remnant_bosses:textures/entities/hollow_sovereign.png`.

## Animation clips

All names start with `animation.hollow_sovereign.`

| Clip | Seconds | Behavior |
| --- | ---: | --- |
| idle | 4 | Loop: breathing, heart pulse, crown float |
| walk | 1.2 | Loop: alternating strides and arm swing |
| awaken | 3 | Rise and jaw opening |
| roar | 2 | Chest extension and crown lift |
| claw_combo | 1.8 | Right/left attack, impacts around 0.55 and 1.15 s |
| ground_slam | 2.2 | Both arms raise, impact around 1.05 s |
| core_blast | 2.6 | Charge and recoil around 1.45 s |
| enrage | 3 | Spinning crown, pulsing core, open jaw |
| death | 3.5 | Full-body collapse and core disappearance |

The entity implementation now binds these clips to server-timed combat, particles, sounds, registration, a renderer, and a summoning ritual on both Minecraft versions. See [the encounter guide](../../hollow-sovereign/README.md) for abilities, configuration, ritual instructions, and remaining playtest work. Death is held through its 3.5-second collapse before entity removal.

## Validation and authoring

Validated unique bones, one root, acyclic parent links, animation references and finite values, clip timing, positive cube dimensions, every UV rectangle's bounds and opacity, and exact texture identity with Kotsukage in both versions. Inspected Blockbench previews for the rest pose, slam windup, and death collapse. No Minecraft runtime test has been performed.

`python tools/build_hollow_sovereign.py` creates a new Blockbench project through the plugin at `http://localhost:3000/bb-mcp`; it requires Python and Pillow. `python tools/verify_hollow_sovereign.py` validates and copies the exports, captures selected poses from the active project, and starts idle playback. Select the Hollow Sovereign project before running the latter.
