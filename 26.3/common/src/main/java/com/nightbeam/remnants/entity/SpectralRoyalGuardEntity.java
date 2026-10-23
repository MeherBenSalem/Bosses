package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Hollow Sentinel variant summoned by the Sovereign (phase 2 and 3) or by a player's Sovereign's
 * Sigil. Boss guards fight for the Legion; Sigil guards fight what their owner fights and never harm
 * players or anything a player owns. Neither kind is saved, drops loot or gives experience.
 */
public class SpectralRoyalGuardEntity extends HollowSentinelEntity implements OwnableEntity {
	private static final EntityDataAccessor<Boolean> PLAYER_ALLIED = SynchedEntityData.defineId(SpectralRoyalGuardEntity.class, EntityDataSerializers.BOOLEAN);
	private UUID ownerId;
	private int lifeTicks = 600;

	public SpectralRoyalGuardEntity(EntityType<? extends SpectralRoyalGuardEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(20, 4, 4, 0.25);
	}

	/** Spawns a guard bound to {@code boss}, rising out of the floor. */
	public static SpectralRoyalGuardEntity forBoss(ServerLevel level, HollowSovereignEntity boss, Vec3 at) {
		SpectralRoyalGuardEntity guard = ModEntities.SPECTRAL_ROYAL_GUARD.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (guard == null) return null;
		guard.ownerId = boss.getUUID();
		guard.lifeTicks = (int) guard.cfg("boss_life_ticks", 20, 24000);
		guard.place(level, at, boss.getYRot());
		if (boss.getTarget() != null) guard.setTarget(boss.getTarget());
		return guard;
	}

	/** Spawns a guard allied to {@code player} (Sovereign's Sigil). */
	public static SpectralRoyalGuardEntity forPlayer(ServerLevel level, Player player, Vec3 at) {
		SpectralRoyalGuardEntity guard = ModEntities.SPECTRAL_ROYAL_GUARD.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (guard == null) return null;
		guard.ownerId = player.getUUID();
		guard.entityData.set(PLAYER_ALLIED, true);
		guard.lifeTicks = (int) guard.cfg("sigil_life_ticks", 20, 24000);
		guard.place(level, at, player.getYRot());
		return guard;
	}

	private void place(ServerLevel level, Vec3 at, float yaw) {
		snapTo(at.x, at.y, at.z, yaw, 0);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		setPersistenceRequired();
		begin("rise", null);
		level.addFreshEntity(this);
		level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.2, at.z, 14, 0.4, 0.1, 0.4, 0.03);
	}

	@Override
	public String configId() {
		return "spectral_royal_guard";
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PLAYER_ALLIED, false);
	}

	public boolean isPlayerAllied() {
		return entityData.get(PLAYER_ALLIED);
	}

	public UUID ownerId() {
		return ownerId;
	}

	@Override
	public EntityReference<LivingEntity> getOwnerReference() {
		return isPlayerAllied() && ownerId != null ? EntityReference.of(ownerId) : null;
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (p, targetLevel) -> !isPlayerAllied()));
	}

	@Override
	protected void serverTick() {
		super.serverTick();
		if (--lifeTicks <= 0) {
			vanish();
			return;
		}
		if (tickCount % 10 != 0) return;
		Entity owner = ownerId == null || !(level() instanceof ServerLevel server) ? null
				: isPlayerAllied() ? level().getPlayerByUUID(ownerId) : server.getEntity(ownerId);
		if (!(owner instanceof LivingEntity living) || !living.isAlive() || owner.level() != level()) {
			vanish();
			return;
		}
		if (isPlayerAllied()) followPlayer((Player) living);
		else if (living instanceof Mob boss && boss.getTarget() != null && boss.getTarget() != getTarget() && canHit(boss.getTarget())) {
			setTarget(boss.getTarget());
		}
	}

	private void followPlayer(Player owner) {
		LivingEntity current = getTarget();
		if (current != null && (!current.isAlive() || !canHit(current))) setTarget(null);
		LivingEntity wanted = null;
		if (owner.getLastHurtByMob() != null) wanted = owner.getLastHurtByMob();
		else if (owner.getLastHurtMob() != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100) wanted = owner.getLastHurtMob();
		if (wanted == null) {
			for (Mob mob : level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(16), m -> m.getTarget() == owner)) {
				if (canHit(mob)) {
					wanted = mob;
					break;
				}
			}
		}
		if (wanted != null && wanted != getTarget() && canHit(wanted)) setTarget(wanted);
		if (getTarget() == null && action().isEmpty() && distanceToSqr(owner) > 36) {
			getNavigation().moveTo(owner, 1.1);
		}
		if (distanceToSqr(owner) > 32 * 32) teleportTo(owner.getX(), owner.getY(), owner.getZ());
	}

	public void vanish() {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.2, getZ(), 18, 0.35, 0.7, 0.35, 0.02);
			playSound(SoundEvents.SOUL_ESCAPE.value(), 0.8f, 0.8f);
		}
		discard();
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target != null && isPlayerAllied() && !canHit(target)) return;
		super.setTarget(target);
	}

	@Override
	public boolean canHit(LivingEntity entity) {
		if (!isPlayerAllied()) return super.canHit(entity);
		return entity != this && entity.isAlive() && entity.isAttackable() && !(entity instanceof Player)
				&& !HollowLegion.ownedByAPlayer(entity) && !entity.getUUID().equals(ownerId);
	}

	@Override
	protected boolean isFriend(Entity entity) {
		if (!isPlayerAllied()) return super.isFriend(entity);
		return entity instanceof Player || HollowLegion.ownedByAPlayer(entity);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isPlayerAllied() && source.getEntity() != null && source.getEntity().getUUID().equals(ownerId)) return false;
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SOUL_ESCAPE.value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SOUL_ESCAPE.value();
	}

	@Override
	public float getVoicePitch() {
		return 0.7f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}

	/** The renderer's culling box. */
	public AABB cullingBox() {
		return getBoundingBox().inflate(0.5);
	}
}
