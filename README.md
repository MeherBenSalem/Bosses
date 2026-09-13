# Remnant Bosses

A Minecraft boss content mod featuring deadly bosses, custom mobs, weapons, and altar rituals.

## Summoning bosses

Three bosses are summoned through an **Ancient Altar** ritual. Craft **one Ancient Altar** and **four Ancient Pedestals**, arrange the pedestals three blocks out on each cardinal side, place the correct offering block on top of each pedestal, then right-click the altar with the activation item in your main hand.

| Boss | Activation item | Pedestal tops (east → west → south → north) |
| --- | --- | --- |
| **Ossukage** | Nether Star | Skeleton Skull ×4 |
| **Kotsukage** | Wither Skeleton Skull | Bone Block, Soul Sand, Soul Soil, Nether Wart Block |
| **Umbrakar** | Echo Shard | Amethyst Block, Crying Obsidian, End Stone, Sculk |

```
        [north offering]
               |
[west] — [ Ancient Altar ] — [east]
               |
        [south offering]
```

Pedestals must be **Ancient Pedestal** blocks. Offerings sit **one block above** each pedestal. The activation item is consumed in survival. Ossukage also spawns skeleton minions (default 2).

**Crafting**

- **Ancient Altar** — top row: 3× Chiseled Deepslate; center: Diamond; bottom corners: Emerald
- **Ancient Pedestal** — top: Emerald; middle: Chiseled Deepslate; bottom row: 3× Chiseled Deepslate

**Commands:** `/summon remnants:ossukage`, `/summon remnants:kotsukage`, `/summon remnants:umbrakar`

**Configuration:** JAuml files `remnant/bosses/{boss}_summon` — keys `portal_activation_item`, `pedestal_one_activation_block` … `pedestal_four_activation_block`. See [docs/store-listing.md](docs/store-listing.md) for paste-ready Modrinth/CurseForge copy and full registry IDs.

## Supported versions

| Workspace | Minecraft | Loaders | Java |
| --- | --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge | 17 |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge | 21 |

Each version folder is an independent MultiLoader Gradle project. Shared gameplay lives in `common/`; loader modules register content, events, and networking.

The published version is `2.5.1` in both workspaces. Keep those `version=` lines identical.

## Requirements

- JDK 17 for `1.20.1/`
- JDK 21 for `1.21.1/`
- GeckoLib (required)
- JAuml 2.1.1 (required at runtime)

## Build

From a version folder:

```
cd 1.20.1
./gradlew :forge:build :fabric:build

cd ../1.21.1
./gradlew :neoforge:build :fabric:build
```

From the repository root:

```
./gradlew buildAll
```

That builds both workspaces and copies the four loader jars into `releases/`.

## Run clients

```
cd 1.20.1
./gradlew :forge:runClient
./gradlew :fabric:runClient

cd ../1.21.1
./gradlew :neoforge:runClient
./gradlew :fabric:runClient
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Security

See [.github/SECURITY.md](.github/SECURITY.md).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
