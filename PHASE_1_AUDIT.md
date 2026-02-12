# PHASE 1: MCREATOR REMOVAL AUDIT & ANALYSIS

**Created:** 2026-02-12
**Project:** Remnant Bosses Mod
**Current Version:** 1.20.1 Forge (Will add 1.21.1 NeoForge)
**Scope:** Complete procedural-to-event refactor

---

## EXECUTIVE SUMMARY

- **Current Structure:** Single-module MCreator workspace
- **Total Procedure Files:** 27 files (~857 lines)
- **Procedure Calls:** 26 direct references from entities, items, blocks, renderers
- **MCreator References:** 3 files need deletion, 2 files need major rewrite

**Effort:** Converting ALL procedural logic to event-driven architecture.

---

## FILES TO DELETE (COMPLETE REMOVAL)

### Procedure Files - ALL 27 to be DELETED
Location: `src/main/java/tn/naizo/remnants/procedures/`

```
1.  AncientRuinBlockOnBlockRightClickedProcedure.java       → BLOCK_INTERACTION event
2.  CheckAttackAnimProcedure.java                            → Animation state logic (tick)
3.  CheckIsIdleAnimProcedure.java                            → Animation state logic (tick)
4.  CreateMainConfigProcedure.java                           → CONFIG_LOAD event
5.  CreateRemnantConfigProcedure.java                        → CONFIG_LOAD event
6.  CreateRemnantOssukageConfigProcedure.java                → CONFIG_LOAD event
7.  DashAttackProcedureProcedure.java                        → ENTITY_ATTACK event
8.  NinjaSkeletonEntityIsHurtProcedure.java                  → ENTITY_HURT event
9.  NinjaSkeletonOnEntityTickUpdateProcedure.java            → ENTITY_TICK event (50+ lines)
10. OssukageEntityDiesProcedure.java                         → ENTITY_DEATH event
11. OssukageOnInitialEntitySpawnProcedure.java               → ENTITY_JOIN_LEVEL event
12. OssukageSwordLivingEntityIsHitWithToolProcedure.java     → LIVING_HURT event
13. OssukageSwordRightclickedProcedure.java                  → PLAYER_INTERACT event (throws kunai)
14. OssukageSwordToolInHandTickProcedure.java                → ITEM_TICK event
15. RatDisplayCondition2Procedure.java                       → Renderer condition (simple check)
16. RatDisplayCondition3Procedure.java                       → Renderer condition (simple check)
17. RatDisplayCondition4Procedure.java                       → Renderer condition (simple check)
18. RatDisplayConditionProcedure.java                        → Renderer condition (entity skin check)
19. RatOnInitialEntitySpawnProcedure.java                    → ENTITY_JOIN_LEVEL event
20. SkeletonMinionOnInitialEntitySpawnProcedure.java         → ENTITY_JOIN_LEVEL event
21. SkeletonNinjaDisplayConditionProcedure.java              → Renderer condition (simple check)
22. SkeletonNinjaLeapConditionProcedure.java                 → Animation state logic
23. SkeletonNinjaPlaybackConditionProcedure.java             → Animation state logic
24. SkeletonNinjaSpawnConditionProcedure.java                → Animation state logic
25. SkeletonSpawnConditionProcedure.java                     → Spawn condition check
26. SwordOnDeathProcedure.java                               → ITEM_DEATH event (or just drop)
27. ThrowKunaisProcedureProcedure.java                       → PLAYER_INTERACT event

Total: 857 lines of procedural code
```

### Other MCreator-Specific Files to DELETE

```
src/main/java/tn/naizo/remnants/init/
├── RemnantBossesModEntityRenderers.java       [DELETE - move to ClientSetup]
├── RemnantBossesModKeyMappings.java           [DELETE - move to event handler]
└── RemnantBossesModModels.java                [DELETE - move to ClientSetup]

Root directory:
└── remnant_bosses.mcreator                    [DELETE - no longer needed]

src/main/java/tn/naizo/remnants/
└── models/                                    [DELETE - duplicate of src structure]
```

---

## FILES TO KEEP & REWRITE (in-place modification)

### Registration Classes (Keep names, modernize contents)

