package com.nightbeam.remnants.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A floating body on the Legion action machine for families other than the Forgotten Souls (the Ember
 * Servitor and the Shardling): flying navigation, no gravity, no fall damage, and a gentle lift that keeps it
 * {@link #hoverHeight()} above the ground whenever it is not travelling somewhere. It works like
 * {@link FloatingSoulEntity}, but takes its allegiance from {@link #familyVictim} and {@link #familyMember}.
 */
public abstract class HoveringRemnantEntity extends LegionEntity {
	private int repath;

	protected HoveringRemnantEntity(EntityType<? extends HoveringRemnantEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder hoveringAttributes(double health, double damage, double armor, double speed) {
		return legionAttributes(health, damage, armor, speed).add(Attributes.FLYING_SPEED, speed);
	}

	/** Blocks between the ground and this body's feet while it idles. */
	protected abstract double hoverHeight();

	/** Preferred distance band to its target: it closes in beyond the far edge and backs off inside the near one. */
	protected abstract double[] holdRange();

	/** A living target this body's family may damage. */
	protected abstract boolean familyVictim(LivingEntity entity);

	/** Whether {@code entity} belongs to this body's family: its damage is ignored and it is never a target. */
	protected abstract boolean familyMember(Entity entity);

	@Override
	public boolean canHit(LivingEntity entity) {
		return entity != this && familyVictim(entity);
	}

	@Override
	protected boolean isFriend(Entity entity) {
		return familyMember(entity);
	}

	/** Constructs and relics are vessels, not the dead: smite and healing potions treat them as ordinary creatures. */
	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	/** Flying uses its own speed attribute; the configured movement speed drives both. */
	@Override
	protected void applyConfig() {
		setBase(Attributes.FLYING_SPEED, cfg("movement_speed", 0.05, 0.6));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		navigation.setCanPassDoors(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 0.8));
	}

	@Override
	protected void moveTick(LivingEntity target, double distance) {
		getLookControl().setLookAt(target, 30, 30);
		double[] range = holdRange();
		boolean far = distance > range[1] || !hasLineOfSight(target), near = distance < range[0];
		if (!far && !near) {
			getNavigation().stop();
			return;
		}
		if (--repath > 0 && !getNavigation().isDone()) return;
		repath = 10;
		Vec3 away = position().subtract(target.position()).multiply(1, 0, 1);
		if (away.lengthSqr() < 1.0E-4) away = Vec3.directionFromRotation(0, getYRot() + 180);
		double want = far ? (range[0] + range[1]) / 2 : range[1];
		Vec3 spot = target.position().add(away.normalize().scale(want));
		getNavigation().moveTo(spot.x, groundBelow(spot) + hoverHeight(), spot.z, 1.0);
	}

	/** Y of the first solid top within 8 blocks below {@code at}, or {@code at}'s own Y when none. */
	protected double groundBelow(Vec3 at) {
		BlockPos.MutableBlockPos pos = BlockPos.containing(at.x, at.y + 1, at.z).mutable();
		for (int i = 0; i < 10; i++, pos.move(0, -1, 0)) {
			if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) return pos.getY() + 1;
		}
		return at.y;
	}

	@Override
	protected void serverTick() {
		super.serverTick();
		// Hover: drift up to the idle height when nothing else moves it, never down through a floor.
		if (getNavigation().isDone() && action().isEmpty()) {
			double lift = groundBelow(position()) + hoverHeight() - getY();
			if (lift > 0.3) setDeltaMovement(getDeltaMovement().add(0, Math.min(0.05, lift * 0.05), 0));
		}
	}

	@Override
	public void travel(Vec3 input) {
		if (isControlledByLocalInstance()) {
			if (isInWater()) {
				moveRelative(0.02f, input);
				move(MoverType.SELF, getDeltaMovement());
				setDeltaMovement(getDeltaMovement().scale(0.8));
			} else if (isInLava()) {
				moveRelative(0.02f, input);
				move(MoverType.SELF, getDeltaMovement());
				setDeltaMovement(getDeltaMovement().scale(0.5));
			} else {
				moveRelative(getSpeed(), input);
				move(MoverType.SELF, getDeltaMovement());
				setDeltaMovement(getDeltaMovement().scale(0.91));
			}
		}
		calculateEntityAnimation(false);
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean onClimbable() {
		return false;
	}
}
