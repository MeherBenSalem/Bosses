package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ember Servitor: a Kharum maintenance drone that hovers 2-4 blocks up on a spinning rotor. In a fight it holds
 * 4-9 blocks from its target and fires ember bolts (released at tick 10 of {@code shoot}; 5 damage and a brief
 * burn). Out of combat it flies to damaged Ashbound constructs nearby and welds them back together with the
 * {@code repair} loop (0.5 health at ticks 8, 16 and 24 of each 32-tick loop: about 1 health a second).
 */
public class EmberServitorEntity extends HoveringRemnantEntity {
	public static final int SHOT_RELEASE = 10, REPAIR_PULSE = 8;
	private LivingEntity patient;

	public EmberServitorEntity(EntityType<? extends EmberServitorEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return hoveringAttributes(14, 3, 2, 0.30);
	}

	@Override
	public String configId() {
		return "ember_servitor";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.EMBER_SERVITOR;
	}

	@Override
	protected double hoverHeight() {
		return 2.5;
	}

	@Override
	protected double[] holdRange() {
		return new double[] {cfg("hold_min_distance", 1, 32), cfg("hold_max_distance", 2, 48)};
	}

	@Override
	protected String movementClip(boolean moving) {
		return moving ? "fly" : "idle";
	}

	@Override
	protected boolean familyVictim(LivingEntity entity) {
		return Ashbound.victim(entity);
	}

	@Override
	protected boolean familyMember(Entity entity) {
		return Ashbound.isAshbound(entity);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(1, new RepairGoal());
	}

	@Override
	protected void serverTick() {
		super.serverTick();
		// A fight always comes first: a weld in progress stops as soon as it has a target.
		if ("repair".equals(action()) && (getTarget() != null || !treatable(patient))) cancelAction();
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (distance <= cfg("bolt_range", 4, 64) - 2) {
			begin("shoot", target.position());
			playSound(SoundEvents.BLAZE_AMBIENT, 0.8f, 1.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		switch (action) {
			case "shoot" -> {
				LivingEntity target = getTarget();
				if (target != null && age < SHOT_RELEASE) faceToward(target.position(), 20);
				if (age == SHOT_RELEASE && target != null && level() instanceof ServerLevel server) {
					Vec3 lens = lens();
					Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0);
					EmberBoltEntity.fire(server, this, lens, aim.subtract(lens), cfg("bolt_speed", 0.1, 3), (float) cfg("bolt_damage", 0, 100),
							cfg("bolt_range", 4, 64), (int) cfg("bolt_burn_ticks", 0, 600));
					playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.5f);
				}
			}
			case "repair" -> {
				if (!treatable(patient)) return;
				faceToward(patient.position(), 15);
				if (age % REPAIR_PULSE == 0 && age < clips().ticks("repair")) weld();
			}
			default -> {
			}
		}
	}

	@Override
	protected boolean onActionEnd(String action) {
		// The repair loop keeps going while the patient is hurt, in reach, and nothing attacks.
		if ("repair".equals(action) && getTarget() == null && treatable(patient) && inReach(patient)) {
			begin("repair", patient.position());
			return true;
		}
		return false;
	}

	/** The eye lens the bolts leave from. */
	private Vec3 lens() {
		return position().add(Vec3.directionFromRotation(0, getYRot()).scale(0.35)).add(0, 0.95, 0);
	}

	/** The welding claw (right arm) under the hull. */
	private Vec3 claw() {
		Vec3 forward = Vec3.directionFromRotation(0, getYRot());
		Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
		return position().add(forward.scale(0.3)).add(right.scale(-0.35)).add(0, 0.4, 0);
	}

	/** One heal pulse: sparks run from the claw to the patient. */
	private void weld() {
		patient.heal((float) cfg("repair_heal", 0, 20));
		if (level() instanceof ServerLevel server) {
			Vec3 from = claw(), to = patient.position().add(0, patient.getBbHeight() * 0.6, 0);
			for (int i = 0; i <= 6; i++) {
				Vec3 p = from.lerp(to, i / 6.0);
				server.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
			}
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.2, 0.2, 0.2, 0.05);
		}
		playSound(SoundEvents.SMITHING_TABLE_USE, 0.4f, 1.7f);
	}

	/** A damaged construct it may weld (never itself). */
	private boolean treatable(LivingEntity entity) {
		return entity != null && entity != this && entity.isAlive() && !entity.isRemoved() && Ashbound.isAshbound(entity)
				&& entity.getHealth() < entity.getMaxHealth() && entity.distanceToSqr(this) <= 24 * 24;
	}

	private boolean inReach(LivingEntity entity) {
		double reach = cfg("repair_reach", 1, 8);
		return entity.getBoundingBox().inflate(reach).contains(position().add(0, getBbHeight() / 2, 0));
	}

	/** Out of combat: fly to the most damaged construct nearby and weld it while it stays hurt. */
	private final class RepairGoal extends Goal {
		private int repath, search;

		RepairGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (getTarget() != null || !action().isEmpty()) return false;
			// Look around every few checks. Vanilla tries canUse on every other tick with a fixed parity per mob,
			// so a tickCount modulus here could never line up for half of all servitors.
			if (search-- > 0) return false;
			search = 4;
			double radius = cfg("repair_radius", 1, 32);
			LivingEntity best = null;
			float worst = 1;
			for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), e -> treatable(e)
					&& e.distanceToSqr(EmberServitorEntity.this) <= radius * radius)) {
				float share = ally.getHealth() / Math.max(1, ally.getMaxHealth());
				if (share < worst) {
					worst = share;
					best = ally;
				}
			}
			patient = best;
			return best != null;
		}

		@Override
		public boolean canContinueToUse() {
			return getTarget() == null && treatable(patient);
		}

		@Override
		public void start() {
			repath = 0;
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
			if (!action().isEmpty()) return;
			getLookControl().setLookAt(patient, 30, 30);
			if (inReach(patient)) {
				getNavigation().stop();
				begin("repair", patient.position());
				return;
			}
			if (--repath > 0 && !getNavigation().isDone()) return;
			repath = 10;
			// Hover beside the patient, a little above its middle.
			Vec3 side = position().subtract(patient.position()).multiply(1, 0, 1);
			if (side.lengthSqr() < 1.0E-4) side = Vec3.directionFromRotation(0, getYRot() + 180);
			Vec3 spot = patient.position().add(side.normalize().scale(patient.getBbWidth() / 2 + 1.2));
			getNavigation().moveTo(spot.x, patient.getY() + patient.getBbHeight() * 0.5, spot.z, 1.0);
		}
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		// The exhaust under the hull glows; the lens flares while it aims.
		if (tickCount % 4 == 0 && random.nextDouble() < density) {
			level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.3, getZ(), 0, -0.03, 0);
		}
		if ("shoot".equals(action()) && actionAge() < SHOT_RELEASE && tickCount % 2 == 0) {
			Vec3 lens = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(0.35)).add(0, 0.95, 0);
			level().addParticle(ParticleTypes.FLAME, lens.x, lens.y, lens.z, 0, 0.005, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BEEHIVE_WORK;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 120;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COPPER_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.COPPER_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
	}

	@Override
	public float getVoicePitch() {
		return 1.3f + (random.nextFloat() - random.nextFloat()) * 0.1f;
	}
}