| File | Current Lines | Changes |
|------|---------------|---------|
| `RemnantBossesModItems.java` | 44 | Rename → `ModItems.java`, modernize DeferredRegister usage |
| `RemnantBossesModBlocks.java` | 22 | Rename → `ModBlocks.java`, add texture handling |
| `RemnantBossesModEntities.java` | 113 | Rename → `ModEntities.java`, complete rebuild (spawn eggs as items) |
| `RemnantBossesModSounds.java` | 28 | Rename → `ModSounds.java`, verify sound definitions |
| `RemnantBossesModTabs.java` | 55 | Rename → `ModTabs.java`, ensure all items included |

### Main Mod Class (Total rewrite)

**File:** `RemnantBossesMod.java`
**Current:** MCreator-style with static work queue
**New:** Clean event-subscriber pattern with no static hacks

**WHAT CHANGES:**
- Remove `queueServerWork()` static method
- Remove `PACKET_HANDLER` and networking boilerplate (replace with clean NetworkHandler)
- Replace `addNetworkMessage()` with proper Forge network API
- Register all event subscribers via `@SubscribeEvent` classes
- Keep only mod ID constant and registration calls

### Entity Classes (Keep, but remove procedure calls)

| Entity | Procedures Called | Replacement |
|--------|------------------|-------------|
| `RatEntity.java` | `RatOnInitialEntitySpawnProcedure`, `CheckIsIdleAnimProcedure`, `CheckAttackAnimProcedure` | EntitySpawnEvents, EntityTickEvents |
| `RemnantOssukageEntity.java` | `OssukageOnInitialEntitySpawnProcedure`, `OssukageEntityDiesProcedure`, `NinjaSkeletonEntityIsHurtProcedure`, `NinjaSkeletonOnEntityTickUpdateProcedure`, `CheckIs/AttackAnimProcedure` (4 animation states) | EntitySpawnEvents, EntityDeathEvents, EntityHurtEvents, EntityTickEvents |
| `SkeletonMinionEntity.java` | `SkeletonMinionOnInitialEntitySpawnProcedure`, `CheckIsIdleAnimProcedure` | EntitySpawnEvents, EntityTickEvents |
| `KunaiEntity.java` | (None expected) | Keep as-is |

**ACTION:** Remove all `.execute()` calls, keep entity definition and AI goals.

### Item Classes (Keep, but remove procedure calls)

| Item | Procedures Called | Replacement |
|------|-----------------|-------------|
| `OssukageSwordItem.java` | `OssukageSwordRightclickedProcedure`, `OssukageSwordLivingEntityIsHitWithToolProcedure`, `OssukageSwordToolInHandTickProcedure` | PlayerInteractionEvents (rightclick), EntityHurtEvents (hit), ItemTickEvents (inventory tick) |
| `RatFangItem.java` | (Unknown - check) | TBD |
| `FangOnAStickItem.java` | (Unknown - check) | TBD |
| `OldSkeletonBoneItem.java` | (Unknown - check) | TBD |
| `OldSkeletonHeadItem.java` | (Unknown - check) | TBD |

**ACTION:** Remove all procedure calls, replace with event handling.

### Block Classes (Keep, but remove procedure calls)

| Block | Procedures Called | Replacement |
|-------|-----------------|-------------|
| `AncientAltarBlock.java` | `AncientRuinBlockOnBlockRightClickedProcedure` | BlockInteractionEvents (rightclick) |
| `AncientPedestalBlock.java` | (Unknown) | TBD |

**ACTION:** Remove procedure calls from `use()` method.

### Renderer Classes (Keep client-side code, remove procedure calls)

| Renderer | Procedures Called | Replacement |
|----------|-----------------|-------------|
| `RatRenderer.java` | `RatDisplayConditionProcedure`, `RatDisplayCondition2/3/4Procedure` | Direct entity field checks (inline) |
| `RemnantOssukageRenderer.java` | `SkeletonNinjaDisplayConditionProcedure` | Direct entity field check (inline) |
| `SkeletonMinionRenderer.java` | (Unknown) | TBD |
| `KunaiRenderer.java` | (Unknown) | TBD |

**ACTION:** Replace procedure calls with direct entity data/state checks in rendering method.

---

## PROCEDURE CATEGORIZATION & MIGRATION MAP

### Category 1: Entity Lifecycle Procedures (3 total)
**Event:** `EntityJoinLevelEvent` (runs once when entity spawned)

