package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.event.GameEvents;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.platform.Services;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
	public static final EntityDataAccessor<Boolean> DATA_isAttacking = SynchedEntityData
			.defineId(RemnantOssukageEntity.class, EntityDataSerializers.BOOLEAN);
	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(),
			ServerBossEvent.BossBarColor.PINK, ServerBossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> playersHearingMusic = new HashSet<>();

	public RemnantOssukageEntity(EntityType<RemnantOssukageEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_transform, false);
		this.entityData.define(DATA_AI, 0);
		this.entityData.define(DATA_state, "");
		this.entityData.define(DATA_isAttacking, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			this.entityData.set(DATA_isAttacking, this.getTarget() != null || "attack".equals(this.getEntityState()));
			if (this.tickCount % 20 == 0) {
				updateBossMusic();
			}
			GameEvents.updateOssukageServerTick(this);
		} else {
			GameEvents.updateOssukageAnimations(this);
		}
	}

	private void updateBossMusic() {
		if (JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "boss_music_enabled") <= 0)
			return;

		if (!this.isAlive())
			return;

		int startRadius = (int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "boss_music_radius");
		int stopRadius = startRadius + 15;
		double startRadiusSqr = startRadius * startRadius;
		double stopRadiusSqr = stopRadius * stopRadius;

		playersHearingMusic.removeIf(uuid -> {
			Player p = this.level().getPlayerByUUID(uuid);
			return p == null || !p.isAlive() || p.distanceToSqr(this) > stopRadiusSqr;
		});

		for (Player player : this.level().players()) {
			if (player instanceof ServerPlayer serverPlayer) {
				double distSqr = this.distanceToSqr(player);
				boolean isHearing = playersHearingMusic.contains(player.getUUID());

				if (distSqr <= startRadiusSqr && !isHearing) {
					Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), true);
					playersHearingMusic.add(player.getUUID());
				} else if (distSqr > stopRadiusSqr && isHearing) {
					Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), false);
					playersHearingMusic.remove(player.getUUID());
				}
			}
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

	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
		super.dropCustomDeathLoot(source, looting, recentlyHitIn);
		this.spawnAtLocation(new ItemStack(ModItems.OSSUKAGE_SWORD.get()));
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
	public boolean hurt(DamageSource damagesource, float amount) {
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!this.level().isClientSide) {
			for (UUID uuid : playersHearingMusic) {
				Player p = this.level().getPlayerByUUID(uuid);
				if (p instanceof ServerPlayer serverPlayer) {
					Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), false);
				}
			}
			playersHearingMusic.clear();
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		return super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
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
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canChangeDimensions() {
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
		return builder;
	}

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
