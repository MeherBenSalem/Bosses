# Remnant Bosses

A Minecraft boss content mod featuring deadly bosses, custom mobs, weapons, and altar rituals.

## The Eclipsed Realm

The Remnants live in a dimension of their own: **the Eclipsed Realm**, an open world under a black sun ringed with gold, with the Gracetree burning on the horizon. Its seven biomes (the Tarnished Fields, the Gracewood, the Gravefield Barrows, the Ashen Wastes, the Lantern Heights, the Stilled Sea and the Ossuary Depths) hold the Hollow Legion, the Cryptborn, the Forgotten Souls, the Ashbound Constructs, the Corrupted Relics and the boss arenas.

1. Craft an **Eclipse Shard** (Amethyst Shard + Ender Pearl + Gold Ingot) and a **Waygate** (6 Polished Deepslate around an Eclipse Shard, Gold Ingot at the bottom centre).
2. Place the Waygate. It raises a 3-wide, 4-tall arch; keep the space clear.
3. Use an Eclipse Shard on it. The arch kindles for six seconds, then opens onto the eclipse.
4. Walk through. You arrive at an Arrival Shrine with a Waygate home and a **Grace Ember**.

Resting at a Grace Ember heals you, sets your respawn point and shows a trail toward the nearest arena. From any grace you can travel to every grace you have discovered, listed nearest first with the distance in meters, or depart the Realm. The Realm itself does not change: outside creative mode nothing can be built or broken there, buckets do not pour and explosions leave the terrain whole (config `remnant/realm/realm` → `build_protection`). Arena doorways are sealed by fog gates that players can walk through but bosses cannot. See [the Eclipsed Realm guide](docs/curseforge/remnant-bosses.md) for the biomes, bosses and rewards.

## Boss Hunts

Craft a **Boss Hunts Journal** with a Book, Bone and Ink Sac, then use it to open your personal journal. You can also sneak and interact with an Ancient Altar with an empty main hand. Witness bosses to uncover their rites, and defeat them to complete their entries. The journal displays the server's effective ritual configuration.

Progress belongs to your player in that world. Trading or losing the journal does not transfer or erase it. See [the Boss Hunts guide](docs/boss-hunts.md) for discovery, multiplayer credit and persistence rules.

## Summoning bosses

Bosses are summoned through an **Ancient Altar** ritual. Craft **one Ancient Altar** and **four Ancient Pedestals**, arrange the pedestals three blocks out on each cardinal side, place the correct offering block on top of each pedestal, then right-click the altar with the activation item in your main hand.

| Boss | Activation item | Pedestal tops (east → west → south → north) |
| --- | --- | --- |
| **Ossukage** | Nether Star | Skeleton Skull ×4 |
| **Forgotten Watcher** | Lens of Memory | Amethyst Block, Calcite, Chiseled Sandstone, Lectern |
| **Hollow Sovereign** | Sovereign's Seal | Gold Block, Gold Block, Soul Lantern, Soul Lantern |
| **Gravebound Warden** | Crypt Key | Bone Block, Soul Soil, Chain, Iron Bars |

The Hollow Sovereign and the Gravebound Warden answer only inside the Eclipsed Realm (config `remnant/realm/realm` → `rituals_realm_only`).

```
        [north offering]
               |
[west] — [ Ancient Altar ] — [east]
               |
        [south offering]
```

Pedestals must be **Ancient Pedestal** blocks. Offerings sit **one block above** each pedestal. The activation item is consumed in survival. Ossukage also raises Gravebound Soldiers (default 2).

**Crafting**

- **Ancient Altar** — top row: 3× Chiseled Deepslate; center: Diamond; bottom corners: Emerald
- **Ancient Pedestal** — top: Emerald; middle: Chiseled Deepslate; bottom row: 3× Chiseled Deepslate

**Commands:** `/summon remnants:ossukage`, `/summon remnants:forgotten_watcher`, `/summon remnants:hollow_sovereign`, `/summon remnants:gravebound_warden`

**Configuration:** JAuml files `remnant/bosses/{boss}_summon` — keys `portal_activation_item`, `pedestal_one_activation_block` … `pedestal_four_activation_block`. See [docs/store-listing.md](docs/store-listing.md) for paste-ready Modrinth/CurseForge copy and full registry IDs.

## Supported versions

| Workspace | Minecraft | Loaders | Java |
| --- | --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge | 17 |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge | 21 |

Each version folder is an independent MultiLoader Gradle project. Shared gameplay lives in `common/`; loader modules register content, events, and networking.

The release version is `3.1.0` in both workspaces. Keep those `version=` lines identical. See [the release notes](patchnotes/3.1.0.md).

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
./1.20.1/gradlew -p . buildAll
```

Run Gradle with JDK 21. The workspace wrapper selects Gradle 8.11, which can run on that JVM; the older root wrapper is Gradle 8.2.1. Java toolchains compile 1.20.1 with Java 17 and 1.21.1 with Java 21. On Windows use `1.20.1\gradlew.bat -p . buildAll`. The task builds both workspaces and copies the four loader jars into `releases/`.

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
