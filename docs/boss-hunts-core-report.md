Final verification outcomes are consolidated in [the test report](boss-hunts-test-report.md). The implementation-stage evidence below is retained as a record.

# Boss Hunts common server implementation report

Date: 2026-10-12. Branch: codex/boss-hunts. Scope: Task 1 common server core in both Minecraft workspaces.

## Changes

In both 1.20.1/common and 1.21.1/common:
- Added journal/BossHunt.java and HuntState.java, canonical remnants entity IDs and monotonic personal states.
- Added HuntSavedData.java: overworld SavedData, UUID records, actual CompoundTag serialization, preserved unknown data, lossless read-only handling of unsupported or malformed schema versions.
- Added EncounterCredits.java and JournalRequests.java: bounded temporary maps, 1,200-tick/64-block contribution checks, current online or last disconnected position, logout refresh, 48-block/sight discovery policy, request eligibility and 20-tick cooldown.
- Added BossHunts.java: server lifecycle APIs, projectile/tame-pet owner attribution, personal snapshots, discovery, accepted damage and death credit.
- Added mixin/JournalLivingEntityMixin.java and registered it in common mixin JSON. Positive health change is measured immediately before/after the setHealth call inside actuallyHurt; death uses LivingEntity.dead after die returns. Completion marker survives entity NBT save/reload.
- Added JournalRegression.java and JournalPolicyRegression.java, plus verifyJournal in common/build.gradle. The umbrella test invokes ritual, packet and layout suites owned by other workers.

In 1.21.1 only, EntityDeathEvents.onLivingDeath now retains DamageSource in its signature.

No loader, packet, client, ritual, language, item or altar implementation files were edited by this worker.

## Commands and observed results

From the repository root, using PowerShell:
1. Expected red before core implementation:
   `& 'C:/Program Files/Java/jdk-26.0.1/bin/javac.exe' --release 17 -d .gradle/journal-policy-red 1.20.1/common/src/test/java/com/nightbeam/remnants/JournalPolicyRegression.java`
   **Observed exit 1**, missing production EncounterCredits and JournalRequests types. Log: .gradle/journal-policy-red/compiler.log.
2. Java 17 policy green:
   `& 'C:/Program Files/Java/jdk-26.0.1/bin/javac.exe' --release 17 -d .gradle/journal-policy-red 1.20.1/common/src/main/java/com/nightbeam/remnants/journal/EncounterCredits.java 1.20.1/common/src/main/java/com/nightbeam/remnants/journal/JournalRequests.java 1.20.1/common/src/test/java/com/nightbeam/remnants/JournalPolicyRegression.java`
   then `& 'C:/Program Files/Java/jdk-26.0.1/bin/java.exe' -cp .gradle/journal-policy-red com.nightbeam.remnants.JournalPolicyRegression`
   **Observed exit 0**, “Journal policy regressions passed”.
3. Java 21 policy green:
   Same command with `--release 21`, sources under 1.21.1, and output/classpath .gradle/journal-policy-121.
   **Observed exit 0**, “Journal policy regressions passed”.
4. `git diff --check`: **observed exit 0**. Existing workspace line-ending warnings were reported.
5. Gradle `:common:verifyJournal` in both workspaces: **awaiting centralized integration execution by root**, which owns Gradle coordination. No concurrent Gradle was started by this worker.

The standalone runs use JDK 26 to compile Java 17/21-compatible bytecode; they do not establish runtime compatibility on the target JVMs. Gradle uses each workspace's configured toolchain.

## Acceptance mapping

| Criterion | Status / evidence |
| --- | --- |
| Test-first regression | Done: expected missing-core javac failure, then passing production policy fixtures |
| Monotonic and idempotent personal progress | Implemented; real NBT regression written, integration execution pending |
| Unknown/future/corrupt data | Implemented and regression written: unknown keys retained, unsupported schema read-only |
| Contribution eligibility and disconnected positions | Done in standalone policy tests: 64 vs 64.01 blocks, 1,200 vs 1,201 ticks, dimensions, spectators, online movement and logout refresh |
| Bounded encounters/contributors/expiry | Done in standalone policy tests; 1,024 encounters and 128 contributors maximum |
| Discovery eligibility | Done in standalone policy tests; real periodic mixin application pending runtime verification |
| Positive damage/cancelled death/idempotent guard | Pure policy tests pass; actual mixin hooks require loader runtime verification |
| Projectile/tame owner attribution | Implemented with explicit projectile-owner traversal and tame-owner UUID lookup; live entity fixture unrun |
| Persisted processed entity marker | Implemented at save/load RETURN; actual entity unload/reload runtime scenario unrun |
| Request version, alive/spectator and cooldown | Done in standalone policy tests |
| Lifecycle/cleanup | Common APIs implemented, bounded maps are world SavedData instance fields and cleared on stop |
| Both verifyJournal tasks / four-loader builds | Pending centralized root integration |
| No optional rewards | Done |

