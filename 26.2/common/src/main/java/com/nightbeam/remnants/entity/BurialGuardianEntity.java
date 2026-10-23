package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Burial Guardian: the Catacombs' vault miniboss, a stone tomb on legs. Boss bar,
 * no ritual. {@code slam} is a 4-block ground ring (jump it), {@code sweep} throws back (knockback 1.4).
 * At 50% it unseals: a soul blast cone (10 damage, Wither I for 60 ticks), after which the lid stays
 * open. It turns at most 6 degrees per tick, so flanking pays. Persistent.
 */
public class BurialGuardianEntity extends CryptbornEntity {
	private static final EntityDataAccessor<Boolean> UNSEALED = SynchedEntityData.defineId(BurialGuardianEntity.class, EntityDataSerializers.BOOLEAN);
	public static final int SLAM_HIT = 26, SWEEP_HIT = 18, UNSEAL_BLAST = 24;
	/** Attacks start only with the target inside this half-angle of its facing. */
	private static final float ATTACK_HALF_ANGLE = 35;
	private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), 
			net.minecraft.network.chat.Component.translatable("entity.remnants.burial_guardian"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean phaseTwo;
	private int unsealCooldown;

	public BurialGuardianEntity(EntityType<? extends BurialGuardianEntity> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(140, 14, 14, 0.18).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public String configId() {
		return "burial_guardian";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.BURIAL_GUARDIAN;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(UNSEALED, false);
	}

	/** Set by the first unseal; the model keeps the lid open from then on. */
	public boolean lidOpen() {
		return entityData.get(UNSEALED);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// It guards the vault: no wandering off.
		goalSelector.removeAllGoals(goal -> goal instanceof WaterAvoidingRandomStrollGoal);
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected double stopDistance() {
		return 2.6;
	}

	private float turnRate() {
		return (float) cfg("turn_degrees", 0.5, 45);
	}

	@Override
	public int getHeadRotSpeed() {
		return Math.max(1, (int) turnRate());
	}

	@Override
	public int getMaxHeadYRot() {
		return 15;
	}

	/** Very slow turning: every rotation source is capped at {@code turn_degrees} per tick. */
	@Override
	public void aiStep() {
		float yaw = getYRot(), body = yBodyRot, head = yHeadRot;
		super.aiStep();
		if (level().isClientSide()) return;
		float limit = turnRate();
		setYRot(yaw + Mth.clamp(Mth.wrapDegrees(getYRot() - yaw), -limit, limit));
		yBodyRot = body + Mth.clamp(Mth.wrapDegrees(yBodyRot - body), -limit, limit);
		yHeadRot = head + Mth.clamp(Mth.wrapDegrees(yHeadRot - head), -limit * 2, limit * 2);
	}

	@Override
	protected void moveTick(LivingEntity target, double distance) {
		faceToward(target.position(), turnRate());
		if (distance > stopDistance() || !hasLineOfSight(target)) getNavigation().moveTo(target, 1.0);
		else getNavigation().stop();
	}

	private boolean facing(LivingEntity target) {
		float wanted = HollowLegion.yawToward(position(), target.position());
		return Math.abs(Mth.wrapDegrees(wanted - getYRot())) <= ATTACK_HALF_ANGLE;
	}

	@Override
	protected void serverTick() {
		if (unsealCooldown > 0) unsealCooldown--;
		if (tickCount % 10 == 0) updateBar();
		bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0, 1));
		if (!phaseTwo && action().isEmpty() && getHealth() / Math.max(1, getMaxHealth()) * 100 <= cfg("phase_two_percent", 1, 99)) {
			phaseTwo = true;
			begin("unseal", null);
			unsealCooldown = (int) cfg("unseal_cooldown_ticks", 0, 2400);
			playSound(SoundEvents.STONE_BREAK, 2.0f, 0.5f);
		}
	}

	/** The bar shows within 24 blocks and hides past 40, like a dungeon miniboss rather than a world boss. */
	private void updateBar() {
		for (ServerPlayer player : java.util.List.copyOf(bar.getPlayers())) {
			if (!player.isAlive() || player.level() != level() || player.distanceToSqr(this) > 40 * 40) bar.removePlayer(player);
		}
		if (!isAlive()) return;
		for (Player player : level().players()) {
			if (player instanceof ServerPlayer serverPlayer && !player.isSpectator() && player.distanceToSqr(this) <= 24 * 24) bar.addPlayer(serverPlayer);
		}
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (!facing(target)) return;
		double reach = distance - target.getBbWidth() / 2;
		if (phaseTwo && unsealCooldown <= 0 && reach <= 7 && random.nextFloat() < 0.5f) {
			begin("unseal", null);
			unsealCooldown = (int) cfg("unseal_cooldown_ticks", 0, 2400);
			playSound(SoundEvents.STONE_BREAK, 1.6f, 0.6f);
		} else if (reach <= cfg("slam_radius", 1, 12) - 0.5 && random.nextFloat() < 0.45f) {
			begin("slam", null);
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.4f, 0.5f);
		} else if (reach <= 3.2) {
			begin("sweep", null);
			playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.6f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "slam" -> {
				if (age == SLAM_HIT) {
					double radius = cfg("slam_radius", 1, 12);
					AABB box = getBoundingBox().inflate(radius + 1, 1, radius + 1);
					for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
						double dx = victim.getX() - getX(), dz = victim.getZ() - getZ();
						double lift = victim.getY() - getY();
						// A ground ring: anyone in the air above it is spared.
						if (Math.sqrt(dx * dx + dz * dz) - victim.getBbWidth() / 2 <= radius && lift < 0.75 && lift > -1.5) {
							strike(victim, (float) cfg("slam_damage", 0, 1000), 0.5);
						}
					}
					playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.6f);
					if (level() instanceof ServerLevel server) {
						server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 2, 0.5, 0, 0.5, 0);
						server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 12, radius * 0.4, 0.05, radius * 0.4, 0.01);
					}
				}
			}
			case "sweep" -> {
				if (target != null && age < SWEEP_HIT - 4) faceToward(target.position(), turnRate());
				if (age == SWEEP_HIT) {
					strikeArc(getBbWidth() / 2 + 2.6, 130, (float) cfg("sweep_damage", 0, 1000), cfg("sweep_knockback", 0, 5));
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
				}
			}
			case "unseal" -> {
				if (target != null && age < UNSEAL_BLAST - 6) faceToward(target.position(), turnRate());
				if (age == UNSEAL_BLAST) {
					entityData.set(UNSEALED, true);
					int wither = (int) cfg("unseal_wither_ticks", 0, 1200);
					for (LivingEntity hit : HollowLegion.arc(this, 7, 60, this::canHit)) {
						if (strike(hit, (float) cfg("unseal_damage", 0, 1000), 0.4) && wither > 0) {
							hit.addEffect(new MobEffectInstance(MobEffects.WITHER, wither, 0), this);
						}
					}
					playSound(SoundEvents.SOUL_ESCAPE.value(), 2.0f, 0.6f);
					if (level() instanceof ServerLevel server) {
						Vec3 forward = Vec3.directionFromRotation(0, getYRot());
						for (int i = 1; i <= 6; i++) {
							Vec3 p = position().add(forward.scale(i)).add(0, 1.6, 0);
							server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 4 + i, i * 0.12, 0.3, i * 0.12, 0.02);
						}
					}
				}
			}
			default -> {
			}
		}
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		String action = action();
		int age = actionAge();
		if ("slam".equals(action) && age > 10 && age < SLAM_HIT && tickCount % 2 == 0) {
			// Windup: the ring on the floor that the slam will cover.
			double radius = com.nightbeam.remnants.config.BoneMonsterConfig.value("burial_guardian", "slam_radius", 1, 12);
			int n = Math.max(8, (int) (24 * density));
			for (int i = 0; i < n; i++) {
				double a = i * Math.PI * 2 / n + tickCount * 0.02;
				level().addParticle(ParticleTypes.SOUL, getX() + Math.cos(a) * radius, getY() + 0.1, getZ() + Math.sin(a) * radius, 0, 0.01, 0);
			}
		}
		if ("unseal".equals(action) && age < UNSEAL_BLAST && tickCount % 2 == 0) {
			Vec3 forward = Vec3.directionFromRotation(0, yBodyRot);
			level().addParticle(ParticleTypes.SOUL, getX() + forward.x * 1.2, getY() + 2.4, getZ() + forward.z * 1.2, forward.x * 0.05, 0.02, forward.z * 0.05);
		}
		if (lidOpen() && tickCount % 6 == 0 && random.nextDouble() < density) {
			level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + random.nextGaussian() * 0.3, getY() + 2.6, getZ() + random.nextGaussian() * 0.3, 0, 0.02, 0);
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide()) playSound(SoundEvents.SOUL_ESCAPE.value(), 2.0f, 0.5f);
	}

	@Override
	public void remove(RemovalReason reason) {
		bar.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bar.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.STONE_HIT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.5f);
	}

	@Override
	public float getVoicePitch() {
		return 0.5f;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("GuardianUnsealed", phaseTwo);
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		phaseTwo = tag.getBooleanOr("GuardianUnsealed", false);
		entityData.set(UNSEALED, phaseTwo);
	}
}
