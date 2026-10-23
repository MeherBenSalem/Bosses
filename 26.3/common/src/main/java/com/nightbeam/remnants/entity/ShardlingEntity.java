package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shardling: a fragile cluster of Oracle crystal that floats 1.6 blocks up and keeps 6-12 blocks from its target.
 * It charges for 4 ticks (a chime and gathering motes), then plays {@code shoot} and releases a crystal bolt at
 * its tick 8: a 12-tick tell for 5 damage. Shardlings come in groups and take turns: when one starts charging,
 * the others within 12 blocks hold their shots for 16 ticks.
 */
public class ShardlingEntity extends HoveringRemnantEntity {
	public static final int CHARGE_TICKS = 4, SHOT_RELEASE = 8;
	private int charge, shotHold;

	public ShardlingEntity(EntityType<? extends ShardlingEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return hoveringAttributes(8, 5, 0, 0.28);
	}

	@Override
	public String configId() {
		return "shardling";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.SHARDLING;
	}

	@Override
	protected double hoverHeight() {
		return 1.6;
	}

	@Override
	protected double[] holdRange() {
		return new double[] {cfg("hold_min_distance", 1, 32), cfg("hold_max_distance", 2, 48)};
	}

	/** A floating crystal has no walk: drifting plays idle. */
	@Override
	protected String movementClip(boolean moving) {
		return "idle";
	}

	@Override
	protected boolean familyVictim(LivingEntity entity) {
		return CorruptedRelics.victim(entity);
	}

	@Override
	protected boolean familyMember(Entity entity) {
		return CorruptedRelics.isRelic(entity);
	}

	/** The crystal's heart, where bolts form. */
	private Vec3 core() {
		return position().add(0, 0.75, 0);
	}

	@Override
	protected void serverTick() {
		super.serverTick();
		if (shotHold > 0) shotHold--;
		if (charge <= 0) return;
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !action().isEmpty()) {
			charge = 0;
			return;
		}
		getLookControl().setLookAt(target, 30, 30);
		if (level() instanceof ServerLevel server) {
			Vec3 core = core();
			server.sendParticles(CrystalBoltEntity.ROSE, core.x, core.y, core.z, 3, 0.35, 0.35, 0.35, 0);
		}
		if (--charge == 0) begin("shoot", target.position());
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (charge > 0 || shotHold > 0 || distance > cfg("bolt_range", 4, 64) - 2) return;
		charge = CHARGE_TICKS;
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
		// Groups take turns: the others wait while this one fires.
		double radius = cfg("group_radius", 0, 32);
		int stagger = (int) cfg("group_stagger_ticks", 0, 200);
		for (ShardlingEntity other : level().getEntitiesOfClass(ShardlingEntity.class, getBoundingBox().inflate(radius),
				s -> s != this && s.isAlive() && s.distanceToSqr(this) <= radius * radius)) {
			other.shotHold = Math.max(other.shotHold, stagger);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		if (!"shoot".equals(action)) return;
		LivingEntity target = getTarget();
		if (target != null && age < SHOT_RELEASE) faceToward(target.position(), 30);
		if (age == SHOT_RELEASE && target != null && level() instanceof ServerLevel server) {
			Vec3 core = core().add(Vec3.directionFromRotation(0, getYRot()).scale(0.3));
			Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0);
			CrystalBoltEntity.fire(server, this, core, aim.subtract(core), cfg("bolt_speed", 0.1, 3), (float) cfg("bolt_damage", 0, 100),
					cfg("bolt_range", 4, 64));
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.8f);
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel server) {
			Vec3 core = core();
			server.sendParticles(CrystalBoltEntity.ROSE, core.x, core.y, core.z, 16, 0.3, 0.3, 0.3, 0);
			server.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z, 4, 0.2, 0.2, 0.2, 0.05);
		}
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || tickCount % 10 != 0 || random.nextDouble() > density) return;
		level().addParticle(CrystalBoltEntity.PALE_ROSE, getX() + random.nextGaussian() * 0.3, getY() + 0.75 + random.nextGaussian() * 0.2,
				getZ() + random.nextGaussian() * 0.3, 0, 0.01, 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_CLUSTER_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
	}

	@Override
	public float getVoicePitch() {
		return 1.3f + (random.nextFloat() - random.nextFloat()) * 0.1f;
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		charge = 0;
		shotHold = 0;
	}
}
