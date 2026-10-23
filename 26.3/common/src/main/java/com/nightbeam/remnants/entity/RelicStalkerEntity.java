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
 * Relic Stalker: an elite assembled from Sanctum artifacts (candlestick legs, scepter-blade arms, a censer for a
 * head). Like the Crypt Lurker it waits hidden: invisible, motionless, untargetable and unhurtable, until a player
 * comes within 7 blocks. Then it {@code ambush}es: it folds low for 12 ticks (the tell), springs at tick 12 and
 * strikes on contact between ticks 15 and 22 (8 damage and Slowness I for 60 ticks). After that it fights with
 * {@code slash} combos (9 damage at tick 10, up to two in a row). Natural, structure and spawner stalkers start
 * hidden; eggs and commands spawn them visible ({@code Hidden:1b} hides). Left without a target it hides again.
 */
public class RelicStalkerEntity extends CorruptedRelicEntity {
	private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(RelicStalkerEntity.class, EntityDataSerializers.BOOLEAN);
	/** Ambush: fold 0-12, spring at 12, contact window 15-22. Slash: windup 0-8, hit 10. */
	public static final int LEAP = 12, AMBUSH_HIT_START = 15, AMBUSH_HIT_END = 22, SLASH_HIT = 10;
	private int ambushCooldown, idleTicks, combo;
	private boolean ambushLanded;

