# Remnant Bosses

Minecraft mod project for multi-version support across Forge and NeoForge.

## Modules

- `1.20.1-forge`: Forge 1.20.1 (Java 17)
- `1.21.1-neoforge`: NeoForge 1.21.1 (Java 21)

## Requirements

- JDK 17 for Forge 1.20.1
- JDK 21 for NeoForge 1.21.1
- Gradle wrapper (included)

## Quick start

From the repository root:

```bash
# Forge 1.20.1 client
./gradlew :1.20.1-forge:runClient

# NeoForge 1.21.1 client
./gradlew :1.21.1-neoforge:runClient
```

## Build jars

```bash
# Forge 1.20.1 jar
./gradlew :1.20.1-forge:jar

# NeoForge 1.21.1 jar
./gradlew :1.21.1-neoforge:jar
```

Outputs are placed under each module's `build/libs` directory.

## Optional dependency

- JAuml is an optional runtime dependency. For development, add the JAuml jar to a module's runtime classpath or place it under `libs/` and wire it in the module build script if needed.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

All Rights Reserved.
