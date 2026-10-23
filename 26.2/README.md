# Remnant Bosses 26.2

Independent MultiLoader workspace for Minecraft 26.2.

| Module | Loader |
| --- | --- |
| `common/` | Shared gameplay and assets |
| `neoforge/` | NeoForge 26.2 |
| `fabric/` | Fabric 26.2 |

Java 25 is required, for Gradle itself as well as the toolchain (Fabric Loom 1.19 does not run on older JVMs).
Minecraft 26.2 ships unobfuscated, so there are no mappings: the code uses Mojang's names directly.

```powershell
.\gradlew.bat :neoforge:build :fabric:build
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :fabric:runClient
```

`build` also runs the native regressions in `common/` (aliases, animation clips, recipes and loot, journal, mixin
policy). To check the jars the way players run them, use `tools/prod-boot/` at the repository root.
