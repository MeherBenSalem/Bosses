package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.event.GameEvents;
import com.nightbeam.remnants.init.ModEntities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class SkeletonMinionEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_Spawned = SynchedEntityData.defineId(SkeletonMinionEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> DATA_isAttacking = SynchedEntityData.defineId(SkeletonMinionEntity.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();
	public final AnimationState animationState3 = new AnimationState();
	private int leapCooldown;
	private int trapCooldown;
	private boolean statsApplied;

	public SkeletonMinionEntity(EntityType<SkeletonMinionEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_Spawned, false);
		this.entityData.define(DATA_isAttacking, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			GameEvents.updateSkeletonMinionAnimations(this);
			return;
		}
		this.entityData.set(DATA_isAttacking, this.getTarget() != null);
		if (!statsApplied) {
			statsApplied = true;
			setMinionAttr(Attributes.MAX_HEALTH, minionCfg("minion_health", 24));
			setMinionAttr(Attributes.ATTACK_DAMAGE, minionCfg("minion_attack_damage", 5));
			setMinionAttr(Attributes.MOVEMENT_SPEED, minionCfg("minion_movement_speed", 0.32));
			setMinionAttr(Attributes.ARMOR, minionCfg("minion_armor", 2));
			this.setHealth(this.getMaxHealth());
		}
		if (this.leapCooldown > 0) this.leapCooldown--;
		if (this.trapCooldown > 0) this.trapCooldown--;
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) return;
		double dist = this.distanceTo(target);
		if (this.leapCooldown <= 0 && dist > 4.0 && dist < 12.0 && this.onGround()) {
			this.setDeltaMovement(target.position().subtract(this.position()).normalize().scale(0.85).add(0, 0.42, 0));
			this.leapCooldown = (int) minionCfg("leap_cooldown", 80);
		}
		if (this.trapCooldown <= 0 && dist < 8.0 && this.random.nextInt(100) < minionCfg("trap_chance", 18)
				&& this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			KotsukageTrapEntity trap = ModEntities.KOTSUKAGE_TRAP.get().create(serverLevel);
			if (trap != null) {
				trap.moveTo(target.getX(), target.getY(), target.getZ(), 0, 0);
				trap.setOwner(this);
				serverLevel.addFreshEntity(trap);
			}
			this.trapCooldown = (int) minionCfg("trap_cooldown", 160);
		}
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.POISON, (int) minionCfg("poison_duration", 60), 0));
		}
		return hit;
	}

	private void setMinionAttr(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
		var instance = this.getAttribute(attribute);
		if (instance != null) instance.setBaseValue(value);
	}

	private static double minionCfg(String key, double fallback) {
		double value = JaumlConfigLib.getNumberValue("remnant/balance", "skeleton_minion_stats", key);
		return value <= 0.0 ? fallback : value;
	}

	public boolean isAttacking() {
		return this.entityData.get(DATA_isAttacking);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
			}
		});
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse("entity.generic.death"));
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		return super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("DataSpawned", this.entityData.get(DATA_Spawned));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataSpawned"))
			this.entityData.set(DATA_Spawned, compound.getBoolean("DataSpawned"));
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 20);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		return builder;
	}

	public boolean isSpawned() {
		return this.entityData.get(DATA_Spawned);
	}

	public void setSpawned(boolean spawned) {
		this.entityData.set(DATA_Spawned, spawned);
	}
}
