package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class SkeletonArcherEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<String> DATA_ATTACK = SynchedEntityData.defineId(SkeletonArcherEntity.class, EntityDataSerializers.STRING);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation ATK1 = RawAnimation.begin().thenPlay("draw");
	private static final RawAnimation ATK2 = RawAnimation.begin().thenPlay("volley");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	private int attackCooldown;
	private int attackTicks;
	private int attackDuration;
	private int impactAt;
	private boolean impactDone;
	private String pendingAttack = "";
	private boolean statsApplied;

	public SkeletonArcherEntity(EntityType<? extends SkeletonArcherEntity> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 22.0)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.FOLLOW_RANGE, 28.0)
				.add(Attributes.ARMOR, 2.0)
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
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0f));
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
		setAttr(Attributes.MAX_HEALTH, cfg("max_health", 22));
		setAttr(Attributes.ATTACK_DAMAGE, cfg("attack_damage", 4));
		setAttr(Attributes.MOVEMENT_SPEED, Math.max(0.16, cfg("movement_speed", 0.26)));
		setAttr(Attributes.ARMOR, cfg("armor", 2));
		setAttr(Attributes.FOLLOW_RANGE, cfg("follow_range", 28));
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
		if (dist < 4.0 || dist > 18.0) {
			return;
		}
		if (dist > 11.0 || this.random.nextInt(100) < 35) {
			queueAttack("volley", 60, 28);
		} else {
			queueAttack("draw", 40, 18);
		}
	}

	private void queueAttack(String name, int ticks, int impactTick) {
		this.entityData.set(DATA_ATTACK, name);
		this.attackDuration = ticks;
		this.attackTicks = ticks;
		this.impactAt = impactTick;
		this.impactDone = false;
		this.pendingAttack = name;
		this.attackCooldown = ticks + 16;
		this.triggerAnim("combat", name);
	}

	private void resolveImpact() {
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		float damage = (float) ("volley".equals(this.pendingAttack) ? cfg("charged_damage", 6) : cfg("arrow_damage", 4));
		float inaccuracy = "volley".equals(this.pendingAttack) ? 4.0f : 10.0f;
		shootArrow(target, damage, inaccuracy);
		if ("volley".equals(this.pendingAttack)) {
			shootArrow(target, damage * 0.75f, 8.0f);
		}
	}

	private void shootArrow(LivingEntity target, float damage, float inaccuracy) {
		Arrow arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
		double dx = target.getX() - this.getX();
		double dy = target.getY(0.33333333) - arrow.getY();
		double dz = target.getZ() - this.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, inaccuracy);
		arrow.setBaseDamage(damage);
		this.level().addFreshEntity(arrow);
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
	}

	private static double cfg(String key, double fallback) {
		double value = JaumlConfigLib.getNumberValue("remnant/balance", "skeleton_archer_stats", key);
		return value <= 0.0 ? fallback : value;
	}

	public String getAttackName() {
		return this.entityData.get(DATA_ATTACK);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return this.getBoundingBox().inflate(1.4, 0.4, 1.4);
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
		AnimationController<SkeletonArcherEntity> combat = new AnimationController<>(this, "combat", 3, this::combatController);
		combat.triggerableAnim("draw", ATK1);
		combat.triggerableAnim("volley", ATK2);
		controllers.add(combat);
	}

	private PlayState movementController(AnimationState<SkeletonArcherEntity> state) {
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

	private PlayState combatController(AnimationState<SkeletonArcherEntity> state) {
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
			LivingEntity target = SkeletonArcherEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void tick() {
			LivingEntity target = SkeletonArcherEntity.this.getTarget();
			if (target == null) {
				return;
			}
			SkeletonArcherEntity.this.getLookControl().setLookAt(target, 30.0f, 30.0f);
			if (SkeletonArcherEntity.this.attackTicks > 0) {
				SkeletonArcherEntity.this.getNavigation().stop();
				return;
			}
			double dist = SkeletonArcherEntity.this.distanceTo(target);
			if (dist < 6.0) {
				Vec3 away = SkeletonArcherEntity.this.position().subtract(target.position()).normalize().scale(4.0);
				SkeletonArcherEntity.this.getNavigation().moveTo(
						SkeletonArcherEntity.this.getX() + away.x,
						SkeletonArcherEntity.this.getY(),
						SkeletonArcherEntity.this.getZ() + away.z,
						1.15);
			} else if (dist > 14.0) {
				SkeletonArcherEntity.this.getNavigation().moveTo(target, 1.05);
			} else {
				SkeletonArcherEntity.this.getNavigation().stop();
			}
		}
	}
}
