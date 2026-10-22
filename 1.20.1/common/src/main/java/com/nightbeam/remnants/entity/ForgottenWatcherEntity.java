package com.nightbeam.remnants.entity;

import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.config.WatcherConfig;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Forgotten Watcher, Seyr's guardian of the last memory. It floats, keeps its distance and fights
 * with patterns: Ring Volleys, a sweeping Focused Beam, Lens Shifts between the four lenses around its
 * home, memory illusions and a rotating aegis in phase 2. The server owns phases, attack timing and
 * damage; clients draw bounded telegraphs from synced state.
 */
public class ForgottenWatcherEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SERIAL = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> AIM_X = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_Y = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_Z = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.FLOAT);
	/** Aegis: game tick it was raised, game tick it falls, and the open segment's bearing when raised. */
	private static final EntityDataAccessor<Integer> AEGIS_START = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> AEGIS_END = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> AEGIS_YAW = SynchedEntityData.defineId(ForgottenWatcherEntity.class, EntityDataSerializers.FLOAT);

	public static final RemnantClips.Sheet CLIPS = RemnantClips.FORGOTTEN_WATCHER;
	/** Key ticks of the clips. */
	public static final int VOLLEY_RELEASE = 20, VOLLEY_SECOND = 28, BEAM_LOCK = 26, BEAM_START = 30, BEAM_MID = 50, BEAM_END = 70,
			SHIFT = 16, AEGIS_CLOSED = 20, ILLUSIONS = 20, PULSE = 30, LAST_MEMORY = 30;
	/** The eye sits 2.1 blocks above the feet (the entity's eye height). */
	public static final double EYE = 2.1;
	private static final Set<String> TRANSITIONS = Set.of("awaken_memory");
	private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.62f, 0.53f, 0.88f), 1.3f);
	private static final DustParticleOptions PALE = new DustParticleOptions(new Vector3f(0.82f, 0.78f, 1.0f), 1.0f);
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(new Vector3f(0.31f, 0.79f, 0.76f), 1.2f);
	private static final int[][] LENSES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.remnants.forgotten_watcher"),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> musicListeners = new HashSet<>();
	private final Map<UUID, Long> beamHits = new HashMap<>();
	private boolean configured, loaded;
	private Vec3 home;
	private int cooldown = 40, illusionCooldown = 120, aegisCooldown, aegisHits, exposedTicks, hugTicks, attacksSinceShift;
	private float lockedYaw;
	private String lastAttack = "";
	private int seenSerial = -1;

	public ForgottenWatcherEntity(EntityType<? extends ForgottenWatcherEntity> type, Level level) {
		super(type, level);
		this.xpReward = 400;
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 680).add(Attributes.ATTACK_DAMAGE, 7)
				.add(Attributes.ARMOR, 8).add(Attributes.ARMOR_TOUGHNESS, 2).add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.FOLLOW_RANGE, 48).add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(ACTION, "");
		entityData.define(AGE, 0);
		entityData.define(SERIAL, 0);
		entityData.define(PHASE, 1);
		entityData.define(AIM_X, 0f);
		entityData.define(AIM_Y, 0f);
		entityData.define(AIM_Z, 0f);
		entityData.define(AEGIS_START, -1);
		entityData.define(AEGIS_END, -1);
		entityData.define(AEGIS_YAW, 0f);
	}

	@Override
	protected void registerGoals() {
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

	/** Whether the aegis plates are up at {@code time} (they show from the start of the {@code shield} clip). */
	public boolean aegisShown(float time) {
		int start = entityData.get(AEGIS_START), end = entityData.get(AEGIS_END);
		return start >= 0 && time >= start && time < end && isAlive();
	}

	/** Whether the aegis blocks hits at {@code time}: from the moment its plates close. */
	private boolean aegisBlocks(long time) {
		int start = entityData.get(AEGIS_START);
		return aegisShown(time) && time >= start + AEGIS_CLOSED;
	}

	/** World bearing (mob yaw convention) of the open segment at {@code time}. */
	public float aegisGapYaw(float time) {
		double turn = WatcherConfig.value("aegis_turn_ticks", 20, 2400);
		return Mth.wrapDegrees((float) (entityData.get(AEGIS_YAW) + 360 * (time - entityData.get(AEGIS_START)) / turn));
	}

	private double cfg(String key, double min, double max) {
		return WatcherConfig.value(key, min, max);
	}

	private double horizontalDistance(Entity entity) {
		double dx = entity.getX() - getX(), dz = entity.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private double healthPercent() {
		return getHealth() / Math.max(1, getMaxHealth()) * 100;
	}

	public Vec3 eyePoint() {
		return position().add(0, EYE, 0);
	}

	private void configure() {
		if (configured) return;
		configured = true;
		setBase(Attributes.MAX_HEALTH, cfg("max_health", 1, 100000));
		setBase(Attributes.ARMOR, cfg("armor", 0, 30));
		setBase(Attributes.ARMOR_TOUGHNESS, cfg("armor_toughness", 0, 20));
		xpReward = (int) cfg("xp_reward", 0, 100000);
		setHealth(loaded ? Math.min(getHealth(), getMaxHealth()) : getMaxHealth());
		if (home == null) home = position();
		if (!loaded) {
			playSound(SoundEvents.BEACON_ACTIVATE, 3.0f, 0.6f);
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.5f);
		}
	}

	private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) instance.setBaseValue(value);
	}

	/** Summoned over an altar: the altar is the centre of the four lenses. */
	public void setHome(Vec3 home) {
		this.home = home;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			clientVisuals();
			return;
		}
		configure();
		bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0, 1));
		if (!isAlive()) return;
		if (tickCount % 20 == 0) updateBossMusic();
		if (illusionCooldown > 0) illusionCooldown--;
		long now = level().getGameTime();
		if (entityData.get(AEGIS_START) >= 0 && now >= entityData.get(AEGIS_END)) dropAegis(false);
		else if (aegisCooldown > 0 && entityData.get(AEGIS_START) < 0) aegisCooldown--;
		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || !canHit(target))) {
			setTarget(null);
			target = null;
		}
		trackHugging(target);
		String action = action();
		if (!action.isEmpty()) {
			int age = entityData.get(AGE) + 1;
			entityData.set(AGE, age);
			setDeltaMovement(Vec3.ZERO);
			holdYaw();
			resolve(action, age);
			if (action.equals(action()) && age >= CLIPS.ticks(action)) finish(action);
			return;
		}
		drift(target);
		if (target != null) faceToward(target.position(), 12);
		if (exposedTicks > 0) {
			exposedTicks--;
			return;
		}
		if (startTransition()) return;
		if (target == null || !hasLineOfSight(target)) return;
		if (hugTicks >= cfg("hug_ticks", 1, 400)) {
			startAttack("teleport", target);
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		chooseAttack(target);
	}

	private void trackHugging(LivingEntity target) {
		if (target != null && horizontalDistance(target) <= cfg("hug_distance", 1, 12)) hugTicks++;
		else hugTicks = 0;
	}

	/** Float at the hover height, 8 to 14 blocks from the target, never past the leash around home. */
	private void drift(LivingEntity target) {
		Vec3 want = home;
		if (target != null) {
			Vec3 away = position().subtract(target.position()).multiply(1, 0, 1);
			if (away.lengthSqr() < 1.0E-4) away = Vec3.directionFromRotation(0, getYRot() + 180);
			double distance = Mth.clamp(horizontalDistance(target), 8, 14);
			want = target.position().add(away.normalize().scale(distance));
		}
		Vec3 offset = want.subtract(home).multiply(1, 0, 1);
		double leash = cfg("leash_radius", 6, 64);
		if (offset.length() > leash) want = home.add(offset.normalize().scale(leash));
		double floor = groundBelow(new Vec3(want.x, Math.max(want.y, getY()) + 3, want.z), 16);
		Vec3 delta = new Vec3(want.x - getX(), floor + cfg("hover_height", 0, 6) - getY(), want.z - getZ());
		double speed = cfg("drift_speed", 0, 0.5);
		Vec3 flat = delta.multiply(1, 0, 1);
		if (flat.length() > speed) flat = flat.normalize().scale(speed);
		if (flat.length() < 0.02) flat = Vec3.ZERO;
		setDeltaMovement(flat.x, Mth.clamp(delta.y * 0.2, -0.12, 0.12), flat.z);
	}

	/** Y of the first standing surface below {@code from}, at most {@code depth} blocks down. */
	private double groundBelow(Vec3 from, int depth) {
		BlockPos.MutableBlockPos pos = BlockPos.containing(from).mutable();
		for (int i = 0; i < depth; i++, pos.move(0, -1, 0)) {
			BlockState state = level().getBlockState(pos);
			if (!state.getCollisionShape(level(), pos).isEmpty()) return pos.getY() + state.getCollisionShape(level(), pos).max(net.minecraft.core.Direction.Axis.Y);
		}
		return from.y - depth;
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
		setDeltaMovement(Vec3.ZERO);
		entityData.set(AGE, 0);
		entityData.set(SERIAL, entityData.get(SERIAL) + 1);
		entityData.set(ACTION, action);
	}

	private void finish(String action) {
		entityData.set(ACTION, "");
		if (TRANSITIONS.contains(action)) {
			// The Last Memory: the aegis rises as soon as the transition ends.
			LivingEntity target = getTarget();
			begin("shield", target != null ? target.position() : null);
			raiseAegis(target);
			return;
		}
		if ("hurt".equals(action)) return;
		double scale = phase() >= 2 ? cfg("phase_two_cooldown_multiplier", 0.1, 2) : 1;
		cooldown = (int) Math.round(cfg("attack_cooldown_ticks", 0, 400) * scale);
		if ("teleport".equals(action) || "shield".equals(action) || "summon".equals(action)) cooldown /= 2;
	}

	/** The phase threshold takes effect only between attacks; the transition is invulnerable and harmless. */
	private boolean startTransition() {
		if (phase() == 1 && healthPercent() <= cfg("phase_two_percent", 1, 99)) {
			setPhase(2);
			begin("awaken_memory", null);
			playSound(SoundEvents.BEACON_POWER_SELECT, 3.0f, 0.5f);
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.4f);
			return true;
		}
		return false;
	}

	private void chooseAttack(LivingEntity target) {
		double distance = horizontalDistance(target);
		if (phase() >= 2 && aegisCooldown <= 0 && entityData.get(AEGIS_START) < 0) {
			startAttack("shield", target);
			return;
		}
		if (phase() >= 2 && illusionCooldown <= 0 && illusions().size() < (int) cfg("illusion_count", 0, 3)) {
			startAttack("summon", target);
			return;
		}
		if (attacksSinceShift >= 3 && !"teleport".equals(lastAttack)) {
			startAttack("teleport", target);
			return;
		}
		List<String> options = new ArrayList<>();
		if (distance <= cfg("volley_range", 4, 64)) options.add("volley");
		if (distance >= 6 && distance <= 22) options.add("beam");
		if (options.size() > 1) options.remove(lastAttack);
		if (!options.isEmpty()) startAttack(options.get(random.nextInt(options.size())), target);
	}

	private void startAttack(String attack, LivingEntity target) {
		begin(attack, target.position());
		setAim(target.position().add(0, target.getBbHeight() * 0.5, 0));
		lastAttack = attack;
		hugTicks = 0;
		switch (attack) {
			case "volley" -> {
				attacksSinceShift++;
				playSound(SoundEvents.BEACON_ACTIVATE, 2.0f, 1.4f);
			}
			case "beam" -> {
				attacksSinceShift++;
				beamHits.clear();
				playSound(SoundEvents.BEACON_AMBIENT, 3.0f, 1.6f);
				playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 0.7f);
			}
			case "teleport" -> {
				attacksSinceShift = 0;
				playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.6f);
			}
			case "summon" -> playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 3.0f, 0.7f);
			case "shield" -> raiseAegis(target);
			default -> {
			}
		}
	}

	private void resolve(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "volley" -> {
				if (target != null && age < VOLLEY_RELEASE) {
					faceToward(target.position(), 10);
					setAim(target.position().add(0, target.getBbHeight() * 0.5, 0));
				}
				if (age == VOLLEY_RELEASE) ringVolley(0);
				if (age == VOLLEY_SECOND && phase() >= 2) ringVolley(0.5);
			}
			case "beam" -> {
				if (target != null && age < BEAM_LOCK) {
					faceToward(target.position(), 8);
					setAim(target.position().add(0, target.getBbHeight() * 0.4, 0));
				}
				if (age == BEAM_LOCK) playSound(SoundEvents.BEACON_POWER_SELECT, 2.5f, 1.5f);
				if (age >= BEAM_START && age < BEAM_END) fireBeam(age);
				if (age == BEAM_END) playSound(SoundEvents.BEACON_DEACTIVATE, 2.0f, 1.4f);
			}
			case "teleport" -> {
				if (age == SHIFT) lensShift(target);
			}
			case "summon" -> {
				if (age == ILLUSIONS) summonIllusions();
			}
			case "awaken_memory" -> {
				if (age == PULSE) {
					// The eye cracks open: everyone is pushed back, nobody is hurt.
					for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6), this::canHit)) {
						Vec3 away = victim.position().subtract(position());
						victim.knockback(1.2, -away.x, -away.z);
						victim.hurtMarked = true;
					}
					playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0f, 0.5f);
					burst(eyePoint(), 40);
				}
			}
			default -> {
			}
		}
	}

	/** A full ring of memory bolts; the first is aimed at the target, each tilted to cross the target's height there. */
	private void ringVolley(double halfStep) {
		if (!(level() instanceof ServerLevel server)) return;
		int count = (int) cfg(phase() >= 2 ? "phase_two_volley_bolts" : "volley_bolts", 3, 48);
		ringVolley(server, this, eyePoint(), aim(), lockedYaw, count, halfStep, (float) cfg("bolt_damage", 0, 1000),
				cfg("bolt_speed", 0.1, 2), cfg("volley_range", 4, 64) + 2);
		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5f, 1.3f);
		playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 1.2f);
	}

	/** Shared with the illusions: {@code count} bolts in a horizontal ring around {@code eye}. */
	static void ringVolley(ServerLevel server, LivingEntity owner, Vec3 eye, Vec3 aim, float yaw, int count, double halfStep,
			float damage, double speed, double range) {
		Vec3 flat = aim.subtract(eye).multiply(1, 0, 1);
		double distance = Math.max(3, flat.length());
		double slope = Mth.clamp((aim.y - eye.y) / distance, -0.8, 0.8);
		float baseYaw = flat.lengthSqr() > 1.0E-4 ? HollowLegion.yawToward(Vec3.ZERO, flat) : yaw;
		for (int i = 0; i < count; i++) {
			float boltYaw = baseYaw + (float) ((i + halfStep) * 360.0 / count);
			Vec3 dir = Vec3.directionFromRotation(0, boltYaw);
			Vec3 motion = new Vec3(dir.x, slope, dir.z);
			MemoryBoltEntity.fire(server, owner, eye.add(dir.scale(1.1)), motion, speed, damage, range);
		}
	}

	/** Yaw offset of the sweep at {@code age}: 0 to +sweep (30-50), then +sweep to -sweep (50-70). */
	private static double sweepOffset(int age, double sweep) {
		if (age <= BEAM_MID) return sweep * (age - BEAM_START) / (double) (BEAM_MID - BEAM_START);
		return sweep - 2 * sweep * (age - BEAM_MID) / (double) (BEAM_END - BEAM_MID);
	}

	/** The beam's direction at {@code age}: the locked aim, turned by the sweep offset. */
	private Vec3 beamDirection(int age) {
		Vec3 base = aim().subtract(eyePoint());
		if (base.lengthSqr() < 1.0E-4) base = Vec3.directionFromRotation(0, lockedYaw);
		base = base.normalize();
		double offset = Math.toRadians(sweepOffset(Mth.clamp(age, BEAM_START, BEAM_END), WatcherConfig.value("beam_sweep_degrees", 0, 90)));
		double cos = Math.cos(offset), sin = Math.sin(offset);
		// Rotating about Y by +offset turns the beam toward higher mob yaw.
		return new Vec3(base.x * cos - base.z * sin, base.y, base.x * sin + base.z * cos);
	}

	/** Where the beam from {@code from} along {@code dir} stops: the first block in the way, or its full length. */
	private Vec3 beamEnd(Vec3 from, Vec3 dir) {
		Vec3 to = from.add(dir.scale(WatcherConfig.value("beam_length", 4, 64)));
		var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
	}

	private void fireBeam(int age) {
		Vec3 from = eyePoint();
		Vec3 to = beamEnd(from, beamDirection(age));
		long now = level().getGameTime();
		long interval = (long) cfg("beam_hit_interval", 1, 100);
		float damage = (float) cfg("beam_damage", 0, 1000);
		AABB box = new AABB(from, to).inflate(1.0);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
			if (victim.getBoundingBox().inflate(0.7).clip(from, to).isEmpty()) continue;
			Long next = beamHits.get(victim.getUUID());
			if (next != null && now < next) continue;
			beamHits.put(victim.getUUID(), now + interval);
			victim.hurt(damageSources().mobAttack(this), damage);
		}
		if (age % 4 == 0 && level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 3, 0.15, 0.15, 0.15, 0.02);
			server.playSound(null, to.x, to.y, to.z, SoundEvents.AMETHYST_BLOCK_HIT, getSoundSource(), 0.8f, 1.6f);
		}
	}

	/** Lens {@code i}'s hover spot: home +- lens_radius due east, west, south or north, over the lens floor. */
	private Vec3 lensSpot(int i) {
		double r = cfg("lens_radius", 3, 24);
		Vec3 column = home.add(LENSES[i][0] * r, 0, LENSES[i][1] * r);
		double floor = groundBelow(column.add(0, 4, 0), 12);
		return new Vec3(column.x, floor + cfg("hover_height", 0, 6), column.z);
	}

	private boolean spotFree(Vec3 spot) {
		AABB box = getType().getAABB(spot.x, spot.y, spot.z);
		if (!level().noCollision(box)) return false;
		return level().getEntitiesOfClass(WatcherIllusionEntity.class, box.inflate(1.5), Entity::isAlive).isEmpty()
				&& position().distanceToSqr(spot) > 9;
	}

	/** Free lens spots, farthest from {@code target} first. */
	private List<Vec3> freeLenses(LivingEntity target) {
		List<Vec3> spots = new ArrayList<>();
		for (int i = 0; i < LENSES.length; i++) {
			Vec3 spot = lensSpot(i);
			if (spotFree(spot)) spots.add(spot);
		}
		if (target != null) spots.sort((a, b) -> Double.compare(b.distanceToSqr(target.position()), a.distanceToSqr(target.position())));
		return spots;
	}

	private boolean sees(Vec3 from, LivingEntity target) {
		Vec3 eye = from.add(0, EYE, 0);
		return level().clip(new ClipContext(eye, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
				.getType() == HitResult.Type.MISS;
	}

	private void lensShift(LivingEntity target) {
		if (!(level() instanceof ServerLevel server)) return;
		Vec3 destination = null;
		List<Vec3> lenses = freeLenses(target);
		for (Vec3 spot : lenses) {
			if (target == null || sees(spot, target)) {
				destination = spot;
				break;
			}
		}
		if (destination == null && !lenses.isEmpty()) destination = lenses.get(0);
		for (int tries = 0; destination == null && tries < 16; tries++) {
			double angle = random.nextDouble() * Math.PI * 2, r = 6 + random.nextDouble() * 4;
			Vec3 column = home.add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
			Vec3 spot = new Vec3(column.x, groundBelow(column.add(0, 4, 0), 12) + cfg("hover_height", 0, 6), column.z);
			if (spotFree(spot)) destination = spot;
		}
		if (destination == null) return;
		Vec3 eye = eyePoint();
		server.sendParticles(ParticleTypes.REVERSE_PORTAL, eye.x, eye.y, eye.z, 40, 0.6, 1.0, 0.6, 0.05);
		server.sendParticles(VIOLET, eye.x, eye.y, eye.z, 20, 0.8, 1.2, 0.8, 0);
		moveTo(destination.x, destination.y, destination.z, getYRot(), 0);
		setDeltaMovement(Vec3.ZERO);
		if (target != null) faceToward(target.position(), 360);
		Vec3 arrived = eyePoint();
		server.sendParticles(ParticleTypes.REVERSE_PORTAL, arrived.x, arrived.y, arrived.z, 40, 0.6, 1.0, 0.6, 0.05);
		server.sendParticles(ParticleTypes.END_ROD, arrived.x, arrived.y, arrived.z, 12, 0.5, 0.8, 0.5, 0.05);
		playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.8f);
	}

	private void summonIllusions() {
		if (!(level() instanceof ServerLevel server)) return;
		int room = Math.max(0, (int) cfg("illusion_count", 0, 3) - illusions().size());
		int made = 0;
		for (Vec3 spot : freeLenses(getTarget())) {
			if (made >= room) break;
			if (WatcherIllusionEntity.conjure(server, this, spot, (int) cfg("illusion_life_ticks", 20, 2400),
					(float) cfg("illusion_bolt_damage", 0, 1000)) != null) made++;
		}
		illusionCooldown = (int) cfg("illusion_cooldown_ticks", 0, 24000);
		playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2.5f, 0.7f);
	}

	public List<WatcherIllusionEntity> illusions() {
		return level().getEntitiesOfClass(WatcherIllusionEntity.class, getBoundingBox().inflate(48),
				e -> e.isAlive() && getUUID().equals(e.ownerId()));
	}

	private void raiseAegis(LivingEntity target) {
		int now = (int) level().getGameTime();
		float yaw = target != null ? HollowLegion.yawToward(position(), target.position()) + 180 : getYRot() + 180;
		entityData.set(AEGIS_YAW, Mth.wrapDegrees(yaw));
		entityData.set(AEGIS_START, now);
		entityData.set(AEGIS_END, now + CLIPS.ticks("shield") + (int) cfg("aegis_ticks", 20, 2400));
		aegisHits = 0;
		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.6f);
		playSound(SoundEvents.BEACON_POWER_SELECT, 2.0f, 0.8f);
	}

	/** The aegis falls: its time ran out, or (shattered) three hits got through the open segment. */
	private void dropAegis(boolean shattered) {
		entityData.set(AEGIS_START, -1);
		entityData.set(AEGIS_END, -1);
		aegisCooldown = (int) cfg("aegis_cooldown_ticks", 0, 24000);
		if (!shattered) {
			playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 2.0f, 0.6f);
			return;
		}
		exposedTicks = (int) cfg("exposed_ticks", 0, 400);
		begin("hurt", null);
		playSound(SoundEvents.GLASS_BREAK, 3.0f, 0.5f);
		playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0f, 0.6f);
		if (level() instanceof ServerLevel server) {
			Vec3 eye = eyePoint();
			server.sendParticles(VIOLET, eye.x, eye.y, eye.z, 50, 1.4, 1.2, 1.4, 0);
			server.sendParticles(ParticleTypes.END_ROD, eye.x, eye.y, eye.z, 20, 1.0, 1.0, 1.0, 0.1);
		}
	}

	/** Whether {@code from} lies within the open segment of the aegis at {@code time}. */
	private boolean throughGap(Vec3 from, long time) {
		Vec3 centre = position().add(0, getBbHeight() / 2, 0);
		Vec3 offset = from.subtract(centre).multiply(1, 0, 1);
		if (offset.lengthSqr() < 1.0E-4) return true;
		float bearing = HollowLegion.yawToward(Vec3.ZERO, offset);
		return Math.abs(Mth.wrapDegrees(bearing - aegisGapYaw(time))) <= cfg("aegis_open_degrees", 5, 180);
	}

	public boolean canHit(LivingEntity entity) {
		return entity != this && ForgottenSouls.victim(entity);
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return super.isAlliedTo(other) || (other != this && ForgottenSouls.isWatcherSide(other));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && canHit(target);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) return super.hurt(source, amount);
		if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FALL)) return false;
		boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (!bypass && TRANSITIONS.contains(action())) return false;
		Entity attacker = source.getEntity();
		if (attacker != null && attacker != this && ForgottenSouls.isWatcherSide(attacker)) return false;
		long now = level().getGameTime();
		if (!bypass && aegisBlocks(now)) {
			Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : attacker != null ? attacker.position() : null;
			if (from == null || !throughGap(from, now)) {
				deflect(from);
				return false;
			}
			if (++aegisHits >= (int) cfg("aegis_break_hits", 1, 20)) dropAegis(true);
		}
		float scale = exposedTicks > 0 ? (float) cfg("exposed_damage_multiplier", 1, 5) : 1;
		return super.hurt(source, amount * scale);
	}

	private void deflect(Vec3 from) {
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, 0.6f + random.nextFloat() * 0.3f);
		playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5f, 1.2f);
		if (from == null || !(level() instanceof ServerLevel server)) return;
		Vec3 centre = position().add(0, getBbHeight() / 2, 0);
		Vec3 offset = from.subtract(centre).multiply(1, 0, 1);
		Vec3 spark = offset.lengthSqr() < 1.0E-4 ? centre : centre.add(offset.normalize().scale(1.4));
		server.sendParticles(PALE, spark.x, spark.y, spark.z, 10, 0.2, 0.5, 0.2, 0);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, spark.x, spark.y, spark.z, 6, 0.2, 0.4, 0.2, 0.2);
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushEntities() {
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

	/** The last memory leaves the crystal: violet and cyan light. */
	private void burst(Vec3 at, int count) {
		if (!(level() instanceof ServerLevel server)) return;
		server.sendParticles(VIOLET, at.x, at.y, at.z, count, 1.0, 1.0, 1.0, 0);
		server.sendParticles(SOUL_LIGHT, at.x, at.y, at.z, count / 2, 1.2, 1.0, 1.2, 0);
		server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, count / 2, 0.6, 0.8, 0.6, 0.12);
	}

	private void particle(ParticleOptions type, Vec3 p, double vy) {
		level().addParticle(type, p.x, p.y, p.z, 0, vy, 0);
	}

	private void ring(Vec3 centre, double radius, int count, double twist, ParticleOptions type) {
		for (int i = 0; i < count; i++) {
			double a = i * Math.PI * 2 / count + twist;
			particle(type, centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 0.0);
		}
	}

	private void clientVisuals() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		int n = Math.max(8, (int) (32 * density));
		int age = actionAge();
		Vec3 eye = eyePoint();
		if (!isAlive()) {
			if (deathTime < LAST_MEMORY && tickCount % 2 == 0) particle(VIOLET, eye.add(random.nextGaussian() * 0.6, -deathTime * 0.05, random.nextGaussian() * 0.6), -0.05);
			return;
		}
		if (tickCount % 8 == 0 && random.nextDouble() < density) {
			particle(tickCount % 16 == 0 ? ParticleTypes.END_ROD : PALE, eye.add(random.nextGaussian() * 0.9, random.nextGaussian() * 0.7, random.nextGaussian() * 0.9), 0.01);
		}
		float time = level().getGameTime() + 0.5f;
		if (aegisShown(time) && tickCount % 3 == 0) {
			// The open segment, marked by a column of light at its bearing.
			Vec3 gap = Vec3.directionFromRotation(0, aegisGapYaw(time)).scale(1.5);
			for (int h = 0; h < 4; h++) particle(ParticleTypes.END_ROD, position().add(gap).add(0, 1.0 + h * 0.6, 0), 0.0);
		}
		switch (action()) {
			case "volley" -> {
				if (age < VOLLEY_RELEASE && tickCount % 2 == 0) ring(eye, 0.4 + 2.2 * age / VOLLEY_RELEASE, n / 2, age * 0.1, VIOLET);
			}
			case "beam" -> {
				if (age < BEAM_START && tickCount % 2 == 0) {
					// Charge: a thin aim line from the eye; it stops following at the lock.
					Vec3 to = aim();
					int steps = (int) Math.min(40, eye.distanceTo(to) * 2);
					for (int i = 1; i <= steps; i += 2) particle(age >= BEAM_LOCK ? PALE : VIOLET, eye.lerp(to, i / (double) steps), 0);
				}
				if (age >= BEAM_START && age < BEAM_END) {
					Vec3 to = beamEnd(eye, beamDirection(age));
					double length = eye.distanceTo(to);
					for (double s = 0.6; s < length; s += 0.45) {
						Vec3 p = eye.lerp(to, s / length);
						particle(((int) (s * 2)) % 3 == 0 ? ParticleTypes.END_ROD : VIOLET, p, 0);
					}
				}
			}
			case "teleport" -> {
				if (age < SHIFT) particle(ParticleTypes.REVERSE_PORTAL, eye.add(random.nextGaussian() * 0.8, random.nextGaussian(), random.nextGaussian() * 0.8), 0);
			}
			case "summon" -> {
				if (age < ILLUSIONS && tickCount % 2 == 0) ring(eye, 1.6, n / 3, -age * 0.15, PALE);
			}
			case "awaken_memory" -> {
				if (tickCount % 3 == 0) for (int h = 0; h < 3; h++) ring(position().add(0, h * 1.1 + 0.4, 0), 1.6 + h * 0.3, n / 2, age * 0.09 + h, h == 1 ? SOUL_LIGHT : VIOLET);
			}
			case "hurt" -> {
				if (tickCount % 2 == 0) particle(ParticleTypes.CRIT, eye.add(random.nextGaussian() * 0.5, random.nextGaussian() * 0.5, random.nextGaussian() * 0.5), 0.05);
			}
			default -> {
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide) {
			entityData.set(ACTION, "");
			entityData.set(AEGIS_START, -1);
			for (WatcherIllusionEntity illusion : illusions()) illusion.shatter();
			stopBossMusic();
			playSound(SoundEvents.BEACON_DEACTIVATE, 3.0f, 0.6f);
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		setDeltaMovement(Vec3.ZERO);
		if (deathTime == LAST_MEMORY && !level().isClientSide) {
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.4f);
			playSound(SoundEvents.BEACON_DEACTIVATE, 3.0f, 0.5f);
			burst(eyePoint().add(0, -0.5, 0), 70);
		}
		if (deathTime >= CLIPS.ticks("death") && !level().isClientSide && !isRemoved()) {
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

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public boolean canChangeDimensions() {
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
		if (!level().isClientSide) {
			stopBossMusic();
			if (reason.shouldDestroy()) for (WatcherIllusionEntity illusion : illusions()) illusion.shatter();
		}
		bar.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BEACON_DEACTIVATE;
	}

	@Override
	public float getVoicePitch() {
		return 0.6f;
	}

	@Override
	protected float getSoundVolume() {
		return 1.6f;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("WatcherPhase", phase());
		tag.putInt("WatcherIllusionCooldown", illusionCooldown);
		tag.putInt("WatcherAegisCooldown", aegisCooldown);
		if (home != null) {
			tag.putDouble("WatcherHomeX", home.x);
			tag.putDouble("WatcherHomeY", home.y);
			tag.putDouble("WatcherHomeZ", home.z);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		loaded = true;
		configured = false;
		setPhase(tag.contains("WatcherPhase") ? tag.getInt("WatcherPhase") : 1);
		illusionCooldown = tag.getInt("WatcherIllusionCooldown");
		aegisCooldown = tag.getInt("WatcherAegisCooldown");
		if (tag.contains("WatcherHomeX")) home = new Vec3(tag.getDouble("WatcherHomeX"), tag.getDouble("WatcherHomeY"), tag.getDouble("WatcherHomeZ"));
		entityData.set(ACTION, "");
		entityData.set(AEGIS_START, -1);
		entityData.set(AEGIS_END, -1);
		cooldown = 40;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "watcher", 3, state -> {
			if (!isAlive()) return state.setAndContinue(RemnantAnims.of(CLIPS, "death"));
			String action = action();
			if (!action.isEmpty() && CLIPS.has(action)) {
				int serial = entityData.get(SERIAL);
				if (serial != seenSerial) {
					seenSerial = serial;
					state.getController().forceAnimationReset();
				}
				return state.setAndContinue(RemnantAnims.of(CLIPS, action));
			}
			return state.setAndContinue(RemnantAnims.of(CLIPS, aegisShown(level().getGameTime()) ? "shielded" : "idle"));
		}));
	}

	/** The eye of the crystal (1.21.1 sets it with the entity type builder's eyeHeight). */
	@Override
	protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
		return (float) EYE;
	}

	/** The Watcher is a soul-bound vessel, undead like the other Remnants (1.21.1 lists it in the minecraft:undead entity tag instead). */
	@Override
	public MobType getMobType() {
		return MobType.UNDEAD;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
