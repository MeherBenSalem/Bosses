package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Gravebound Soldier: a rusted guisarme guard that fights in groups. Soldiers
 * within 8 blocks share a target; the thrust reaches 3.2 blocks and the guisarme sweep hooks and pulls
 * its victims 1 block in. The Warden raises them with the {@code rise} clip: raised soldiers belong to
 * it, fall apart when it is gone, drop nothing and are never saved.
 */
public class GraveboundSoldierEntity extends CryptbornEntity {
	public static final int THRUST_HIT = 10, SWEEP_HIT = 13;
	private UUID ownerId;
	private boolean raised;

	public GraveboundSoldierEntity(EntityType<? extends GraveboundSoldierEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(24, 5, 5, 0.26);
	}

	/** A soldier rising out of the floor for {@code owner} (the Warden, or Ossukage's altar ritual when null). */
	public static GraveboundSoldierEntity raise(ServerLevel level, Mob owner, Vec3 at) {
		GraveboundSoldierEntity soldier = ModEntities.GRAVEBOUND_SOLDIER.get().create(level);
		if (soldier == null) return null;
		soldier.raised = true;
		float yaw = owner == null ? level.random.nextFloat() * 360 : owner.getYRot();
		if (owner instanceof GraveboundWardenEntity) soldier.ownerId = owner.getUUID();
		soldier.moveTo(at.x, at.y, at.z, yaw, 0);
		soldier.setYBodyRot(yaw);
		soldier.setYHeadRot(yaw);
		soldier.setPersistenceRequired();
		soldier.begin("rise", null);
		if (owner != null && owner.getTarget() != null && soldier.canHit(owner.getTarget())) soldier.setTarget(owner.getTarget());
		level.addFreshEntity(soldier);
		level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.2, at.z, 14, 0.4, 0.1, 0.4, 0.03);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SKELETON_AMBIENT, soldier.getSoundSource(), 1.0f, 0.6f);
		return soldier;
	}

	@Override
	public String configId() {
		return "gravebound_soldier";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.GRAVEBOUND_SOLDIER;
	}

	public UUID ownerId() {
		return ownerId;
	}

	public boolean isRaised() {
		return raised;
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected double stopDistance() {
		return 2.4;
	}

	@Override
	protected void serverTick() {
		if (tickCount % 10 != 0) return;
		if (ownerId != null) {
			if (!(level() instanceof ServerLevel server) || !(server.getEntity(ownerId) instanceof GraveboundWardenEntity warden) || !warden.isAlive()) {
				crumble();
				return;
			}
			LivingEntity wanted = warden.getTarget();
			if (getTarget() == null && wanted != null && wanted.isAlive() && canHit(wanted)) setTarget(wanted);
		}
		// Group fighters: a soldier's quarry becomes the quarry of every idle soldier nearby.
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		double radius = cfg("share_radius", 0, 32);
		for (GraveboundSoldierEntity other : level().getEntitiesOfClass(GraveboundSoldierEntity.class, getBoundingBox().inflate(radius),
				s -> s != this && s.isAlive() && s.getTarget() == null && s.distanceToSqr(this) <= radius * radius)) {
			if (other.canHit(target)) other.setTarget(target);
		}
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		double reach = distance - target.getBbWidth() / 2;
		if (reach <= 2.6 && random.nextFloat() < 0.4f) {
			begin("sweep", target.position());
			playSound(SoundEvents.CHAIN_HIT, 0.8f, 1.1f);
		} else if (reach <= cfg("thrust_reach", 1, 8)) {
			begin("thrust", target.position());
			playSound(SoundEvents.SKELETON_AMBIENT, 0.6f, 0.7f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "thrust" -> {
				if (target != null && age < THRUST_HIT - 3) faceToward(target.position(), 10);
				if (age == THRUST_HIT) {
					Vec3 forward = Vec3.directionFromRotation(0, getYRot());
					double length = getBbWidth() / 2 + cfg("thrust_reach", 1, 8);
					for (LivingEntity hit : HollowLegion.line(this, position(), forward, length, 0.6, this::canHit)) {
						strike(hit, (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), 0.3);
					}
					playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 0.8f);
				}
			}
			case "sweep" -> {
				if (target != null && age < SWEEP_HIT - 4) faceToward(target.position(), 10);
				if (age == SWEEP_HIT) {
					float damage = (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
					double pull = cfg("sweep_pull_blocks", 0, 4);
					for (LivingEntity hit : HollowLegion.arc(this, 3.0, 120, this::canHit)) {
						// The guisarme hooks: a pull of about one block toward the soldier.
						if (strike(hit, damage, 0) && pull > 0) {
							hit.knockback(0.45 * pull, hit.getX() - getX(), hit.getZ() - getZ());
							hit.hurtMarked = true;
						}
					}
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.9f);
				}
			}
			default -> {
			}
		}
	}

	/** The owner is gone: the bones collapse with no loot. */
	public void crumble() {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 10, 0.3, 0.6, 0.3, 0.02);
			playSound(SoundEvents.SKELETON_DEATH, 0.8f, 0.6f);
		}
		discard();
	}

	@Override
	protected boolean shouldDropLoot() {
		return !raised && super.shouldDropLoot();
	}

	@Override
	public boolean shouldDropExperience() {
		return !raised && super.shouldDropExperience();
	}

	/** Raised soldiers belong to a fight in progress; like the Sovereign's guards they are not saved. */
	@Override
	public boolean shouldBeSaved() {
		return !raised && super.shouldBeSaved();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !raised && super.removeWhenFarAway(distance);
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
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SKELETON_STEP, 0.5f, 0.8f);
	}

	@Override
	public float getVoicePitch() {
		return 0.75f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}
}
