package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class SkeletonMeleeEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<String> DATA_ATTACK = SynchedEntityData.defineId(SkeletonMeleeEntity.class, EntityDataSerializers.STRING);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation ATK1 = RawAnimation.begin().thenPlay("slash");
	private static final RawAnimation ATK2 = RawAnimation.begin().thenPlay("smash");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	private int attackCooldown;
	private int attackTicks;
	private int attackDuration;
	private int impactAt;
	private boolean impactDone;
	private String pendingAttack = "";
	private boolean statsApplied;

	public SkeletonMeleeEntity(EntityType<? extends SkeletonMeleeEntity> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 28.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.15)
				.add(Attributes.STEP_HEIGHT, 0.6);
	}

	public static void init() {
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ATTACK, "");
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new HuntGoal());
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0f));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			return;
		}
		applyConfiguredStats();
		tickCombat();
	}

	private void applyConfiguredStats() {
		if (statsApplied) {
			return;
		}
		statsApplied = true;
		setAttr(Attributes.MAX_HEALTH, cfg("max_health", 28));
		setAttr(Attributes.ATTACK_DAMAGE, cfg("attack_damage", 6));
		setAttr(Attributes.MOVEMENT_SPEED, Math.max(0.18, cfg("movement_speed", 0.28)));
		setAttr(Attributes.ARMOR, cfg("armor", 4));
		setAttr(Attributes.FOLLOW_RANGE, cfg("follow_range", 24));
		this.setHealth(this.getMaxHealth());
	}

	private void setAttr(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = this.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	private void tickCombat() {
		if (this.attackTicks > 0) {
			int elapsed = this.attackDuration - this.attackTicks;
			if (!this.impactDone && elapsed >= this.impactAt) {
				this.impactDone = true;
				resolveImpact();
			}
			this.attackTicks--;
			if (this.attackTicks == 0) {
				this.entityData.set(DATA_ATTACK, "");
				this.pendingAttack = "";
			}
		}
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || this.attackCooldown > 0 || this.attackTicks > 0) {
			return;
		}
		double dist = this.distanceTo(target);
		if (dist > 2.8) {
			return;
		}
		if (this.random.nextBoolean()) {
			queueAttack("smash", 20, 8);
		} else {
			queueAttack("slash", 43, 16);
		}
	}

	private void queueAttack(String name, int ticks, int impactTick) {
		this.entityData.set(DATA_ATTACK, name);
		this.attackDuration = ticks;
		this.attackTicks = ticks;
		this.impactAt = impactTick;
		this.impactDone = false;
		this.pendingAttack = name;
		this.attackCooldown = ticks + 12;
		this.triggerAnim("combat", name);
	}

	private void resolveImpact() {
		LivingEntity target = this.getTarget();
		Vec3 center = target != null ? target.position() : this.position().add(this.getLookAngle().scale(1.4));
		double radius = "slash".equals(this.pendingAttack) ? 2.2 : 1.6;
		double damage = "slash".equals(this.pendingAttack) ? cfg("heavy_damage", 8) : cfg("attack_damage", 6);
		double knock = "slash".equals(this.pendingAttack) ? 0.55 : 0.28;
		AABB box = new AABB(center, center).inflate(radius);
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
			living.hurt(this.damageSources().mobAttack(this), (float) damage);
			Vec3 push = living.position().subtract(this.position()).normalize().scale(knock).add(0, 0.12, 0);
			living.push(push.x, push.y, push.z);
		}
		this.level().playSound(null, this.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, 0.9f, 0.85f);
	}

	private static double cfg(String key, double fallback) {
		double value = JaumlConfigLib.getNumberValue("remnant/balance", "skeleton_melee_stats", key);
		return value <= 0.0 ? fallback : value;
	}

	public String getAttackName() {
		return this.entityData.get(DATA_ATTACK);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return this.getBoundingBox().inflate(1.2, 0.4, 1.2);
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime >= 72 && !this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, (byte) 60);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.statsApplied = false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 6, this::movementController));
		AnimationController<SkeletonMeleeEntity> combat = new AnimationController<>(this, "combat", 3, this::combatController);
		combat.triggerableAnim("slash", ATK1);
		combat.triggerableAnim("smash", ATK2);
		controllers.add(combat);
	}

	private PlayState movementController(AnimationState<SkeletonMeleeEntity> state) {
		if (!this.isAlive()) {
			return state.setAndContinue(DEATH);
		}
		if (!this.getAttackName().isEmpty()) {
			return PlayState.STOP;
		}
		double moving = this.getDeltaMovement().horizontalDistanceSqr();
		if (moving > 0.003 || state.getLimbSwingAmount() > 0.2) {
			return state.setAndContinue(WALK);
		}
		return state.setAndContinue(IDLE);
	}

	private PlayState combatController(AnimationState<SkeletonMeleeEntity> state) {
		return PlayState.CONTINUE;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	private class HuntGoal extends Goal {
		HuntGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = SkeletonMeleeEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void tick() {
			LivingEntity target = SkeletonMeleeEntity.this.getTarget();
			if (target == null) {
				return;
			}
			SkeletonMeleeEntity.this.getLookControl().setLookAt(target, 30.0f, 30.0f);
			if (SkeletonMeleeEntity.this.attackTicks > 0) {
				SkeletonMeleeEntity.this.getNavigation().stop();
				return;
			}
			SkeletonMeleeEntity.this.getNavigation().moveTo(target, 1.15);
		}
	}
}