```
OssukageOnInitialEntitySpawnProcedure.java
RatOnInitialEntitySpawnProcedure.java
SkeletonMinionOnInitialEntitySpawnProcedure.java

MIGRATION: EntitySpawnEvents class
@SubscribeEvent
public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getEntity() instanceof RemnantOssukageEntity) {
        setupOssukageSpawn(...);
    }
}
```

### Category 2: Entity Death Procedures (1 total)
**Event:** `LivingDeathEvent`

```
OssukageEntityDiesProcedure.java
  - Runs command: "stopsound @p music remnant_bosses:skeletonfight_theme"

MIGRATION: EntityDeathEvents class
@SubscribeEvent
public static void onEntityDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof RemnantOssukageEntity) {
        stopBossMusic(event);
    }
}
```

### Category 3: Entity Hurt Procedures (1 total)
**Event:** `LivingHurtEvent`

```
NinjaSkeletonEntityIsHurtProcedure.java

MIGRATION: EntityHurtEvents class
@SubscribeEvent
public static void onLivingHurt(LivingHurtEvent event) {
    if (event.getEntity() instanceof RemnantOssukageEntity) {
        handleEntityHurt(event);
    }
}
```

### Category 4: Entity Tick Update Procedures (1 total)
**Event:** `LivingEvent.LivingTickEvent`

```
NinjaSkeletonOnEntityTickUpdateProcedure.java  [50+ lines - MAJOR]
  - AI behavior updates
  - State machine logic
  - Phase transitions
  - Animation checks

MIGRATION: EntityTickEvents class
@SubscribeEvent
public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    if (event.getEntity() instanceof RemnantOssukageEntity) {
        updateOssukageTick((RemnantOssukageEntity) event.getEntity());
    }
}
```

### Category 5: Item/Sword Procedures (3 total)
**Events:** `PlayerInteractEvent.RightClickItem`, `LivingHurtEvent`, `ItemTickEvents`

```
OssukageSwordRightclickedProcedure.java        → PlayerInteractEvent.RightClickItem
OssukageSwordLivingEntityIsHitWithToolProcedure.java → LivingHurtEvent (sword hit)
OssukageSwordToolInHandTickProcedure.java      → LivingEvent.LivingTickEvent (player tick)

MIGRATION: Item-specific event handlers or PlayerInteractionEvents class
```

### Category 6: Block Interaction Procedures (1 total)
**Event:** `PlayerInteractEvent.RightClickBlock`

```
AncientRuinBlockOnBlockRightClickedProcedure.java

MIGRATION: BlockInteractionEvents class
@SubscribeEvent
public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
    if (event.getClickedBlock().getBlock() instanceof AncientAltarBlock) {
        handleAltarClick(event);
    }
}
```

### Category 7: Animation/Condition Procedures (12 total)
**Action:** Inline into renderer or entity tick logic

```
CheckAttackAnimProcedure.java
CheckIsIdleAnimProcedure.java
SkeletonNinjaPlaybackConditionProcedure.java
SkeletonNinjaLeapConditionProcedure.java
SkeletonNinjaSpawnConditionProcedure.java
RatDisplayConditionProcedure.java
RatDisplayCondition2Procedure.java
RatDisplayCondition3Procedure.java
RatDisplayCondition4Procedure.java
SkeletonNinjaDisplayConditionProcedure.java

MIGRATION: Keep logic, inline into:
  - Entity tick update (for animation state)
  - Renderer (for layer display conditions)

Example: Instead of calling procedure, check entity state directly:
animationState.animateWhen(entity.isAttacking(), tickCount);
```

### Category 8: Configuration Procedures (3 total)
**Action:** Delete (config system should be handled elsewhere)

```
CreateMainConfigProcedure.java
CreateRemnantConfigProcedure.java
CreateRemnantOssukageConfigProcedure.java

MIGRATION: Move to proper config file loading or remove if redundant
```

### Category 9: Other Attack/Combat Procedures (2 total)
**Events:** Unclear - needs investigation

```
DashAttackProcedureProcedure.java         → ENTITY_ATTACK event or custom logic
ThrowKunaisProcedureProcedure.java        → PLAYER_INTERACT event (likely sword rightclick)
SwordOnDeathProcedure.java                → Entity death or just item drop
SkeletonSpawnConditionProcedure.java      → Spawn condition (delete or move to entity spawn check)
```

---

## MCREATOR NETWORKING PATTERN TO REMOVE

