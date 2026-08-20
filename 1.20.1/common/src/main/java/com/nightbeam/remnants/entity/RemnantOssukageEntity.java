package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.platform.Services;
import com.nightbeam.remnants.procedures.ThrowKunaisProcedureProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RemnantOssukageEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<Boolean> DATA_transform = SynchedEntityData.defineId(RemnantOssukageEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_AI = SynchedEntityData.defineId(RemnantOssukageEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> DATA_state = SynchedEntityData.defineId(RemnantOssukageEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> DATA_STATE_TICK = SynchedEntityData.defineId(RemnantOssukageEntity.class, EntityDataSerializers.INT);

	private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
	private static final RawAnimation SPOT_PLAYER = RawAnimation.begin().thenPlay("spot_player");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
	private static final RawAnimation ATTACK_ONE = RawAnimation.begin().thenPlay("attack_1");
	private static final RawAnimation SWING_ONE = RawAnimation.begin().thenPlay("swing_1");
	private static final RawAnimation SWING_TWO = RawAnimation.begin().thenPlay("swing_2");
	private static final RawAnimation DASH = RawAnimation.begin().thenPlay("attack_dash");
	private static final RawAnimation HIT_ONE = RawAnimation.begin().thenPlay("hit1");
	private static final RawAnimation HIT_TWO = RawAnimation.begin().thenPlay("hit2");
	private static final RawAnimation HIT_THREE = RawAnimation.begin().thenPlay("hit3");
	private static final RawAnimation IN_AIR = RawAnimation.begin().thenLoop("in_air");
	private static final RawAnimation LAND = RawAnimation.begin().thenPlay("land");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

	private static final int SPAWN_TICKS = 73;
	private static final int SPOT_TICKS = 70;
	private static final int ATTACK_ONE_TICKS = 45;
	private static final int SWING_ONE_TICKS = 41;
	private static final int SWING_TWO_TICKS = 70;
	private static final int DASH_TICKS = 80;
	private static final int LAND_TICKS = 15;
	private static final int HIT_TICKS = 5;
	private static final int DEATH_TICKS = 75;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.PINK, ServerBossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> playersHearingMusic = new HashSet<>();
	private UUID engagedTarget;
	private boolean initialized;
	private boolean rangedAttack;
	private boolean firstHitDone;
	private boolean secondHitDone;
	private boolean wasAirborne;
	private int attackCycle;
	private int hitCycle;
	private int attackCooldown;

	public RemnantOssukageEntity(EntityType<RemnantOssukageEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_transform, false);
		this.entityData.define(DATA_AI, 0);
		this.entityData.define(DATA_state, "spawn");
		this.entityData.define(DATA_STATE_TICK, 0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
		this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public void initializeEncounter() {
		if (this.initialized || this.level().isClientSide()) return;
		this.initialized = true;
		setAttribute(Attributes.MAX_HEALTH, config("max_health_phase_1"));
		setAttribute(Attributes.ATTACK_DAMAGE, config("attack_damage_phase_1"));
		setAttribute(Attributes.MOVEMENT_SPEED, 0.0);
		this.setHealth(this.getMaxHealth());
		enterState("spawn");
		spawnRuneEffect(OssukageRuneEffectEntity.SPAWN_VARIANT);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
		SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, spawnData, tag);
		initializeEncounter();
		return result;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			if (!this.initialized) initializeEncounter();
			serverTickEncounter();
			if (this.tickCount % 20 == 0) updateBossMusic();
		}
	}

	public void serverTickEncounter() {
		if (!this.isAlive()) return;
		String state = getEntityState();
		this.entityData.set(DATA_STATE_TICK, getStateTick() + 1);
		if (this.attackCooldown > 0) this.attackCooldown--;

		if (!isTransformed() && !"phase_transition".equals(state)
				&& this.getHealth() <= this.getMaxHealth() * config("hp_threshold_phase_2") / 100.0) {
			startPhaseTransition();
			return;
		}

		LivingEntity target = validTarget();
		if (target == null) {
			this.engagedTarget = null;
			this.entityData.set(DATA_AI, 0);
			if (!isLockedState(state) && !"idle".equals(state)) enterState("idle");
			return;
		}

		if (this.engagedTarget == null || !this.engagedTarget.equals(target.getUUID())) {
			this.engagedTarget = target.getUUID();
			if (!isLockedState(state)) enterState("spot_player");
		}

		state = getEntityState();
		int stateTick = getStateTick();
		if (isMovementLocked(state)) {
			this.getNavigation().stop();
			this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
			this.getLookControl().setLookAt(target, 30.0f, 30.0f);
		}

		switch (state) {
			case "spawn" -> {
				if (stateTick >= SPAWN_TICKS) {
					setAttribute(Attributes.MOVEMENT_SPEED, config("movement_speed_phase_1"));
					enterState("idle");
				}
			}
			case "spot_player" -> {
				if (stateTick >= SPOT_TICKS) enterState("idle");
			}
			case "phase_transition" -> tickPhaseTransition(stateTick);
			case "attack_1" -> tickAttackOne(stateTick);
			case "swing_1" -> tickSwingOne(stateTick);
			case "swing_2" -> tickSwingTwo(stateTick);
			case "attack_dash" -> tickDash(target, stateTick);
			case "land" -> {
				if (stateTick >= LAND_TICKS) enterState("idle");
			}
			case "hit1", "hit2", "hit3" -> {
				if (stateTick >= HIT_TICKS) enterState("idle");
			}
			default -> tickLocomotionAndSelection(target);
		}
	}

	private void tickLocomotionAndSelection(LivingEntity target) {
		if (!this.onGround()) {
			this.wasAirborne = true;
			if (!"in_air".equals(getEntityState())) enterState("in_air");
			return;
		}
		if (this.wasAirborne) {
			this.wasAirborne = false;
			enterState("land");
			return;
		}

		int combatClock = this.entityData.get(DATA_AI) + 1;
		this.entityData.set(DATA_AI, combatClock);
		double distance = this.distanceTo(target);
		int shurikenTimer = Math.max(1, (int) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_timer"));
		int dashTimer = Math.max(shurikenTimer + 1, (int) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "dash_timer"));

		if (isTransformed() && combatClock >= dashTimer) {
			enterState("attack_dash");
			return;
		}
		if (distance > 5.0 && combatClock == shurikenTimer) {
			this.rangedAttack = true;
			enterState("attack_1");
			return;
		}
		if (distance <= 4.3 && this.attackCooldown <= 0) {
			this.rangedAttack = false;
			switch (this.attackCycle++ % 3) {
				case 0 -> enterState("attack_1");
				case 1 -> enterState("swing_1");
				default -> enterState("swing_2");
			}
			return;
		}
		this.getNavigation().moveTo(target, isTransformed() ? 1.25 : 1.0);
		setStateWithoutReset(isTransformed() && distance > 8.0 ? "run" : "walk");
	}

	private void tickAttackOne(int tick) {
		if (tick == 20 && !this.firstHitDone) {
			this.firstHitDone = true;
			if (this.rangedAttack) ThrowKunaisProcedureProcedure.execute(this);
			else strikeArc(4.0, 0.35, 1.0f);
		}
		if (tick >= ATTACK_ONE_TICKS) finishAttack();
	}

	private void tickSwingOne(int tick) {
		if (tick == 18 && !this.firstHitDone) {
			this.firstHitDone = true;
			strikeArc(4.5, 0.05, 1.0f);
		}
		if (tick >= SWING_ONE_TICKS) finishAttack();
	}

	private void tickSwingTwo(int tick) {
		if (tick == 20 && !this.firstHitDone) {
			this.firstHitDone = true;
			strikeArc(4.5, 0.0, 0.85f);
		}
		if (tick == 42 && !this.secondHitDone) {
			this.secondHitDone = true;
			strikeArc(4.8, -0.2, 1.1f);
		}
		if (tick >= SWING_TWO_TICKS) finishAttack();
	}

	private void tickDash(LivingEntity target, int tick) {
		if (tick == 30) {
			Vec3 direction = target.position().subtract(this.position());
			if (direction.horizontalDistanceSqr() > 0.001) {
				direction = direction.normalize();
				double distance = JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "dash_distance");
				this.setDeltaMovement(direction.x * distance, Math.max(0.12, direction.y * 0.35), direction.z * distance);
				this.hasImpulse = true;
			}
			this.level().playSound(null, this.blockPosition(), BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("remnant_bosses", "dash_sfx")), SoundSource.HOSTILE, 1.0f, 1.0f);
		}
		if (tick == 40 && !this.firstHitDone) {
			this.firstHitDone = true;
			if (this.distanceTo(target) <= 5.0) {
				strikeArc(5.0, -0.3, 1.25f);
				applyDashFollowUp(target);
			}
		}
		if (tick >= DASH_TICKS) {
			this.entityData.set(DATA_AI, 0);
			finishAttack();
		}
	}

	private void applyDashFollowUp(LivingEntity target) {
		int chance = Mth.clamp((int) config("special_attack_chance_phase_2"), 0, 33);
		int roll = this.random.nextInt(100);
		if (roll < chance) {
			this.level().explode(this, target.getX(), target.getY(), target.getZ(), 4.0f, Level.ExplosionInteraction.MOB);
		} else if (roll < chance * 2) {
			target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 150, 1, false, false));
		} else if (roll < chance * 3 && this.level() instanceof ServerLevel server) {
			spawnMinions(server, (int) config("skeletons_on_dash_phase_2"), this.blockPosition());
		}
	}

	private void strikeArc(double range, double minimumDot, float multiplier) {
		Vec3 look = this.getLookAngle().normalize();
		for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range),
				entity -> entity.isAlive() && entity != this && !(entity instanceof SkeletonMinionEntity))) {
			Vec3 direction = victim.position().subtract(this.position());
			if (direction.lengthSqr() > range * range || direction.lengthSqr() < 0.001) continue;
			if (look.dot(direction.normalize()) < minimumDot) continue;
			victim.hurt(this.damageSources().mobAttack(this), (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier));
		}
	}

	private void finishAttack() {
		this.attackCooldown = 12;
		this.rangedAttack = false;
		enterState("idle");
	}

	private void startPhaseTransition() {
		enterState("phase_transition");
		setAttribute(Attributes.MOVEMENT_SPEED, 0.0);
		this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.max(60, (int) config("transform_delay_phase_2")), 5, false, true));
		spawnRuneEffect(OssukageRuneEffectEntity.LEAVES_VARIANT);
		if (this.level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 2, 1, 1, 1, 0.05);
			spawnMinions(server, (int) config("skeletons_on_transform_phase_2"), this.blockPosition());
		}
	}

	private void tickPhaseTransition(int tick) {
		if (tick < Math.max(1, (int) config("transform_delay_phase_2"))) return;
		this.setTransformed(true);
		setAttribute(Attributes.KNOCKBACK_RESISTANCE, 1.0);
		setAttribute(Attributes.MOVEMENT_SPEED, config("movement_speed_phase_2"));
		setAttribute(Attributes.ATTACK_DAMAGE, config("attack_damage_phase_2"));
		this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) config("health_boost_timer_phase_2"), 1, false, true));
		enterState("idle");
	}

	private void spawnMinions(ServerLevel server, int count, BlockPos center) {
		for (int i = 0; i < Math.max(0, count); i++) {
			BlockPos pos = center.offset(this.random.nextInt(11) - 5, 0, this.random.nextInt(11) - 5);
			Entity minion = ModEntities.SKELETON_MINION.get().spawn(server, pos, MobSpawnType.MOB_SUMMONED);
			if (minion != null) minion.setDeltaMovement(Vec3.ZERO);
		}
	}

	private void spawnRuneEffect(int variant) {
		if (!(this.level() instanceof ServerLevel server)) return;
		OssukageRuneEffectEntity effect = new OssukageRuneEffectEntity(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), server);
		effect.setVariant(variant);
		effect.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0);
		server.addFreshEntity(effect);
	}

	public void onIncomingDamage() {
		if (this.level().isClientSide() || isMovementLocked(getEntityState()) || this.isDeadOrDying()) return;
		enterState(switch (this.hitCycle++ % 3) {
			case 0 -> "hit1";
			case 1 -> "hit2";
			default -> "hit3";
		});
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if ("spawn".equals(getEntityState()) || "phase_transition".equals(getEntityState())) return false;
		return super.hurt(source, amount);
	}

	private LivingEntity validTarget() {
		LivingEntity target = this.getTarget();
		return target != null && target.isAlive() && !target.isRemoved() ? target : null;
	}

	private void enterState(String state) {
		this.entityData.set(DATA_state, state);
		this.entityData.set(DATA_STATE_TICK, 0);
		this.firstHitDone = false;
		this.secondHitDone = false;
	}

	private void setStateWithoutReset(String state) {
		if (!state.equals(getEntityState())) enterState(state);
	}

	private static boolean isLockedState(String state) {
		return isMovementLocked(state) || "land".equals(state) || state.startsWith("hit");
	}

	private static boolean isMovementLocked(String state) {
		return "spawn".equals(state) || "spot_player".equals(state) || "phase_transition".equals(state)
				|| "attack_1".equals(state) || "swing_1".equals(state) || "swing_2".equals(state) || "attack_dash".equals(state);
	}

	private double config(String key) {
		return JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", key);
	}

	private void setAttribute(Attribute attribute, double value) {
		if (this.getAttribute(attribute) != null) this.getAttribute(attribute).setBaseValue(value);
	}

	private void updateBossMusic() {
		if (config("boss_music_enabled") <= 0 || !this.isAlive()) return;
		int startRadius = (int) config("boss_music_radius");
		int stopRadius = startRadius + 15;
		double startRadiusSqr = startRadius * startRadius;
		double stopRadiusSqr = stopRadius * stopRadius;
		this.playersHearingMusic.removeIf(uuid -> {
			Player player = this.level().getPlayerByUUID(uuid);
			return player == null || !player.isAlive() || player.distanceToSqr(this) > stopRadiusSqr;
		});
		for (Player player : this.level().players()) {
			if (!(player instanceof ServerPlayer serverPlayer)) continue;
			boolean hearing = this.playersHearingMusic.contains(player.getUUID());
			if (this.distanceToSqr(player) <= startRadiusSqr && !hearing) {
				Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), true);
				this.playersHearingMusic.add(player.getUUID());
			} else if (this.distanceToSqr(player) > stopRadiusSqr && hearing) {
				Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), false);
				this.playersHearingMusic.remove(player.getUUID());
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		enterState("death");
		super.die(source);
		if (!this.level().isClientSide()) {
			for (UUID uuid : this.playersHearingMusic) {
				Player player = this.level().getPlayerByUUID(uuid);
				if (player instanceof ServerPlayer serverPlayer) Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), false);
			}
			this.playersHearingMusic.clear();
		}
	}

	@Override
	protected void tickDeath() {
		this.deathTime++;
		if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, (byte) 60);
			this.remove(RemovalReason.KILLED);
		}
	}

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		this.spawnAtLocation(new ItemStack(ModItems.OSSUKAGE_SWORD.get()));
	}

	@Override
	public SoundEvent getHurtSound(DamageSource source) {
		return BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("minecraft", "entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("minecraft", "entity.generic.death"));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Datatransform", isTransformed());
		tag.putInt("DataAI", this.entityData.get(DATA_AI));
		tag.putString("Datastate", getEntityState());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.entityData.set(DATA_transform, tag.getBoolean("Datatransform"));
		this.entityData.set(DATA_AI, tag.getInt("DataAI"));
		this.initialized = true;
		setAttribute(Attributes.MAX_HEALTH, config("max_health_phase_1"));
		if (isTransformed()) {
			setAttribute(Attributes.MOVEMENT_SPEED, config("movement_speed_phase_2"));
			setAttribute(Attributes.ATTACK_DAMAGE, config("attack_damage_phase_2"));
			setAttribute(Attributes.KNOCKBACK_RESISTANCE, 1.0);
		} else {
			setAttribute(Attributes.MOVEMENT_SPEED, config("movement_speed_phase_1"));
			setAttribute(Attributes.ATTACK_DAMAGE, config("attack_damage_phase_1"));
		}
		enterState("idle");
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

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 2, this::animationController));
	}

	private PlayState animationController(AnimationState<RemnantOssukageEntity> state) {
		RawAnimation animation = switch (this.isDeadOrDying() ? "death" : getEntityState()) {
			case "spawn", "phase_transition" -> SPAWN;
			case "spot_player" -> SPOT_PLAYER;
			case "walk" -> WALK;
			case "run" -> RUN;
			case "attack_1" -> ATTACK_ONE;
			case "swing_1" -> SWING_ONE;
			case "swing_2" -> SWING_TWO;
			case "attack_dash" -> DASH;
			case "hit1" -> HIT_ONE;
			case "hit2" -> HIT_TWO;
			case "hit3" -> HIT_THREE;
			case "in_air" -> IN_AIR;
			case "land" -> LAND;
			case "death" -> DEATH;
			default -> IDLE;
		};
		return state.setAndContinue(animation);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.MAX_HEALTH, 10)
				.add(Attributes.ARMOR, 0)
				.add(Attributes.ATTACK_DAMAGE, 3)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0)
				.add(Attributes.FOLLOW_RANGE, 64);
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
		enterState(state == null || state.isBlank() ? "idle" : state);
	}

	public int getStateTick() {
		return this.entityData.get(DATA_STATE_TICK);
	}
}
