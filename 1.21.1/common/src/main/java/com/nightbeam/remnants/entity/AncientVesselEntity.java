package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.BoneMonsterConfig;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ancient Vessel: a living Sanctum urn that shuffles after intruders and cannot be knocked back. While a target
 * is within 5 blocks it releases a soul pulse every 5 seconds: the lid lifts and a rose-violet ring gathers on the
 * floor for 14 ticks, then the ring bursts (6 damage and Slowness I for 40 ticks). Breaking it is dangerous too:
 * it swells for 18 ticks and bursts (8 damage within 3 blocks). No pulse or burst ever damages a block.
 */
public class AncientVesselEntity extends CorruptedRelicEntity {
	public static final int PULSE_RELEASE = 14, BURST_TICK = 18;

	public AncientVesselEntity(EntityType<? extends AncientVesselEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(30, 6, 6, 0.16).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	public String configId() {
		return "ancient_vessel";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.ANCIENT_VESSEL;
	}

	@Override
	protected void applyConfig() {
		setBase(Attributes.KNOCKBACK_RESISTANCE, cfg("knockback_resistance", 0, 1));
	}

	@Override
	protected double stopDistance() {
		return 2.5;
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		double dx = target.getX() - getX(), dz = target.getZ() - getZ();
		if (Math.sqrt(dx * dx + dz * dz) - target.getBbWidth() / 2 > cfg("pulse_radius", 1, 12)) return;
		begin("pulse", target.position());
		// The tell: the lid lifts with a rising hum.
		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 0.6f);
		playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.7f, 1.4f);
	}

	@Override
	protected void actionTick(String action, int age) {
		if ("pulse".equals(action) && age == PULSE_RELEASE) pulse();
	}

	/** The soul ring bursts: everything within the radius takes the hit, unless it stepped out in time. */
	private void pulse() {
		double radius = cfg("pulse_radius", 1, 12);
		int slow = (int) cfg("pulse_slow_ticks", 0, 600);
		AABB box = getBoundingBox().inflate(radius + 1, 2, radius + 1);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
			double dx = victim.getX() - getX(), dz = victim.getZ() - getZ();
			if (Math.sqrt(dx * dx + dz * dz) - victim.getBbWidth() / 2 > radius || Math.abs(victim.getY() - getY()) > 2.5) continue;
			if (strike(victim, (float) cfg("pulse_damage", 0, 100), 0.3) && slow > 0) {
				victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 0), this);
			}
		}
		playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2f, 1.3f);
		playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.7f);
		if (level() instanceof ServerLevel server) {
			for (double r = 1; r <= radius + 0.01; r += 1) {
				int n = (int) (8 * r);
				for (int i = 0; i < n; i++) {
					double a = i * Math.PI * 2 / n;
					server.sendParticles(CrystalBoltEntity.ROSE, getX() + Math.cos(a) * r, getY() + 0.3, getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
				}
			}
			server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.2, getZ(), 10, 0.4, 0.3, 0.4, 0.04);
		}
	}

	@Override
	protected void tickDeath() {
		super.tickDeath();
		if (!level().isClientSide && deathTime == BURST_TICK) unstableBurst();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		// The tell for the burst: the cracks hum while the urn swells.
		if (!level().isClientSide) playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 0.5f);
	}

	/** The broken urn bursts: explosion light and sound, damage in a small sphere, and never a block. */
	private void unstableBurst() {
		if (!(level() instanceof ServerLevel server)) return;
		double radius = cfg("burst_radius", 0.5, 8);
		float damage = (float) cfg("burst_damage", 0, 100);
		Vec3 centre = position().add(0, 0.6, 0);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius + 1), this::canHit)) {
			double reach = radius + victim.getBbWidth() / 2;
			if (victim.position().add(0, victim.getBbHeight() / 2, 0).distanceToSqr(centre) > reach * reach) continue;
			victim.hurt(damageSources().explosion(this, this), damage);
		}
		server.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 3, 0.6, 0.4, 0.6, 0);
		server.sendParticles(CrystalBoltEntity.ROSE, centre.x, centre.y, centre.z, 40, radius * 0.4, 0.5, radius * 0.4, 0);
		server.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y, centre.z, 8, 0.3, 0.3, 0.3, 0.12);
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.3f);
		playSound(SoundEvents.GLASS_BREAK, 1.0f, 0.7f);
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		if ("pulse".equals(action()) && actionAge() < PULSE_RELEASE && tickCount % 2 == 0) {
			// Windup: the ring the pulse will cover, drawn on the floor.
			double radius = BoneMonsterConfig.value("ancient_vessel", "pulse_radius", 1, 12);
			int n = Math.max(10, (int) (32 * density));
			for (int i = 0; i < n; i++) {
				double a = i * Math.PI * 2 / n - tickCount * 0.03;
				level().addParticle(CrystalBoltEntity.ROSE, getX() + Math.cos(a) * radius, getY() + 0.1, getZ() + Math.sin(a) * radius, 0, 0.01, 0);
			}
			level().addParticle(ParticleTypes.SOUL, getX(), getY() + 1.3, getZ(), 0, 0.03, 0);
		}
		if (tickCount % 12 == 0 && random.nextDouble() < density) {
			level().addParticle(CrystalBoltEntity.PALE_ROSE, getX() + random.nextGaussian() * 0.1, getY() + 1.15, getZ() + random.nextGaussian() * 0.1, 0, 0.02, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.DECORATED_POT_STEP;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DECORATED_POT_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DECORATED_POT_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.STONE_STEP, 0.3f, 1.4f);
	}

	@Override
	public float getVoicePitch() {
		return 0.8f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}
}
