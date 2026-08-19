# Remnant Bosses

A Minecraft boss content mod featuring deadly bosses, custom mobs, weapons, and altar rituals.

## Supported versions

| Workspace | Minecraft | Loaders | Java |
| --- | --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge | 17 |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge | 21 |

Each version folder is an independent MultiLoader Gradle project. Shared gameplay lives in `common/`; loader modules register content, events, and networking.

## Requirements

- JDK 17 for `1.20.1/`, JDK 21 for `1.21.1/`
- [GeckoLib](https://github.com/bernie-g/geckolib) (required)
- JAuml 2.1.1 (required at runtime; bundled in `libs/`)

## Build

From the repository root (builds both workspaces):

```
./gradlew buildAll
```

Or from a single version folder:

```
cd 1.20.1
./gradlew :forge:build :fabric:build
```

## Run a client

```
cd 1.20.1
./gradlew :fabric:runClient
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](.github/SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
