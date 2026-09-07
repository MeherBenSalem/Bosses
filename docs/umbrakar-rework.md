# Umbrakar: the broken crown

Implemented in both Minecraft workspaces (1.20.1 and 1.21.1). Registry names,
spawn eggs, the existing texture atlas, and configuration keys remain compatible.

## Art and motion

The rig now has 30 bones and 218 cubes. Articulated layered cheek armour creates
a blade mane around the face; paired crown spires, upper canines, shoulder plates,
a dorsal crest and a tail crown extend the original silhouette. New pieces reuse
the existing teal atlas.

Ten authored clips cover breathing, walking, running, bite, front slam, tail slam,
orb cast, roar, rift claw and death. Attack poses use anticipation, a held windup,
contact, recoil and recovery. The mane and tail follow through with the body.
Curves are baked at 30 Hz, retaining exact contact timestamps, because GeckoLib
4.4 does not retain `lerp_mode` on Bedrock `post` keyframes. One full-body animation
controller prevents locomotion and combat from overriding each other. Death has
92 ticks to finish before removal, with a late particle release.

## Combat

| Attack | Windup / contact | Duration | Behaviour |
| --- | --- | --- | --- |
| Bite | 10 ticks | 25 ticks | Fixed forward strike with a 3.2-block marked radius |
| Front slam | 22 ticks | 50 ticks | Raised forelegs; marked 4.4-block impact zone |
| Tail slam | 18 ticks | 40 ticks | Rear counter when a target is behind; 4.6-block radius |
| Orb cast | 16 ticks | 35 ticks | Existing homing projectile released from the tail |
| Roar | 24 ticks | 70 ticks | Expanding shockwave over 21 ticks; one hit per victim |
| Rift claw | 24 ticks | 52 ticks | Five eruptions every four ticks, reaching 15 blocks forward |

Facing and navigation lock during attacks. Windup markers commit to their world
positions, so stepping out remains useful. Damage checks use actual distance,
line of sight and allied-entity filtering. Rift claw is selected at medium range;
the phase transition waits for the current animation to finish. Reloading preserves
health and reapplies phase attributes. Ambient particles have been reduced so the
silhouette and attack effects remain legible.

## Tools and validation

From the repository root:

```text
python tools/rework_umbrakar_assets.py
python tools/validate_umbrakar.py
python tools/preview_umbrakar.py
./gradlew.bat buildAll --console=plain
```

Open `docs/umbrakar-preview.html` in a browser for the embedded textured rig,
animation selector, pose scrubber and orbit controls. It works offline. This is
an inspection renderer, not a substitute for Minecraft's lighting and GeckoLib.

The validator checks hierarchy cycles, UV bounds, finite keyframes, loop endpoints,
Java clip references, server contact times, death duration and cross-port parity.

## In-game acceptance pass still required

Test both loaders for each version in a disposable flat creative world, then switch
to survival near the boss. Use the existing Umbrakar spawn egg or
`/summon remnants:umbrakar` (the registered namespace).

- Confirm the mane, crown and canines render without missing faces; inspect the
  front-slam windup, orb cast, running feet and full death sequence.
- Dodge each ground marker before contact. Check the roar's moving edge, the rift
  lane, and damage obstruction by a solid wall. Repeat with two players to check
  the roar's per-victim hit limit.
- Trigger phase two during an attack. Confirm the attack finishes before the roar,
  then save/reload and verify remaining health and phase speed are preserved.
- Kill during a windup: no subsequent impact should occur. Confirm drops and XP,
  boss-bar cleanup, death particles, and removal after the full collapse.
- Repeat on slopes and near walls to assess ground marker readability. Ground
  effects currently use the committed attack plane rather than following terrain.

No in-game playtest has been performed by the automated asset/build checks.
