# Remnant Bosses

A Minecraft boss content mod featuring deadly bosses, custom mobs, weapons, and altar rituals.

## Supported versions

| Workspace | Minecraft | Loaders | Java |
| --- | --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge | 17 |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge | 21 |

Each version folder is an independent MultiLoader Gradle project. Shared gameplay lives in `common/`; loader modules register content, events, and networking.

The published version is `2.4.1` in both workspaces. Keep those `version=` lines identical.

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
