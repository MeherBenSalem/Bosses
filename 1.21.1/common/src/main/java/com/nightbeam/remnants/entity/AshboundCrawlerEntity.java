package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ashbound Crawler: a fast furnace bug, the Ashbound Constructs' fodder. It closes in and rears up to
 * {@code scorch}: a short flame-breath cone (3 blocks, 60 degrees) that burns for 3 seconds. When it dies its
 * furnace swells and bursts in a small ring of embers. It never lights a block.
 */
public class AshboundCrawlerEntity extends AshboundEntity {
	/** Scorch: rears up 0-8 (the tell), flame 8-18, damage window 10-18 (each target once), recovery to 24. */
	public static final int SCORCH_REAR = 8, SCORCH_FLAME_START = 10, SCORCH_FLAME_END = 18;
	/** Death: the furnace swells until tick 16, then bursts. */
	public static final int BURST_TICK = 16;

	public AshboundCrawlerEntity(EntityType<? extends AshboundCrawlerEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(16, 3, 4, 0.33);
	}

	@Override
	public String configId() {
		return "ashbound_crawler";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.ASHBOUND_CRAWLER;
	}

	@Override
	protected double stopDistance() {
		return 1.8;
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		double reach = distance - target.getBbWidth() / 2 - getBbWidth() / 2;
		if (reach <= cfg("scorch_range", 1, 8) - 0.8) {
			begin("scorch", target.position());
			// The tell: the furnace draws breath with a hiss.
			playSound(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		if (!"scorch".equals(action)) return;
		LivingEntity target = getTarget();
		if (target != null && age < SCORCH_REAR) faceToward(target.position(), 20);
		if (age == SCORCH_REAR) playSound(SoundEvents.BLAZE_SHOOT, 0.9f, 1.3f);
		if (age >= SCORCH_REAR && age <= SCORCH_FLAME_END) breath();
		if (age >= SCORCH_FLAME_START && age <= SCORCH_FLAME_END) {
			int burn = (int) cfg("scorch_burn_ticks", 0, 600);
			float damage = (float) cfg("scorch_damage", 0, 100);
			for (LivingEntity hit : HollowLegion.arc(this, cfg("scorch_range", 1, 8), cfg("scorch_arc_degrees", 10, 180), this::canHit)) {
				if (strike(hit, damage, 0.1)) Ashbound.ignite(hit, burn);
			}
		}
	}

	/** Where the flame leaves the snout while it rears. */
	private Vec3 mouth() {
		return position().add(Vec3.directionFromRotation(0, getYRot()).scale(0.55)).add(0, 0.45, 0);
	}

	/** The flame cone, drawn by the server so every client sees the same reach. */
	private void breath() {
		if (!(level() instanceof ServerLevel server)) return;
		Vec3 forward = Vec3.directionFromRotation(0, getYRot());
		Vec3 mouth = mouth();
		double range = cfg("scorch_range", 1, 8);
		for (double d = 0.4; d <= range; d += 0.6) {
			Vec3 p = mouth.add(forward.scale(d));
			double spread = 0.08 + d * 0.22;
			server.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, spread, spread * 0.5, spread, 0.01);
		}
		Vec3 end = mouth.add(forward.scale(range));
		server.sendParticles(ParticleTypes.SMOKE, end.x, end.y + 0.2, end.z, 2, 0.4, 0.2, 0.4, 0.01);
	}

	@Override
	protected void tickDeath() {
		super.tickDeath();
		if (!level().isClientSide && deathTime == BURST_TICK) emberBurst();
	}

	/** The swollen furnace bursts: embers in a small ring that burn, but never a block. */
	private void emberBurst() {
		if (!(level() instanceof ServerLevel server)) return;
		double radius = cfg("burst_radius", 0.5, 8);
		float damage = (float) cfg("burst_damage", 0, 100);
		int burn = (int) cfg("burst_burn_ticks", 0, 600);
		Vec3 centre = position().add(0, 0.4, 0);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius + 1), this::canHit)) {
			double reach = radius + victim.getBbWidth() / 2;
			if (victim.position().add(0, victim.getBbHeight() / 2, 0).distanceToSqr(centre) > reach * reach) continue;
			if (victim.hurt(damageSources().explosion(this, this), damage)) Ashbound.ignite(victim, burn);
		}
		server.sendParticles(ParticleTypes.FLAME, centre.x, centre.y, centre.z, 36, radius * 0.35, 0.3, radius * 0.35, 0.08);
		server.sendParticles(ParticleTypes.LAVA, centre.x, centre.y, centre.z, 6, 0.3, 0.2, 0.3, 0);
		server.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 0.3, centre.z, 10, 0.5, 0.3, 0.5, 0.02);
		server.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.6f);
		playSound(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.6f);
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		// The chimney on its back smokes; it glows hotter while it rears to breathe.
		if (tickCount % 8 == 0 && random.nextDouble() < density) {
			Vec3 back = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(-0.2));
			level().addParticle(ParticleTypes.SMOKE, back.x, getY() + 0.95, back.z, 0, 0.03, 0);
		}
		if ("scorch".equals(action()) && actionAge() < SCORCH_REAR && tickCount % 2 == 0) {
			Vec3 mouth = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(0.55)).add(0, 0.4, 0);
			level().addParticle(ParticleTypes.SMALL_FLAME, mouth.x, mouth.y, mouth.z, 0, 0.01, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLAZE_BURN;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COPPER_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLAZE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.METAL_STEP, 0.25f, 1.6f);
	}

	@Override
	public float getVoicePitch() {
		return 1.5f + (random.nextFloat() - random.nextFloat()) * 0.1f;
	}
}
