# Boss Hunts disposable-world runtime verification

Development-only Fabric 1.20.1 mod. Enabled only with `-PjournalRuntimeTest`; its
source set and `fabric.mod.json` are separate from `main` and never included by
the normal `jar` / `remapJar` / release tasks.

## Launch

Copy a Minecraft 1.20.1 single-player world into
`1.20.1/fabric/run/saves/BossHuntsValidation`. Use a fresh copy with no existing
Boss Hunts progress. Keep the original world untouched. The probe refuses to
mutate any other world folder. It builds a stone platform at y=200, teleports
the local player there, sets creative mode and spawns real bosses in that copy.

From `1.20.1` using Java 17:

```powershell
.\gradlew.bat :fabric:runJournalRuntimeTestClient -PjournalRuntimeTest
```

Quick Play loads exactly `BossHuntsValidation`. The requested window is
1280x960, sufficient for actual GUI scales 2, 3 and 4. The probe waits for all
asynchronous screenshot writes, prints `RUNTIME REPORT` and calls vanilla
`Minecraft.stop()` to exit with normal integrated-server save/shutdown.
The conditional run uses a fixed `BossHuntsProbe` username and disposable test
UUID `8cf1259a-6c08-4ae3-a343-903f54454301` so restart checks use the same actor.

After the first client exits normally, run the same disposable world again to
check real persistence across a full Minecraft process restart:

```powershell
.\gradlew.bat :fabric:runJournalRuntimeTestClient -PjournalRuntimeTest -PjournalRuntimeReload
```

The reload run requires all four saved defeat states for the same real player
UUID, captures the loaded defeated pages and repeats open/Escape checks. Save the
first run report separately because the report filename is reused.

## What it checks

- Real integrated server, real registered entities, actual production discovery
  mixin with distance/line of sight, accepted positive damage then accepted death
  for all four bosses, real SavedData and persistent entity processed markers.
- An actual stone wall and a boss initially beyond 48 blocks keep two entries
  undiscovered. Removing the wall/moving the distant entity into range allows
  normal production tick discovery without invoking discovery directly.
- Repeated `die` is idempotent; saved snapshots reload with all four defeats.
- Real owned Arrow and tamed Wolf objects provide vanilla arrow/mob DamageSources
  for nonlethal contribution hits. A later environmental final blow retains the
  player contributor. These are programmatic actual `hurt` calls: physical arrow
  impact and pet AI attacks remain UNRUN.
- A development-only Fabric `ALLOW_DEATH` listener cancels one Hollow Sovereign
  death. The probe checks no defeat/no processed marker, restores health, removes
  the cancellation and checks accepted death advances normally.
- Actual journal item `use` entrypoint on the server sends real production
  packets to the client; no fabricated snapshots or direct screen constructor.
- Actual BossHuntsScreen key/scroll handlers: arrows, Home/End, Page Down,
  mouse wheel, Escape and item reopen.
- Real game framebuffer captures for each boss in undiscovered, encountered and
  defeated states at scales 2/3/4, including bottom-of-detail captures after
  scrolling encountered/defeated views (60 PNGs total).

Evidence is in `1.20.1/fabric/run/screenshots/boss-hunts-*.png`,
`1.20.1/fabric/run/boss-hunts-runtime-report.txt` and the normal game log.
`VIEW` log records include actual scale, GUI dimensions and narration. Screenshots
must be visually reviewed; a successful screenshot call alone does not prove
layout quality.

## Explicit limits

This harness is not a multiplayer simulator. Actual second-player contributor,
bystander and logout attribution, dimension transitions, reconnect,
Forge/NeoForge cancelled deaths and other loader/version clients remain UNRUN.
Actual process restart remains UNRUN until the optional second launch passes.
Snapshot reload checks are not a substitute for restarting Minecraft. It calls
real screen input handlers programmatically, not native OS input. It invokes
the real journal item server entrypoint, not a forged item-use network packet.

Compilation/launch/visual-review results are recorded separately by the operator.
No runtime pass is claimed just because this harness exists or compiles.

## Dedicated-server smoke runs

`server-smoke.init.gradle` redirects the selected loader's ordinary server task
to `.gradle/boss-hunts-server-smoke/<version>/<loader>` and forwards console
input. It is never active in normal builds. Prepare a copied disposable world,
localhost-only `server.properties`, and an accepted `eula.txt` in that fixture.
Then, from the relevant version workspace:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
.\gradlew.bat :fabric:runServer -I ../tools/boss-hunts-runtime/server-smoke.init.gradle -PjournalSmokeLoader=fabric
# substitute :forge / forge for 1.20.1, :neoforge / neoforge for 1.21.1
```

Use an installed Java 21 JDK for Gradle; the init script selects the workspace's
game toolchain (Java 17 for 1.20.1, Java 21 for 1.21.1). For Fabric, the script
passes Loom a path relative to the loader project. Loom 1.8.13 constructs the
execution directory from the project path and `runDir`, so an absolute Windows
`runDir` produces an invalid concatenated directory despite `workingDir` being
configured on the task.

For the Forge 47.4.20 development artifact used by the 1.20.1 smoke run, set
`advertiseDedicatedServerToLan = false` under `[server]` in the disposable
`world/serverconfig/forge-server.toml` before launching. Its optional LAN
advertising path loads a class marked client-only in the generated artifact and
crashes the dedicated server just after `Done`. This is a fixture configuration
workaround; normal server worlds and release code are unchanged.

Wait for `Done (...)!`, verify the server remains responsive after startup
(for example, enter `list`), then enter `stop` and verify normal shutdown. Check
the entire log for unexpected exceptions and crash reports: Forge can report
`BUILD SUCCESSFUL` and exit zero after an in-game server crash. These checks
prove dedicated-side initialization, actual mixin application during world
startup, and lifecycle teardown; they do not prove multiplayer contribution or
cancelled Forge/NeoForge deaths. Logs and outcomes belong in the test report.
