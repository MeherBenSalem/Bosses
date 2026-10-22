package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.BoneMonsterConfig;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rusted Guardian: an elite Kharum soldier, a boiler on legs with a piston hammer. {@code smash} lifts the hammer
 * over its head for a full second and drives it down at tick 26 as a 3-block ground ring (jump it or step out);
 * {@code sweep} winds back and swings a wide frontal arc at tick 18 that throws its victims back. Both leave a long
 * recovery. It turns at most 8 degrees per tick and only attacks what is in front of it, so flanking pays.
 */
public class RustedGuardianEntity extends AshboundEntity {
	public static final int SMASH_HIT = 26, SWEEP_HIT = 18;
	/** Attacks start only with the target inside this half-angle of its facing. */
	private static final float ATTACK_HALF_ANGLE = 40;

	public RustedGuardianEntity(EntityType<? extends RustedGuardianEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(70, 14, 14, 0.20).add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public String configId() {
		return "rusted_guardian";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.RUSTED_GUARDIAN;
	}

	@Override
	protected void applyConfig() {
		setBase(Attributes.KNOCKBACK_RESISTANCE, cfg("knockback_resistance", 0, 1));
	}

	@Override
	protected double stopDistance() {
		return 2.4;
	}

	private float turnRate() {
		return (float) cfg("turn_degrees", 0.5, 45);
	}

	@Override
	public int getHeadRotSpeed() {
		return Math.max(1, (int) turnRate());
	}

	@Override
	public int getMaxHeadYRot() {
		return 20;
	}

	/** Slow turning: every rotation source is capped at {@code turn_degrees} per tick. */
	@Override
	public void aiStep() {
		float yaw = getYRot(), body = yBodyRot, head = yHeadRot;
		super.aiStep();
		if (level().isClientSide) return;
		float limit = turnRate();
		setYRot(yaw + Mth.clamp(Mth.wrapDegrees(getYRot() - yaw), -limit, limit));
		yBodyRot = body + Mth.clamp(Mth.wrapDegrees(yBodyRot - body), -limit, limit);
		yHeadRot = head + Mth.clamp(Mth.wrapDegrees(yHeadRot - head), -limit * 2, limit * 2);
	}

	@Override
	protected void moveTick(LivingEntity target, double distance) {
		faceToward(target.position(), turnRate());
		if (distance > stopDistance() || !hasLineOfSight(target)) getNavigation().moveTo(target, 1.0);
		else getNavigation().stop();
	}

	private boolean facing(LivingEntity target) {
		float wanted = HollowLegion.yawToward(position(), target.position());
		return Math.abs(Mth.wrapDegrees(wanted - getYRot())) <= ATTACK_HALF_ANGLE;
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (!facing(target)) return;
		double reach = distance - target.getBbWidth() / 2;
		if (reach <= cfg("smash_radius", 1, 8) - 0.5 && random.nextFloat() < 0.5f) {
			begin("smash", null);
			playSound(SoundEvents.PISTON_EXTEND, 1.2f, 0.5f);
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.5f);
		} else if (reach <= getBbWidth() / 2 + cfg("sweep_reach", 1, 8)) {
			begin("sweep", null);
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "smash" -> {
				if (target != null && age < 12) faceToward(target.position(), turnRate());
				if (age == SMASH_HIT) smash();
			}
			case "sweep" -> {
				if (target != null && age < SWEEP_HIT - 6) faceToward(target.position(), turnRate());
				if (age == SWEEP_HIT) {
					strikeArc(getBbWidth() / 2 + cfg("sweep_reach", 1, 8), 150, (float) cfg("sweep_damage", 0, 1000),
							cfg("sweep_knockback", 0, 5));
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
				}
			}
			default -> {
			}
		}
	}

	/** The hammer lands: a ground ring around the guardian; anyone in the air above it is spared. */
	private void smash() {
		double radius = cfg("smash_radius", 1, 8);
		AABB box = getBoundingBox().inflate(radius + 1, 1, radius + 1);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
			double dx = victim.getX() - getX(), dz = victim.getZ() - getZ();
			double lift = victim.getY() - getY();
			if (Math.sqrt(dx * dx + dz * dz) - victim.getBbWidth() / 2 <= radius && lift < 0.75 && lift > -1.5) {
				strike(victim, (float) cfg("smash_damage", 0, 1000), 0.6);
			}
		}
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.7f);
		playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
		if (level() instanceof ServerLevel server) {
			Vec3 forward = Vec3.directionFromRotation(0, getYRot());
			Vec3 impact = position().add(forward.scale(1.2));
			server.sendParticles(ParticleTypes.EXPLOSION, impact.x, getY() + 0.2, impact.z, 1, 0, 0, 0, 0);
			server.sendParticles(ParticleTypes.LAVA, impact.x, getY() + 0.2, impact.z, 5, 0.4, 0.1, 0.4, 0);
			for (int i = 0; i < 24; i++) {
				double a = i * Math.PI * 2 / 24;
				server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + Math.cos(a) * radius, getY() + 0.1,
						getZ() + Math.sin(a) * radius, 1, 0, 0.02, 0, 0.01);
			}
		}
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		String action = action();
		int age = actionAge();
		if ("smash".equals(action) && age > 6 && age < SMASH_HIT && tickCount % 2 == 0) {
			// Windup: embers trace the ring on the floor that the hammer will cover.
			double radius = BoneMonsterConfig.value("rusted_guardian", "smash_radius", 1, 8);
			int n = Math.max(8, (int) (24 * density));
			for (int i = 0; i < n; i++) {
				double a = i * Math.PI * 2 / n + tickCount * 0.02;
				level().addParticle(ParticleTypes.SMALL_FLAME, getX() + Math.cos(a) * radius, getY() + 0.1, getZ() + Math.sin(a) * radius, 0, 0.01, 0);
			}
		}
		if ("sweep".equals(action) && age < SWEEP_HIT && tickCount % 3 == 0) {
			Vec3 side = position().add(Vec3.directionFromRotation(0, yBodyRot + 70).scale(1.4));
			level().addParticle(ParticleTypes.SMOKE, side.x, getY() + 1.2, side.z, 0, 0.02, 0);
		}
		// The boiler on its back vents now and then.
		if (tickCount % 20 == 0 && random.nextDouble() < density) {
			Vec3 back = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(-0.6));
			level().addParticle(ParticleTypes.LARGE_SMOKE, back.x, getY() + 2.3, back.z, 0, 0.03, -0.01);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLASTFURNACE_FIRE_CRACKLE;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f);
	}

	@Override
	public float getVoicePitch() {
		return 0.6f;
	}
}
