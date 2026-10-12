# Dedicated-server smoke launcher diagnosis

Recorded 2026-10-12. This documents smoke tooling only; no production code was
changed for this launcher repair.

## Failure and evidence

The original Fabric 1.20.1 smoke run failed before Minecraft started. The full
trace is in `boss-hunts-fabric120-server-debug.log` and ends with `BUILD FAILED
in 12s`:

```text
Cannot run program "C:\Users\mahou\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2\bin\java.exe"
(in directory "C:\Users\mahou\OneDrive\Documents\GitHub\Bosses\1.20.1\fabric\C:\Users\mahou\OneDrive\Documents\GitHub\Bosses\.gradle\boss-hunts-server-smoke\1.20.1\fabric"):
CreateProcess error=267, The directory name is invalid
```

Two launcher hypotheses were considered:

1. **Invalid working-directory construction.** The trace contains the module
   path followed by a second absolute Windows path. This directly explains
   error 267.
2. **Missing or incompatible Java 17 game executable.** Initially plausible
   from the top-level process-start error, but contradicted by the nested
   invalid-directory exception and the successful retry with the same Java 17
   game toolchain.

Inspection with local `javap -c` of the installed
`net.fabricmc:fabric-loom:1.8.13` JAR confirms that
`net.fabricmc.loom.task.AbstractRunTask.exec()` calls
`setWorkingDir(new File(projectDir, internalRunDir))` immediately before
`JavaExec.exec()`. It overrides the init script's earlier task working directory.

## Repair

`tools/boss-hunts-runtime/server-smoke.init.gradle` now passes Loom
`project.projectDir.toPath().relativize(runDirectory.toPath()).toString()`.
The Forge/NeoForge game-directory settings are unchanged. Console forwarding
and the selected game Java toolchain remain configured as before.

## Retry and verification

The first attempted retry under the restricted command sandbox did not reach
Gradle or start a server: the wrapper attempted a Gradle distribution download
and failed with `java.net.SocketException: Permission denied: getsockopt`.
The subsequent runtime check used the authorized elevated command environment
and the existing user Gradle cache. Its log is
`boss-hunts-fabric120-server-retry.log` (the successful attempt replaces the
initial wrapper-only retry log).

Exact successful command, from `1.20.1` in an interactive PTY:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :fabric:runServer -I ../tools/boss-hunts-runtime/server-smoke.init.gradle -PjournalSmokeLoader=fabric --no-daemon --console=plain 2>&1 | Tee-Object -FilePath ..\boss-hunts-fabric120-server-retry.log
```

Observed results:

- Fabric Loader 0.16.9 loaded Minecraft 1.20.1, Java 17 and Remnant Bosses 2.7.0.
- Remnant Bosses and JAUML initialization completed.
- The copied fixture started on `127.0.0.1:25585`.
- At 13:45:44, vanilla logged `Done (5.471s)! For help, type "help"`.
- Sending `stop\r\n` through the PTY reached the server console at 13:45:54.
- All three dimensions were saved; Gradle reported `BUILD SUCCESSFUL in 56s`
  and the command exited with code 0.
- A post-exit TCP query found no remaining listener on port 25585.

This is a before/after runtime regression check of the original launch failure
and console shutdown. It does not verify multiplayer attribution, player
reconnect, boss combat, or Forge/NeoForge cancelled deaths. The other three
loader/version smoke outcomes are recorded by the main validation operator.

The successful run still logs pre-existing missing-data-fixer messages for mod
entities, the fixture's expected offline-mode notice, and a copied-world warning
for `remnant_bosses:remnant_ossukage`. These messages did not prevent startup or
clean shutdown; this tooling repair does not change those conditions.

## Forge 1.20.1 post-ready crash

The first Forge check in `boss-hunts-forge120-server.log` is a **runtime failure**,
despite Gradle reporting `BUILD SUCCESSFUL in 2m 4s` and exit zero. At 13:48:57 it
logged `Done (5.050s)!` and immediately crashed:

```text
Attempted to load class net/minecraft/client/server/LanServerPinger for invalid dist DEDICATED_SERVER
java.lang.RuntimeException: Attempted to load class net/minecraft/client/server/LanServerPinger for invalid dist DEDICATED_SERVER
    at net.minecraftforge.fml.loading.RuntimeDistCleaner.processClassWithFlags(RuntimeDistCleaner.java:56)
    ...
    at net.minecraft.server.dedicated.DedicatedServer.initServer(DedicatedServer.java:197)
