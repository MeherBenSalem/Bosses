package com.nightbeam.remnants.journal;

import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.ArrayList;
import java.util.UUID;

/** All entrypoints run on the logical server thread; no progress is supplied by clients. */
public final class BossHunts {
    public static final int SCHEMA_VERSION = HuntSavedData.SCHEMA_VERSION;
    private static final String SAVE_ID = "remnant_bosses_hunts";
    private BossHunts() {}
    private static HuntSavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(HuntSavedData::new, HuntSavedData::load, net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE), SAVE_ID);
    }
    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
    public static BossHunt boss(Entity entity) {
        return BossHunt.fromId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
    private static EncounterCredits.Position position(Entity entity) {
        return new EncounterCredits.Position(entity.level().dimension().location().toString(), entity.getX(), entity.getY(), entity.getZ());
    }
    private static EncounterCredits.Participant participant(ServerPlayer player) {
        return new EncounterCredits.Participant(player.getUUID(), position(player), player.isSpectator());
    }
    private static ServerPlayer contributor(MinecraftServer server, DamageSource source) {
        Entity actor = source.getEntity();
        if (actor == null) actor = source.getDirectEntity();
        // Explicit projectile resolution also covers damage sources that report the projectile itself.
        for (int i = 0; i < 4 && actor instanceof Projectile projectile; i++) actor = projectile.getOwner();
        if (actor instanceof ServerPlayer player) return player;
        if (actor instanceof TamableAnimal pet) {
            UUID owner = pet.getOwnerUUID();
            return owner == null ? null : server.getPlayerList().getPlayer(owner);
        }
        return null;
    }
    public static void damage(LivingEntity entity, DamageSource source, float healthLost) {
        if (entity.level().isClientSide || boss(entity) == null || !EncounterCredits.acceptedDamage(healthLost, 0)) return;
        MinecraftServer server = entity.getServer();
        if (server == null) return;
        ServerPlayer player = contributor(server, source);
        if (player == null || !player.isAlive()) return;
        HuntSavedData data = data(server);
        data.encounters.record(entity.getUUID(), now(server), position(entity), participant(player));
    }
    /** Called only after LivingEntity's actual dead flag was set, once per saved entity. */
    public static void died(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        BossHunt boss = boss(entity);
        MinecraftServer server = entity.getServer();
        if (boss == null || server == null) return;
        HuntSavedData data = data(server);
        for (UUID id : data.encounters.complete(entity.getUUID(), now(server), position(entity), uuid -> {
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            return online == null ? null : participant(online);
        })) progress(server, data, id, boss, HuntState.DEFEATED);
    }
    public static void discover(LivingEntity entity) {
        if (entity.level().isClientSide || !entity.isAlive() || entity.tickCount % 10 != 0) return;
        BossHunt boss = boss(entity);
        MinecraftServer server = entity.getServer();
        if (boss == null || server == null) return;
        HuntSavedData data = data(server);
        EncounterCredits.Position location = position(entity);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator() || player.level() != entity.level() || player.distanceToSqr(entity) > 48 * 48) continue;
            if (EncounterCredits.canDiscover(location, position(player), false, player.hasLineOfSight(entity))) {
                progress(server, data, player.getUUID(), boss, HuntState.ENCOUNTERED);
            }
        }
    }
    private static void progress(MinecraftServer server, HuntSavedData data, UUID player, BossHunt boss, HuntState state) {
        if (!data.advance(player, boss, state)) return;
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) send(online, data, false);
    }
    public static void open(ServerPlayer player) { request(player, SCHEMA_VERSION); }
    public static void request(ServerPlayer player, int schemaVersion) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        HuntSavedData data = data(server);
        if (data.requests.allow(player.getUUID(), now(server), schemaVersion, player.isAlive(), player.isSpectator())) send(player, data, true);
    }
    private static void send(ServerPlayer player, HuntSavedData data, boolean open) {
        ArrayList<ClientboundJournalPacket.Entry> entries = new ArrayList<>(4);
        for (BossHunt boss : BossHunt.values()) {
            entries.add(new ClientboundJournalPacket.Entry(boss.id(), data.state(player.getUUID(), boss), RitualInfo.resolve(boss)));
        }
        Services.NETWORK.sendJournal(player, new ClientboundJournalPacket(open, entries));
    }
    public static void tick(MinecraftServer server) {
        if (now(server) % 20 != 0) return;
        HuntSavedData data = data(server);
        data.encounters.expire(now(server));
        data.requests.expire(now(server));
    }
    public static void logout(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        HuntSavedData data = data(server);
        data.encounters.logout(participant(player));
        data.requests.logout(player.getUUID());
    }
    public static void stop(MinecraftServer server) {
        HuntSavedData data = data(server);
        data.encounters.clear();
        data.requests.clear();
    }
}