### Current (Bad)
```java
// RemnantBossesMod.java
private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue =
    new ConcurrentLinkedQueue<>();

public static void queueServerWork(int tick, Runnable action) {
    if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
        workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
}

@SubscribeEvent
public void tick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
        // Process queue
    }
}
```

**Why bad:**
- Global mutable state
- Thread-group checking instead of proper sided logic
- Manual tick counting hack
- Hard to test, hard to debug

### New (Clean)
```java
// Use Minecraft's scheduler instead:
server.tell(new TickTask(delay, () -> {
    // Will run on main thread after delay ticks
}));

// Or use modded events directly for immediate processing
```

---

## SUMMARY TABLE: ALL FILES & ACTIONS

| File Path | Type | Action | Priority |
|-----------|------|--------|----------|
| `procedures/` (27 files) | Code | **DELETE ALL** | CRITICAL |
| `init/RemnantBossesModEntityRenderers.java` | Code | DELETE | HIGH |
| `init/RemnantBossesModKeyMappings.java` | Code | DELETE | HIGH |
| `init/RemnantBossesModModels.java` | Code | DELETE | HIGH |
| `init/RemnantBossesModItems.java` | Code | RENAME → ModItems + REWRITE | HIGH |
| `init/RemnantBossesModBlocks.java` | Code | RENAME → ModBlocks + UPDATE | MEDIUM |
| `init/RemnantBossesModEntities.java` | Code | RENAME → ModEntities + REWRITE | HIGH |
| `init/RemnantBossesModSounds.java` | Code | RENAME → ModSounds + UPDATE | MEDIUM |
| `init/RemnantBossesModTabs.java` | Code | RENAME → ModTabs + UPDATE | MEDIUM |
| `RemnantBossesMod.java` | Code | **COMPLETE REWRITE** | CRITICAL |
| `entity/RatEntity.java` | Code | REMOVE PROCEDURE CALLS | HIGH |
| `entity/RemnantOssukageEntity.java` | Code | REMOVE PROCEDURE CALLS | HIGH |
| `entity/SkeletonMinionEntity.java` | Code | REMOVE PROCEDURE CALLS | HIGH |
| `item/OssukageSwordItem.java` | Code | REMOVE PROCEDURE CALLS | HIGH |
| `block/AncientAltarBlock.java` | Code | REMOVE PROCEDURE CALLS | HIGH |
| `client/renderer/*.java` | Code | INLINE PROCEDURE LOGIC | MEDIUM |
| `models/` folder | Assets | DELETE | LOW |
| `remnant_bosses.mcreator` | File | DELETE | LOW |

---

## NEW FILES TO CREATE (Phase 2+)

These will be created as part of the event-driven refactor:

```
event/
├── EntitySpawnEvents.java       (3 spawn procedures)
├── EntityDeathEvents.java       (1 death procedure)
├── EntityTickEvents.java        (1 tick procedure, 12 animation checks)
├── EntityHurtEvents.java        (1 hurt procedure)
├── PlayerInteractionEvents.java (3 sword/item procedures)
├── BlockInteractionEvents.java  (1 block procedure)
├── ClientEvents.java            (client-side rendering events)
└── CommonEvents.java            (shared event setup)

client/
└── ClientSetup.java             (renderer/model registration)

network/
└── NetworkHandler.java          (clean Forge networking)
```

---

## VALIDATION CHECKLIST

- [ ] All 27 procedure files will be deleted
- [ ] All procedure calls removed from entity/item/block classes
- [ ] All MCreator init files deleted or renamed
- [ ] No more "RemnantBossesModXxx" class names (rename to ModXxx)
- [ ] No more `RemnantBossesMod.queueServerWork()` calls
- [ ] No more `if (world.isClientSide()) ...` pattern (use @OnlyIn)
- [ ] All game logic converted to @SubscribeEvent handlers
- [ ] All resources in `src/main/resources/assets/remnant_bosses/`
- [ ] No models/ folder in root

---

## NEXT STEPS

Once this audit is approved:

1. **PHASE 2:** Create multi-module Gradle structure
2. **PHASE 3:** Move source to version-specific modules
3. **PHASE 4:** Rewrite registration classes
4. **PHASE 5:** Create event handler classes
5. **PHASE 6:** Convert entities, items, blocks
6. **PHASE 7:** Clean up resources and MCCreator references
7. **PHASE 8:** Compile and test

---
