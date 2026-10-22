package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Crossings: the destination is resolved when a crossing starts, the player rises inside
 * the closing eclipse for {@code crossing_ticks}, then lands and the screen opens on the area title.
 */
public final class RealmTravel {
	private static final String REALM_TITLE = "realm.remnant_bosses.eclipsed_realm";
	private static final String RETURN_TITLE = "realm.remnant_bosses.waking_world";

	private record Destination(ServerLevel level, Vec3 pos, float yaw, String title, String subtitle) {
	}

	private static final class Crossing {
		final Destination destination;
		int ticks;

		Crossing(Destination destination) {
			this.destination = destination;
		}
	}

	private static final Map<UUID, Crossing> ACTIVE = new HashMap<>();
	private static final Map<UUID, Integer> COOLDOWN_UNTIL = new HashMap<>();

	private RealmTravel() {
	}

	public static boolean isCrossing(UUID player) {
		return ACTIVE.containsKey(player);
	}

	private static boolean busy(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID())
				|| COOLDOWN_UNTIL.getOrDefault(player.getUUID(), Integer.MIN_VALUE) > player.server.getTickCount();
	}

	/** Walking into an open Waygate. */
	public static void beginWaygate(ServerPlayer player, ServerLevel level, BlockPos core, Direction facing) {
		if (player.isPassenger() || player.isSpectator() || busy(player)) return;
		MinecraftServer server = level.getServer();
		RealmLinks links = RealmLinks.get(server);
		Destination destination;
		try {
			if (EclipsedRealm.isRealm(level)) {
				GlobalPos back = links.outsideFor(server, core);
				destination = back != null ? inFrontOf(server.getLevel(back.dimension()), back.pos(), RETURN_TITLE, "")
						: fallbackOutside(player, RETURN_TITLE);
			} else {
				ServerLevel realm = EclipsedRealm.level(server);
				if (realm == null) {
					player.displayClientMessage(Component.translatable("message.remnant_bosses.waygate.no_realm"), true);
					cooldown(player);
					return;
				}
				GlobalPos here = GlobalPos.of(level.dimension(), core.immutable());
				BlockPos realmCore = links.realmSideFor(realm, here);
				links.setLast(player.getUUID(), here);
				destination = inFrontOf(realm, realmCore, REALM_TITLE, null);
			}
		} catch (RuntimeException e) {
			Constants.LOG.error("Waygate crossing failed for {}", player.getGameProfile().getName(), e);
			cooldown(player);
			return;
		}
		start(player, destination);
	}

	/** Travel between discovered graces, chosen in the grace menu. */
	public static void beginGrace(ServerPlayer player, GlobalPos grace, String name) {
		if (player.isPassenger() || busy(player)) return;
		ServerLevel level = player.server.getLevel(grace.dimension());
		if (level == null) return;
		BlockPos spot = GraceRegistry.standSpot(level, grace.pos());
		float yaw = (float) Math.toDegrees(Math.atan2(spot.getX() - grace.pos().getX(), grace.pos().getZ() - spot.getZ()));
		start(player, new Destination(level, Vec3.atBottomCenterOf(spot), yaw, "", name));
	}

	/** Leave the Realm from a grace: back to the last Waygate this player came through. */
	public static void beginDepart(ServerPlayer player) {
		if (player.isPassenger() || busy(player)) return;
		GlobalPos last = RealmLinks.get(player.server).last(player.server, player.getUUID());
		Destination destination = last != null ? inFrontOf(player.server.getLevel(last.dimension()), last.pos(), RETURN_TITLE, "")
				: fallbackOutside(player, RETURN_TITLE);
		start(player, destination);
	}

	private static void start(ServerPlayer player, Destination destination) {
		int ticks = RealmConfig.crossingTicks();
		ACTIVE.put(player.getUUID(), new Crossing(destination));
		player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0, false, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks + 20, 4, false, false, false));
		player.level().playSound(null, player.blockPosition(), ModSounds.WAYGATE_CROSS.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
		Services.NETWORK.sendRealmFx(player, new ClientboundRealmFxPacket(ClientboundRealmFxPacket.CROSS_BEGIN, ticks, "", ""));
	}

	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		int total = RealmConfig.crossingTicks();
		Iterator<Map.Entry<UUID, Crossing>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Crossing> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Crossing crossing = entry.getValue();
			if (player == null || player.isRemoved() || !player.isAlive()) {
				it.remove();
				continue;
			}
			crossing.ticks++;
			if (crossing.ticks % 2 == 0) {
				double angle = crossing.ticks * 0.6;
				player.serverLevel().sendParticles(ModParticles.GRACE_MOTE.get(), player.getX() + Math.cos(angle) * 0.8,
						player.getY() + 0.2 + crossing.ticks * 0.04, player.getZ() + Math.sin(angle) * 0.8, 2, 0.05, 0.05, 0.05, 0.01);
			}
			if (crossing.ticks < total) continue;
			it.remove();
			arrive(player, crossing.destination);
		}
	}

	private static void arrive(ServerPlayer player, Destination destination) {
		player.removeEffect(MobEffects.LEVITATION);
		player.fallDistance = 0;
		player.teleportTo(destination.level(), destination.pos().x, destination.pos().y, destination.pos().z, destination.yaw(), 0.0f);
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, false));
		cooldown(player);
		String subtitle = destination.subtitle() != null ? destination.subtitle()
				: EclipsedRealm.biomeKey(destination.level(), BlockPos.containing(destination.pos()));
		Services.NETWORK.sendRealmFx(player, new ClientboundRealmFxPacket(ClientboundRealmFxPacket.ARRIVE, 0, destination.title(), subtitle));
		destination.level().playSound(null, BlockPos.containing(destination.pos()), ModSounds.WAYGATE_OPEN.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
	}

	private static void cooldown(ServerPlayer player) {
		COOLDOWN_UNTIL.put(player.getUUID(), player.server.getTickCount() + RealmConfig.crossingCooldown());
	}

	public static void logout(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
		COOLDOWN_UNTIL.remove(player.getUUID());
	}

	public static void stop() {
		ACTIVE.clear();
		COOLDOWN_UNTIL.clear();
	}

	/** Two blocks in front of a Waygate, facing away from it; climbs a little if something stands there. */
	private static Destination inFrontOf(@Nullable ServerLevel level, BlockPos core, String title, @Nullable String subtitle) {
		if (level == null) throw new IllegalStateException("missing level for waygate at " + core);
		Direction facing = level.getBlockState(core).getBlock() instanceof EclipseWaygateBlock
				? level.getBlockState(core).getValue(EclipseWaygateBlock.FACING) : Direction.SOUTH;
		BlockPos land = core.relative(facing, 2);
		for (int i = 0; i < 6 && !(free(level, land) && free(level, land.above())); i++) land = land.above();
		return new Destination(level, Vec3.atBottomCenterOf(land), facing.toYRot(), title, subtitle);
	}

	/** No Waygate to return to: the player's own respawn point outside the Realm, else the world spawn. */
	private static Destination fallbackOutside(ServerPlayer player, String title) {
		MinecraftServer server = player.server;
		BlockPos respawn = player.getRespawnPosition();
		ServerLevel level = server.getLevel(player.getRespawnDimension());
		if (respawn != null && level != null && !EclipsedRealm.isRealm(level)) {
			BlockPos spot = respawn;
			for (int i = 0; i < 4 && !(free(level, spot) && free(level, spot.above())); i++) spot = spot.above();
			return new Destination(level, Vec3.atBottomCenterOf(spot), player.getRespawnAngle(), title, "");
		}
		ServerLevel overworld = server.overworld();
		BlockPos spawn = overworld.getSharedSpawnPos();
		BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
		return new Destination(overworld, Vec3.atBottomCenterOf(top), overworld.getSharedSpawnAngle(), title, "");
	}

	private static boolean free(Level level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}
}
