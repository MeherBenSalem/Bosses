package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Banner carrier of the Hollow Legion. Keeps 4 to 8 blocks behind the nearest ally, refreshes a
 * Resistance and Speed aura every 40 ticks and, when struck, plants the banner to rally the line.
 */
public class FallenStandardBearerEntity extends LegionEntity {
	private int auraTimer = 20, rallyCooldown;
	private boolean wantsRally;

	public FallenStandardBearerEntity(EntityType<? extends FallenStandardBearerEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(34, 6, 6, 0.24);
	}

	@Override
	public String configId() {
		return "fallen_standard_bearer";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.FALLEN_STANDARD_BEARER;
	}

	private double auraRadius() {
		return cfg("aura_radius", 1, 32);
	}

	private List<LivingEntity> allies(double radius) {
		return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
				e -> e != this && e.isAlive() && HollowLegion.isLegion(e) && e.distanceToSqr(this) <= radius * radius);
	}

	@Override
	protected void serverTick() {
		if (rallyCooldown > 0) rallyCooldown--;
		if (--auraTimer <= 0) {
			auraTimer = 40;
			List<LivingEntity> allies = allies(auraRadius());
			for (LivingEntity ally : allies) {
				ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true), this);
				ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, true), this);
			}
			if (!allies.isEmpty()) level().broadcastEntityEvent(this, EVENT_AURA);
		}
		if (wantsRally && action().isEmpty() && rallyCooldown <= 0) {
			wantsRally = false;
			rallyCooldown = (int) cfg("rally_cooldown_ticks", 20, 6000);
			begin("rally", getTarget() == null ? null : getTarget().position());
			playSound(ModSounds.LEGION_HORN.get(), 2.0f, 1.0f);
		}
	}

	@Override
	protected void moveTick(LivingEntity target, double distance) {
		getLookControl().setLookAt(target, 30, 30);
		LivingEntity ally = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity candidate : allies(16)) {
			if (candidate instanceof FallenStandardBearerEntity) continue;
			double d = candidate.distanceToSqr(this);
			if (d < best) {
				best = d;
				ally = candidate;
			}
		}
		if (ally == null || distance < 3.5) {
			if (distance > 2.2 || !hasLineOfSight(target)) getNavigation().moveTo(target, 1.0);
			else getNavigation().stop();
			return;
		}
		Vec3 back = ally.position().subtract(target.position()).multiply(1, 0, 1);
		if (back.lengthSqr() < 1.0E-4) back = Vec3.directionFromRotation(0, ally.getYRot()).reverse();
		Vec3 post = ally.position().add(back.normalize().scale(6));
		double offset = post.distanceTo(position());
		double behind = ally.distanceTo(this);
		if (offset > 2 || behind < 4 || behind > 8) getNavigation().moveTo(post.x, post.y, post.z, 1.0);
		else getNavigation().stop();
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (distance - target.getBbWidth() / 2 <= 3.0) {
			begin("sweep", target.position());
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 0.9f, 0.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		switch (action) {
			case "sweep" -> {
				if (age == 12) {
					strikeArc(3.0, 120, (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), 0.6);
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
				}
			}
			case "rally" -> {
				if (age == 30) {
					double radius = auraRadius();
					for (LivingEntity ally : allies(radius)) {
						ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true), this);
					}
					addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true), this);
					if (level() instanceof ServerLevel server) {
						server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 30, radius * 0.3, 0.1, radius * 0.3, 0.02);
					}
					level().broadcastEntityEvent(this, EVENT_AURA);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		Entity attacker = source.getEntity();
		if (hurt && !level().isClientSide && attacker instanceof LivingEntity && !isFriend(attacker) && rallyCooldown <= 0) {
			wantsRally = true;
		}
		return hurt;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_AURA) {
			drawLinks();
			return;
		}
		super.handleEntityEvent(id);
	}

	private void drawLinks() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		Vec3 banner = position().add(0, 2.6, 0);
		for (LivingEntity ally : allies(cfg("aura_radius", 1, 32))) {
			Vec3 to = ally.position().add(0, ally.getBbHeight() * 0.6, 0);
			int steps = Math.max(3, (int) (banner.distanceTo(to) * 2 * density));
			for (int i = 0; i <= steps; i++) {
				Vec3 p = banner.lerp(to, i / (double) steps);
				level().addParticle(i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, p.x, p.y, p.z, 0, 0.005, 0);
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.65f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}
}
