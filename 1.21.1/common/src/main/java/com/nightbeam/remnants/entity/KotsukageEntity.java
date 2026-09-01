package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
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
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KotsukageEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<String> DATA_ATTACK = SynchedEntityData.defineId(KotsukageEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(KotsukageEntity.class, EntityDataSerializers.BOOLEAN);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation ATK_RSWIPE = RawAnimation.begin().thenPlay("swipe_right");
	private static final RawAnimation ATK_LSWIPE = RawAnimation.begin().thenPlay("swipe_left");
	private static final RawAnimation SKL_RSTOMP = RawAnimation.begin().thenPlay("stomp_right");
	private static final RawAnimation SKL_LSTOMP = RawAnimation.begin().thenPlay("stomp_left");
	private static final RawAnimation SKL_POISON = RawAnimation.begin().thenPlay("poison_breath");
	private static final RawAnimation SKL_ROAR = RawAnimation.begin().thenPlay("roar");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.remnants.kotsukage"),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> playersHearingMusic = new HashSet<>();

	private int attackCooldown;
	private int attackTicks;
	private int attackDuration;
	private int impactAt;
	private boolean impactDone;
	private String pendingAttack = "";
	private Vec3 pendingCenter = Vec3.ZERO;
	private boolean statsApplied;
	private boolean phaseRoared;

	public KotsukageEntity(EntityType<? extends KotsukageEntity> type, Level level) {
		super(type, level);
		this.xpReward = 70;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 700.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 12.0)
				.add(Attributes.FOLLOW_RANGE, 40.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
				.add(Attributes.ATTACK_KNOCKBACK, 1.2)
				.add(Attributes.STEP_HEIGHT, 1.2);
	}

	public static void init() {
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		GroundPathNavigation navigation = new GroundPathNavigation(this, level);
		navigation.setCanFloat(true);
		navigation.setCanOpenDoors(false);
		navigation.setCanPassDoors(true);
		return navigation;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ATTACK, "");
		builder.define(DATA_PHASE_TWO, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new HuntGoal());
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0f));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			tickClientFx();
			return;
		}
		applyConfiguredStats();
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		tickCombat();
		if (this.tickCount % 20 == 0) {
			updateBossMusic();
		}
	}

	private void applyConfiguredStats() {
		if (statsApplied) {
			return;
		}
		statsApplied = true;
		setAttr(Attributes.MAX_HEALTH, cfg("max_health", 700));
		setAttr(Attributes.ATTACK_DAMAGE, cfg("attack_damage", 12));
		setAttr(Attributes.MOVEMENT_SPEED, Math.max(0.22, cfg("movement_speed", 0.28)));
		setAttr(Attributes.ARMOR, cfg("armor", 8));
		setAttr(Attributes.FOLLOW_RANGE, cfg("follow_range", 40));
		setAttr(Attributes.KNOCKBACK_RESISTANCE, cfg("knockback_resistance", 0.75));
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

		float threshold = (float) cfg("hp_threshold_phase_2", 45);
		boolean phaseTwo = this.getHealth() <= this.getMaxHealth() * (threshold / 100.0f);
		if (phaseTwo && !this.entityData.get(DATA_PHASE_TWO)) {
			this.entityData.set(DATA_PHASE_TWO, true);
			setAttr(Attributes.MOVEMENT_SPEED, Math.max(0.26, cfg("movement_speed_phase_2", 0.34)));
			setAttr(Attributes.ATTACK_DAMAGE, cfg("attack_damage_phase_2", 16));
			if (!phaseRoared) {
				phaseRoared = true;
				queueAttack("roar", 90, 24, this.position());
			}
		}

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || this.attackCooldown > 0 || this.attackTicks > 0) {
			return;
		}

		double dist = this.distanceTo(target);
		int roll = this.random.nextInt(100);
		boolean phase = this.entityData.get(DATA_PHASE_TWO);
		if (dist < 3.6 && roll < 55) {
			queueAttack(this.random.nextBoolean() ? "swipe_right" : "swipe_left", 50, 18, target.position());
		} else if (dist < 5.8 && roll < 78) {
			queueAttack(this.random.nextBoolean() ? "stomp_right" : "stomp_left", 60, 22, this.position());
		} else if (dist < 10.0 && (phase || roll < 90)) {
			queueAttack("poison_breath", 60, 16, this.position().add(this.getLookAngle().scale(3.2)));
		} else {
			queueAttack("roar", 90, 24, this.position());
		}
	}

	private void queueAttack(String name, int ticks, int impactTick, Vec3 center) {
		this.entityData.set(DATA_ATTACK, name);
		this.attackDuration = ticks;
		this.attackTicks = ticks;
		this.impactAt = impactTick;
		this.impactDone = false;
		this.pendingAttack = name;
		this.pendingCenter = center;
		this.attackCooldown = ticks + (this.entityData.get(DATA_PHASE_TWO) ? 12 : 24);
		this.triggerAnim("combat", name);
		if (this.level() instanceof ServerLevel server) {
			burst(server, this.position().add(0, 1.6, 0), name, false);
		}
	}

	private void resolveImpact() {
		if (!(this.level() instanceof ServerLevel server)) {
			return;
		}
		switch (this.pendingAttack) {
			case "swipe_right", "swipe_left" -> {
				hurtAround(this.pendingCenter, 2.8, cfg("swipe_damage", 14), 0.55);
				burst(server, this.pendingCenter.add(0, 1.2, 0), this.pendingAttack, true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, 1.4f, 0.6f);
			}
			case "stomp_right", "stomp_left" -> {
				hurtAround(this.position(), cfg("stomp_radius", 5.0), cfg("stomp_damage", 16), 1.05);
				burst(server, this.position(), this.pendingAttack, true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1f, 0.7f);
			}
			case "poison_breath" -> {
				poisonCone(cfg("poison_range", 8), cfg("poison_damage", 10), (int) cfg("poison_duration", 120));
				burst(server, this.pendingCenter, "poison_breath", true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 1.3f, 0.5f);
			}
			case "roar" -> {
				hurtAround(this.position(), cfg("roar_range", 14), cfg("roar_damage", 8), 1.6);
				spawnTraps();
				spawnMinions();
				burst(server, this.position().add(0, 2.0, 0), "roar", true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.6f, 0.55f);
			}
			default -> {
			}
		}
	}

	private void hurtAround(Vec3 center, double radius, double damage, double knock) {
		AABB box = new AABB(center, center).inflate(radius);
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
			living.hurt(this.damageSources().mobAttack(this), (float) damage);
			Vec3 push = living.position().subtract(center).normalize().scale(knock).add(0, 0.22, 0);
			living.push(push.x, push.y, push.z);
		}
	}

	private void poisonCone(double range, double damage, int duration) {
		Vec3 look = this.getLookAngle();
		Vec3 origin = this.position().add(0, 1.6, 0);
		AABB box = this.getBoundingBox().inflate(range);
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
			Vec3 to = living.position().add(0, living.getBbHeight() * 0.5, 0).subtract(origin);
			double dist = to.length();
			if (dist > range || dist < 0.01) {
				continue;
			}
			if (look.normalize().dot(to.normalize()) < 0.45) {
				continue;
			}
			living.hurt(this.damageSources().mobAttack(this), (float) damage);
			living.addEffect(new MobEffectInstance(MobEffects.POISON, duration, this.entityData.get(DATA_PHASE_TWO) ? 1 : 0));
		}
	}

	private void spawnTraps() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		int count = Math.max(1, (int) cfg("trap_count", 2));
		if (this.entityData.get(DATA_PHASE_TWO)) {
			count += 1;
		}
		int spawned = 0;
		for (Player player : this.level().players()) {
			if (spawned >= count) {
				break;
			}
			if (!player.isAlive() || player.distanceTo(this) > cfg("roar_range", 14) + 4) {
				continue;
			}
			spawnTrap(serverLevel, player.position());
			spawned++;
		}
		while (spawned < count) {
			double ox = (this.random.nextDouble() - 0.5) * 8.0;
			double oz = (this.random.nextDouble() - 0.5) * 8.0;
			spawnTrap(serverLevel, this.position().add(ox, 0, oz));
			spawned++;
		}
	}

	private void spawnTrap(ServerLevel serverLevel, Vec3 pos) {
		KotsukageTrapEntity trap = ModEntities.KOTSUKAGE_TRAP.get().create(serverLevel);
		if (trap == null) {
			return;
		}
		trap.moveTo(pos.x, pos.y, pos.z, 0, 0);
		trap.setOwner(this);
		serverLevel.addFreshEntity(trap);
	}

	private void spawnMinions() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		int count = Math.max(0, (int) cfg("minion_count", 2));
		if (this.entityData.get(DATA_PHASE_TWO)) {
			count += 1;
		}
		for (int i = 0; i < count; i++) {
			double ox = (this.random.nextDouble() - 0.5) * 4.0;
			double oz = (this.random.nextDouble() - 0.5) * 4.0;
			SkeletonMinionEntity minion = ModEntities.SKELETON_MINION.get().create(serverLevel);
			if (minion == null) {
				continue;
			}
			minion.moveTo(this.getX() + ox, this.getY(), this.getZ() + oz, this.getYRot(), 0);
			minion.setTarget(this.getTarget());
			serverLevel.addFreshEntity(minion);
		}
	}

	private void burst(ServerLevel server, Vec3 pos, String attack, boolean impact) {
		int extra = this.entityData.get(DATA_PHASE_TWO) ? 10 : 0;
		switch (attack) {
			case "swipe_right", "swipe_left" -> {
				server.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 16 + extra, 0.5, 0.4, 0.5, 0.2);
				server.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 8, 0.3, 0.3, 0.3, 0.02);
			}
			case "stomp_right", "stomp_left" -> {
				if (impact) {
					server.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 2, 0.6, 0.1, 0.6, 0);
				}
				server.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.15, pos.z, 24 + extra, 1.4, 0.2, 1.4, 0.06);
				server.sendParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.2, pos.z, 12, 1.0, 0.2, 1.0, 0.03);
			}
			case "poison_breath" -> {
				server.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.6, pos.z, 28 + extra, 1.2, 0.6, 1.2, 0.04);
				server.sendParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y + 0.5, pos.z, 10, 0.8, 0.4, 0.8, 0.02);
				server.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.8, pos.z, 16, 0.7, 0.4, 0.7, 0.02);
			}
			case "roar" -> {
				server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 20 + extra, 1.6, 1.0, 1.6, 0.03);
				server.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.4, pos.z, 18, 1.8, 0.6, 1.8, 0.05);
			}
			default -> server.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 8, 0.4, 0.4, 0.4, 0.02);
		}
	}

	private void tickClientFx() {
		Vec3 pos = this.position();
		boolean phaseTwo = this.entityData.get(DATA_PHASE_TWO);
		int ambient = phaseTwo ? 5 : 2;
		for (int i = 0; i < ambient; i++) {
			this.level().addParticle(ParticleTypes.SOUL,
					pos.x + (this.random.nextDouble() - 0.5) * 1.8,
					pos.y + 0.8 + this.random.nextDouble() * 2.4,
					pos.z + (this.random.nextDouble() - 0.5) * 1.8,
					0, 0.02, 0);
		}
		if (phaseTwo) {
			this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
					pos.x + (this.random.nextDouble() - 0.5) * 1.4,
					pos.y + 1.2 + this.random.nextDouble() * 1.8,
					pos.z + (this.random.nextDouble() - 0.5) * 1.4,
					0, 0.01, 0);
		}
		String attack = this.getAttackName();
		if ("poison_breath".equals(attack) && this.tickCount % 2 == 0) {
			Vec3 mouth = pos.add(this.getLookAngle().scale(1.6)).add(0, 2.4, 0);
			this.level().addParticle(ParticleTypes.WITCH, mouth.x, mouth.y, mouth.z, 0, 0.04, 0);
			this.level().addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 0, 0.02, 0);
		}
	}

	private void updateBossMusic() {
		if (cfg("boss_music_enabled", 1) <= 0 || !this.isAlive()) {
			return;
		}
		int startRadius = (int) cfg("boss_music_radius", 64);
		int stopRadius = startRadius + 15;
		double startRadiusSqr = startRadius * (double) startRadius;
		double stopRadiusSqr = stopRadius * (double) stopRadius;
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

	private static double cfg(String key, double fallback) {
		double value = JaumlConfigLib.getNumberValue("remnant/bosses", "kotsukage", key);
		if (value <= 0.0 || (value == 1.0 && Math.abs(fallback - 1.0) > 1.0E-4)) {
			return fallback;
		}
		return value;
	}

	public String getAttackName() {
		return this.entityData.get(DATA_ATTACK);
	}

	@Override
	public boolean isWithinMeleeAttackRange(LivingEntity entity) {
		return this.distanceTo(entity) < 3.2;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return this.getBoundingBox().inflate(3.5, 2.0, 3.5);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return EntityDimensions.scalable(1.6f, 3.8f).withEyeHeight(3.2f);
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime >= 125 && !this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, (byte) 60);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (!this.level().isClientSide) {
			for (UUID uuid : playersHearingMusic) {
				Player p = this.level().getPlayerByUUID(uuid);
				if (p instanceof ServerPlayer serverPlayer) {
					Services.NETWORK.sendBossMusic(serverPlayer, this.getId(), false);
				}
			}
			playersHearingMusic.clear();
		}
		super.remove(reason);
		this.bossEvent.removeAllPlayers();
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
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
	protected float getSoundVolume() {
		return 1.4f;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("PhaseTwo", this.entityData.get(DATA_PHASE_TWO));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.entityData.set(DATA_PHASE_TWO, tag.getBoolean("PhaseTwo"));
		this.statsApplied = false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 8, this::movementController));
		AnimationController<KotsukageEntity> combat = new AnimationController<>(this, "combat", 4, this::combatController);
		combat.triggerableAnim("swipe_right", ATK_RSWIPE);
		combat.triggerableAnim("swipe_left", ATK_LSWIPE);
		combat.triggerableAnim("stomp_right", SKL_RSTOMP);
		combat.triggerableAnim("stomp_left", SKL_LSTOMP);
		combat.triggerableAnim("poison_breath", SKL_POISON);
		combat.triggerableAnim("roar", SKL_ROAR);
		controllers.add(combat);
	}

	private PlayState movementController(AnimationState<KotsukageEntity> state) {
		if (!this.isAlive()) {
			return state.setAndContinue(DEATH);
		}
		String attack = this.getAttackName();
		if (!attack.isEmpty()) {
			return PlayState.STOP;
		}
		double moving = this.getDeltaMovement().horizontalDistanceSqr();
		if (moving > 0.003 || state.getLimbSwingAmount() > 0.2) {
			return state.setAndContinue(WALK);
		}
		return state.setAndContinue(IDLE);
	}

	private PlayState combatController(AnimationState<KotsukageEntity> state) {
		return PlayState.CONTINUE;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	private class HuntGoal extends Goal {
		HuntGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = KotsukageEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void tick() {
			LivingEntity target = KotsukageEntity.this.getTarget();
			if (target == null) {
				return;
			}
			KotsukageEntity.this.getLookControl().setLookAt(target, 40.0f, 40.0f);
			if (KotsukageEntity.this.attackTicks > 0) {
				KotsukageEntity.this.getNavigation().stop();
				return;
			}
			double dist = KotsukageEntity.this.distanceTo(target);
			double speed = KotsukageEntity.this.entityData.get(DATA_PHASE_TWO) ? 1.25 : 1.1;
			if (dist > 2.8) {
				boolean path = KotsukageEntity.this.getNavigation().moveTo(target, speed);
				if (!path || KotsukageEntity.this.getNavigation().isDone() || KotsukageEntity.this.horizontalCollision) {
					KotsukageEntity.this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
					if (KotsukageEntity.this.horizontalCollision && KotsukageEntity.this.onGround()) {
						KotsukageEntity.this.setDeltaMovement(KotsukageEntity.this.getDeltaMovement().add(0.0, 0.42, 0.0));
					}
				}
			}
		}
	}
}
