package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Crypt Lurker: a low bone crawler that waits buried. While buried it is invisible,
 * cannot be targeted or hurt and does not move; when a player comes within 6 blocks it plays
 * {@code emerge} and pounces (6 damage and Slowness I for 40 ticks on a hit). Natural, structure and
 * spawner lurkers start buried; eggs and commands spawn them above ground ({@code Buried:1b} buries).
 * A lurker left without a target for a while digs back in.
 */
public class CryptLurkerEntity extends CryptbornEntity {
	private static final EntityDataAccessor<Boolean> BURIED = SynchedEntityData.defineId(CryptLurkerEntity.class, EntityDataSerializers.BOOLEAN);
	/** Pounce: crouch to 12, leap 12-17, hit window 17-25. */
	public static final int LEAP_START = 12, LEAP_END = 17, POUNCE_HIT_END = 25, BITE_HIT = 8;
	private int pounceCooldown, idleTicks;
	private boolean pounceLanded;

	public CryptLurkerEntity(EntityType<? extends CryptLurkerEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(22, 5, 3, 0.30);
	}

	@Override
	public String configId() {
		return "crypt_lurker";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.CRYPT_LURKER;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BURIED, false);
	}

	public boolean isBuried() {
		return entityData.get(BURIED);
	}

	public void setBuried(boolean buried) {
		entityData.set(BURIED, buried);
		if (buried) {
			setTarget(null);
			cancelAction();
			getNavigation().stop();
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION || reason == EntitySpawnReason.STRUCTURE
				|| reason == EntitySpawnReason.SPAWNER || reason == EntitySpawnReason.TRIAL_SPAWNER) {
			setBuried(true);
		}
		return result;
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (p, targetLevel) -> !isBuried()));
	}

	@Override
	protected double stopDistance() {
		return 1.6;
	}

	@Override
	protected void serverTick() {
		if (pounceCooldown > 0) pounceCooldown--;
		if (isBuried()) {
			if (getTarget() != null) setTarget(null);
			getNavigation().stop();
			if (tickCount % 5 == 0) watch();
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive()) idleTicks = 0;
		else if (++idleTicks >= cfg("rebury_ticks", 20, 24000) && onGround() && action().isEmpty()) {
			idleTicks = 0;
			dig(true);
		}
	}

	/** Buried: the first survival player within the ambush radius wakes it. */
	private void watch() {
		double radius = cfg("ambush_radius", 1, 24);
		Player nearest = null;
		double best = radius * radius;
		for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), p -> canHit(p) && !p.isSpectator())) {
			double d = player.distanceToSqr(this);
			if (d <= best) {
				best = d;
				nearest = player;
			}
		}
		if (nearest == null) return;
		dig(false);
		setTarget(nearest);
		begin("emerge", nearest.position());
	}

	/** Digs in ({@code down}) or bursts out, with dirt and bone dust. */
	private void dig(boolean down) {
		// A buried lurker is silent, so these play while it is above ground.
		if (down) playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.8f);
		setBuried(down);
		if (!down) {
			playSound(SoundEvents.GRAVEL_BREAK, 1.2f, 0.6f);
			playSound(SoundEvents.SILVERFISH_AMBIENT, 1.2f, 0.6f);
		}
		if (level() instanceof ServerLevel server) {
			BlockState floor = level().getBlockState(blockPosition().below());
			if (!floor.isAir()) {
				server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), getX(), getY() + 0.1, getZ(), 30, 0.6, 0.2, 0.6, 0.12);
			}
		}
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (isBuried()) return;
		double reach = distance - target.getBbWidth() / 2 - getBbWidth() / 2;
		if (reach <= 1.2) {
			begin("bite", target.position());
			playSound(SoundEvents.SPIDER_AMBIENT, 0.8f, 1.3f);
		} else if (reach <= 6 && pounceCooldown <= 0) {
			startPounce(target);
		}
	}

	private void startPounce(LivingEntity target) {
		begin("pounce", target.position());
		pounceLanded = false;
		pounceCooldown = (int) cfg("pounce_cooldown_ticks", 0, 2400);
		playSound(SoundEvents.SILVERFISH_AMBIENT, 1.0f, 0.5f);
	}

	@Override
	protected boolean onActionEnd(String action) {
		LivingEntity target = getTarget();
		if ("emerge".equals(action) && target != null && target.isAlive() && canHit(target) && distanceTo(target) <= 9) {
			startPounce(target);
			return true;
		}
		return false;
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "pounce" -> {
				if (target != null && age < LEAP_START) faceToward(target.position(), 15);
				if (age == LEAP_START) {
					// Leap toward where the target stands now: it lands short of a target that sidesteps.
					Vec3 to = target != null ? target.position() : position().add(Vec3.directionFromRotation(0, getYRot()).scale(4));
					Vec3 path = to.subtract(position()).multiply(1, 0, 1);
					double distance = Math.min(6, path.length());
					Vec3 dir = path.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : path.normalize();
					setDeltaMovement(dir.scale(distance / 5.0).add(0, 0.38, 0));
					needsSync = true;
					playSound(SoundEvents.SPIDER_STEP, 1.0f, 0.6f);
				}
				if (age >= LEAP_END && age <= POUNCE_HIT_END && !pounceLanded) {
					AABB reach = getBoundingBox().inflate(0.5, 0.3, 0.5);
					for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, reach, this::canHit)) {
						if (strike(victim, (float) cfg("pounce_damage", 0, 100), 0.3)) {
							int slow = (int) cfg("pounce_slow_ticks", 0, 1200);
							if (slow > 0) victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 0), this);
							pounceLanded = true;
							playSound(SoundEvents.SPIDER_HURT, 0.8f, 0.6f);
						}
					}
				}
			}
			case "bite" -> {
				if (target != null && age < BITE_HIT - 2) faceToward(target.position(), 15);
				if (age == BITE_HIT) {
					strikeArc(getBbWidth() / 2 + 1.4, 100, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.2);
					playSound(SoundEvents.SPIDER_HURT, 0.6f, 1.4f);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isBuried() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurtServer(level, source, amount);
	}

	/**
	 * Buried means unseen, on every client. The synced flag decides it: vanilla recomputes the shared invisible flag from
	 * potion effects (on the first tick, and whenever effects change), which would reveal a lurker buried at spawn.
	 */
	@Override
	public boolean isInvisible() {
		return isBuried() || super.isInvisible();
	}

	@Override
	public boolean isPickable() {
		return !isBuried() && super.isPickable();
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !isBuried() && super.canBeSeenAsEnemy();
	}

	@Override
	public boolean isPushable() {
		return !isBuried() && super.isPushable();
	}

	@Override
	protected boolean isImmobile() {
		return isBuried() || super.isImmobile();
	}

	@Override
	public boolean isSilent() {
		return isBuried() || super.isSilent();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SPIDER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SPIDER_STEP, 0.4f, 1.2f);
	}

	@Override
	public float getVoicePitch() {
		return 0.6f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Buried", isBuried());
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		setBuried(tag.getBooleanOr("Buried", false));
	}
}
