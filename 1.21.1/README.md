# Remnant Bosses 1.21.1

Independent MultiLoader workspace for Minecraft 1.21.1.

| Module | Loader |
| --- | --- |
| `common/` | Shared gameplay and assets |
| `neoforge/` | NeoForge 1.21.1 |
| `fabric/` | Fabric 1.21.1 |

Java 21 is required.

```powershell
.\gradlew.bat :neoforge:build :fabric:build
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :fabric:runClient
```
