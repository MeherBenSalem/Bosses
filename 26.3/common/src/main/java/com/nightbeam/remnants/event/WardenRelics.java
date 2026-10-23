package com.nightbeam.remnants.event;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.entity.Equipment;
import net.minecraft.util.ARGB;
import com.nightbeam.remnants.entity.HollowLegion;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.item.WardenArmorItem;
import com.nightbeam.remnants.item.WardensGraspItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-side rules for the Gravebound Warden's rewards, loader-agnostic: the "chained" state (Crypt
 * Sigil, Jailer's Hold), the Crypt Sigil passive, Warden armor bonuses and the Warden's Grasp sweep.
 * Loaders call {@link #serverTick}, {@link #afterDamage}, {@link #onLogout} and {@link #stop};
 * {@code RelicDamageMixin} calls {@link #scaleIncoming} for every hurt.
 */
public final class WardenRelics {
	public static final Identifier CHAINED = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chained");
	public static final double SIGIL_REDUCTION = 0.10, UNDEAD_BONUS = 0.10, JAILER_CHANCE = 0.15, SIGIL_RADIUS = 12;
	public static final int SIGIL_CHAIN_TICKS = 60, SIGIL_WITHER_TICKS = 60, JAILER_TICKS = 20, JAILER_COOLDOWN = 100,
			GRASP_SLOW_TICKS = 40;
	private static final DustParticleOptions RUST = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.65f, 0.38f, 0.23f), 0.9f);
	/** Chained creatures and the game time their chains fall away. */
	private static final Map<LivingEntity, Long> CHAINS = new WeakHashMap<>();
	private static final Map<UUID, Long> JAILER_READY = new HashMap<>();
	private static final Map<Player, Long> SWEEPS = new WeakHashMap<>();

	private WardenRelics() {
	}

	/** Holds {@code target} in place for {@code ticks}: it cannot move but can still attack. */
	public static void chain(LivingEntity target, int ticks) {
		if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) return;
		long until = level.getGameTime() + ticks;
		CHAINS.merge(target, until, Math::max);
		AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && !speed.hasModifier(CHAINED)) {
			speed.addTransientModifier(new AttributeModifier(CHAINED, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
		target.syncVelocity = true;
		level.sendParticles(RUST, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 16,
				target.getBbWidth() * 0.6, target.getBbHeight() * 0.4, target.getBbWidth() * 0.6, 0);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2f, 0.7f);
	}

	public static boolean isChained(LivingEntity entity) {
		Long until = CHAINS.get(entity);
		return until != null && until > entity.level().getGameTime();
	}

	private static void release(LivingEntity entity) {
		AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.removeModifier(CHAINED);
	}

	/** Every tick: chained creatures stay put; expired chains fall away. */
	public static void serverTick(MinecraftServer server) {
		if (CHAINS.isEmpty()) return;
		for (LivingEntity entity : new ArrayList<>(CHAINS.keySet())) {
			Long until = CHAINS.get(entity);
			if (until == null) continue;
			if (entity.isRemoved() || !entity.isAlive() || until <= entity.level().getGameTime()) {
				CHAINS.remove(entity);
				release(entity);
				continue;
			}
			if (!(entity instanceof Player)) {
				Vec3 motion = entity.getDeltaMovement();
				entity.setDeltaMovement(0, Math.min(0, motion.y), 0);
			}
			if (entity.tickCount % 5 == 0 && entity.level() instanceof ServerLevel level) {
				level.sendParticles(RUST, entity.getX(), entity.getY() + 0.2, entity.getZ(), 4, entity.getBbWidth() * 0.5, 0.1, entity.getBbWidth() * 0.5, 0);
			}
		}
	}

	public static int wardenPieces(LivingEntity entity) {
		int pieces = 0;
		for (ItemStack armor : Equipment.armor(entity)) {
			if (armor.getItem() instanceof WardenArmorItem) pieces++;
		}
		return pieces;
	}

	private static boolean undead(Entity entity) {
		return entity != null && entity.is(EntityTypeTags.UNDEAD);
	}

	/**
	 * Called from LivingEntity.hurt before anything else. Crypt Sigil (off-hand): undead deal 10% less to
	 * the holder. Warden armor, two pieces: +10% damage against undead.
	 */
	public static float scaleIncoming(LivingEntity victim, DamageSource source, float amount) {
		if (victim.level().isClientSide() || amount <= 0) return amount;
		Entity attacker = source.getEntity();
		if (attacker == null || attacker == victim) return amount;
		float scaled = amount;
		if (victim instanceof Player player && undead(attacker) && player.getOffhandItem().is(ModItems.CRYPT_SIGIL.get())) {
			scaled *= (float) (1 - SIGIL_REDUCTION);
		}
		if (attacker instanceof LivingEntity living && undead(victim) && wardenPieces(living) >= 2) {
			scaled *= (float) (1 + UNDEAD_BONUS);
		}
		return scaled;
	}

	/** Four pieces, Jailer's Hold: a melee hit has a 15% chance to chain the target for 20 ticks (every 100 ticks at most). */
	public static void afterDamage(LivingEntity victim, DamageSource source, float dealt) {
		if (dealt <= 0 || !victim.isAlive() || victim.level().isClientSide()) return;
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity wearer) || attacker == victim || source.getDirectEntity() != attacker) return;
		if (wardenPieces(wearer) < 4) return;
		long now = victim.level().getGameTime();
		Long ready = JAILER_READY.get(wearer.getUUID());
		if (ready != null && now < ready) return;
		if (wearer.getRandom().nextDouble() >= JAILER_CHANCE) return;
		JAILER_READY.put(wearer.getUUID(), now + JAILER_COOLDOWN);
		chain(victim, JAILER_TICKS);
	}

	/** Sigil active: chain the nearest enemy within 12 blocks for 60 ticks; the holder takes Wither I for 60 ticks. */
	public static boolean sigilActive(ServerPlayer player) {
		ServerLevel level = player.level();
		LivingEntity nearest = null;
		double best = SIGIL_RADIUS * SIGIL_RADIUS;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SIGIL_RADIUS),
				e -> e != player && e.isAlive() && !e.isSpectator() && isEnemy(player, e))) {
			double d = entity.distanceToSqr(player);
			if (d <= best && player.hasLineOfSight(entity)) {
				best = d;
				nearest = entity;
			}
		}
		if (nearest == null) return false;
		chain(nearest, SIGIL_CHAIN_TICKS);
		player.addEffect(new MobEffectInstance(MobEffects.WITHER, SIGIL_WITHER_TICKS, 0));
		Vec3 from = player.position().add(0, 1.0, 0), to = nearest.position().add(0, nearest.getBbHeight() * 0.5, 0);
		for (int i = 0; i <= 12; i++) {
			Vec3 p = from.lerp(to, i / 12.0);
			level.sendParticles(i % 3 == 0 ? ParticleTypes.SOUL : RUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 1.2f, 0.6f);
		return true;
	}

	/** Hostile creatures and anything hunting the player; never players, the player's allies or tamed pets. */
	private static boolean isEnemy(Player player, LivingEntity entity) {
		if (entity instanceof Player || HollowLegion.ownedByAPlayer(entity)) return false;
		if (entity instanceof OwnableEntity ownable && (ownable.getOwnerReference() != null && player.getUUID().equals(ownable.getOwnerReference().getUUID()))) return false;
		return entity instanceof Enemy || (entity instanceof Mob mob && mob.getTarget() == player);
	}

	/**
	 * Called by {@code PlayerSweepMixin} when a player's attack sweeps, after the sweep victims were hit.
	 * With Warden's Grasp in the main hand, the struck target and every creature the sweep hurt this tick
	 * get Slowness I.
	 */
	public static void onSweep(Player player, Entity target) {
		if (!(player.level() instanceof ServerLevel level) || !(player.getMainHandItem().getItem() instanceof WardensGraspItem)) return;
		SWEEPS.put(player, level.getGameTime());
		AABB box = target.getBoundingBox().inflate(1.0, 0.25, 1.0);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
			boolean hurtNow = victim.getLastHurtByMob() == player && victim.getLastHurtByMobTimestamp() == victim.tickCount;
			if (victim != target && (!hurtNow || player.distanceToSqr(victim) >= 9.0)) continue;
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, GRASP_SLOW_TICKS, 0), player);
		}
	}

	/** Whether {@code attacker}'s attack this tick was a Warden's Grasp sweep. */
	public static boolean sweptThisTick(LivingEntity attacker) {
		return attacker instanceof Player player && SWEEPS.getOrDefault(player, Long.MIN_VALUE) == player.level().getGameTime();
	}

	public static void onLogout(ServerPlayer player) {
		JAILER_READY.remove(player.getUUID());
		SWEEPS.remove(player);
		if (CHAINS.remove(player) != null) release(player);
	}

	public static void stop() {
		CHAINS.clear();
		JAILER_READY.clear();
		SWEEPS.clear();
	}
}
