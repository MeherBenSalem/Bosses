# Boss Hunts independent source review

Date: 2026-10-12. Branch: `codex/boss-hunts`. Comparison base: `origin/main` at `3a47ea949e71deef157c47ab88cf8c0c5a44b6e2`; HEAD during review: `b8baf9b7faeb70e80dc25bb3dfc32364314c818c`.

Reviewed the working-tree Java/resource/build changes and the untracked journal core, mixin, network codecs, screen, item, resources, and regression sources in both version workspaces against the approved design and plan. Concurrent integration work continued while this review ran. Line references below describe the reviewed source, before subsequent fixes. No production files were edited, and no builds, tests, dedicated servers, or clients were launched by this reviewer.

Snapshot anchors (SHA-256): both `JournalLivingEntityMixin.java` files `FAFF95D1DFB52A75E3135E89CE4C26840E0E26AA244C3B74259908536CC1AB43`; 1.21.1 `BossHunts.java` `825B8FBA7FF6432986FE4332266F53104DB9261EFCB8A5AB5A373FDEE477A883`; 1.21.1 `RitualInfo.java` `8D645851FE5AA320E0D9C7362CF153371AD0764BA432396B826AF497AF310221`.

## Blocking

### P1 — Measure the accepted damage of this invocation, not a mutable whole-call health delta

Locations: `1.20.1/common/src/main/java/com/nightbeam/remnants/mixin/JournalLivingEntityMixin.java:19`, `:22`, `:29`; identical lines in `1.21.1/common/src/main/java/com/nightbeam/remnants/mixin/JournalLivingEntityMixin.java`. Symbols: `remnant$healthBeforeDamage`, `remnant$beforeDamage`, `remnant$afterDamage`.

The HEAD/RETURN pair stores one baseline on the entity and attributes any intervening health loss to the current `DamageSource`. That interval contains synchronous loader callbacks. Forge 1.20.1 invokes `onLivingHurt` and `onLivingDamage` before the health write. NeoForge 1.21.1 additionally invokes `onLivingDamagePost` after the health write but before `actuallyHurt` returns. These positions are visible in the official [Forge patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/patches/minecraft/net/minecraft/world/entity/LivingEntity.java.patch) and [NeoForge patch](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/world/entity/LivingEntity.java.patch).

Concrete failure: a Forge listener handling player A's hit applies a separate, accepted hit from B to the same boss, then cancels A's original event. If the nested hit reaches `actuallyHurt` (for example, with damage exceeding the existing hurt cooldown threshold), B's hit reduces health. A's RETURN sees that loss and records A too, despite A's damage being cancelled. A can later receive permanent defeat credit. Conversely, a NeoForge post-damage listener that performs a nested call which reaches `actuallyHurt` but changes no health overwrites the baseline with the already-reduced health; the legitimate outer hit then appears to have dealt zero damage.

Remedy: observe the accepted health mutation/final damage belonging to each invocation and record it before post-damage callbacks can trigger death, or use invocation-local accounting that explicitly excludes nested changes. Merely replacing the field with a stack does not solve the cancelled-outer-hit case: the outer before/after interval would still include B's health loss. Preserve the accepted-death guard and add cancellation/reentrancy fixtures against the real loader hook path.

Related ordering gap: `remnant$acceptedDeath` sets the persisted processed marker before calling `BossHunts.died(entity)` at line 38, and `BossHunts.died` (both versions, line 56) consumes only recorded contributions. If a post-damage callback completes an accepted `die(source)` before `actuallyHurt` RETURN, the final accepted hit has not yet entered the encounter map and the marker prevents retry. The preserved death source is discarded here. Normal vanilla single-call ordering records the final hit before death; this review does **not** claim ordinary final hits fail. Cover the callback-driven ordering explicitly when fixing the attribution hook, and ensure the final-hit contract is met without awarding cancelled attempts.

## Should fix

### P2 — Make validated activation IDs agree with altar matching

Locations: both versions' `common/src/main/java/com/nightbeam/remnants/journal/RitualInfo.java:40`, `:42`, `:50`, `:66`; both `event/SovereignRitual.java:24`; `1.21.1/common/src/main/java/com/nightbeam/remnants/event/BlockInteractionEvents.java:59`; corresponding activation comparisons in 1.20.1 `event/GameEvents.java`.

