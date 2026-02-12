package tn.naizo.remnants.entity;

import tn.naizo.remnants.init.ModItems;
import tn.naizo.remnants.init.ModEntities;

import tn.naizo.remnants.network.PacketHandler;
import tn.naizo.remnants.network.ClientboundBossMusicPacket;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

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
import net.minecraft.world.entity.MobType;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

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

	public RemnantOssukageEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(ModEntities.REMNANT_OSSUKAGE.get(), world);
	}

	public RemnantOssukageEntity(EntityType<RemnantOssukageEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
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
		}
	}

	private void updateBossMusic() {
		if (tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage",
				"boss_music_enabled") <= 0)
			return;

		// Hysteresis: Start at configured radius, stop at radius + 15
		int startRadius = (int) tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage",
				"boss_music_radius");
		int stopRadius = startRadius + 15;
		double startRadiusSqr = startRadius * startRadius;
		double stopRadiusSqr = stopRadius * stopRadius;

		// Cleanup invalid players from set (using stop radius)
		playersHearingMusic.removeIf(uuid -> {
			Player p = this.level().getPlayerByUUID(uuid);
			return p == null || !p.isAlive() || p.distanceToSqr(this) > stopRadiusSqr;
		});

		for (Player player : this.level().players()) {
			if (player instanceof ServerPlayer serverPlayer) {
				double distSqr = this.distanceToSqr(player);
				boolean isHearing = playersHearingMusic.contains(player.getUUID());

				if (distSqr <= startRadiusSqr && !isHearing) {
					// Start Music (Enter Range)
					PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
							new ClientboundBossMusicPacket(this.getId(), true));
					playersHearingMusic.add(player.getUUID());
				} else if (distSqr > stopRadiusSqr && isHearing) {
					// Stop Music (Exit Outer Range)
					PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
							new ClientboundBossMusicPacket(this.getId(), false));
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
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, Player.class, false, false));
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
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.death"));
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		// Event system handles procedure logic
		return super.hurt(damagesource, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!this.level().isClientSide) {
			for (UUID uuid : playersHearingMusic) {
				Player p = this.level().getPlayerByUUID(uuid);
				if (p instanceof ServerPlayer serverPlayer) {
					PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
							new ClientboundBossMusicPacket(this.getId(), false));
				}
			}
			playersHearingMusic.clear();
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
		// Event system handles procedure logic
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
	public void baseTick() {
		super.baseTick();
		// Event system handles procedure logic
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