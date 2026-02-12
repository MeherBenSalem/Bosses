package tn.naizo.remnants.entity;

import tn.naizo.remnants.init.ModItems;
import tn.naizo.remnants.init.ModEntities;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class RemnantOssukageEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_transform = SynchedEntityData
			.defineId(RemnantOssukageEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_AI = SynchedEntityData.defineId(RemnantOssukageEntity.class,
			EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> DATA_state = SynchedEntityData.defineId(RemnantOssukageEntity.class,
			EntityDataSerializers.STRING);
	public final AnimationState animationState0 = new AnimationState();
	public final AnimationState animationState2 = new AnimationState();
	public final AnimationState animationState3 = new AnimationState();
	public final AnimationState animationState4 = new AnimationState();
	public final AnimationState animationState5 = new AnimationState();
	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(),
			ServerBossEvent.BossBarColor.PINK, ServerBossEvent.BossBarOverlay.PROGRESS);

	public RemnantOssukageEntity(EntityType<RemnantOssukageEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_transform, false);
		builder.define(DATA_AI, 0);
		builder.define(DATA_state, "");
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, Player.class, false, false));
	}

	protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(serverLevel, source, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(ModItems.OSSUKAGE_SWORD.get()));
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.death"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Level world = this.level();
		Entity entity = this;
		Entity sourceentity = damagesource.getEntity();
		Entity immediatesourceentity = damagesource.getDirectEntity();

		// Procedure call removed - will be handled by event system
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		// Procedure call removed - will be handled by event system
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData livingdata) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata);
		// Procedure call removed - will be handled by event system
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("Datatransform", this.entityData.get(DATA_transform));
		compound.putInt("DataAI", this.entityData.get(DATA_AI));
		compound.putString("Datastate", this.entityData.get(DATA_state));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Datatransform"))
			this.entityData.set(DATA_transform, compound.getBoolean("Datatransform"));
		if (compound.contains("DataAI"))
			this.entityData.set(DATA_AI, compound.getInt("DataAI"));
		if (compound.contains("Datastate"))
			this.entityData.set(DATA_state, compound.getString("Datastate"));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			// Animation state updates moved to event handler
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		// Procedure call removed - will be handled by event system
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	@Override
	public void customServerAiStep() {
		super.customServerAiStep();
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 10);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.STEP_HEIGHT, 0.6);
		return builder;
	}

	// Public accessor methods for event handlers
	public boolean isTransformed() {
		return this.entityData.get(DATA_transform);
	}

	public void setTransformed(boolean transformed) {
		this.entityData.set(DATA_transform, transformed);
	}

	public int getAIState() {
		return this.entityData.get(DATA_AI);
	}

	public void setAIState(int state) {
		this.entityData.set(DATA_AI, state);
	}

	public String getEntityState() {
		return this.entityData.get(DATA_state);
	}

	public void setEntityState(String state) {
		this.entityData.set(DATA_state, state);
	}
}