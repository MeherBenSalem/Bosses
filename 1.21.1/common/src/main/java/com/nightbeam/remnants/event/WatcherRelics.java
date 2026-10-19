package com.nightbeam.remnants.event;

import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.item.ArchivistArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side rules for the Forgotten Watcher's rewards, loader-agnostic: the Watcher's Eye passive and
 * blink, and the Archivist set bonuses (Quiet Mind, Archive Ward). Loaders call {@link #serverTick},
 * {@link #onLogout} and {@link #stop}; {@code RelicDamageMixin} asks {@link #wardNegates} before every
 * hurt.
 */
public final class WatcherRelics {
	public static final int EYE_INTERVAL = 200, EYE_GLOW_TICKS = 60, BLINK_HUNGER = 2, WARD_COOLDOWN = 600;
	public static final double EYE_RADIUS = 24, BLINK_DISTANCE = 8;
	private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.62f, 0.53f, 0.88f), 1.0f);
	private static final Map<UUID, Long> WARD_READY = new HashMap<>();

	private WatcherRelics() {
	}

	public static int archivistPieces(LivingEntity entity) {
		int pieces = 0;
		for (ItemStack armor : entity.getArmorSlots()) {
			if (armor.getItem() instanceof ArchivistArmorItem) pieces++;
		}
		return pieces;
	}

	/** Once per second: Quiet Mind; every 200 ticks: the Watcher's Eye passive. */
	public static void serverTick(MinecraftServer server) {
		int tick = server.getTickCount();
		if (tick % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || player.isSpectator()) continue;
			if (archivistPieces(player) >= 2) {
				player.removeEffect(MobEffects.BLINDNESS);
				player.removeEffect(MobEffects.DARKNESS);
			}
			if (tick % EYE_INTERVAL == 0 && player.getOffhandItem().is(ModItems.WATCHERS_EYE.get())) reveal(player);
		}
	}

	/** Watcher's Eye passive: hostile creatures within 24 blocks glow for 60 ticks. */
	private static void reveal(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		boolean any = false;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(EYE_RADIUS),
				e -> e.isAlive() && e instanceof Enemy && e.distanceToSqr(player) <= EYE_RADIUS * EYE_RADIUS)) {
			entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EYE_GLOW_TICKS, 0, false, false), player);
			any = true;
		}
		if (any) level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	/**
	 * Watcher's Eye active: blink up to 8 blocks toward where the player looks, stopping short of walls; the
	 * player lands safely and pays 2 hunger points (not in creative). Returns false when there is no room.
	 */
	public static boolean blink(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 look = player.getLookAngle();
		Vec3 eye = player.getEyePosition();
		var hit = level.clip(new ClipContext(eye, eye.add(look.scale(BLINK_DISTANCE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double reach = hit.getType() == HitResult.Type.MISS ? BLINK_DISTANCE : Math.max(0, hit.getLocation().distanceTo(eye) - 0.6);
		Vec3 feetOffset = player.position().subtract(eye);
		Vec3 destination = null;
		for (double d = reach; d >= 1.0; d -= 0.5) {
			Vec3 feet = eye.add(look.scale(d)).add(feetOffset);
			AABB box = player.getBoundingBox().move(feet.subtract(player.position()));
			if (level.noCollision(player, box)) {
				destination = feet;
				break;
			}
		}
		if (destination == null) return false;
		Vec3 from = player.position().add(0, 1, 0);
		level.sendParticles(VIOLET, from.x, from.y, from.z, 20, 0.3, 0.6, 0.3, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y, from.z, 16, 0.3, 0.6, 0.3, 0.05);
		player.teleportTo(destination.x, destination.y, destination.z);
		player.resetFallDistance();
		Vec3 to = player.position().add(0, 1, 0);
		level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 10, 0.3, 0.6, 0.3, 0.04);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
		if (!player.getAbilities().instabuild) {
			player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - BLINK_HUNGER));
		}
		return true;
	}

	/**
	 * Archivist set, four pieces (Archive Ward): once every 600 ticks the next projectile that would hit the
	 * wearer is negated. Called before every hurt; returns true when this hit is negated.
	 */
	public static boolean wardNegates(LivingEntity victim, DamageSource source) {
		if (victim.level().isClientSide || !(victim instanceof Player player) || !source.is(DamageTypeTags.IS_PROJECTILE)) return false;
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || archivistPieces(player) < 4) return false;
		long now = victim.level().getGameTime();
		Long ready = WARD_READY.get(player.getUUID());
		if (ready != null && now < ready) return false;
		WARD_READY.put(player.getUUID(), now + WARD_COOLDOWN);
		if (victim.level() instanceof ServerLevel level) {
			Vec3 at = player.position().add(0, 1.1, 0);
			level.sendParticles(VIOLET, at.x, at.y, at.z, 16, 0.5, 0.6, 0.5, 0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 8, 0.4, 0.5, 0.4, 0.2);
			level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.8f);
		}
		return true;
	}

	public static void onLogout(ServerPlayer player) {
		WARD_READY.remove(player.getUUID());
	}

	public static void stop() {
		WARD_READY.clear();
	}
}