	public RelicStalkerEntity(EntityType<? extends RelicStalkerEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return legionAttributes(50, 9, 8, 0.30).add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public String configId() {
		return "relic_stalker";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.RELIC_STALKER;
	}

	@Override
	protected void applyConfig() {
		setBase(Attributes.KNOCKBACK_RESISTANCE, cfg("knockback_resistance", 0, 1));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(HIDDEN, false);
	}

	public boolean isHidden() {
		return entityData.get(HIDDEN);
	}

	public void setHidden(boolean hidden) {
		entityData.set(HIDDEN, hidden);
		if (hidden) {
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
			setHidden(true);
		}
		return result;
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (p, targetLevel) -> !isHidden()));
	}

	@Override
	protected double stopDistance() {
		return 1.8;
	}

	@Override
	protected void serverTick() {
		if (ambushCooldown > 0) ambushCooldown--;
		if (isHidden()) {
			if (getTarget() != null) setTarget(null);
			getNavigation().stop();
			if (tickCount % 5 == 0) watch();
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive()) idleTicks = 0;
		else if (++idleTicks >= cfg("rehide_ticks", 20, 24000) && onGround() && action().isEmpty()) {
			idleTicks = 0;
			cloak(true);
		}
	}

	/** Hidden: the first survival player within the ambush radius wakes it. */
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
		cloak(false);
		setTarget(nearest);
		startAmbush(nearest);
	}

	/** Fades out ({@code hide}) or back into sight, in a shimmer of rose light. */
	private void cloak(boolean hide) {
		// A hidden stalker is silent, so these play while it is visible.
		if (hide) playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 0.5f);
		setHidden(hide);
		if (!hide) playSound(SoundEvents.CHAIN_BREAK, 1.0f, 0.6f);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(CrystalBoltEntity.ROSE, getX(), getY() + 1.3, getZ(), 24, 0.5, 0.9, 0.5, 0);
			server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.3, getZ(), 4, 0.4, 0.6, 0.4, 0.02);
		}
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		if (isHidden()) return;
		double reach = distance - target.getBbWidth() / 2 - getBbWidth() / 2;
		if (reach <= cfg("slash_reach", 1, 6)) {
			startSlash(target, 1);
		} else if (reach >= 2.5 && reach <= cfg("ambush_radius", 1, 24) && ambushCooldown <= 0) {
			startAmbush(target);
		}
	}

	private void startAmbush(LivingEntity target) {
		begin("ambush", target.position());
		ambushLanded = false;
		ambushCooldown = (int) cfg("ambush_cooldown_ticks", 0, 2400);
		// The tell: the limbs fold with a scrape of gilt and glass.
		playSound(SoundEvents.SPIDER_AMBIENT, 1.0f, 0.5f);
		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.5f);
	}

	private void startSlash(LivingEntity target, int count) {
		begin("slash", target.position());
		combo = count;
		playSound(SoundEvents.CHAIN_HIT, 0.9f, 1.2f);
	}

	@Override
	protected boolean onActionEnd(String action) {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !canHit(target)) return false;
		double reach = distanceTo(target) - target.getBbWidth() / 2 - getBbWidth() / 2;
		if ("ambush".equals(action) && reach <= cfg("slash_reach", 1, 6) + 0.6) {
			startSlash(target, 1);
			return true;
		}
		if ("slash".equals(action) && combo < (int) cfg("max_combo", 1, 4) && reach <= cfg("slash_reach", 1, 6) + 0.6) {
			startSlash(target, combo + 1);
			return true;
		}
		return false;
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "ambush" -> {
				if (target != null && age < LEAP) faceToward(target.position(), 20);
				if (age == LEAP) {
					// Spring at where the target stands now: it lands short of a target that sidesteps.
					Vec3 to = target != null ? target.position() : position().add(Vec3.directionFromRotation(0, getYRot()).scale(4));
					Vec3 path = to.subtract(position()).multiply(1, 0, 1);
					double distance = Math.min(cfg("ambush_radius", 1, 24), path.length());
					Vec3 dir = path.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : path.normalize();
					setDeltaMovement(dir.scale(distance / 5.0).add(0, 0.42, 0));
					needsSync = true;
					playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, 0.8f);
				}
				if (age >= AMBUSH_HIT_START && age <= AMBUSH_HIT_END && !ambushLanded) {
					AABB reach = getBoundingBox().inflate(0.6, 0.3, 0.6);
					int slow = (int) cfg("ambush_slow_ticks", 0, 1200);
					for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, reach, this::canHit)) {
						if (strike(victim, (float) cfg("ambush_damage", 0, 100), 0.4)) {
							if (slow > 0) victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 0), this);
							ambushLanded = true;
							playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
						}
					}
				}
			}
			case "slash" -> {
				if (target != null && age < SLASH_HIT - 2) faceToward(target.position(), 15);
				if (age == SLASH_HIT) {
					strikeArc(getBbWidth() / 2 + cfg("slash_reach", 1, 6), 110, (float) cfg("slash_damage", 0, 100), 0.35);
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isHidden() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurtServer(level, source, amount);
	}

	/**
	 * Hidden means unseen, on every client. The synced flag decides it: vanilla recomputes the shared invisible flag from
	 * potion effects (on the first tick, and whenever effects change), which would reveal a stalker hidden at spawn.
	 */
	@Override
	public boolean isInvisible() {
		return isHidden() || super.isInvisible();
	}

	@Override
	public boolean isPickable() {
		return !isHidden() && super.isPickable();
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !isHidden() && super.canBeSeenAsEnemy();
	}

	@Override
	public boolean isPushable() {
		return !isHidden() && super.isPushable();
	}

	@Override
	protected boolean isImmobile() {
		return isHidden() || super.isImmobile();
	}

	@Override
	public boolean isSilent() {
		return isHidden() || super.isSilent();
	}

	@Override
	protected void clientTick() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || isHidden()) return;
		// The censer head smoulders with Oracle light.
		if (tickCount % 6 == 0 && random.nextDouble() < density) {
			Vec3 censer = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(0.35)).add(0, 2.2, 0);
			level().addParticle(CrystalBoltEntity.ROSE, censer.x, censer.y, censer.z, 0, 0.02, 0);
		}
		if ("ambush".equals(action()) && actionAge() < LEAP && tickCount % 2 == 0) {
			level().addParticle(ParticleTypes.END_ROD, getX() + random.nextGaussian() * 0.4, getY() + 0.8, getZ() + random.nextGaussian() * 0.4, 0, 0.02, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.CHAIN_STEP;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.CHAIN_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.CHAIN_STEP, 0.4f, 1.2f);
	}

	@Override
	public float getVoicePitch() {
		return 0.7f + (random.nextFloat() - random.nextFloat()) * 0.05f;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Hidden", isHidden());
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		setHidden(tag.getBooleanOr("Hidden", false));
	}
}
