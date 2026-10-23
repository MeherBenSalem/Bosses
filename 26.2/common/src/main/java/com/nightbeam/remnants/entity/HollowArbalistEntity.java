package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Crossbowman of the Hollow Legion: keeps 8 to 14 blocks, telegraphs every bolt with a 16-tick aim,
 * reloads for 40 ticks and kicks away anyone who corners it.
 */
public class HollowArbalistEntity extends LegionEntity {
	private static final float BOLT_SPEED = 2.4f;

	public HollowArbalistEntity(EntityType<? extends HollowArbalistEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(24, 3, 4, 0.25).add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	public String configId() {
		return "hollow_arbalist";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.HOLLOW_ARBALIST;
	}

	@Override
	protected void moveTick(LivingEntity target, double distance) {
		getLookControl().setLookAt(target, 30, 30);
		boolean sees = hasLineOfSight(target);
		if (distance < cfg("retreat_distance", 0, 32)) {
			if (getNavigation().isDone()) {
				Vec3 away = DefaultRandomPos.getPosAway(this, 8, 4, target.position());
				if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.2);
			}
		} else if (!sees || distance > cfg("hold_max_distance", 2, 48)) {
			getNavigation().moveTo(target, 1.0);
		} else if (distance >= cfg("hold_min_distance", 0, 48)) {
			getNavigation().stop();
		}
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (distance - target.getBbWidth() / 2 <= 2.5) {
			begin("kick", target.position());
			playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0f, 0.7f);
		} else if (distance >= 3.5 && distance <= cfg("hold_max_distance", 2, 48) + 6) {
			begin("aim", target.position());
			yawLocked = false;
			playSound(SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 1.0f, 0.8f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		switch (action) {
			case "aim" -> {
				LivingEntity target = getTarget();
				if (target != null) {
					faceToward(target.position(), 20);
					getLookControl().setLookAt(target, 30, 30);
				}
			}
			case "kick" -> {
				if (age == 8) {
					strikeArc(2.5, 110, (float) cfg("kick_damage", 0, 100), 1.2);
					playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.7f);
				}
			}
			default -> {
			}
		}
	}

	@Override
	protected boolean onActionEnd(String action) {
		switch (action) {
			case "aim" -> {
				fireBolt();
				begin("fire", null);
				return true;
			}
			case "fire" -> {
				begin("reload", null);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private void fireBolt() {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !hasLineOfSight(target)) return;
		Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		arrow.setBaseDamage(cfg("bolt_damage", 0, 100) / BOLT_SPEED);
		double dx = target.getX() - getX();
		double dy = target.getY(0.4) - arrow.getY();
		double dz = target.getZ() - getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		arrow.shoot(dx, dy + horizontal * 0.04, dz, BOLT_SPEED, 1.0f);
		level().addFreshEntity(arrow);
		playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.75f);
	}

	@Override
	protected void clientTick() {
		if (!"aim".equals(action()) || SovereignConfig.visualDensity() <= 0 || tickCount % 3 != 0) return;
		Vec3 tip = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(0.7)).add(0, 1.55, 0);
		level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, tip.x, tip.y, tip.z, 0, 0.01, 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.75f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}
}
