package com.nightbeam.runtime;

import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.item.BossJournalItem;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.journal.BossHunt;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.journal.HuntSavedData;
import com.nightbeam.remnants.journal.HuntState;
import java.util.EnumMap;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Development-only probe. Progress always comes from the production gameplay hooks. */
public final class BossHuntsRuntimeProbe implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("BossHuntsRuntimeTest");
    public static volatile int phase; // 1 pristine, 2 encountered, 3 defeated, -1 failure
    public static volatile String failure;
    private static final EnumMap<BossHunt, LivingEntity> bosses = new EnumMap<>(BossHunt.class);
    private static int ticks;
    private static int action;
    private static int actionTick;
    private static boolean initialized;
    private static ServerPlayer actor;
    private static UUID cancelledDeath;
    private static boolean cancellationObserved;

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(BossHuntsRuntimeProbe::tick);
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity.getUUID().equals(cancelledDeath)) {
                cancellationObserved = true;
                return false;
            }
            return true;
        });
        LOG.info("DEV ONLY: armed for exact world folder BossHuntsValidation; multiplayer checks UNRUN");
    }

    private static HuntSavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HuntSavedData::load,
                HuntSavedData::new, "remnant_bosses_hunts");
    }

    private static void tick(MinecraftServer server) {
        try {
            ticks++;
            if (phase < 0 || server.getPlayerList().getPlayers().isEmpty()) return;
            if (!initialized) {
                if (!server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("BossHuntsValidation")) {
                    fail("Refusing to mutate a world outside exact disposable folder BossHuntsValidation");
                    return;
                }
                actor = server.getPlayerList().getPlayers().get(0);
                if (Boolean.getBoolean("bossHuntsRuntimeReload")) {
                    for (BossHunt boss : BossHunt.values()) require(data(server).state(actor.getUUID(), boss)
                            == HuntState.DEFEATED, "actual restarted-world SavedData " + boss.id());
                    initialized = true;
                    phase = 3;
                    LOG.info("PASS actual restarted-world persistence for actor {}", actor.getUUID());
                    return;
                }
                for (BossHunt boss : BossHunt.values()) require(data(server).state(actor.getUUID(), boss)
                        == HuntState.UNDISCOVERED, "disposable world has pristine " + boss.id());
                actor.setGameMode(GameType.CREATIVE);
                // A small isolated platform high above terrain, only inside the disposable copy.
                for (int x = 48; x <= 80; x++) for (int z = 48; z <= 80; z++)
                    server.overworld().setBlock(new BlockPos(x, 200, z), Blocks.STONE.defaultBlockState(), 2);
                actor.teleportTo(server.overworld(), 64.5, 201, 64.5, 0, 0);
                actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOSS_JOURNAL.get()));
                initialized = true;
                phase = 1;
                LOG.info("PASS pristine SavedData, disposable-world guard, real integrated server actor {}", actor.getUUID());
            }
            if (action == 1 && ticks - actionTick == 50) {
                require(data(server).state(actor.getUUID(), BossHunt.KOTSUKAGE) == HuntState.UNDISCOVERED,
                        "real stone wall prevents line-of-sight discovery");
                require(data(server).state(actor.getUUID(), BossHunt.HOLLOW_SOVEREIGN) == HuntState.UNDISCOVERED,
                        "real boss outside 48 blocks remains undiscovered");
                for (int z = 54; z <= 64; z++) for (int y = 201; y <= 206; y++)
                    server.overworld().setBlock(new BlockPos(68, y, z), Blocks.AIR.defaultBlockState(), 2);
                bosses.get(BossHunt.HOLLOW_SOVEREIGN).moveTo(72.5, 201, 72.5, 0, 0);
            }
            if (action == 1 && ticks - actionTick > 100) {
                for (BossHunt boss : BossHunt.values()) require(data(server).state(actor.getUUID(), boss)
                        == HuntState.ENCOUNTERED, "natural tick/LOS discovery " + boss.id());
                phase = 2;
                action = 0;
                LOG.info("PASS all four real registered entities discovered through production tick mixin");
            } else if (action == 2 && ticks - actionTick == 30) {
                for (BossHunt boss : BossHunt.values()) {
                    LivingEntity entity = bosses.get(boss);
                    if (entity instanceof RemnantOssukageEntity ossukage) ossukage.setEntityState("idle");
                    entity.invulnerableTime = 0;
                    if (boss == BossHunt.HOLLOW_SOVEREIGN) {
                        cancelledDeath = entity.getUUID();
                        cancellationObserved = false;
                        entity.hurt(actor.damageSources().playerAttack(actor), 1000000F);
                        require(cancellationObserved, "real Fabric ALLOW_DEATH cancellation observed");
                        require(data(server).state(actor.getUUID(), boss) == HuntState.ENCOUNTERED,
                                "cancelled death awards no defeat");
                        CompoundTag cancelled = new CompoundTag();
                        entity.saveWithoutId(cancelled);
                        require(!cancelled.getBoolean("RemnantBossHuntProcessed"), "cancelled death has no processed marker");
                        entity.setHealth(entity.getMaxHealth());
                        cancelledDeath = null;
                        entity.invulnerableTime = 0;
                    }
                    DamageSource lethal = boss == BossHunt.HOLLOW_SOVEREIGN ? actor.damageSources().playerAttack(actor)
                            : actor.damageSources().generic();
                    require(entity.hurt(lethal, 1000000F), "accepted lethal hit " + boss.id());
                    require(!entity.isAlive(), "actual accepted death " + boss.id());
                    require(data(server).state(actor.getUUID(), boss) == HuntState.DEFEATED,
                            "production accepted-death credit " + boss.id());
                    if (boss != BossHunt.HOLLOW_SOVEREIGN) LOG.info("PASS environmental final blow retains earlier contributor {}", boss.id());
                    CompoundTag tag = new CompoundTag();
                    entity.saveWithoutId(tag);
                    require(tag.getBoolean("RemnantBossHuntProcessed"), "persistent accepted-death marker " + boss.id());
                    CompoundTag before = data(server).snapshot();
                    entity.die(actor.damageSources().playerAttack(actor));
                    require(before.equals(data(server).snapshot()), "repeated die idempotence " + boss.id());
                }
                server.saveAllChunks(false, true, true);
                HuntSavedData reread = HuntSavedData.load(data(server).snapshot());
                for (BossHunt boss : BossHunt.values()) require(reread.state(actor.getUUID(), boss)
                        == HuntState.DEFEATED, "SavedData reload " + boss.id());
                phase = 3;
                action = 0;
                LOG.info("PASS all four accepted positive damage/death credits, owned Arrow/Wolf DamageSources, "
                        + "environmental finish, Fabric death cancellation, markers, idempotence, save/reload");
            }
        } catch (Throwable error) {
            fail(error.toString());
            LOG.error("Runtime verification failed", error);
        }
    }

    /** Called only via MinecraftServer.execute from the real client. */
    public static void discover(MinecraftServer server) {
        try {
            int i = 0;
            for (BossHunt boss : BossHunt.values()) {
                var type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(boss.id()));
                var created = type.create(server.overworld());
                require(created instanceof LivingEntity, "registered living boss " + boss.id());
                LivingEntity entity = (LivingEntity) created;
                int[][] positions = {{56, 56}, {72, 56}, {56, 72}, {72, 72}};
                entity.moveTo(positions[i][0] + 0.5, 201, positions[i++][1] + 0.5, 0, 0);
                if (boss == BossHunt.HOLLOW_SOVEREIGN) entity.moveTo(128.5, 201, 64.5, 0, 0);
                entity.setNoGravity(true);
                if (entity instanceof Mob mob) mob.setNoAi(true);
                require(server.overworld().addFreshEntity(entity), "real spawn " + boss.id());
                bosses.put(boss, entity);
            }
            for (int z = 54; z <= 64; z++) for (int y = 201; y <= 206; y++)
                server.overworld().setBlock(new BlockPos(68, y, z), Blocks.STONE.defaultBlockState(), 2);
            action = 1;
            actionTick = ticks;
        } catch (Throwable error) { fail(error.toString()); }
    }

    public static void defeat(MinecraftServer server) {
        try {
            for (BossHunt boss : BossHunt.values()) {
                LivingEntity entity = bosses.get(boss);
                if (entity instanceof RemnantOssukageEntity ossukage) ossukage.setEntityState("idle");
                entity.invulnerableTime = 0;
                float before = entity.getHealth();
                DamageSource source = actor.damageSources().playerAttack(actor);
                if (boss == BossHunt.KOTSUKAGE) {
                    Arrow arrow = new Arrow(actor.level(), actor);
                    arrow.setOwner(actor);
                    source = actor.damageSources().arrow(arrow, actor);
                    LOG.info("SOURCE real owned Arrow object / vanilla arrow DamageSource; programmatic hurt, physics impact UNRUN");
                } else if (boss == BossHunt.UMBRAKAR) {
                    Wolf wolf = EntityType.WOLF.create(actor.level());
                    require(wolf != null, "real Wolf factory");
                    wolf.tame(actor);
                    source = actor.damageSources().mobAttack(wolf);
                    LOG.info("SOURCE real tamed Wolf object / mobAttack DamageSource; programmatic hurt, pet AI attack UNRUN");
                }
                require(entity.hurt(source, 2F) && entity.getHealth() < before,
                        "accepted positive contributor hit " + boss.id());
            }
            action = 2;
            actionTick = ticks;
        } catch (Throwable error) { fail(error.toString()); }
    }

    public static void open(MinecraftServer server, boolean itemRoute) {
        if (actor == null || phase < 0) return;
        if (itemRoute) ((BossJournalItem) ModItems.BOSS_JOURNAL.get()).use(actor.level(), actor, InteractionHand.MAIN_HAND);
        else BossHunts.open(actor);
    }

    public static void require(boolean value, String check) {
        if (!value) throw new IllegalStateException(check);
        LOG.info("PASS {}", check);
    }
    public static void fail(String message) {
        failure = message;
        phase = -1;
        LOG.error("FAIL {}", message);
    }
}