`fromValues` validates activation IDs through `ResourceLocation.tryParse` but retains the original string. An unqualified vanilla ID such as `echo_shard` resolves to the registered `minecraft:echo_shard` and is marked valid, so the screen presents an available recipe and the correct icon. Altar execution compares `minecraft:echo_shard` to the original `echo_shard`; the comparison fails. `matchesActivation` has the same mismatch, and the existing three boss handlers still compare the raw JAuml value. This violates the requirement that the displayed effective ritual and gameplay use the same resolution.

Remedy: either reject values which gameplay intentionally does not accept and display unavailable, or canonicalize parsed IDs and route every activation comparison/consumption check through the same validated representation. Add a registry-backed fixture for unqualified versus explicit `minecraft:` IDs and assert that a recipe marked valid matches the intended held item.

### P3 — Supply the journal item's two tooltip translations

Locations: `1.20.1/common/src/main/java/com/nightbeam/remnants/item/BossJournalItem.java:29` and `:30`; identical lines in the 1.21.1 item; both `common/src/main/resources/assets/remnant_bosses/lang/en_us.json`.

`appendHoverText` requests `item.remnant_bosses.boss_journal.tooltip` and `item.remnant_bosses.boss_journal.progress`. Neither key exists in the reviewed language assets. Hovering the new item therefore displays translation identifiers instead of its usage/progress explanation.

Remedy: add both English strings in each workspace and include journal item keys in the language-completeness check.

## Optional

- Avoid resolving or creating SavedData inside `BossHunts.stop` (both versions, line 114). Loader adapters currently call it on the stopped event. Both loaders document this event as occurring after shutdown, and their lifecycle hooks can also reach it after startup fails. Cleaning an already obtained journal instance, or using an earlier available-world lifecycle stage with a null-safe stopped fallback, would avoid acquiring world state during teardown. This review did not establish a normal-shutdown null dereference or lost save; the late event alone is not a blocking finding. See the official [Forge stopped-event contract](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/event/server/ServerStoppedEvent.java) and [NeoForge stopped-event contract](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/event/server/ServerStoppedEvent.java).

## Coverage and evidence limitations

The existing policy tests use production policy functions and meaningful independent time/distance boundaries. The codec fixtures exercise real `FriendlyByteBuf` parsing, truncation, duplicate/unknown IDs, direction-independent payload bounds and trailing identity data. The NBT fixtures use actual `CompoundTag` save/load, verify monotonic state and dirty behavior, and retain unknown/future-schema data. Layout fixtures check geometric bounds and scrolling. These are useful tests, but their source does not prove Minecraft hook execution.

Required checks still missing from the reviewed automated coverage or runtime evidence:

- Real accepted/cancelled Forge and NeoForge damage/death ordering, nested callbacks, and final-hit delivery before the processed marker.
- Actual projectile and tame-pet owner resolution through `BossHunts.contributor`; the policy tests construct participants directly and never exercise that resolver.
- A real entity save/load cycle retaining `RemnantBossHuntProcessed`; `acceptedDeath(true, true)` tests the guard expression, not the mixin's NBT hooks.
- Periodic discovery through living boss ticks, including a player entering line of sight later; current fixtures test only `canDiscover`.
- Actual world SavedData reload after restart and player death/dimension/reconnect transitions. The in-memory NBT test is useful but does not exercise the data-storage adapter.
- All four loader compilation/startup checks, including physical dedicated-side class loading and the actual transport handlers. Pure packet parsing cannot establish this.
- Actual client rendering, keyboard activation, narration, long labels, missing registry entries, repeated opens and stale queued packets across reconnect/dimension changes. Geometry and source inspection do not establish rendered quality.

The coordinator reported Gradle cache recreation in progress. Full `buildAll`, both `verifyJournal`/`verifyCrafting` tasks, existing recipe/publisher checks, release artifacts and exact JVM/runtime checks must be reported from their actual subsequent executions. This reviewer claims none of them passed.

## Additional source observations

No additional concrete defect was found in personal server ownership, UUID/canonical-ID save keys, monotonic progression, bounded encounter/request maps, the 1,200-tick and 64-block contribution checks, current-online versus refreshed-logout positions, same-dimension/spectator checks, explicit packet directions, transport-derived C2S identity, or client snapshot bounds. Unsupported SavedData schemas are copied and made read-only. Client UI references are isolated behind nested client receivers or physical-client entrypoints, and queued journal receives compare connection/world identities. These observations do not substitute for loader startup or security testing.

