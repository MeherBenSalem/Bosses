package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.config.WardenConfig;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Gravebound Warden, keeper of the Ossuary Covenant's soul-prisons. The server owns phases, attack
 * timing and damage; clients draw bounded telegraphs from synced state, as the Hollow Sovereign does.
 */
public class GraveboundWardenEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SERIAL = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> AIM_X = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_Y = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_Z = SynchedEntityData.defineId(GraveboundWardenEntity.class, EntityDataSerializers.FLOAT);

	public static final RemnantClips.Sheet CLIPS = RemnantClips.GRAVEBOUND_WARDEN;
	/** Key ticks of the clips. */
	public static final int SWEEP_HIT = 25, CHOP_HIT = 28, CHOP_FREED = 38, CHAIN_OUT = 20, SLAM = 24,
			ERUPTION_FIRST = 26, ERUPTION_STEP = 3, ERUPTION_WARNING = 12, SUMMON_RISE = 26, WALL_RAISE = 30,
			CELL_SOULS = 24, DEATH_SOULS = 56;
	private static final Set<String> TRANSITIONS = Set.of("cell_break");
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.31f, 0.79f, 0.76f), 1.3f);
	private static final DustParticleOptions RUST = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.65f, 0.38f, 0.23f), 1.1f);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.remnants.gravebound_warden"),
			BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> struck = new HashSet<>();
	private final Set<UUID> musicListeners = new HashSet<>();
	private boolean configured, loaded, queuedSweep;
	private int cooldown = 40, summonCooldown = 100, wallCooldown, grabCooldown, staggerLength = 24;
	private int wallId, wallPlaced, wallDestroyed;
	private float lockedYaw;
	private String lastAttack = "";
	private int seenSerial = -1;

	public GraveboundWardenEntity(EntityType<? extends GraveboundWardenEntity> type, Level level) {
		super(type, level);
		this.xpReward = 200;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 520).add(Attributes.ATTACK_DAMAGE, 12)
				.add(Attributes.ARMOR, 10).add(Attributes.ARMOR_TOUGHNESS, 2).add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.FOLLOW_RANGE, 48).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, "");
		builder.define(AGE, 0);
		builder.define(SERIAL, 0);
		builder.define(PHASE, 1);
		builder.define(AIM_X, 0f);
		builder.define(AIM_Y, 0f);
		builder.define(AIM_Z, 0f);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Goal() {
			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

			@Override
			public boolean canUse() {
				return getTarget() != null && getTarget().isAlive();
			}

			@Override
			public boolean requiresUpdateEveryTick() {
				return true;
			}

			@Override
			public void stop() {
				getNavigation().stop();
			}

			@Override
			public void tick() {
				LivingEntity target = getTarget();
				if (target == null || !action().isEmpty()) {
					getNavigation().stop();
					return;
				}
				getLookControl().setLookAt(target, 20, 20);
				if (horizontalDistance(target) > 3.4 || !hasLineOfSight(target)) {
					getNavigation().moveTo(target, phase() >= 2 ? cfg("phase_two_speed_multiplier", 1, 3) : 1.0);
				} else {
					getNavigation().stop();
				}
			}
		});
		goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 24));
		goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public String action() {
		return entityData.get(ACTION);
	}

	public int actionAge() {
		return entityData.get(AGE);
	}

	public int phase() {
		return entityData.get(PHASE);
	}

	private void setPhase(int phase) {
		entityData.set(PHASE, Mth.clamp(phase, 1, 2));
	}

	private Vec3 aim() {
		return new Vec3(entityData.get(AIM_X), entityData.get(AIM_Y), entityData.get(AIM_Z));
	}

	private void setAim(Vec3 at) {
		entityData.set(AIM_X, (float) at.x);
		entityData.set(AIM_Y, (float) at.y);
		entityData.set(AIM_Z, (float) at.z);
	}

	/**
	 * After {@code cell_break} the souls bone stays at 0.2 scale for the rest of the fight. The transition
	 * clip shrinks it itself and the death clip frees the souls, so neither is overridden.
	 */
	public boolean soulsShrunk() {
		return phase() >= 2 && isAlive() && !"cell_break".equals(action());
	}

	private double cfg(String key, double min, double max) {
		return WardenConfig.value(key, min, max);
	}

	private double horizontalDistance(Entity entity) {
		double dx = entity.getX() - getX(), dz = entity.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private double healthPercent() {
		return getHealth() / Math.max(1, getMaxHealth()) * 100;
	}

	private void configure() {
		if (configured) return;
		configured = true;
		setBase(Attributes.MAX_HEALTH, cfg("max_health", 1, 100000));
		setBase(Attributes.ARMOR, cfg("armor", 0, 30));
		setBase(Attributes.ARMOR_TOUGHNESS, cfg("armor_toughness", 0, 20));
		setBase(Attributes.MOVEMENT_SPEED, cfg("movement_speed", 0.05, 0.6));
		xpReward = (int) cfg("xp_reward", 0, 100000);
		setHealth(loaded ? Math.min(getHealth(), getMaxHealth()) : getMaxHealth());
		if (!loaded) {
			playSound(SoundEvents.CHAIN_BREAK, 3.0f, 0.5f);
			playSound(SoundEvents.IRON_GOLEM_REPAIR, 2.0f, 0.5f);
		}
	}

	private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) instance.setBaseValue(value);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			clientVisuals();
			return;
		}
		configure();
		bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0, 1));
		if (!isAlive()) return;
		if (tickCount % 20 == 0) updateBossMusic();
		if (summonCooldown > 0) summonCooldown--;
		if (wallCooldown > 0) wallCooldown--;
		if (grabCooldown > 0) grabCooldown--;
		String action = action();
		if (!action.isEmpty()) {
			int age = entityData.get(AGE) + 1;
			entityData.set(AGE, age);
			getNavigation().stop();
			setDeltaMovement(0, getDeltaMovement().y, 0);
			holdYaw();
			resolve(action, age);
			if (action.equals(action()) && age >= actionLength(action)) finish(action);
			return;
		}
		if (startTransition()) return;
		LivingEntity target = getTarget();
		if (queuedSweep) {
			// Chain Grab's follow-up: the pull ends in front of the blade.
			queuedSweep = false;
			if (target != null && target.isAlive() && canHit(target) && horizontalDistance(target) <= cfg("sweep_radius", 1, 12) + 2) {
				startAttack("sweep", target);
				return;
			}
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (target != null && target.isAlive() && hasLineOfSight(target)) chooseAttack(target);
	}

	private int actionLength(String action) {
		return "stagger".equals(action) ? staggerLength : CLIPS.ticks(action);
	}

	private void holdYaw() {
		setYRot(lockedYaw);
		setYBodyRot(lockedYaw);
		setYHeadRot(lockedYaw);
	}

	private void faceToward(Vec3 point, float maxDegrees) {
		float wanted = HollowLegion.yawToward(position(), point);
		lockedYaw = getYRot() + Mth.clamp(Mth.wrapDegrees(wanted - getYRot()), -maxDegrees, maxDegrees);
		holdYaw();
	}

	private void begin(String action, Vec3 face) {
		if (face != null) faceToward(face, 360);
		lockedYaw = getYRot();
		struck.clear();
		getNavigation().stop();
		entityData.set(AGE, 0);
		entityData.set(SERIAL, entityData.get(SERIAL) + 1);
		entityData.set(ACTION, action);
	}

	private void finish(String action) {
		entityData.set(ACTION, "");
		if (TRANSITIONS.contains(action)) {
			cooldown = 20;
			return;
		}
		double scale = phase() >= 2 ? cfg("phase_two_cooldown_multiplier", 0.1, 2) : 1;
		cooldown = (int) Math.round(cfg("attack_cooldown_ticks", 0, 400) * scale);
	}

	/** The phase threshold takes effect only between attacks; the transition is invulnerable and harmless. */
	private boolean startTransition() {
		if (phase() == 1 && healthPercent() <= cfg("phase_two_percent", 1, 99)) {
			setPhase(2);
			begin("cell_break", null);
			playSound(SoundEvents.IRON_DOOR_OPEN, 3.0f, 0.4f);
			playSound(SoundEvents.CHAIN_BREAK, 3.0f, 0.6f);
			return true;
		}
		return false;
	}

	private void chooseAttack(LivingEntity target) {
		double distance = horizontalDistance(target);
		if (phase() >= 2 && wallCooldown <= 0 && anchors().isEmpty() && distance <= cfg("chain_wall_radius", 3, 24) + 4) {
			startAttack("chain_wall", target);
			return;
		}
		if (summonCooldown <= 0 && !"summon".equals(lastAttack) && soldiers().size() < (int) cfg("max_soldiers", 0, 16)) {
			startAttack("summon", target);
			return;
		}
		double reach = getBbWidth() / 2;
		List<String> options = new ArrayList<>();
		if (distance <= cfg("sweep_radius", 1, 12)) options.add("sweep");
		if (distance <= reach + cfg("chop_length", 1, 16) + 0.3) options.add("chop");
		if (distance >= 5 && distance <= cfg("chain_range", 4, 32) && grabCooldown <= 0) options.add("chain_grab");
		double eruptionReach = reach + 1 + cfg("eruption_spacing", 0.5, 4) * (cfg("eruption_count", 1, 12) - 1);
		if (distance >= 3 && distance <= eruptionReach + 1) options.add("eruption");
		if (options.size() > 1) options.remove(lastAttack);
		if (!options.isEmpty()) startAttack(options.get(random.nextInt(options.size())), target);
	}

	private void startAttack(String attack, LivingEntity target) {
		begin(attack, target.position());
		setAim(target.position());
		lastAttack = attack;
		switch (attack) {
			case "sweep" -> playSound(SoundEvents.CHAIN_HIT, 2.0f, 0.5f);
			case "chop" -> playSound(SoundEvents.ANVIL_PLACE, 1.2f, 0.5f);
			case "chain_grab" -> playSound(SoundEvents.CHAIN_PLACE, 2.5f, 0.6f);
			case "eruption" -> playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0f, 0.5f);
			case "summon" -> playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.5f);
			case "chain_wall" -> playSound(SoundEvents.CHAIN_FALL, 3.0f, 0.5f);
			default -> {
			}
		}
	}

	private void resolve(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "sweep" -> {
				if (target != null && age < 18) faceToward(target.position(), 8);
				if (age == SWEEP_HIT) {
					int slow = (int) cfg("sweep_slow_ticks", 0, 1200);
					for (LivingEntity hit : HollowLegion.arc(this, cfg("sweep_radius", 1, 12), cfg("sweep_arc", 10, 360), this::canHit)) {
						if (strike(hit, (float) cfg("sweep_damage", 0, 1000), 0.6) && slow > 0) {
							hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 0), this);
						}
					}
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
				}
			}
			case "chop" -> {
				if (target != null && age < 16) {
					faceToward(target.position(), 6);
					setAim(position().add(Vec3.directionFromRotation(0, lockedYaw).scale(6)));
				}
				if (age == CHOP_HIT) {
					Vec3 dir = Vec3.directionFromRotation(0, lockedYaw);
					double length = getBbWidth() / 2 + cfg("chop_length", 1, 16);
					for (LivingEntity hit : HollowLegion.line(this, position(), dir, length, cfg("chop_width", 0.5, 8) / 2, this::canHit)) {
						strike(hit, (float) cfg("chop_damage", 0, 1000), 0.3);
					}
					playSound(SoundEvents.ANVIL_LAND, 1.6f, 0.5f);
					if (level() instanceof ServerLevel server) {
						Vec3 tip = position().add(dir.scale(length * 0.6));
						server.sendParticles(ParticleTypes.CRIT, tip.x, getY() + 0.2, tip.z, 14, 0.4, 0.1, 0.4, 0.2);
					}
				}
				// Wedged: the punish window (CHOP_HIT..CHOP_FREED) is read by hurt().
				if (age == CHOP_FREED) playSound(SoundEvents.CHAIN_BREAK, 1.2f, 0.8f);
			}
			case "chain_grab" -> {
				if (target != null && age < 18) {
					faceToward(target.position(), 10);
					setAim(target.position().add(0, target.getBbHeight() * 0.6, 0));
				}
				if (age == CHAIN_OUT && level() instanceof ServerLevel server) {
					Vec3 from = handPosition();
					Vec3 toward = aim().subtract(from);
					WardenChainEntity.forWarden(server, this, from, toward, cfg("chain_speed", 0.3, 4), cfg("chain_range", 4, 32),
							(int) cfg("chain_latch_ticks", 0, 100), cfg("chain_pull_fraction", 0, 1));
					grabCooldown = (int) cfg("chain_grab_cooldown_ticks", 0, 2400);
					playSound(SoundEvents.CHAIN_FALL, 2.0f, 0.8f);
				}
			}
			case "eruption" -> {
				if (target != null && age <= 12) {
					faceToward(target.position(), 10);
					setAim(target.position());
				}
				if (age == SLAM) {
					playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.5f);
					if (level() instanceof ServerLevel server) {
						Vec3 fist = position().add(Vec3.directionFromRotation(0, lockedYaw).scale(getBbWidth() / 2 + 0.4));
						server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, fist.x, getY() + 0.1, fist.z, 6, 0.4, 0.05, 0.4, 0.01);
					}
				}
				int count = (int) cfg("eruption_count", 1, 12);
				for (int k = 0; k < count; k++) {
					if (age == ERUPTION_FIRST + k * ERUPTION_STEP) erupt(k);
				}
			}
			case "summon" -> {
				if (age == SUMMON_RISE) {
					raiseSoldiers();
					summonCooldown = (int) cfg("summon_cooldown_ticks", 0, 24000);
				}
			}
			case "chain_wall" -> {
				if (age == WALL_RAISE) {
					placeWall();
					wallCooldown = (int) cfg("chain_wall_cooldown_ticks", 0, 24000);
				}
			}
			case "cell_break" -> {
				if (age == CELL_SOULS) {
					freeSouls(30);
					// The cage bursts open: everyone is pushed back, nobody is hurt.
					for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6), this::canHit)) {
						Vec3 away = victim.position().subtract(position());
						victim.knockback(1.0, -away.x, -away.z, damageSources().generic(), 0f);
						victim.syncVelocity = true;
					}
					playSound(SoundEvents.SOUL_ESCAPE.value(), 3.0f, 0.6f);
				}
			}
			case "stagger" -> {
				// A long stagger replays the clip instead of freezing on its last frame.
				if (age % CLIPS.ticks("stagger") == 0 && age < staggerLength) entityData.set(SERIAL, entityData.get(SERIAL) + 1);
			}
			default -> {
			}
		}
	}

	/** Left-hand shackle, where the chain leaves the model. */
	public Vec3 handPosition() {
		float yaw = level().isClientSide() ? yBodyRot : getYRot();
		return position().add(Vec3.directionFromRotation(0, yaw - 90).scale(1.0))
				.add(Vec3.directionFromRotation(0, yaw).scale(0.7)).add(0, 2.4, 0);
	}

	/** Point {@code k} of the eruption row, from the synced aim; the server and client agree on it. */
	private Vec3 eruptionPoint(int k) {
		Vec3 dir = aim().subtract(position()).multiply(1, 0, 1);
		if (dir.lengthSqr() < 1.0E-4) dir = Vec3.directionFromRotation(0, yBodyRot);
		dir = dir.normalize();
		double spacing = WardenConfig.value("eruption_spacing", 0.5, 4);
		return position().add(dir.scale(getBbWidth() / 2 + 1.0 + spacing * k));
	}

	private void erupt(int k) {
		Vec3 ground = ground(eruptionPoint(k), 2, 3);
		if (ground == null || !(level() instanceof ServerLevel server)) return;
		float damage = (float) cfg("eruption_damage", 0, 1000);
		struck.clear();
		AABB area = new AABB(ground.x - 1.5, ground.y - 0.5, ground.z - 1.5, ground.x + 1.5, ground.y + 2.0, ground.z + 1.5);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area, this::canHit)) {
			double dx = victim.getX() - ground.x, dz = victim.getZ() - ground.z;
			double reach = 0.9 + victim.getBbWidth() / 2;
			if (dx * dx + dz * dz > reach * reach) continue;
			if (strike(victim, damage, 0)) {
				victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.45, 0));
				victim.syncVelocity = true;
			}
		}
		BlockState floor = level().getBlockState(BlockPos.containing(ground.x, ground.y - 0.5, ground.z));
		if (!floor.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), ground.x, ground.y + 0.2, ground.z, 24, 0.35, 0.3, 0.35, 0.15);
		server.sendParticles(ParticleTypes.SOUL, ground.x, ground.y + 0.4, ground.z, 6, 0.25, 0.5, 0.25, 0.05);
		server.sendParticles(ParticleTypes.CRIT, ground.x, ground.y + 0.6, ground.z, 8, 0.2, 0.6, 0.2, 0.3);
		server.playSound(null, ground.x, ground.y, ground.z, SoundEvents.BONE_BLOCK_BREAK, getSoundSource(), 1.6f, 0.6f);
		if (k == 0) server.playSound(null, ground.x, ground.y, ground.z, SoundEvents.GENERIC_EXPLODE.value(), getSoundSource(), 0.8f, 1.2f);
	}

	/** First standing spot at {@code around} between {@code up} blocks above and {@code down} below the feet. */
	private Vec3 ground(Vec3 around, int up, int down) {
		for (int dy = up; dy >= -down; dy--) {
			BlockPos pos = BlockPos.containing(around.x, Math.floor(getY()) + dy, around.z);
			BlockPos below = pos.below();
			if (level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()
					&& !level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
				return new Vec3(around.x, pos.getY(), around.z);
			}
		}
		return null;
	}

	/** Where soldiers rise: flanks first, then behind. Shared by the server and the client telegraph. */
	private Vec3 summonSpot(int i, float yaw) {
		float[] angles = {-60, 60, 180, -120, 120};
		return position().add(Vec3.directionFromRotation(0, yaw + angles[i % angles.length]).scale(getBbWidth() / 2 + 2.2));
	}

	private void raiseSoldiers() {
		if (!(level() instanceof ServerLevel server)) return;
		int wanted = (int) cfg(phase() >= 2 ? "phase_two_summon_count" : "summon_count", 0, 5);
		int room = Math.max(0, (int) cfg("max_soldiers", 0, 16) - soldiers().size());
		var type = ModEntities.GRAVEBOUND_SOLDIER.get();
		int raised = 0;
		for (int i = 0; i < 5 && raised < Math.min(wanted, room); i++) {
			Vec3 spot = ground(summonSpot(i, lockedYaw), 2, 3);
			if (spot == null || !level().noCollision(type.getSpawnAABB(spot.x, spot.y, spot.z))) continue;
			if (GraveboundSoldierEntity.raise(server, this, spot) != null) raised++;
		}
		playSound(SoundEvents.SOUL_ESCAPE.value(), 2.0f, 0.5f);
	}

	public List<GraveboundSoldierEntity> soldiers() {
		return level().getEntitiesOfClass(GraveboundSoldierEntity.class, getBoundingBox().inflate(48),
				s -> s.isAlive() && getUUID().equals(s.ownerId()));
	}

	private Vec3 wallSpot(int i, float yaw, double radius) {
		return position().add(Vec3.directionFromRotation(0, yaw + 45 + 90 * i).scale(radius));
	}

	private void placeWall() {
		if (!(level() instanceof ServerLevel server)) return;
		var type = ModEntities.CHAIN_ANCHOR.get();
		double radius = cfg("chain_wall_radius", 3, 24);
		List<ChainAnchorEntity> placed = new ArrayList<>();
		wallId++;
		for (int i = 0; i < 4; i++) {
			// Walls and pillars pull an anchor inward, never closer than 3 blocks.
			for (double r = radius; r >= 3; r -= 1) {
				Vec3 spot = ground(wallSpot(i, lockedYaw, r), 3, 4);
				if (spot == null || !level().noCollision(type.getSpawnAABB(spot.x, spot.y, spot.z))) continue;
				ChainAnchorEntity anchor = ChainAnchorEntity.place(server, this, spot, wallId, (int) cfg("anchor_life_ticks", 20, 24000),
						(float) cfg("anchor_health", 1, 1000), (float) cfg("chain_damage", 0, 1000));
				if (anchor != null) placed.add(anchor);
				break;
			}
		}
		// Neighbours are linked in a ring; two anchors share a single chain.
		for (int i = 0; i < placed.size() && placed.size() >= 2; i++) {
			if (placed.size() == 2 && i == 1) break;
			placed.get(i).linkTo(placed.get((i + 1) % placed.size()));
		}
		wallPlaced = placed.size();
		wallDestroyed = 0;
		playSound(SoundEvents.CHAIN_PLACE, 3.0f, 0.5f);
		playSound(SoundEvents.ANVIL_PLACE, 1.5f, 0.6f);
	}

	public List<ChainAnchorEntity> anchors() {
		return level().getEntitiesOfClass(ChainAnchorEntity.class, getBoundingBox().inflate(48), a -> a.isAlive() && a.ownedBy(this));
	}

	/** An anchor was destroyed by damage. Breaking every anchor of the current wall staggers the Warden. */
	public void onAnchorDestroyed(ChainAnchorEntity anchor) {
		if (!isAlive() || anchor.wallId() != wallId) return;
		wallDestroyed++;
		if (wallPlaced > 0 && wallDestroyed >= wallPlaced && !TRANSITIONS.contains(action())) {
			staggerLength = (int) cfg("anchor_stagger_ticks", 1, 1200);
			begin("stagger", null);
			playSound(SoundEvents.IRON_GOLEM_DAMAGE, 2.0f, 0.5f);
			playSound(SoundEvents.CHAIN_BREAK, 2.5f, 0.5f);
		}
	}

	/** The Chain Grab pull finished: a Sweep follows. */
	public void onChainPulled(LivingEntity victim) {
		if (isAlive() && victim.isAlive() && canHit(victim)) queuedSweep = true;
	}

	private void discardHelpers() {
		for (ChainAnchorEntity anchor : anchors()) anchor.crumble();
		for (WardenChainEntity chain : level().getEntitiesOfClass(WardenChainEntity.class, getBoundingBox().inflate(48),
				c -> c.getOwner() == this)) {
			chain.discard();
		}
		for (GraveboundSoldierEntity soldier : soldiers()) soldier.crumble();
	}

	public boolean canHit(LivingEntity entity) {
		return entity != this && Cryptborn.victim(entity);
	}

	private boolean strike(LivingEntity target, float damage, double knockback) {
		if (!struck.add(target.getUUID())) return false;
		boolean hurt = Damage.hurt(target, damageSources().mobAttack(this), damage);
		if (hurt && knockback > 0) target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ(), damageSources().generic(), 0f);
		return hurt;
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return super.considersEntityAsAlly(other) || (other != this && Cryptborn.isWardenSide(other));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (level().isClientSide()) return super.hurtServer(level, source, amount);
		String action = action();
		boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (!bypass && TRANSITIONS.contains(action)) return false;
		Entity attacker = source.getEntity();
		if (attacker != null && attacker != this && Cryptborn.isWardenSide(attacker)) return false;
		float scale = 1;
		int age = actionAge();
		if ("chop".equals(action) && age >= CHOP_HIT && age <= CHOP_FREED) scale = (float) cfg("chop_wedge_multiplier", 1, 5);
		return super.hurtServer(level, source, amount * scale);
	}

	private void updateBossMusic() {
		if (cfg("boss_music_enabled", 0, 1) <= 0) return;
		double start = cfg("boss_music_radius", 8, 256), stop = start + 15;
		musicListeners.removeIf(uuid -> {
			Player player = level().getPlayerByUUID(uuid);
			boolean gone = player == null || !player.isAlive() || player.distanceToSqr(this) > stop * stop;
			if (gone && player instanceof ServerPlayer serverPlayer) Services.NETWORK.sendBossMusic(serverPlayer, getId(), false);
			return gone;
		});
		for (Player player : level().players()) {
			if (player instanceof ServerPlayer serverPlayer && player.distanceToSqr(this) <= start * start && musicListeners.add(player.getUUID())) {
				Services.NETWORK.sendBossMusic(serverPlayer, getId(), true);
			}
		}
	}

	private void stopBossMusic() {
		for (UUID uuid : musicListeners) {
			if (level().getPlayerByUUID(uuid) instanceof ServerPlayer player) Services.NETWORK.sendBossMusic(player, getId(), false);
		}
		musicListeners.clear();
	}

	/** The cage opens and its souls leave it. */
	private void freeSouls(int count) {
		if (!(level() instanceof ServerLevel server)) return;
		Vec3 cage = position().add(0, 2.6, 0);
		server.sendParticles(ParticleTypes.SOUL, cage.x, cage.y, cage.z, count, 0.6, 0.5, 0.6, 0.08);
		server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cage.x, cage.y, cage.z, count / 2, 0.5, 0.4, 0.5, 0.04);
		server.sendParticles(SOUL_LIGHT, cage.x, cage.y, cage.z, count / 2, 0.8, 0.6, 0.8, 0);
	}

	private void particle(ParticleOptions type, Vec3 p, double vy) {
		level().addParticle(type, p.x, p.y, p.z, 0, vy, 0);
	}

	private void ring(Vec3 centre, double radius, int count, double twist, ParticleOptions type) {
		for (int i = 0; i < count; i++) {
			double a = i * Math.PI * 2 / count + twist;
			particle(type, centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 0.01);
		}
	}

	private void clientVisuals() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		int n = Math.max(8, (int) (36 * density));
		int age = actionAge();
		Vec3 feet = position().add(0, 0.1, 0);
		if (!isAlive()) {
			if (deathTime >= DEATH_SOULS && deathTime < DEATH_SOULS + 30 && tickCount % 2 == 0) {
				for (int i = 0; i < 3; i++) {
					particle(ParticleTypes.SOUL, position().add(random.nextGaussian() * 0.6, 2.6 + (deathTime - DEATH_SOULS) * 0.08, random.nextGaussian() * 0.6), 0.08);
				}
			}
			return;
		}
		// The Open Cell: freed souls circle the Warden.
		if (phase() >= 2 && random.nextDouble() < density) {
			for (int i = 0; i < 2; i++) {
				double a = tickCount * 0.12 + i * Math.PI;
				double r = 1.7 + Math.sin(tickCount * 0.05 + i) * 0.3;
				particle(i == 0 ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME,
						position().add(Math.cos(a) * r, 2.0 + Math.sin(tickCount * 0.09 + i * 2) * 0.8, Math.sin(a) * r), 0.01);
			}
		} else if (tickCount % 10 == 0) {
			particle(ParticleTypes.SOUL, position().add(random.nextGaussian() * 0.15, 2.6, random.nextGaussian() * 0.15), 0.01);
		}
		Vec3 forward = Vec3.directionFromRotation(0, yBodyRot);
		switch (action()) {
			case "cell_break" -> {
				if (tickCount % 3 == 0) for (int h = 0; h < 3; h++) ring(position().add(0, h * 1.3 + 0.3, 0), 2.2 + h * 0.2, n / 2, age * 0.08 + h, SOUL_LIGHT);
				if (age == CELL_SOULS) ring(feet, 4, n * 2, 0, ParticleTypes.SOUL_FIRE_FLAME);
			}
			case "sweep" -> {
				double radius = WardenConfig.value("sweep_radius", 1, 12);
				double arc = Math.toRadians(WardenConfig.value("sweep_arc", 10, 360));
				if (age >= 10 && age < SWEEP_HIT && tickCount % 2 == 0) {
					// Windup: the arc the blade will cover, traced on the floor.
					for (int i = 0; i <= n / 3; i++) {
						double a = Math.toRadians(yBodyRot) + (i / (double) (n / 3) - 0.5) * arc;
						particle(RUST, feet.add(-Math.sin(a) * radius, 0, Math.cos(a) * radius), 0);
					}
				}
				if (age >= SWEEP_HIT - 2 && age <= SWEEP_HIT + 1) {
					for (int i = 0; i < n / 2; i++) {
						double a = Math.toRadians(yBodyRot) + (i / (double) (n / 2) - 0.5) * arc;
						particle(ParticleTypes.SWEEP_ATTACK, position().add(-Math.sin(a) * radius * 0.7, 1.4, Math.cos(a) * radius * 0.7), 0);
					}
				}
			}
			case "chop" -> {
				if (age >= 8 && age < CHOP_HIT && tickCount % 2 == 0) {
					// The strip the blade lands in: step out of it sideways.
					double half = WardenConfig.value("chop_width", 0.5, 8) / 2;
					double length = getBbWidth() / 2 + WardenConfig.value("chop_length", 1, 16);
					Vec3 side = new Vec3(-forward.z, 0, forward.x);
					for (double s = getBbWidth() / 2; s <= length; s += 0.5) {
						particle(RUST, feet.add(forward.scale(s)).add(side.scale(half)), 0);
						particle(RUST, feet.add(forward.scale(s)).add(side.scale(-half)), 0);
					}
				}
				if (age > CHOP_HIT && age <= CHOP_FREED && tickCount % 3 == 0) {
					particle(ParticleTypes.CRIT, feet.add(forward.scale(getBbWidth() / 2 + 2.5)).add(0, 0.3, 0), 0.1);
				}
			}
			case "chain_grab" -> {
				if (age < CHAIN_OUT && tickCount % 2 == 0) particle(RUST, handPosition().add(random.nextGaussian() * 0.2, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2), 0.02);
			}
			case "eruption" -> {
				int count = (int) WardenConfig.value("eruption_count", 1, 12);
				for (int k = 0; k < count; k++) {
					int start = ERUPTION_FIRST + k * ERUPTION_STEP - ERUPTION_WARNING;
					if (age < start || age >= start + ERUPTION_WARNING) continue;
					// Cracking ground: the row reveals itself one point at a time.
					Vec3 p = eruptionPoint(k);
					BlockPos below = BlockPos.containing(p.x, getY() - 0.5, p.z);
					BlockState floor = level().getBlockState(below);
					for (int i = 0; i < 2; i++) {
						Vec3 q = new Vec3(p.x + random.nextGaussian() * 0.35, getY() + 0.05, p.z + random.nextGaussian() * 0.35);
						if (!floor.isAir()) level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, floor), q.x, q.y, q.z, 0, 0.05, 0);
						particle(ParticleTypes.SMOKE, q, 0.01);
					}
					if (age - start > ERUPTION_WARNING / 2) particle(ParticleTypes.SOUL, p.add(0, 0.1, 0), 0.03);
				}
			}
			case "summon" -> {
				if (age < SUMMON_RISE && tickCount % 2 == 0) {
					int count = phase() >= 2 ? 3 : 2;
					for (int i = 0; i < count; i++) ring(summonSpot(i, yBodyRot).add(0, 0.1, 0), 0.6, n / 4, age * 0.2, ParticleTypes.SOUL);
				}
			}
			case "chain_wall" -> {
				if (age < WALL_RAISE && tickCount % 2 == 0) {
					double radius = WardenConfig.value("chain_wall_radius", 3, 24);
					for (int i = 0; i < 4; i++) {
						Vec3 spot = wallSpot(i, yBodyRot, radius);
						particle(ParticleTypes.SOUL_FIRE_FLAME, spot.add(random.nextGaussian() * 0.2, 0.1 + random.nextDouble() * 2, random.nextGaussian() * 0.2), 0.02);
						particle(RUST, spot.add(0, 0.1, 0), 0.05);
					}
				}
			}
			case "stagger" -> {
				if (tickCount % 3 == 0) particle(ParticleTypes.CRIT, position().add(random.nextGaussian() * 0.6, 4.2, random.nextGaussian() * 0.6), 0.05);
			}
			default -> {
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide()) {
			entityData.set(ACTION, "");
			discardHelpers();
			stopBossMusic();
			playSound(SoundEvents.CHAIN_BREAK, 3.0f, 0.4f);
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		getNavigation().stop();
		if (deathTime == DEATH_SOULS && !level().isClientSide()) {
			playSound(ModSounds.SOUL_RELEASE.get(), 3.0f, 1.0f);
			freeSouls(60);
		}
		if (deathTime >= CLIPS.ticks("death") && !level().isClientSide() && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	/** Like the Hollow Sovereign: persistent always, Peaceful included (it cannot hurt players there). */

	@Override
	public boolean canTeleport(Level oldLevel, Level newLevel) {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bar.removePlayer(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide()) {
			stopBossMusic();
			if (reason.shouldDestroy()) discardHelpers();
		}
		bar.removeAllPlayers();
		super.remove(reason);
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
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f);
		playSound(SoundEvents.CHAIN_STEP, 0.8f, 0.7f);
	}

	@Override
	public float getVoicePitch() {
		return isDeadOrDying() ? 0.5f : 0.55f;
	}

	@Override
	protected float getSoundVolume() {
		return 1.6f;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("WardenPhase", phase());
		tag.putInt("WardenSummonCooldown", summonCooldown);
		tag.putInt("WardenWallCooldown", wallCooldown);
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		loaded = true;
		configured = false;
		setPhase(tag.getIntOr("WardenPhase", 1));
		summonCooldown = tag.getIntOr("WardenSummonCooldown", 0);
		wallCooldown = tag.getIntOr("WardenWallCooldown", 0);
		entityData.set(ACTION, "");
		cooldown = 40;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>("warden", 3, state -> {
			if (!isAlive()) return state.setAndContinue(RemnantAnims.of(CLIPS, "death"));
			String action = action();
			if (!action.isEmpty() && CLIPS.has(action)) {
				int serial = entityData.get(SERIAL);
				if (serial != seenSerial) {
					seenSerial = serial;
					state.controller().reset();
				}
				return state.setAndContinue(RemnantAnims.of(CLIPS, action));
			}
			return state.setAndContinue(RemnantAnims.of(CLIPS, state.isMoving() ? "walk" : "idle"));
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
