# PHASE 1B: DETAILED DELETION & REFACTOR CHECKLIST

**This document shows EXACTLY what gets deleted and what gets refactored.**

---

## DELETION VERIFICATION SCRIPT

These 37 files will be completely deleted:

### Procedure Files (27 total - 857 lines)
```bash
# EXACT DELETION LIST
rm src/main/java/tn/naizo/remnants/procedures/AncientRuinBlockOnBlockRightClickedProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/CheckAttackAnimProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/CheckIsIdleAnimProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/CreateMainConfigProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/CreateRemnantConfigProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/CreateRemnantOssukageConfigProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/DashAttackProcedureProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/NinjaSkeletonEntityIsHurtProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/NinjaSkeletonOnEntityTickUpdateProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/OssukageEntityDiesProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/OssukageOnInitialEntitySpawnProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/OssukageSwordLivingEntityIsHitWithToolProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/OssukageSwordRightclickedProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/OssukageSwordToolInHandTickProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/RatDisplayCondition2Procedure.java
rm src/main/java/tn/naizo/remnants/procedures/RatDisplayCondition3Procedure.java
rm src/main/java/tn/naizo/remnants/procedures/RatDisplayCondition4Procedure.java
rm src/main/java/tn/naizo/remnants/procedures/RatDisplayConditionProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/RatOnInitialEntitySpawnProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SkeletonMinionOnInitialEntitySpawnProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SkeletonNinjaDisplayConditionProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SkeletonNinjaLeapConditionProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SkeletonNinjaPlaybackConditionProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SkeletonNinjaSpawnConditionProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SkeletonSpawnConditionProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/SwordOnDeathProcedure.java
rm src/main/java/tn/naizo/remnants/procedures/ThrowKunaisProcedureProcedure.java
rmdir src/main/java/tn/naizo/remnants/procedures/
```

### MCreator-Specific Files (10 total)
```bash
# Init files to delete
rm src/main/java/tn/naizo/remnants/init/RemnantBossesModEntityRenderers.java
rm src/main/java/tn/naizo/remnants/init/RemnantBossesModKeyMappings.java
rm src/main/java/tn/naizo/remnants/init/RemnantBossesModModels.java

# Root files to delete
rm remnant_bosses.mcreator
rmdir models/
```

---

## FILE REFACTORING GUIDE

### 1. `RemnantBossesModItems.java` → `ModItems.java`

**Current Status:** Uses DeferredRegister (GOOD)
**Changes Needed:**
- Rename file
- Update package references
- Ensure all item registrations use Lambda patterns
- Remove MCreator-generated comments

**Current Code:**
```java
public class RemnantBossesModItems {
    public static final DeferredRegister<Item> REGISTRY =
        DeferredRegister.create(ForgeRegistries.ITEMS, RemnantBossesMod.MODID);
    public static final RegistryObject<Item> OSSUKAGE_SWORD =
        REGISTRY.register("ossukage_sword", () -> new OssukageSwordItem());
    // ... more items
}
```

**Changes:**
- Rename class to `ModItems`
- Rename `REGISTRY` to `ITEMS`
- All else stays the same

---

### 2. `RemnantBossesModEntities.java` → `ModEntities.java`

**Current Status:** Needs complete redesign
**Reason:** Spawn eggs should be items, not entity types

**Current Code Issues:**
```java
public static final RegistryObject<Item> RAT_SPAWN_EGG = REGISTRY.register(
    "rat_spawn_egg",
    () -> new ForgeSpawnEggItem(RemnantBossesModEntities.RAT, ...));
```

**Fixed Code:**
```java
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RemnantBossesMod.MODID);

    public static final DeferredRegister<Item> SPAWN_EGGS =
        DeferredRegister.create(ForgeRegistries.ITEMS, RemnantBossesMod.MODID);

    // Entity types
    public static final RegistryObject<EntityType<RatEntity>> RAT =
        ENTITIES.register("rat", () => EntityType.Builder.of(...).build("rat"));

    // Spawn eggs (as items, not wrapped in Forge egg item)
    public static final RegistryObject<Item> RAT_SPAWN_EGG =
        SPAWN_EGGS.register("rat_spawn_egg",
            () => new ForgeSpawnEggItem(RAT, 0xCC666B, 0xFF0000, new Item.Properties()));
}
```

---

### 3. `RemnantBossesModSounds.java` → `ModSounds.java`

**Current Status:** Basic but incomplete
**Changes:**
- Rename class
- Rename `REGISTRY` to `SOUNDS`
- Verify all sounds defined

**Check Missing Sounds:**
- Must include `dash_sound_effect`
- Must include `skeletonfight_theme` (used in death procedure)

---

