package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class KotsukageTrapEntity extends Mob implements GeoEntity {
	private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private UUID ownerId;
	private int life = 80;
	private boolean impactDone;

	public KotsukageTrapEntity(EntityType<? extends KotsukageTrapEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.setNoAi(true);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	public static void init() {
	}

	public void setOwner(LivingEntity owner) {
		this.ownerId = owner == null ? null : owner.getUUID();
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(0, 0, 0);
		this.setNoGravity(true);
		if (this.level().isClientSide) {
			if (this.tickCount % 3 == 0) {
				this.level().addParticle(ParticleTypes.SOUL, this.getX(), this.getY() + 0.2, this.getZ(), 0, 0.02, 0);
			}
			return;
		}
		if (--this.life <= 0) {
			this.discard();
			return;
		}
		if (!this.impactDone && this.tickCount >= 32) {
			this.impactDone = true;
			triggerImpact();
		}
	}

	private void triggerImpact() {
		double damage = JaumlConfigLib.getNumberValue("remnant/bosses", "kotsukage", "trap_damage");
		if (damage <= 1.0) {
			damage = 10.0;
		}
		int slow = (int) JaumlConfigLib.getNumberValue("remnant/bosses", "kotsukage", "trap_slow_duration");
		if (slow <= 1) {
			slow = 80;
		}
		AABB box = this.getBoundingBox().inflate(1.4, 1.0, 1.4);
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
			living.hurt(this.damageSources().magic(), (float) damage);
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1));
		}
		this.level().playSound(null, this.blockPosition(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 1.1f, 0.7f);
		if (this.level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.4, this.getZ(), 12, 0.5, 0.3, 0.5, 0.08);
			server.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 10, 0.4, 0.3, 0.4, 0.03);
		}
	}

	private boolean canHit(LivingEntity living) {
		if (!living.isAlive() || living == this) {
			return false;
		}
		if (this.ownerId != null && this.ownerId.equals(living.getUUID())) {
			return false;
		}
		return !(living instanceof KotsukageEntity) && !(living instanceof KotsukageTrapEntity);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.ownerId != null) {
			tag.putUUID("Owner", this.ownerId);
		}
		tag.putInt("Life", this.life);
		tag.putBoolean("Impact", this.impactDone);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Owner")) {
			this.ownerId = tag.getUUID("Owner");
		}
		this.life = tag.getInt("Life");
		this.impactDone = tag.getBoolean("Impact");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "spawn", 0, state -> state.setAndContinue(SPAWN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