## Remaining concerns

- A real loader session must validate the mixin target application and cancelled Forge/NeoForge death behavior. Pure guard tests cannot prove injections execute.
- Projectile and pet ownership, actual player logout/reconnect, boss entity reload, and dedicated-server lifecycle scenarios need runtime verification.
- Final-hit attribution relies on the verified vanilla/loader control flow: the actual setHealth write inside actuallyHurt records its positive health delta before hurt invokes die. Death itself never invents a contribution for a source that dealt no accepted damage.
- Read-only future schemas intentionally refuse state updates rather than destructively migrating an unknown format.
- Encounter map limits evict the least recently updated encounter/contribution when exceeded.


## Reentrancy review correction

Independent review found that a single HEAD/RETURN health baseline could wrongly attribute nested pre-event damage to a cancelled outer source, or lose an outer hit when a post-event nested call overwrote the baseline. Both mixins now use paired injections immediately before/after the actual LivingEntity.setHealth invocation inside actuallyHurt. Cancelled paths that never write health cannot create a contribution, and accepted writes are recorded before post-event reentry. HealthWriteTracker also subtracts nested write losses from outer write frames, avoiding double-counting if setHealth itself is reentered.

New files in both common workspaces: journal/HealthWriteTracker.java and tests/JournalDamageRegression.java. JournalRegression invokes the new suite. BossHunts.damage accepts the observed health loss directly.

Expected red: javac --release 17 on JournalDamageRegression before HealthWriteTracker existed returned exit 1 (missing implementation), recorded in .gradle/journal-policy-red/damage-red.log.

Green: JDK26 javac --release 17 and --release 21, each compiling the corresponding HealthWriteTracker.java plus JournalDamageRegression.java to .gradle/journal-policy-red, followed by java -cp .gradle/journal-policy-red com.nightbeam.remnants.JournalDamageRegression. Both returned exit 0 and printed “Journal damage-write regressions passed”. The fixtures exercise nested pre-event damage with cancelled outer handling, outer accepted damage with a nested zero hit, nested write subtraction, healing and nonfinite health. These pure fixtures validate tracker behavior; live mixin INVOKE application still needs centralized loader/runtime validation.

Root reported that the earlier 1.20.1 verifyJournal passed; the reentrancy correction requires a fresh centralized build before completion claims.
## Additional multi-contributor policy coverage

Added the following fixtures to BOTH JournalPolicyRegression.java files, without production changes:
- Two distinct qualifying contributors A and B with a third nearby online but uninvolved UUID: completion must return exactly {A, B}.
- A last hit at tick 10 and B at tick 11, completed at tick 1211: A expires at 1,201 ticks; B qualifies at exactly 1,200 ticks; result must be exactly {B}.
- Both contribute, then A changes to the Nether or moves to 64.01 blocks at completion: each case must return exactly {B}.

Observed green for both source variants using JDK26 javac --release 17 / --release 21, compiling the version's EncounterCredits.java, JournalRequests.java and JournalPolicyRegression.java into .gradle/journal-multiplayer-17 / .gradle/journal-multiplayer-21 respectively, followed by java -cp <output> com.nightbeam.remnants.JournalPolicyRegression. Both returned exit 0 and printed “Journal policy regressions passed”. These additional cases passed against the current implementation; no pre-existing production bug is claimed.

Mutation check: copied EncounterCredits.java into .gradle/journal-multiplayer-mutation, changed only that disposable copy to add a qualifying UUID only when credited.isEmpty(), compiled it with the real request policy and updated test, and ran the suite. Observed exit 1 at “all qualifying contributors credited; nearby uninvolved player excluded”. Log: .gradle/journal-multiplayer-mutation/result.log. Repository production sources were never mutated. The normal Java17/21 variants were rerun afterward and passed.

These are pure production-policy tests with an in-memory online participant map. A multiplayer session with three actual connections remains unrun; these fixtures do not establish loader/network multiplayer behavior. No Gradle was started by this worker during the centralized dedicated-server checks.
