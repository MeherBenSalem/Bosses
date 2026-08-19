package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.event.GameEvents;

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

public class RatEntity extends Monster {
	public static final EntityDataAccessor<Integer> DATA_skin = SynchedEntityData.defineId(RatEntity.class,
			EntityDataSerializers.INT);
	public static final EntityDataAccessor<Boolean> DATA_isAttacking = SynchedEntityData.defineId(RatEntity.class,
			EntityDataSerializers.BOOLEAN);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();

	public RatEntity(EntityType<RatEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_skin, 0);
		this.entityData.define(DATA_isAttacking, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			this.entityData.set(DATA_isAttacking, this.getTarget() != null);
		} else {
			GameEvents.updateRatAnimations(this);
		}
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
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);

		double health = JaumlConfigLib.getNumberValue("remnant/balance", "rat_stats", "rat_health");
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		this.setHealth(this.getMaxHealth());

		double armor = JaumlConfigLib.getNumberValue("remnant/balance", "rat_stats", "rat_armor");
		this.getAttribute(Attributes.ARMOR).setBaseValue(armor);

		double damage = JaumlConfigLib.getNumberValue("remnant/balance", "rat_stats", "rat_attack_damage");
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);

		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("Dataskin", this.entityData.get(DATA_skin));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Dataskin"))
			this.entityData.set(DATA_skin, compound.getInt("Dataskin"));
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 30.0);
		builder = builder.add(Attributes.ARMOR, 2.0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 4.0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		return builder;
	}

	public int getSkinVariant() {
		return this.entityData.get(DATA_skin);
	}

	public void setSkinVariant(int variant) {
		this.entityData.set(DATA_skin, variant);
	}
}