### 4. `RemnantBossesModBlocks.java` → `ModBlocks.java`

**Current Status:** Minimal but correct
**Changes:**
- Rename class
- Rename `REGISTRY` to `BLOCKS`
- Add any missing blocks

---

### 5. `RemnantBossesModTabs.java` → `ModTabs.java`

**Current Status:** Looks good
**Changes:**
- Rename class
- Rename `REGISTRY` to `TABS`
- Ensure all items are included in creative tab

---

### 6. `RemnantBossesMod.java` → Complete Rewrite

**BEFORE (MCreator-generated):**
```java
@Mod("remnant_bosses")
public class RemnantBossesMod {
    public RemnantBossesMod(FMLJavaModLoadingContext context) {
        // Register static stuff
        MinecraftForge.EVENT_BUS.register(this);
        // Register via init classes
    }

    // BAD: Static work queue
    private static final Collection<...> workQueue = new ConcurrentLinkedQueue<>();

    // BAD: Custom networking boilerplate
    public static <T> void addNetworkMessage(...) {
        PACKET_HANDLER.registerMessage(...);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        // Process queue
    }
}
```

**AFTER (Clean):**
```java
@Mod("remnant_bosses")
public class RemnantBossesMod {
    public static final String MODID = "remnant_bosses";
    public static final Logger LOGGER = LogManager.getLogger(RemnantBossesMod.class);

    public RemnantBossesMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register all registries
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEntities.SPAWN_EGGS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);

        // Setup
        modEventBus.addListener(this::commonSetup);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(EntitySpawnEvents.class);
        MinecraftForge.EVENT_BUS.register(EntityTickEvents.class);
        MinecraftForge.EVENT_BUS.register(EntityDeathEvents.class);
        MinecraftForge.EVENT_BUS.register(PlayerInteractionEvents.class);
        MinecraftForge.EVENT_BUS.register(BlockInteractionEvents.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Remnant Bosses mod loaded");
    }
}
```

---

## ENTITY MODIFICATIONS

### `RatEntity.java`

**Lines to DELETE:**
```java
// DELETE THIS IMPORT:
import tn.naizo.remnants.procedures.RatOnInitialEntitySpawnProcedure;
import tn.naizo.remnants.procedures.CheckIsIdleAnimProcedure;
import tn.naizo.remnants.procedures.CheckAttackAnimProcedure;

// DELETE THIS METHOD CALL:
public void onAddedToWorld() {
    RatOnInitialEntitySpawnProcedure.execute(this);  // ← DELETE
}
```

**In tick method, DELETE:**
```java
@Override
public void tick() {
    super.tick();

    // ✗ DELETE THESE:
    this.animationState0.animateWhen(
        CheckIsIdleAnimProcedure.execute(this),  // ← DELETE
        this.tickCount);

    this.animationState2.animateWhen(
        CheckAttackAnimProcedure.execute(this),  // ← DELETE
        this.tickCount);
}
```

**REPLACE WITH:** (Will be in EntityTickEvents)
```java
// The animation logic moves to EntityTickEvents.onLivingTick()
// Entity stays clean, just defines animation states
```

### `RemnantOssukageEntity.java`

**Delete imports:**
```java
import tn.naizo.remnants.procedures.OssukageOnInitialEntitySpawnProcedure;
import tn.naizo.remnants.procedures.OssukageEntityDiesProcedure;
import tn.naizo.remnants.procedures.NinjaSkeletonEntityIsHurtProcedure;
import tn.naizo.remnants.procedures.NinjaSkeletonOnEntityTickUpdateProcedure;
import tn.naizo.remnants.procedures.CheckIsIdleAnimProcedure;
import tn.naizo.remnants.procedures.CheckAttackAnimProcedure;
import tn.naizo.remnants.procedures.SkeletonNinjaPlaybackConditionProcedure;
import tn.naizo.remnants.procedures.SkeletonNinjaLeapConditionProcedure;
import tn.naizo.remnants.procedures.SkeletonNinjaSpawnConditionProcedure;
```

**Delete methods:**
```java
@Override
protected void hurt(DamageSource source) {
    // ✗ DELETE THIS LINE:
    NinjaSkeletonEntityIsHurtProcedure.execute(...);  // ← DELETE
}

@Override
public void die(DamageSource source) {
    // ✗ DELETE THIS LINE:
    OssukageEntityDiesProcedure.execute(source.getEntity());  // ← DELETE
}

@Override
protected void onAddedToWorld() {
    // ✗ DELETE THIS LINE:
    OssukageOnInitialEntitySpawnProcedure.execute(this.level(), this);  // ← DELETE
}

@Override
public void tick() {
    // ✗ DELETE ALL THESE:
    NinjaSkeletonOnEntityTickUpdateProcedure.execute(...);  // ← DELETE (50+ lines logic)
    this.animationState0.animateWhen(CheckIsIdleAnimProcedure.execute(this), ...);  // ← DELETE
    this.animationState2.animateWhen(CheckAttackAnimProcedure.execute(this), ...);  // ← DELETE
    // ... 4 more animation procedures
}
```

