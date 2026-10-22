package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.BoneMonsterConfig;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Hollow Legion's shared body: one synchronized action at a time, server-timed hits read from
 * {@link RemnantClips}, Legion friendly fire disabled, client effects derived from synced state.
 */
public abstract class LegionEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(LegionEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(LegionEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SERIAL = SynchedEntityData.defineId(LegionEntity.class, EntityDataSerializers.INT);
	/** Client-only event: draw the standard bearer's aura links. */
	protected static final byte EVENT_AURA = 100;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	protected final Set<UUID> struck = new HashSet<>();
	protected int cooldown = 20;
	protected float lockedYaw;
	protected boolean yawLocked;
	private boolean configured, loaded;
	private int seenSerial = -1, seenHurtStamp = Integer.MIN_VALUE;

	protected LegionEntity(EntityType<? extends LegionEntity> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	/** Config file name under remnant/monsters. */
	public abstract String configId();

	/** Animation clips this mob plays. */
	public abstract RemnantClips.Sheet clips();

	/** Called when no action runs, the cooldown is spent and the target is alive and visible. */
	protected abstract void selectAction(LivingEntity target, double distance);

	/** Server tick of the running action; {@code age} starts at 1. */
	protected abstract void actionTick(String action, int age);

	/** Hook after an action's clip ends; return true when it chained into another action. */
	protected boolean onActionEnd(String action) {
		return false;
	}

	protected String movementClip(boolean moving) {
		return moving ? "walk" : "idle";
	}

	protected double stopDistance() {
		return 2.0;
	}

	protected void serverTick() {
	}

	protected void clientTick() {
	}

	protected void applyConfig() {
	}

	public static AttributeSupplier.Builder legionAttributes(double health, double damage, double armor, double speed) {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage)
				.add(Attributes.ARMOR, armor).add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 28);
	}

	/** Every Legion mob is undead (1.21.1 lists them in the minecraft:undead entity tag instead). */
	@Override
	public MobType getMobType() {
		return MobType.UNDEAD;
	}

	protected double cfg(String key, double min, double max) {
		return BoneMonsterConfig.value(configId(), key, min, max);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(ACTION, "");
		entityData.define(AGE, 0);
		entityData.define(SERIAL, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Goal() {
			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

			@Override
			public boolean canUse() {
				return getTarget() != null && getTarget().isAlive();
			}

			@Override
			public void stop() {
				getNavigation().stop();
			}

			@Override
			public boolean requiresUpdateEveryTick() {
				return true;
			}

			@Override
			public void tick() {
				LivingEntity target = getTarget();
				if (target == null || !action().isEmpty()) {
					getNavigation().stop();
					return;
				}
				moveTick(target, distanceTo(target));
			}
		});
		goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16));
		goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		registerTargetGoals();
	}

	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Movement while a target is set and no action runs. */
	protected void moveTick(LivingEntity target, double distance) {
		getLookControl().setLookAt(target, 30, 30);
		if (distance > stopDistance() || !hasLineOfSight(target)) getNavigation().moveTo(target, 1.0);
		else getNavigation().stop();
	}

	public String action() {
		return entityData.get(ACTION);
	}

	public int actionAge() {
		return entityData.get(AGE);
	}

	private void configure() {
		if (configured) return;
		configured = true;
		setBase(Attributes.MAX_HEALTH, cfg("max_health", 1, 1024));
		setBase(Attributes.ATTACK_DAMAGE, cfg("attack_damage", 0, 100));
		setBase(Attributes.ARMOR, cfg("armor", 0, 30));
		setBase(Attributes.MOVEMENT_SPEED, cfg("movement_speed", 0.05, 0.6));
		xpReward = (int) cfg("xp_reward", 0, 1000);
		applyConfig();
		setHealth(loaded ? Math.min(getHealth(), getMaxHealth()) : getMaxHealth());
	}

	protected void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) instance.setBaseValue(value);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (isAlive()) clientTick();
			return;
		}
		configure();
		if (!isAlive() || isNoAi()) return;
		serverTick();
		String action = action();
		if (!action.isEmpty()) {
			int age = entityData.get(AGE) + 1;
			entityData.set(AGE, age);
			if (yawLocked) {
				setYRot(lockedYaw);
				setYBodyRot(lockedYaw);
				setYHeadRot(lockedYaw);
			}
			actionTick(action, age);
			if (action.equals(action()) && age >= clips().ticks(action)) {
				entityData.set(ACTION, "");
				yawLocked = false;
				if (!onActionEnd(action) && action().isEmpty()) {
					cooldown = (int) cfg("attack_cooldown_ticks", 5, 400);
				}
			}
			return;
		}
		if (cooldown > 0) cooldown--;
		LivingEntity target = getTarget();
		if (cooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target)) {
			selectAction(target, distanceTo(target));
		}
	}

	/** Starts {@code action}, facing {@code face} when given; the yaw stays locked until it ends. */
	protected void begin(String action, Vec3 face) {
		if (face != null) faceToward(face, 360);
		lockedYaw = getYRot();
		yawLocked = true;
		struck.clear();
		getNavigation().stop();
		entityData.set(AGE, 0);
		entityData.set(SERIAL, entityData.get(SERIAL) + 1);
		entityData.set(ACTION, action);
	}

	protected void cancelAction() {
		entityData.set(ACTION, "");
		yawLocked = false;
	}

	/** Turn toward {@code point} by at most {@code maxDegrees}; updates the locked yaw. */
	protected void faceToward(Vec3 point, float maxDegrees) {
		float wanted = HollowLegion.yawToward(position(), point);
		float delta = net.minecraft.util.Mth.wrapDegrees(wanted - getYRot());
		float yaw = getYRot() + net.minecraft.util.Mth.clamp(delta, -maxDegrees, maxDegrees);
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		lockedYaw = yaw;
	}

	/** Living targets this mob may damage. */
	public boolean canHit(LivingEntity entity) {
		return entity != this && HollowLegion.sovereignVictim(entity);
	}

	/** Entities whose damage this mob ignores (Legion friendly fire). */
	protected boolean isFriend(Entity entity) {
		return HollowLegion.isLegion(this) && HollowLegion.isSovereignSide(entity);
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return super.isAlliedTo(other) || (other != this && isFriend(other));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && canHit(target);
	}

	/** Melee hit through {@code mobAttack}, so difficulty scaling applies. Each target once per action. */
	protected boolean strike(LivingEntity target, float damage, double knockback) {
		if (!struck.add(target.getUUID())) return false;
		boolean hurt = target.hurt(damageSources().mobAttack(this), damage);
		if (hurt && knockback > 0) {
			target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ());
		}
		return hurt;
	}

	protected void strikeArc(double reach, double arcDegrees, float damage, double knockback) {
		List<LivingEntity> hits = HollowLegion.arc(this, reach, arcDegrees, this::canHit);
		for (LivingEntity hit : hits) strike(hit, damage, knockback);
	}

	protected static boolean isMelee(DamageSource source) {
		return !source.is(DamageTypeTags.IS_PROJECTILE) && source.getEntity() instanceof LivingEntity
				&& source.getDirectEntity() == source.getEntity();
	}

	/** Whether {@code source} comes from within {@code halfAngle} degrees of this mob's facing. */
	protected boolean fromFront(DamageSource source, double halfAngle) {
		Vec3 from = source.getSourcePosition();
		if (from == null) return false;
		Vec3 offset = from.subtract(position()).multiply(1, 0, 1);
		if (offset.lengthSqr() < 1.0E-4) return true;
		Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
		return offset.normalize().dot(facing) >= Math.cos(Math.toRadians(halfAngle));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && attacker != this && isFriend(attacker)) return false;
		return super.hurt(source, amount);
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		getNavigation().stop();
		if (deathTime >= clips().ticks("death") && !level().isClientSide && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		loaded = true;
		configured = false;
		entityData.set(ACTION, "");
		entityData.set(AGE, 0);
		cooldown = 20;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "legion", 3, state -> {
			RemnantClips.Sheet sheet = clips();
			if (!isAlive()) return state.setAndContinue(RemnantAnims.of(sheet, "death"));
			String action = action();
			if (!action.isEmpty() && sheet.has(action)) {
				int serial = entityData.get(SERIAL);
				if (serial != seenSerial) {
					seenSerial = serial;
					state.getController().forceAnimationReset();
				}
				return state.setAndContinue(RemnantAnims.of(sheet, action));
			}
			if (hurtTime > 0 && hurtDuration - hurtTime < sheet.ticks("hurt")) {
				int stamp = tickCount - (hurtDuration - hurtTime);
				if (stamp != seenHurtStamp) {
					seenHurtStamp = stamp;
					state.getController().forceAnimationReset();
				}
				return state.setAndContinue(RemnantAnims.of(sheet, "hurt"));
			}
			return state.setAndContinue(RemnantAnims.of(sheet, movementClip(state.isMoving())));
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
