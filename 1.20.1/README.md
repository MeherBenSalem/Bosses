# Remnant Bosses 1.20.1

Independent MultiLoader workspace for Minecraft 1.20.1.

| Module | Loader |
| --- | --- |
| `common/` | Shared gameplay and assets |
| `forge/` | Forge 1.20.1 |
| `fabric/` | Fabric 1.20.1 |

Java 17 is required.

```powershell
.\gradlew.bat :forge:build :fabric:build
.\gradlew.bat :forge:runClient
.\gradlew.bat :fabric:runClient
```
