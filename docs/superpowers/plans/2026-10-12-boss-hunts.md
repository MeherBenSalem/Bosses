# Boss Hunts Implementation Plan

> **For agentic workers:** Use subagent-driven-development or executing-plans to implement the independently owned tasks below. Steps use checkbox syntax for tracking.

**Goal:** Deliver a persistent personal boss journal across all four supported loaders.

**Architecture:** Common world SavedData and encounter logic own all progress. Thin loader transports carry a bounded personal snapshot; a common client screen renders server ritual descriptors. A common accepted-death mixin avoids cancellable pre-death event awards.

**Tech Stack:** Java 17/21, Minecraft 1.20.1/1.21.1, Fabric/Forge/NeoForge, Gradle, NBT, existing JAuml configuration.

**Spec:** `docs/superpowers/specs/2026-10-12-boss-hunts-design.md`

## Global Constraints

- Minecraft 1.20.1 Fabric/Forge, Java 17; Minecraft 1.21.1 Fabric/NeoForge, Java 21.
- Canonical IDs use remnants; item and payload namespace uses remnant_bosses.
- Contribution window 1,200 ticks, radius 64 blocks; discovery 48 blocks and line of sight every ten ticks.
- No natural boss coordinates, client-authored progress, optional rewards or release publishing.
- Preserve unrelated files and graphify-out; agents have disjoint ownership and must accommodate others' edits.

## Shared interfaces

All paths below are relative to each version's `common/src/main/java/com/nightbeam/remnants/` unless marked loader.

```java
// Core owns these declarations.
public enum BossHunt { OSSUKAGE, KOTSUKAGE, UMBRAKAR, HOLLOW_SOVEREIGN;
    public String id(); // full canonical entity ID
    public String key(); // ossukage / kotsukage / umbrakar / hollow_sovereign
    public static BossHunt fromId(String id); // null for unknown
}
public enum HuntState { UNDISCOVERED, ENCOUNTERED, DEFEATED }
// Ritual task owns this declaration.
public record RitualInfo(String activationItem, java.util.List<String> offerings, boolean valid) {
    public static RitualInfo resolve(BossHunt boss);
}
// Network task owns these declarations and codecs.
public record ClientboundJournalPacket(boolean open, java.util.List<Entry> entries) {
    public record Entry(String bossId, HuntState state, RitualInfo ritual) {}
}
public record ServerboundJournalRequestPacket(int schemaVersion) {}
// Core owns service, called only on server thread.
public final class BossHunts {
    public static final int SCHEMA_VERSION = 1;
    public static void open(net.minecraft.server.level.ServerPlayer player);
    public static void request(net.minecraft.server.level.ServerPlayer player, int schemaVersion);
    public static void tick(net.minecraft.server.MinecraftServer server);
    public static void logout(net.minecraft.server.level.ServerPlayer player);
    public static void stop(net.minecraft.server.MinecraftServer server);
}
// Network extends INetworkHelper with these signatures.
void sendJournal(net.minecraft.server.level.ServerPlayer player, ClientboundJournalPacket packet);
void requestJournal(ServerboundJournalRequestPacket packet);
// Client task owns receiver/reset hooks.
public final class BossHuntsClient {
    public static void receive(ClientboundJournalPacket packet);
    public static void tick();
}
```

### Task 1: Common state, encounter logic and lifecycle

**Files:** Create journal/BossHunt.java, HuntState.java, HuntSavedData.java, EncounterCredits.java, BossHunts.java and a common journal event mixin in both workspaces; modify common mixin JSON, 1.21.1 EntityDeathEvents adapter, common/build.gradle and common/src/test/java/com/nightbeam/remnants/JournalRegression.java.

**Consumes:** RitualInfo.resolve, packet Entry constructor and INetworkHelper.sendJournal.
**Produces:** BossHunt, HuntState and BossHunts APIs above; lifecycle calls consumed by loader task.

- [ ] Write regressions before implementation for NBT round-trip/unknown/future data, monotonic states and repeated completion, distance/time/dimension/disconnected credit, spectator/discovery eligibility and request version/cooldown.

```java
// Independently derived boundary fixtures: 1,200 ticks qualifies, 1,201 does not;
// distance 64 qualifies, 64.01 does not; cross-dimension and spectator fail.
// Repeat encountered after defeated: state remains DEFEATED and no new dirty update.
// Save -> load with identical UUID: all four states remain unchanged.
```

- [ ] Run the new JavaExec regression task and record expected failure before feature code exists.
- [ ] Implement bounded encounter bookkeeping, positive accepted damage with owner resolution, periodic sight discovery and accepted-death guard using the real dead flag.
- [ ] Add schema-aware SavedData, processed entity NBT and request limits; preserve attribution adapters.
- [ ] Run `:common:verifyJournal` in both workspaces and address failures. Do not run concurrent Gradle builds against another worker's workspace.

### Task 2: Loader networking and lifecycle adapters

**Files:** Create common network journal packet records/codecs; modify common INetworkHelper and Fabric/Forge/NeoForge transport implementations and entrypoints in both versions. This task owns loader files exclusively.

**Consumes:** Task 1 BossHunts lifecycle; Task 3 BossHuntsClient.receive/tick.
**Produces:** Bounded S2C snapshot and version-only C2S request plus interface methods above.

- [ ] Write packet regressions for excessive entry count/string length, unknown state/ID, malformed request and schema mismatch, without trusting client target identity.
- [ ] Run regressions to establish expected missing-code failure.
- [ ] Extend existing network abstraction and transports, retaining boss-music behavior. Use server-context player and enqueue work on server/client threads.
- [ ] Register tick/logout/stop lifecycle and client reset tick, with physical-side guards preventing dedicated UI loading. Preserve DamageSource through 1.21.1 loader death events.
- [ ] Compile all four loaders and execute packet regressions after integration.

### Task 3: Journal screen, item, configuration and delivery

**Files:** Create common client/journal/BossHuntsClient.java and BossHuntsScreen.java, journal/RitualInfo.java and item/BossJournalItem.java; modify ModItems, language resources, item model/recipe and altar handlers in both versions. UI worker owns client files and languages; root owns ritual/item/altar/resources/docs.

**Consumes:** Packet Entry, BossHunt/HuntState and server open; requestJournal for refresh.
**Produces:** Client receiver API above, ritual resolver and discoverable journal access.

- [ ] Write a failing regression for layout bounds and scroll behavior at small/large GUI sizes; validate ritual IDs through actual registries in the common regression harness.
- [ ] Implement translated screen, entry buttons, bounded text scrolling, item icons, state badges, keyboard selection/focus, narration, Escape/back and repeated opening/cache clear behavior.
- [ ] Resolve effective ritual values and share safe validity checks with altar activation without changing valid recipes.
- [ ] Register journal item, recipe/model and alternate sneak-empty-main-hand altar access. Progress remains in SavedData.
- [ ] Run root `buildAll`, both `:common:verifyCrafting` and `:common:verifyJournal`, Node publisher/recipe tests and release JAR checks.
- [ ] Launch disposable clients/servers where feasible, capture actual game UI screenshots and iterate. Document every failed or unrun runtime scenario explicitly.
- [ ] Independent whole-branch review; fix significant findings and rerun affected checks.
- [ ] Add changelog/user docs/exact-version test report, collect four JARs, commit and prepare a draft PR without merging or publishing.