---

## ITEM MODIFICATIONS

### `OssukageSwordItem.java`

**Delete imports:**
```java
import tn.naizo.remnants.procedures.OssukageSwordRightclickedProcedure;
import tn.naizo.remnants.procedures.OssukageSwordLivingEntityIsHitWithToolProcedure;
import tn.naizo.remnants.procedures.OssukageSwordToolInHandTickProcedure;
```

**Delete method bodies (replace with event handling):**
```java
// ✗ DELETE ALL LOGIC HERE:
@Override
public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
    boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
    OssukageSwordLivingEntityIsHitWithToolProcedure.execute(entity);  // ← DELETE
    return retval;
}

@Override
public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
    InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
    OssukageSwordRightclickedProcedure.execute(world, ...);  // ← DELETE (large procedure)
    return ar;
}

@Override
public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
    super.inventoryTick(itemstack, world, entity, slot, selected);
    if (selected)
        OssukageSwordToolInHandTickProcedure.execute(entity);  // ← DELETE
}
```

**Keep the item definition, just remove procedure calls.**

---

## BLOCK MODIFICATIONS

### `AncientAltarBlock.java`

**Delete imports:**
```java
import tn.naizo.remnants.procedures.AncientRuinBlockOnBlockRightClickedProcedure;
```

**Delete from use() method:**
```java
@Override
public InteractionResult use(BlockState blockstate, Level world, BlockPos pos, Player entity, InteractionHand hand, BlockHitResult hit) {
    super.use(blockstate, world, pos, entity, hand, hit);

    // ✗ DELETE THIS LINE:
    AncientRuinBlockOnBlockRightClickedProcedure.execute(world, x, y, z, entity);  // ← DELETE

    return InteractionResult.SUCCESS;
}
```

**This will be handled by BlockInteractionEvents instead.**

---

## RENDERER MODIFICATIONS

### `RatRenderer.java`

**Delete these imports:**
```java
import tn.naizo.remnants.procedures.RatDisplayConditionProcedure;
import tn.naizo.remnants.procedures.RatDisplayCondition2Procedure;
import tn.naizo.remnants.procedures.RatDisplayCondition3Procedure;
import tn.naizo.remnants.procedures.RatDisplayCondition4Procedure;
```

**Replace procedure calls with direct entity checks:**

BEFORE:
```java
public void render(RatEntity entity, ...) {
    if (RatDisplayConditionProcedure.execute(entity)) {
        this.addLayer(new RenderLayer(...));
    }
    if (RatDisplayCondition2Procedure.execute(entity)) {
        this.addLayer(new RenderLayer(...));
    }
}
```

AFTER:
```java
public void render(RatEntity entity, ...) {
    // Direct entity data check instead of procedure
    if ((entity.getEntityData().get(RatEntity.DATA_skin) == 0)) {
        this.addLayer(new RenderLayer(...));
    }
    if ((entity.getEntityData().get(RatEntity.DATA_skin) == 1)) {
        this.addLayer(new RenderLayer(...));
    }
}
```

---

## SUMMARY: LINES DELETED VS KEPT

```
Procedure files:        857 lines DELETED ✓
RemnantBossesMod.java:   88 lines → ~40 lines REWRITTEN
Entities:              ~600 lines → ~500 lines (procedure calls removed)
Items:                 ~100 lines → ~50 lines (procedure calls removed)
Blocks:                 ~100 lines → ~80 lines (procedure calls removed)
Renderers:              ~300 lines → ~250 lines (procedures inlined)

TOTAL DELETED: ~1,100+ lines
TOTAL NEW CODE (events): ~500 lines
NET RESULT: 600 lines cleaner, 100% more maintainable
```

---

## GIT COMMIT STRATEGY

Once Phase 1 complete, you'll have 1 commit:

```bash
git add -A
git commit -m "PHASE 1: Remove all MCreator procedures and boilerplate

- Delete all 27 procedure files (857 lines of procedural code)
- Delete MCreator init helpers (EntityRenderers, KeyMappings, Models)
- Remove all procedure.execute() calls from entities, items, blocks
- Remove static queueServerWork() and networking boilerplate
- Rename init classes: RemnantBossesModXxx → ModXxx
- Refactor main mod class to clean event-subscriber pattern

All procedural logic will be re-implemented as event handlers in Phase 2+.
This is a breaking change but necessary for clean architecture.
"
```

---
