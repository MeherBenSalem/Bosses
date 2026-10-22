package com.nightbeam.remnants.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * A Forgotten Soul that floats instead of walking (Memory Wraith, Soul Collector): flying navigation,
 * no gravity, no fall damage, and a gentle lift that keeps it {@link #hoverHeight()} above the ground
 * whenever it is not travelling somewhere.
 */
public abstract class FloatingSoulEntity extends ForgottenSoulEntity {
	private int repath;

	protected FloatingSoulEntity(EntityType<? extends FloatingSoulEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder floatingAttributes(double health, double damage, double armor, double speed) {
		return legionAttributes(health, damage, armor, speed).add(Attributes.FLYING_SPEED, speed);
	}

	/** Blocks between the ground and this soul's feet while it idles. */
	protected abstract double hoverHeight();

	/** Preferred distance band to its target: it closes in beyond the far edge and backs off inside the near one. */
	protected abstract double[] holdRange();

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
