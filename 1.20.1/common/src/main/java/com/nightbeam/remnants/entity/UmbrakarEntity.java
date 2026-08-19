package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.init.ModEntities;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class UmbrakarEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<String> DATA_ATTACK = SynchedEntityData.defineId(UmbrakarEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(UmbrakarEntity.class, EntityDataSerializers.BOOLEAN);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
	private static final RawAnimation BITE = RawAnimation.begin().thenPlay("bite");
	private static final RawAnimation FRONTSLAM = RawAnimation.begin().thenPlay("frontslam");
	private static final RawAnimation TAILSLAM = RawAnimation.begin().thenPlay("tailslam");
	private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("roar");
	private static final RawAnimation TAILORB = RawAnimation.begin().thenPlay("tailorb");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.remnant_bosses.umbrakar"),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

	private int attackCooldown;
	private int attackTicks;
	private int attackDuration;
	private int impactAt;
	private boolean impactDone;
	private String pendingAttack = "";
	private Vec3 pendingCenter = Vec3.ZERO;
	private boolean statsApplied;
	private boolean phaseRoared;

	public UmbrakarEntity(EntityType<? extends UmbrakarEntity> type, Level level) {
		super(type, level);
		this.xpReward = 80;
		this.setMaxUpStep(1.5f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 750.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
				.add(Attributes.ATTACK_KNOCKBACK, 1.6);
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
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_ATTACK, "");
		this.entityData.define(DATA_PHASE_TWO, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new HuntGoal());
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9));
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
	}

	private void applyConfiguredStats() {
		if (statsApplied) {
			return;
		}
		statsApplied = true;
		setAttr(Attributes.MAX_HEALTH, cfg("max_health", 750));
		setAttr(Attributes.ATTACK_DAMAGE, cfg("attack_damage", 14));
		setAttr(Attributes.MOVEMENT_SPEED, Math.max(0.28, cfg("movement_speed", 0.32)));
		setAttr(Attributes.ARMOR, cfg("armor", 10));
		setAttr(Attributes.FOLLOW_RANGE, cfg("follow_range", 48));
		this.setHealth(this.getMaxHealth());
	}

	private void setAttr(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
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

		float threshold = (float) cfg("hp_threshold_phase_2", 40);
		boolean phaseTwo = this.getHealth() <= this.getMaxHealth() * (threshold / 100.0f);
		if (phaseTwo && !this.entityData.get(DATA_PHASE_TWO)) {
			this.entityData.set(DATA_PHASE_TWO, true);
			setAttr(Attributes.MOVEMENT_SPEED, Math.max(0.34, cfg("movement_speed_phase_2", 0.38)));
			setAttr(Attributes.ATTACK_DAMAGE, cfg("attack_damage_phase_2", 18));
			if (!phaseRoared) {
				phaseRoared = true;
				queueAttack("roar", 70, 24, this.position());
			}
		}

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || this.attackCooldown > 0 || this.attackTicks > 0) {
			return;
		}

		double dist = this.distanceTo(target);
		int roll = this.random.nextInt(100);
		if (dist < 4.2 && roll < 50) {
			queueAttack("bite", 25, 10, target.position());
		} else if (dist < 6.8 && roll < 70) {
			queueAttack("frontslam", 50, 22, this.position().add(this.getLookAngle().scale(3.2)));
		} else if (dist < 8.0 && roll < 82) {
			queueAttack("tailslam", 40, 18, this.position().add(this.getLookAngle().scale(-4.0)));
		} else if (dist > 5.5 && (this.entityData.get(DATA_PHASE_TWO) || roll < 90)) {
			queueAttack("tailorb", 35, 16, this.position().add(this.getLookAngle().scale(-2.8)).add(0, 2.2, 0));
		} else if (dist < 12.0 && roll < 96) {
			queueAttack("roar", 70, 24, this.position());
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
		this.attackCooldown = ticks + (this.entityData.get(DATA_PHASE_TWO) ? 16 : 28);
		this.triggerAnim("combat", name);
		if (this.level() instanceof ServerLevel server) {
			burst(server, this.position().add(0, 1.8, 0), name, false);
		}
	}

	private void resolveImpact() {
		if (!(this.level() instanceof ServerLevel server)) {
			return;
		}
		switch (this.pendingAttack) {
			case "bite" -> {
				hurtAround(this.pendingCenter, 3.2, cfg("bite_damage", 16), 0.45);
				burst(server, this.pendingCenter.add(0, 1.2, 0), "bite", true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.3f, 0.7f);
			}
			case "frontslam" -> {
				hurtAround(this.pendingCenter, 4.4, cfg("slam_damage", 18), 1.15);
				burst(server, this.pendingCenter, "frontslam", true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.55f);
			}
			case "tailslam" -> {
				hurtAround(this.pendingCenter, 4.6, cfg("tail_damage", 15), 1.45);
				burst(server, this.pendingCenter, "tailslam", true);
				this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2f, 0.45f);
			}
			case "tailorb" -> {
				LivingEntity target = this.getTarget();
				if (target != null && target.isAlive()) {
					spawnOrb(target);
				}
				burst(server, this.pendingCenter, "tailorb", true);
			}
			case "roar" -> {
				this.level().playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.55f);
				hurtAround(this.position(), cfg("roar_range", 16), cfg("roar_damage", 8), 1.8);
				burst(server, this.position().add(0, 2.2, 0), "roar", true);
			}
			default -> {
			}
		}
	}

	private void hurtAround(Vec3 center, double radius, double damage, double knock) {
		AABB box = new AABB(center, center).inflate(radius);
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
			living.hurt(this.damageSources().mobAttack(this), (float) damage);
			Vec3 push = living.position().subtract(center).normalize().scale(knock).add(0, 0.25, 0);
			living.push(push.x, push.y, push.z);
		}
	}

	private void spawnOrb(LivingEntity target) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		UmbrakarOrbEntity orb = ModEntities.UMBRAKAR_ORB.get().create(serverLevel);
		if (orb == null) {
			return;
		}
		Vec3 tail = this.position().add(this.getLookAngle().scale(-2.8)).add(0, 2.2, 0);
		orb.moveTo(tail.x, tail.y, tail.z, this.getYRot(), 0);
		orb.setOwner(this);
		orb.setTarget(target);
		serverLevel.addFreshEntity(orb);
	}

	private void burst(ServerLevel server, Vec3 pos, String attack, boolean impact) {
		int extra = this.entityData.get(DATA_PHASE_TWO) ? 12 : 0;
		switch (attack) {
			case "bite" -> {
				server.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 18 + extra, 0.6, 0.4, 0.6, 0.25);
				server.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 10 + extra, 0.4, 0.3, 0.4, 0.04);
				server.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 16, 0.5, 0.4, 0.5, 0.6);
			}
			case "frontslam" -> {
				if (impact) {
					server.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 3, 0.8, 0.1, 0.8, 0);
				}
				server.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.2, pos.z, 28 + extra, 1.6, 0.3, 1.6, 0.08);
				server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.4, pos.z, 30 + extra, 1.4, 0.5, 1.4, 0.12);
				server.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.1, pos.z, 10, 1.0, 0.2, 1.0, 0.02);
			}
			case "tailslam" -> {
				server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.3, pos.z, 20 + extra, 1.3, 0.4, 1.3, 0.05);
				server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 24 + extra, 1.2, 0.5, 1.2, 0.1);
				server.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.6, pos.z, 16, 1.0, 0.4, 1.0, 0.02);
			}
			case "tailorb" -> {
				server.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 22 + extra, 0.5, 0.5, 0.5, 0.08);
				server.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 18, 0.45, 0.45, 0.45, 0.03);
				server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 20, 0.4, 0.4, 0.4, 0.12);
			}
			case "roar" -> {
				if (impact) {
					server.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 0.4, pos.z, 1, 0, 0, 0, 0);
				}
				server.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, 40 + extra, 2.4, 1.2, 2.4, 0.06);
				server.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 50 + extra, 2.2, 1.4, 2.2, 0.8);
				server.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 18, 1.8, 1.0, 1.8, 0.05);
			}
			default -> server.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 12, 0.6, 0.6, 0.6, 0.4);
		}
	}

	private void tickClientFx() {
		Vec3 pos = this.position();
		boolean phaseTwo = this.entityData.get(DATA_PHASE_TWO);
		int ambient = phaseTwo ? 6 : 3;
		for (int i = 0; i < ambient; i++) {
			double x = pos.x + (this.random.nextDouble() - 0.5) * 3.4;
			double y = pos.y + 0.6 + this.random.nextDouble() * 2.8;
			double z = pos.z + (this.random.nextDouble() - 0.5) * 3.4;
			this.level().addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.05, 0);
			if (this.random.nextBoolean()) {
				this.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.02, 0);
			}
		}
		if (phaseTwo) {
			for (int i = 0; i < 3; i++) {
				this.level().addParticle(ParticleTypes.DRAGON_BREATH,
						pos.x + (this.random.nextDouble() - 0.5) * 3.2,
						pos.y + 1.2 + this.random.nextDouble() * 2.0,
						pos.z + (this.random.nextDouble() - 0.5) * 3.2,
						0, 0.02, 0);
				this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
						pos.x + (this.random.nextDouble() - 0.5) * 2.6,
						pos.y + 0.4 + this.random.nextDouble() * 2.4,
						pos.z + (this.random.nextDouble() - 0.5) * 2.6,
						0, 0.01, 0);
			}
		}
		if (this.getDeltaMovement().horizontalDistanceSqr() > 0.002) {
			this.level().addParticle(ParticleTypes.CLOUD, pos.x, pos.y + 0.1, pos.z,
					(this.random.nextDouble() - 0.5) * 0.1, 0.02, (this.random.nextDouble() - 0.5) * 0.1);
			this.level().addParticle(ParticleTypes.ASH, pos.x, pos.y + 0.15, pos.z, 0, 0.01, 0);
		}
		String attack = this.getAttackName();
		if (!attack.isEmpty() && this.tickCount % 2 == 0) {
			Vec3 mouth = pos.add(this.getLookAngle().scale(2.2)).add(0, 2.0, 0);
			this.level().addParticle(ParticleTypes.WITCH, mouth.x, mouth.y, mouth.z, 0, 0.04, 0);
			this.level().addParticle(ParticleTypes.ENCHANT, mouth.x, mouth.y, mouth.z,
					(this.random.nextDouble() - 0.5) * 0.4, 0.2, (this.random.nextDouble() - 0.5) * 0.4);
		}
	}

	private static double cfg(String key, double fallback) {
		double value = JaumlConfigLib.getNumberValue("remnant/bosses", "umbrakar", key);
		if (value <= 0.0 || (value == 1.0 && Math.abs(fallback - 1.0) > 1.0E-4)) {
			return fallback;
		}
		return value;
	}

	public String getAttackName() {
		return this.entityData.get(DATA_ATTACK);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return this.getBoundingBox().inflate(6.0, 3.0, 6.0);
	}

	@Override
	protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
		return 2.85f;
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
		super.remove(reason);
		this.bossEvent.removeAllPlayers();
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 1.6f;
	}

	@Override
	public int getMaxHeadXRot() {
		return 30;
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
		AnimationController<UmbrakarEntity> combat = new AnimationController<>(this, "combat", 4, this::combatController);
		combat.triggerableAnim("bite", BITE);
		combat.triggerableAnim("frontslam", FRONTSLAM);
		combat.triggerableAnim("tailslam", TAILSLAM);
		combat.triggerableAnim("roar", ROAR);
		combat.triggerableAnim("tailorb", TAILORB);
		controllers.add(combat);
	}

	private PlayState movementController(AnimationState<UmbrakarEntity> state) {
		if (!this.isAlive()) {
			return state.setAndContinue(DEATH);
		}
		String attack = this.getAttackName();
		if ("bite".equals(attack) || "frontslam".equals(attack) || "tailslam".equals(attack)) {
			return PlayState.STOP;
		}
		double moving = this.getDeltaMovement().horizontalDistanceSqr();
		if (moving > 0.004 || state.getLimbSwingAmount() > 0.25) {
			return state.setAndContinue(this.entityData.get(DATA_PHASE_TWO) ? RUN : WALK);
		}
		return state.setAndContinue(IDLE);
	}

	private PlayState combatController(AnimationState<UmbrakarEntity> state) {
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
			LivingEntity target = UmbrakarEntity.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void tick() {
			LivingEntity target = UmbrakarEntity.this.getTarget();
			if (target == null) {
				return;
			}
			UmbrakarEntity.this.getLookControl().setLookAt(target, 40.0f, 40.0f);
			double dist = UmbrakarEntity.this.distanceTo(target);
			double speed = UmbrakarEntity.this.entityData.get(DATA_PHASE_TWO) ? 1.35 : 1.2;
			if (dist > 3.4) {
				boolean path = UmbrakarEntity.this.getNavigation().moveTo(target, speed);
				if (!path || UmbrakarEntity.this.getNavigation().isDone() || UmbrakarEntity.this.horizontalCollision) {
					UmbrakarEntity.this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
					if (UmbrakarEntity.this.horizontalCollision && UmbrakarEntity.this.onGround()) {
						UmbrakarEntity.this.setDeltaMovement(UmbrakarEntity.this.getDeltaMovement().add(0.0, 0.42, 0.0));
					}
				}
			}
		}
	}
}