No ordinary-drop, music, combat-entity or existing recipe behavior change was identified in the reviewed journal diff beyond the intended shared ritual validation. The new item uses the vanilla enchanted-book texture and version-correct recipe result fields.

**Verdict: request changes.** Fix the attribution and configuration-consistency findings, complete the missing tooltip strings, and obtain the required integration evidence before considering the feature merge-ready.

## Scoped corrective re-review — 2026-10-12

The coordinator requested a source-only re-review of the three findings above after corrections. The original findings and evidence limitations are preserved as the initial review record. This re-review read both current mixins, both `HealthWriteTracker` implementations, both `JournalDamageRegression` sources, the `BossHunts.damage` call sites and method bodies, both ritual resolver/test changes, and both language files. No build, test program, server, or client was launched by this reviewer.

Updated snapshot anchors (SHA-256): both `JournalLivingEntityMixin.java` files `37925D4705B75BA548DFF03BE7EB29A617C31A100A83D99B8B202CEDA227BE2F`; 1.21.1 `HealthWriteTracker.java` `3D1DA6FFB2F7718E9038443ECB1745ADB657EAE73354F3F5491834D649B4254F`; 1.21.1 `RitualInfo.java` `C96A95A8D404BFB80CC4EFA4CED03142034823B66043B6587D179737A6C6FB9D`.

| Initial finding | Current result | Evidence |
| --- | --- | --- |
| P1 accepted-damage reentrancy/ordering | Addressed in source | Both `JournalLivingEntityMixin.java:24` and `:32` now bracket the `LivingEntity.setHealth(F)V` invocation inside `actuallyHurt`, rather than HEAD/RETURN of the entire damage method. |
| P2 validated activation mismatch | Addressed in source | Both `RitualInfo.java:43` require `activationId.toString().equals(item)` before marking the ritual valid. Both `RitualRegression.java:26` reject `echo_shard`. |
| P3 missing tooltip translations | Addressed | Both `en_us.json:108` and `:109` now define the requested tooltip and personal-progress strings; both files were parsed successfully with `ConvertFrom-Json` during this review. |

For P1, the narrower hook interval is the important correction. The official loader patches cited above place Forge `onLivingHurt`/`onLivingDamage` and NeoForge's pre-damage hook before the health write. A cancelled outer event therefore cannot acquire or complete an outer health-write frame. The journal call now executes immediately after the accepted write, before NeoForge's `onDamageTaken`/`onLivingDamagePost`; a nested zero-damage post-event can no longer replace the outer baseline, and a death initiated by those callbacks sees the final accepted contribution already recorded. `BossHunts.damage` now takes the measured `healthLost` and still rejects non-finite/non-positive values. The accepted `dead`/processed-marker guard remains unchanged.

`HealthWriteTracker` additionally uses a stack and subtracts recorded nested loss for reentry during a health write itself. `JournalDamageRegression` models a nested accepted pre-event write followed by a cancelled outer event, an accepted outer write followed by a zero-damage nested post-event write, nested write accounting, healing, and invalid health. Both umbrella journal test sources invoke it. These are useful accounting fixtures; they are not a real Forge/NeoForge hook or mixin-application test, and this reviewer did not execute them.

For P2, rejecting the unqualified activation is consistent with the existing gameplay comparison and causes the screen to show unavailable. Explicit canonical IDs remain eligible; offering IDs still resolve through the block registry for both display and matching. The coordinator reported observing the new assertion fail before the resolver correction. This report records that as coordinator-provided evidence, not an independently executed regression.

No new serious source issue was found within this corrective scope. The earlier optional teardown suggestion and the full-matrix integration/runtime evidence requirements remain. In particular, source placement does not prove the mixin target remaps and applies successfully on each loader. The coordinator reported a Fabric 1.20.1 development harness in preparation and the remaining matrix pending; this review does not claim any subsequent harness outcome.

**Updated scoped verdict: approve-with-nits.** All three source findings are addressed. This verdict approves the reviewed corrections; overall merge readiness still requires the recorded build and integration checks.
