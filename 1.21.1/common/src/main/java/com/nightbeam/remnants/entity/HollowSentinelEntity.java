package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Tower-shield guard of the Hollow Legion. While it is not attacking, the shield takes 60% off melee
 * hits and stops projectiles from the front 140 degrees; an axe knocks the shield aside for 100 ticks.
 */
public class HollowSentinelEntity extends LegionEntity {
	private static final EntityDataAccessor<Boolean> GUARDING = SynchedEntityData.defineId(HollowSentinelEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double SHIELD_HALF_ANGLE = 70;
	private int shieldDisabled;

	public HollowSentinelEntity(EntityType<? extends HollowSentinelEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(30, 7, 8, 0.23).add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	public String configId() {
		return "hollow_sentinel";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.HOLLOW_SENTINEL;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(GUARDING, false);
	}

	public boolean shieldUp() {
		return shieldDisabled <= 0 && action().isEmpty();
	}

	@Override
	protected double stopDistance() {
		return 2.2;
	}

	@Override
	protected void serverTick() {
		if (shieldDisabled > 0) shieldDisabled--;
		LivingEntity target = getTarget();
		entityData.set(GUARDING, shieldUp() && target != null && distanceTo(target) < 7);
	}

	@Override
	protected String movementClip(boolean moving) {
		if (!moving && entityData.get(GUARDING)) return "block";
		return super.movementClip(moving);
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		double reach = distance - target.getBbWidth() / 2;
		if (reach <= 2.5 && random.nextFloat() < 0.45f) {
			begin("bash", target.position());
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0f, 0.6f);
		} else if (reach <= 3.0) {
			begin("strike", target.position());
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 0.9f, 0.6f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		switch (action) {
			case "bash" -> {
				if (age == 10) {
					strikeArc(2.5, 100, (float) cfg("bash_damage", 0, 100), 1.0);
					playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.7f);
				}
			}
			case "strike" -> {
				if (age < 10 && getTarget() != null) faceToward(getTarget().position(), 6);
				if (age == 18) {
					strikeArc(3.0, 90, (float) cfg("strike_damage", 0, 100), 0.4);
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide || source.getEntity() != null && source.getEntity() != this && isFriend(source.getEntity())) {
			return super.hurt(source, amount);
		}
		if (shieldUp() && !source.is(DamageTypeTags.BYPASSES_SHIELD) && fromFront(source, SHIELD_HALF_ANGLE)) {
			if (source.is(DamageTypeTags.IS_PROJECTILE)) {
				shieldSpark(source);
				playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.75f);
				return false;
			}
			if (isMelee(source) && source.getEntity() instanceof LivingEntity attacker) {
				if (attacker.getMainHandItem().is(ItemTags.AXES)) {
					shieldDisabled = (int) cfg("shield_disable_ticks", 0, 1200);
					playSound(SoundEvents.SHIELD_BREAK, 1.0f, 0.7f);
					return super.hurt(source, amount);
				}
				shieldSpark(source);
				playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.75f);
				return super.hurt(source, amount * (float) (1 - cfg("shield_reduction", 0, 1)));
			}
		}
		return super.hurt(source, amount);
	}

	private void shieldSpark(DamageSource source) {
		if (!(level() instanceof ServerLevel server) || SovereignConfig.visualDensity() <= 0) return;
		Vec3 from = source.getSourcePosition();
		Vec3 front = from == null ? getEyePosition() : position().add(from.subtract(position()).multiply(1, 0, 1).normalize().scale(0.6)).add(0, 1.3, 0);
		server.sendParticles(ParticleTypes.CRIT, front.x, front.y, front.z, 6, 0.15, 0.25, 0.15, 0.15);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
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
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.IRON_GOLEM_STEP, 0.35f, 0.8f);
	}

	@Override
	public float getVoicePitch() {
		return 0.6f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}
}
