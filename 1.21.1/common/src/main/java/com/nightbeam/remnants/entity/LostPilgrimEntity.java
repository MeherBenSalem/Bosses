package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Lost Pilgrim: a neutral wanderer with a lantern staff. When a creature hurts it, it
 * plays {@code disturbed}: its bell rings at tick 12 and every pilgrim within 12 blocks turns on the
 * attacker too. Then it strikes with the staff (4 damage and Weakness I for 60 ticks). It calms down 400
 * ticks after the last exchange of blows.
 */
public class LostPilgrimEntity extends ForgottenSoulEntity {
	public static final int BELL = 12, STRIKE_HIT = 13;
	private int angerTicks;
	private LivingEntity disturber;

	public LostPilgrimEntity(EntityType<? extends LostPilgrimEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(20, 4, 2, 0.2);
	}

	@Override
	public String configId() {
		return "lost_pilgrim";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.LOST_PILGRIM;
	}

	/** Neutral: it only fights back. */
	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	protected double stopDistance() {
		return 1.8;
	}

	@Override
	protected void serverTick() {
		LivingEntity target = getTarget();
		if (target == null) return;
		if (!target.isAlive() || --angerTicks <= 0) setTarget(null);
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (distance - target.getBbWidth() / 2 <= cfg("strike_reach", 1, 6)) {
			begin("strike", target.position());
			playSound(SoundEvents.CHAIN_HIT, 0.8f, 0.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		switch (action) {
			case "disturbed" -> {
				if (disturber != null && age < BELL) faceToward(disturber.position(), 20);
				if (age == BELL) ringBell();
			}
			case "strike" -> {
				LivingEntity target = getTarget();
				if (target != null && age < STRIKE_HIT - 3) faceToward(target.position(), 15);
				if (age == STRIKE_HIT) {
					int weakness = (int) cfg("weakness_ticks", 0, 1200);
					for (LivingEntity hit : HollowLegion.arc(this, cfg("strike_reach", 1, 6), 100, this::canHit)) {
						if (strike(hit, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.2) && weakness > 0) {
							hit.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness, 0), this);
						}
					}
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);
				}
			}
			default -> {
			}
		}
	}

	/** The bell: every pilgrim within 12 blocks turns on whoever disturbed this one. */
	private void ringBell() {
		playSound(SoundEvents.BELL_BLOCK, 1.2f, 1.6f);
		playSound(SoundEvents.BELL_RESONATE, 0.8f, 1.4f);
		if (disturber == null || !disturber.isAlive()) return;
		double radius = cfg("alert_radius", 1, 32);
		int anger = (int) cfg("anger_ticks", 20, 6000);
		for (LostPilgrimEntity pilgrim : level().getEntitiesOfClass(LostPilgrimEntity.class, getBoundingBox().inflate(radius),
				p -> p != this && p.isAlive() && p.distanceToSqr(this) <= radius * radius)) {
			pilgrim.setTarget(disturber);
			pilgrim.angerTicks = anger;
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		Entity attacker = source.getEntity();
		if (hurt && !level().isClientSide && isAlive() && attacker instanceof LivingEntity living && living != this && canHit(living)) {
			boolean calm = getTarget() == null;
			angerTicks = (int) cfg("anger_ticks", 20, 6000);
			setTarget(living);
			if (calm && action().isEmpty()) {
				disturber = living;
				begin("disturbed", living.position());
			}
		}
		return hurt;
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		angerTicks = (int) cfg("anger_ticks", 20, 6000);
		return super.doHurtTarget(target);
	}

	@Override
	protected void clientTick() {
		if (tickCount % 12 != 0 || SovereignConfig.visualDensity() <= 0) return;
		// The lantern on the staff: a faint soul flame.
		Vec3 side = Vec3.directionFromRotation(0, yBodyRot - 90).scale(0.35);
		Vec3 lantern = position().add(side).add(Vec3.directionFromRotation(0, yBodyRot).scale(0.2)).add(0, 1.75, 0);
		level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, lantern.x, lantern.y, lantern.z, 0, 0.005, 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SOUL_ESCAPE.value();
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
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.WOOL_STEP, 0.2f, 0.7f);
	}

	@Override
	public float getVoicePitch() {
		return 0.6f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}
}
