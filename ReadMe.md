# Remnant Bosses

Minecraft boss content pack for NightBeam's MultiLoader layout.

## Supported versions

| Workspace | Minecraft | Loaders | Java |
| --- | --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge | 17 |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge | 21 |

Each version folder is an independent Gradle project. Shared gameplay lives in `common/`; loader modules only register content, events, and networking.

The published version is `2.3.0` in **both** workspaces. Keep those `version=` lines identical.

## Requirements

- JDK 17 for `1.20.1/`
- JDK 21 for `1.21.1/`
- GeckoLib (required)
- JAuml 2.1.1 (required at runtime)

## Build

From a version folder:

```powershell
cd 1.20.1
.\gradlew.bat :forge:build :fabric:build

cd ..\1.21.1
.\gradlew.bat :neoforge:build :fabric:build
```

From the repository root:

```powershell
.\gradlew.bat buildAll
```

That builds both workspaces and copies the four loader jars into `releases/`.

## Run clients

```powershell
cd 1.20.1
.\gradlew.bat :forge:runClient
.\gradlew.bat :fabric:runClient

cd ..\1.21.1
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :fabric:runClient
```

## License

All Rights Reserved.
