package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Catacomb Rat: a neutral scavenger with a candle on its back (purely visual). It
 * shies away from light level 10 or more, and when one is hit, 3 to 6 rats nearby swarm the attacker
 * together. Harmless alone, 2 damage per bite.
 */
public class CatacombRatEntity extends CryptbornEntity {
	public static final int BITE_HIT = 6;
	private int angerTicks;

	public CatacombRatEntity(EntityType<? extends CatacombRatEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(8, 2, 0, 0.32);
	}

	@Override
	public String configId() {
		return "catacomb_rat";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.CATACOMB_RAT;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(2, new FleeLightGoal());
	}

	/** Neutral: it only fights back, with its swarm. */
	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	protected double stopDistance() {
		return 0.9;
	}

	@Override
	protected void serverTick() {
		LivingEntity target = getTarget();
		if (target == null) return;
		if (!target.isAlive() || --angerTicks <= 0 && distanceToSqr(target) > 4) setTarget(null);
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (distance - target.getBbWidth() / 2 - getBbWidth() / 2 <= 0.9) {
			begin("attack", target.position());
			playSound(SoundEvents.SILVERFISH_AMBIENT, 0.6f, 1.8f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		if ("attack".equals(action) && age == BITE_HIT) {
			strikeArc(getBbWidth() / 2 + 1.0, 120, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.1);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		Entity attacker = source.getEntity();
		if (hurt && !level().isClientSide() && attacker instanceof LivingEntity living && living != this && canHit(living)) swarm(living);
		return hurt;
	}

	/** One rat is hit: up to 3-6 rats in all (this one included) turn on the attacker. */
	private void swarm(LivingEntity attacker) {
		int anger = (int) cfg("anger_ticks", 20, 6000);
		angerTicks = anger;
		if (getTarget() == null) setTarget(attacker);
		double radius = cfg("swarm_radius", 1, 32);
		int min = (int) cfg("swarm_min", 1, 16), max = Math.max(min, (int) cfg("swarm_max", 1, 16));
		int group = min + random.nextInt(max - min + 1);
		List<CatacombRatEntity> rats = new ArrayList<>(level().getEntitiesOfClass(CatacombRatEntity.class, getBoundingBox().inflate(radius),
				r -> r != this && r.isAlive() && r.getTarget() == null && r.distanceToSqr(this) <= radius * radius));
		Collections.shuffle(rats, new java.util.Random(random.nextLong()));
		for (CatacombRatEntity rat : rats.subList(0, Math.min(rats.size(), group - 1))) {
			rat.setTarget(attacker);
			rat.angerTicks = anger;
		}
	}

	/** Light level 10 or more: scurry to the nearest darker spot found, unless already fighting. */
	private final class FleeLightGoal extends Goal {
		private Vec3 refuge;
		private int search;

		FleeLightGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		private boolean bright(BlockPos pos) {
			return level().getMaxLocalRawBrightness(pos) >= cfg("flee_light_level", 0, 16);
		}

		@Override
		public boolean canUse() {
			if (getTarget() != null || !action().isEmpty()) return false;
			// Check the light every few tries. Vanilla tries canUse on every other tick with a fixed parity per mob,
			// so a tickCount modulus here could never line up for half of all rats.
			if (search-- > 0) return false;
			search = 4;
			if (!bright(blockPosition())) return false;
			for (int i = 0; i < 10; i++) {
				Vec3 spot = DefaultRandomPos.getPos(CatacombRatEntity.this, 10, 4);
				if (spot != null && !bright(BlockPos.containing(spot))) {
					refuge = spot;
					return true;
				}
			}
			return false;
		}

		@Override
		public void start() {
			getNavigation().moveTo(refuge.x, refuge.y, refuge.z, 1.4);
		}

		@Override
		public boolean canContinueToUse() {
			return getTarget() == null && !getNavigation().isDone();
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SILVERFISH_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SILVERFISH_STEP, 0.15f, 1.6f);
	}

	@Override
	public float getVoicePitch() {
		return 1.7f + (random.nextFloat() - random.nextFloat()) * 0.1f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}
}
