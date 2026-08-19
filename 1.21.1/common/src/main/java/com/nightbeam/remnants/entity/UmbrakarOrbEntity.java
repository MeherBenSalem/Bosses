package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class UmbrakarOrbEntity extends Mob implements GeoEntity {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private UUID ownerId;
	private UUID targetId;
	private int life;

	public UmbrakarOrbEntity(EntityType<? extends UmbrakarOrbEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.noPhysics = false;
		this.life = 160;
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.35)
				.add(Attributes.FLYING_SPEED, 0.55)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public static void init() {
	}

	public void setOwner(LivingEntity owner) {
		this.ownerId = owner == null ? null : owner.getUUID();
	}

	public void setTarget(LivingEntity target) {
		this.targetId = target == null ? null : target.getUUID();
		super.setTarget(target);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		navigation.setCanPassDoors(true);
		return navigation;
	}

	@Override
	public void tick() {
		super.tick();
		this.setNoGravity(true);
		if (this.level().isClientSide) {
			for (int i = 0; i < 3; i++) {
				this.level().addParticle(ParticleTypes.END_ROD,
						this.getX() + (this.random.nextDouble() - 0.5) * 0.35,
						this.getY() + 0.25 + this.random.nextDouble() * 0.3,
						this.getZ() + (this.random.nextDouble() - 0.5) * 0.35,
						0, 0.01, 0);
				this.level().addParticle(ParticleTypes.WITCH,
						this.getX(), this.getY() + 0.3, this.getZ(), 0, 0.02, 0);
				this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
						this.getX(), this.getY() + 0.25, this.getZ(), 0, 0.01, 0);
			}
			return;
		}
		if (--this.life <= 0) {
			this.discard();
			return;
		}
		LivingEntity target = this.getTarget();
		if (target == null && this.targetId != null && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			var entity = serverLevel.getEntity(this.targetId);
			if (entity instanceof LivingEntity living) {
				target = living;
				super.setTarget(living);
			}
		}
		if (target == null || !target.isAlive()) {
			return;
		}
		Vec3 dest = target.getEyePosition().add(0, -0.4, 0);
		Vec3 motion = dest.subtract(this.position()).normalize().scale(0.38);
		this.setDeltaMovement(motion);
		this.hasImpulse = true;
		if (this.distanceTo(target) < 1.35) {
			double damage = JaumlConfigLib.getNumberValue("remnant/bosses", "umbrakar", "orb_damage");
			if (damage <= 1.0) {
				damage = 8.0;
			}
			target.hurt(this.damageSources().indirectMagic(this, this), (float) damage);
			this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.1f, 0.7f);
			if (this.level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.3, this.getZ(), 1, 0, 0, 0, 0);
				server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.3, this.getZ(), 24, 0.4, 0.4, 0.4, 0.12);
				server.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.35, 0.35, 0.35, 0.15);
				server.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.3, this.getZ(), 16, 0.4, 0.4, 0.4, 0.04);
			}
			this.discard();
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.ownerId != null) {
			tag.putUUID("Owner", this.ownerId);
		}
		if (this.targetId != null) {
			tag.putUUID("Homing", this.targetId);
		}
		tag.putInt("Life", this.life);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Owner")) {
			this.ownerId = tag.getUUID("Owner");
		}
		if (tag.hasUUID("Homing")) {
			this.targetId = tag.getUUID("Homing");
		}
		this.life = tag.getInt("Life");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "idle", 4, state ->
				state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.002 ? WALK : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