```

It wrote
`.gradle/boss-hunts-server-smoke/1.20.1/forge/crash-reports/crash-2026-10-12_13.48.57-server.txt`.
Its subsequent automatic save/shutdown did not make this a successful runtime
check. Readiness, a zero process exit, and save messages alone are insufficient.

The ranked hypotheses were:

1. **Forge's optional LAN advertising path loads a client-only class.** The
   crashing vanilla/Forge frame is line 197 and identifies `LanServerPinger`.
   The copied world's Forge server config enabled LAN advertising.
2. **The Boss Hunts implementation loads a client class on the dedicated
   server.** Plausible for a distribution-cleaner failure, but the actual stack
   has no Boss Hunts frame and the generated source directly accounts for the
   failing class construction.

The exact generated source was read from
`1.20.1/forge/build/moddev/artifacts/forge-1.20.1-47.4.20-sources.jar`:

- `net/minecraft/server/dedicated/DedicatedServer.java:196` tests
  `ForgeConfig.SERVER.advertiseDedicatedServerToLan.get()`; line 197 constructs
  `new net.minecraft.client.server.LanServerPinger(...)`.
- `net/minecraftforge/common/ForgeConfig.java:74` defines that optional LAN
  advertising setting, documents its purpose, and defaults it to `true` at
  line 77.
- `net/minecraft/client/server/LanServerPinger.java:15` annotates the generated
  class `@OnlyIn(Dist.CLIENT)`.

This establishes an incompatibility in this Forge development artifact's
optional advertising path. No claim is made here about other Forge versions or
the installed production distribution.

### Fixture-only workaround and verified retry

Only the disposable fixture changed: set
`[server].advertiseDedicatedServerToLan = false` in
`.gradle/boss-hunts-server-smoke/1.20.1/forge/world/serverconfig/forge-server.toml`.
The flag remains false after the retry. The fixture's `server.properties` port
was changed to `25586`, still bound to `127.0.0.1`, to avoid sharing port 25585
with another workspace's independent check. No production code, Forge version,
normal run directory, or original world was changed.

Exact successful command, from `1.20.1` in an interactive PTY with the authorized
elevated command environment:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :forge:runServer -I ../tools/boss-hunts-runtime/server-smoke.init.gradle -PjournalSmokeLoader=forge --no-daemon --console=plain 2>&1 | Tee-Object -FilePath ..\boss-hunts-forge120-server-retry.log
```

The retry's complete log records:

- Forge 47.4.20 with Minecraft 1.20.1 and the Java 17 game toolchain; Remnant
  Bosses and JAUML initialization completed.
- `Done (5.392s)!` at 13:52:46 on `127.0.0.1:25586`, followed by Forge permission
  handler initialization and JAUML's server-starting hook.
- Console `list` returned `There are 0 of a max of 3 players online` at 13:53:03,
  proving responsiveness 17 seconds after the ready message.
- Console `stop` was accepted at 13:53:13; all dimensions were saved at 13:53:15.
- `BUILD SUCCESSFUL in 1m 39s`, command exit code 0.
- A full-log search for `ERROR`, `FATAL`, unexpected exceptions,
  `RuntimeException`, and crash-report entries returned zero matches.
- The crash-report directory contains only the original 13:48:57 crash report;
  the retry created none. A post-exit query found no listener on port 25586.

The retry retains development-environment warnings about terminal capabilities,
Forge library metadata, Log4j URI parsing, union resource URLs and the fixture's
offline mode. These are warnings, not the original crash. The pass is scoped to
dedicated initialization, a live console command, and clean shutdown with the
documented fixture setting; LAN advertising itself is not tested.
