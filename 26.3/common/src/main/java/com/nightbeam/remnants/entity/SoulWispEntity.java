package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.BoneMonsterConfig;
import net.minecraft.world.entity.EntitySpawnReason;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * remnants:soul_wisp: a small soul flame raised by a Soul Collector. It homes in on the
 * Collector's target and pops on contact (4 damage), or after 200 ticks. 4 health, no loot. Never saved.
 */
public class SoulWispEntity extends Mob implements GeoEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private UUID ownerId;
	private LivingEntity hunted;
	private int life = 200;

	public SoulWispEntity(EntityType<? extends SoulWispEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.ATTACK_DAMAGE, 4)
				.add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 24);
	}

	public static SoulWispEntity rise(ServerLevel level, SoulCollectorEntity owner, Vec3 at, LivingEntity target) {
		SoulWispEntity wisp = ModEntities.SOUL_WISP.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (wisp == null) return null;
		wisp.ownerId = owner.getUUID();
		wisp.hunted = target;
		wisp.life = (int) BoneMonsterConfig.value("soul_wisp", "life_ticks", 20, 2400);
		wisp.snapTo(at.x, at.y, at.z, owner.getYRot(), 0);
		if (!level.addFreshEntity(wisp)) return null;
		level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.02);
		return wisp;
	}

	public UUID ownerId() {
		return ownerId;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			if (tickCount % 2 == 0 && SovereignConfig.visualDensity() > 0) {
				level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 0, 0.01, 0);
			}
			return;
		}
		if (--life <= 0) {
			pop();
			return;
		}
		if (hunted == null || !hunted.isAlive() || !ForgottenSouls.victim(hunted)) {
			Player near = level().getNearestPlayer(this, 16);
			hunted = near != null && ForgottenSouls.victim(near) ? near : null;
		}
		if (hunted == null) {
			setDeltaMovement(getDeltaMovement().scale(0.8));
			return;
		}
		Vec3 want = hunted.position().add(0, hunted.getBbHeight() * 0.6, 0).subtract(position().add(0, getBbHeight() / 2, 0));
		double speed = BoneMonsterConfig.value("soul_wisp", "movement_speed", 0.05, 1);
		Vec3 motion = getDeltaMovement().scale(0.6).add(want.normalize().scale(speed * 0.4));
		if (motion.length() > speed) motion = motion.normalize().scale(speed);
		setDeltaMovement(motion);
		float yaw = HollowLegion.yawToward(Vec3.ZERO, motion);
		setYRot(yaw);
		setYBodyRot(yaw);
		if (getBoundingBox().inflate(0.25).intersects(hunted.getBoundingBox())) {
			Damage.hurt(hunted, damageSources().mobAttack(this), (float) BoneMonsterConfig.value("soul_wisp", "attack_damage", 0, 100));
			pop();
		}
	}

	@Override
	public void travel(Vec3 input) {
		move(MoverType.SELF, getDeltaMovement());
	}

	private void pop() {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.2, getZ(), 8, 0.15, 0.15, 0.15, 0.05);
			server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 4, 0.1, 0.1, 0.1, 0.02);
		}
		playSound(SoundEvents.SOUL_ESCAPE.value(), 0.8f, 1.6f);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && ForgottenSouls.isWatcherSide(attacker)) return false;
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected void tickDeath() {
		if (!level().isClientSide() && !isRemoved()) pop();
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return super.considersEntityAsAlly(other) || (other != this && ForgottenSouls.isWatcherSide(other));
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SOUL_ESCAPE.value();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>("float", 0,
				state -> state.setAndContinue(RemnantAnims.of(RemnantClips.SOUL_WISP, "float"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
