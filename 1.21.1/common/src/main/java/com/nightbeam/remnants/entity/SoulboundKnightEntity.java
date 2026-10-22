package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Elite of the Hollow Legion: a two-hit combo, a locked-target lunge from mid range and a 25% parry
 * stance that negates melee for 14 ticks before answering with a cut.
 */
public class SoulboundKnightEntity extends LegionEntity {
	private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(SoulboundKnightEntity.class, EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(new Vector3f(0.31f, 0.79f, 0.76f), 1.1f);
	private Vec3 lungeStep = Vec3.ZERO;

	public SoulboundKnightEntity(EntityType<? extends SoulboundKnightEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(60, 10, 12, 0.27).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.ARMOR_TOUGHNESS, 2);
	}

	@Override
	public String configId() {
		return "soulbound_knight";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.SOULBOUND_KNIGHT;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(RUNNING, false);
	}

	@Override
	protected void applyConfig() {
		setBase(Attributes.KNOCKBACK_RESISTANCE, cfg("knockback_resistance", 0, 1));
	}

	@Override
	protected double stopDistance() {
		return 2.6;
	}

	@Override
	protected void moveTick(LivingEntity target, double distance) {
		getLookControl().setLookAt(target, 30, 30);
		boolean run = distance > 6;
		entityData.set(RUNNING, run);
		if (distance > stopDistance() || !hasLineOfSight(target)) getNavigation().moveTo(target, run ? 1.3 : 1.0);
		else getNavigation().stop();
	}

	@Override
	protected String movementClip(boolean moving) {
		if (moving && entityData.get(RUNNING)) return "run";
		return super.movementClip(moving);
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		double reach = distance - target.getBbWidth() / 2;
		if (distance >= 5 && distance <= 10 && onGround()) {
			begin("lunge", target.position());
			yawLocked = false;
			playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.0f, 0.6f);
		} else if (reach <= 3.5) {
			begin("combo", target.position());
			playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0f, 0.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "combo" -> {
				if (target != null && (age < 10 || (age > 14 && age < 22))) faceToward(target.position(), 12);
				if (age == 12 || age == 24) {
					struck.clear();
					strikeArc(3.5, 110, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.4);
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, age == 12 ? 0.8f : 0.65f);
				}
			}
			case "lunge" -> {
				if (age < 14 && target != null) faceToward(target.position(), 15);
				if (age == 14) {
					yawLocked = true;
					lockedYaw = getYRot();
					Vec3 goal = target == null ? position().add(Vec3.directionFromRotation(0, getYRot()).scale(6)) : target.position();
					Vec3 path = goal.subtract(position()).multiply(1, 0, 1);
					double length = Math.min(9, Math.max(0, path.length() - 1.0));
					lungeStep = path.lengthSqr() < 1.0E-4 ? Vec3.ZERO : path.normalize().scale(length / 6.0);
					playSound(ModSounds.SPECTRAL_SLASH.get(), 0.8f, 1.2f);
				}
				if (age > 14 && age <= 20) {
					move(MoverType.SELF, lungeStep);
					if (horizontalCollision) lungeStep = Vec3.ZERO;
				}
				if (age == 18) {
					AABB box = getBoundingBox().inflate(1.8, 0.5, 1.8);
					for (LivingEntity hit : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
						if (hit.distanceToSqr(this) <= 2.6 * 2.6) strike(hit, (float) cfg("lunge_damage", 0, 100), 0.6);
					}
				}
			}
			case "counter" -> {
				if (age < 14 && target != null) faceToward(target.position(), 20);
				if (age == 18) {
					strikeArc(3.0, 120, (float) cfg("counter_damage", 0, 100), 0.5);
					playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.6f);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide || source.getEntity() == null || source.getEntity() == this || isFriend(source.getEntity()) || !isMelee(source)) {
			return super.hurt(source, amount);
		}
		boolean stance = "counter".equals(action()) && actionAge() <= 14;
		if (!stance && action().isEmpty() && isAlive() && random.nextDouble() < cfg("counter_chance", 0, 1)) {
			begin("counter", source.getEntity().position());
			stance = true;
		}
		if (stance) {
			playSound(ModSounds.SOVEREIGN_GUARD.get(), 1.0f, 1.25f);
			if (level() instanceof ServerLevel server) {
				Vec3 p = getEyePosition();
				server.sendParticles(ParticleTypes.CRIT, p.x, p.y - 0.4, p.z, 8, 0.25, 0.25, 0.25, 0.2);
			}
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		String action = action();
		int age = actionAge();
		if ("lunge".equals(action) && age >= 13 && age <= 20) {
			for (int i = 0; i < Math.max(1, (int) (4 * density)); i++) {
				level().addParticle(i % 2 == 0 ? SOUL_LIGHT : ParticleTypes.SOUL, getRandomX(0.6), getY() + random.nextDouble() * 2.2, getRandomZ(0.6), 0, 0.01, 0);
			}
		} else if ("counter".equals(action) && age <= 14 && tickCount % 2 == 0) {
			Vec3 front = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(0.8)).add(0, 1.4, 0);
			level().addParticle(SOUL_LIGHT, front.x, front.y, front.z, 0, 0.02, 0);
		} else if (tickCount % 12 == 0 && random.nextDouble() < density) {
			level().addParticle(ParticleTypes.SOUL, getX(), getY() + 1.6, getZ(), 0, 0.02, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_SKELETON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.7f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}
}
