package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Memory Wraith: an evasive flyer two blocks above the ground. It holds 6-12 blocks
 * from its target and casts soul bolts ({@code memory_bolt}, released at tick 14 of {@code cast}). When hurt
 * (50%) or when a player comes within 3 blocks it blinks 4-7 blocks sideways (at tick 6 of {@code blink}).
 */
public class MemoryWraithEntity extends FloatingSoulEntity {
	public static final int CAST_RELEASE = 14, BLINK_MOVE = 6;
	private static final DustParticleOptions SPIRIT = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.64f, 0.77f, 0.84f), 1.0f);
	private int blinkCooldown;

	public MemoryWraithEntity(EntityType<? extends MemoryWraithEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return floatingAttributes(26, 6, 0, 0.3);
	}

	@Override
	public String configId() {
		return "memory_wraith";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.MEMORY_WRAITH;
	}

	@Override
	protected double hoverHeight() {
		return 2.0;
	}

	@Override
	protected double[] holdRange() {
		return new double[] {cfg("hold_min_distance", 1, 32), cfg("hold_max_distance", 2, 48)};
	}

	@Override
	protected String movementClip(boolean moving) {
		return moving ? "drift" : "idle";
	}

	@Override
	protected void serverTick() {
		super.serverTick();
		if (blinkCooldown > 0) blinkCooldown--;
		if (blinkCooldown <= 0 && action().isEmpty() && tickCount % 5 == 0) {
			Player near = level().getNearestPlayer(this, cfg("blink_trigger_distance", 0, 16));
			if (near != null && canHit(near)) startBlink();
		}
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (distance <= cfg("bolt_range", 4, 64) - 4) {
			begin("cast", target.position());
			playSound(SoundEvents.VEX_CHARGE, 1.0f, 0.6f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "cast" -> {
				if (target != null && age < CAST_RELEASE) faceToward(target.position(), 20);
				if (age == CAST_RELEASE && target != null && level() instanceof ServerLevel server) {
					Vec3 hand = handPosition();
					Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0);
					MemoryBoltEntity.fire(server, this, hand, aim.subtract(hand), cfg("bolt_speed", 0.1, 3),
							(float) cfg("bolt_damage", 0, 1000), cfg("bolt_range", 4, 64));
					playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.6f);
				}
			}
			case "blink" -> {
				if (age == BLINK_MOVE) blinkAside();
			}
			default -> {
			}
		}
	}

	/** The casting hand: the right sleeve, in front of the chest. */
	private Vec3 handPosition() {
		Vec3 forward = Vec3.directionFromRotation(0, getYRot());
		Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
		return position().add(forward.scale(0.5)).add(right.scale(-0.3)).add(0, 1.3, 0);
	}

	private void startBlink() {
		cancelAction();
		begin("blink", null);
		blinkCooldown = (int) cfg("blink_cooldown_ticks", 0, 1200);
		playSound(SoundEvents.ALLAY_ITEM_TAKEN, 1.0f, 0.5f);
	}

	/** Slip 4-7 blocks sideways (left or right of the facing) to a free spot. */
	private void blinkAside() {
		if (!(level() instanceof ServerLevel server)) return;
		Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
		for (int tries = 0; tries < 10; tries++) {
			double side = (random.nextBoolean() ? 1 : -1) * (4 + random.nextDouble() * 3);
			Vec3 spot = position().add(right.scale(side)).add(Vec3.directionFromRotation(0, getYRot()).scale(random.nextGaussian()));
			spot = new Vec3(spot.x, groundBelow(spot.add(0, 2, 0)) + hoverHeight(), spot.z);
			if (!level().noCollision(this, getBoundingBox().move(spot.subtract(position())))) continue;
			Vec3 from = position().add(0, getBbHeight() / 2, 0);
			server.sendParticles(SPIRIT, from.x, from.y, from.z, 16, 0.3, 0.6, 0.3, 0);
			snapTo(spot.x, spot.y, spot.z, getYRot(), getXRot());
			getNavigation().stop();
			Vec3 to = position().add(0, getBbHeight() / 2, 0);
			server.sendParticles(ParticleTypes.SOUL, to.x, to.y, to.z, 8, 0.3, 0.6, 0.3, 0.02);
			playSound(SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.5f);
			return;
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && !level().isClientSide() && isAlive() && blinkCooldown <= 0 && !"blink".equals(action())
				&& random.nextDouble() < cfg("blink_chance", 0, 1)) {
			startBlink();
		}
		return hurt;
	}

	@Override
	protected void clientTick() {
		if (tickCount % 4 != 0 || SovereignConfig.visualDensity() <= 0) return;
		// The robe thins into wisps below the body.
		level().addParticle(SPIRIT, getX() + random.nextGaussian() * 0.2, getY() + 0.2, getZ() + random.nextGaussian() * 0.2, 0, -0.02, 0);
		if ("cast".equals(action()) && actionAge() < CAST_RELEASE) {
			Vec3 hand = handPosition();
			level().addParticle(ParticleTypes.END_ROD, hand.x, hand.y, hand.z, 0, 0.01, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.VEX_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VEX_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VEX_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.55f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("BlinkCooldown", blinkCooldown);
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		blinkCooldown = tag.getIntOr("BlinkCooldown", 0);
	}
}
