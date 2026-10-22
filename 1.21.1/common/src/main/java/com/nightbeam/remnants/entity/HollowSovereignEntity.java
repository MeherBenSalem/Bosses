package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModSounds;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Hollow Sovereign, last king of Vael. The server owns phases, attack timing and damage; clients
 * draw bounded telegraphs from synced state.
 */
public class HollowSovereignEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SERIAL = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> AIM_X = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_Y = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_Z = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);

	public static final RemnantClips.Sheet CLIPS = RemnantClips.HOLLOW_SOVEREIGN;
	/** Tick of {@code phase_shatter} at which the pauldrons leave the model and the guards rise. */
	public static final int SHATTER_SPAWN_TICK = 36;
	/** Tick of {@code phase_soul} at which the breastplate has burst off the model. */
	public static final int SOUL_BREAK_TICK = 30;
	private static final Set<String> TRANSITIONS = Set.of("awaken", "phase_shatter", "phase_soul");
	private static final double[] LUNGE_EASE = {0, 0.3, 0.55, 0.75, 0.9, 0.97, 1.0};
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(new Vector3f(0.31f, 0.79f, 0.76f), 1.3f);
	private static final DustParticleOptions GILT = new DustParticleOptions(new Vector3f(0.85f, 0.72f, 0.38f), 1.1f);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.remnants.hollow_sovereign"),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> struck = new HashSet<>();
	private final Map<UUID, ArrayDeque<Long>> hitLog = new HashMap<>();
	private final Map<UUID, Float> attackStrength = new HashMap<>();
	private final Set<UUID> musicListeners = new HashSet<>();
	private boolean configured, loaded, lastCommandUsed;
	private int cooldown = 40, summonCooldown, guardBroken, pauldronTimer = 100, nextPauldron, cleaveLength;
	private float lockedYaw;
	private String lastAttack = "";
	private Vec3 lungeFrom = Vec3.ZERO, lungeTo = Vec3.ZERO;
	private int seenSerial = -1;

	public HollowSovereignEntity(EntityType<? extends HollowSovereignEntity> type, Level level) {
		super(type, level);
		this.xpReward = 300;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 720).add(Attributes.ATTACK_DAMAGE, 9)
				.add(Attributes.ARMOR, 12).add(Attributes.ARMOR_TOUGHNESS, 4).add(Attributes.MOVEMENT_SPEED, 0.24)
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
				getLookControl().setLookAt(target, 25, 25);
				if (horizontalDistance(target) > 3.4 || !hasLineOfSight(target)) {
					getNavigation().moveTo(target, phase() >= 2 ? SovereignConfig.value("run_speed_multiplier", 1, 3) : 1.0);
				} else {
					getNavigation().stop();
				}
			}
		});
		goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7));
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
		entityData.set(PHASE, Mth.clamp(phase, 1, 3));
	}

	private Vec3 aim() {
		return new Vec3(entityData.get(AIM_X), entityData.get(AIM_Y), entityData.get(AIM_Z));
	}

	private void setAim(Vec3 at) {
		entityData.set(AIM_X, (float) at.x);
		entityData.set(AIM_Y, (float) at.y);
		entityData.set(AIM_Z, (float) at.z);
	}

	/** Whether the model still shows the pauldrons (hidden once they fly off in phase 2). */
	public boolean showsPauldrons() {
		return phase() < 2 || ("phase_shatter".equals(action()) && actionAge() < SHATTER_SPAWN_TICK);
	}

	/** Whether the model still shows the breastplate (hidden once the soul bursts out in phase 3). */
	public boolean showsBreastplate() {
		return phase() < 3 || ("phase_soul".equals(action()) && actionAge() < SOUL_BREAK_TICK);
	}

	private double cfg(String key, double min, double max) {
		return SovereignConfig.value(key, min, max);
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
			begin("awaken", null);
			playSound(ModSounds.SOVEREIGN_TOLL.get(), 3.0f, 0.8f);
		}
	}

	private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) instance.setBaseValue(value);
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
		snapshotAttackStrength();
		if (guardBroken > 0) guardBroken--;
		if (summonCooldown > 0) summonCooldown--;
		tickPauldrons();
		String action = action();
		if (!action.isEmpty()) {
			int age = entityData.get(AGE) + 1;
			entityData.set(AGE, age);
			getNavigation().stop();
			setDeltaMovement(0, getDeltaMovement().y, 0);
			holdYaw();
			resolve(action, age);
			if (action.equals(action()) && age >= CLIPS.ticks(action)) finish(action);
			return;
		}
		if (startTransition()) return;
		if (phase() >= 3 && !lastCommandUsed && healthPercent() <= cfg("last_command_percent", 0, 100)) {
			lastCommandUsed = true;
			begin("last_command", null);
			playSound(ModSounds.SOVEREIGN_TOLL.get(), 3.0f, 0.7f);
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && hasLineOfSight(target)) chooseAttack(target);
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
		double scale = switch (phase()) {
			case 2 -> cfg("phase_two_cooldown_multiplier", 0.1, 2);
			case 3 -> cfg("phase_three_cooldown_multiplier", 0.1, 2);
			default -> 1;
		};
		cooldown = (int) Math.round(cfg("attack_cooldown_ticks", 0, 400) * scale);
	}

	/** Phase thresholds take effect only between attacks; a transition is invulnerable and harmless. */
	private boolean startTransition() {
		double percent = healthPercent();
		double two = cfg("phase_two_percent", 1, 99);
		double three = Math.min(cfg("phase_three_percent", 0, 98), two - 1);
		if (phase() == 1 && percent <= two) {
			setPhase(2);
			begin("phase_shatter", null);
			playSound(SoundEvents.ANVIL_BREAK, 2.0f, 0.5f);
			return true;
		}
		if (phase() == 2 && percent <= three) {
			setPhase(3);
			discardPauldrons();
			begin("phase_soul", null);
			playSound(ModSounds.SOUL_RELEASE.get(), 3.0f, 1.0f);
			return true;
		}
		return false;
	}

	private void chooseAttack(LivingEntity target) {
		double distance = horizontalDistance(target);
		int phase = phase();
		boolean pressed = recentHits(target) >= 3;
		if (distance <= 4 && !"guard".equals(lastAttack) && (pressed || random.nextFloat() < 0.2f)) {
			startAttack("guard", target);
			return;
		}
		if (phase >= 2 && summonCooldown <= 0 && !"summon".equals(lastAttack) && guards().size() < 2) {
			startAttack("summon", target);
			return;
		}
		List<String> options = new ArrayList<>();
		if (distance <= 4.5) options.add("combo");
		if (distance <= 7) options.add("throne_slam");
		if (distance >= 5 && distance <= 14) options.add("rift_cleave");
		if (phase >= 3 && distance >= 5 && distance <= 18) options.add("spectral_slash");
		if (phase >= 3 && distance >= 6 && distance <= 14) options.add("soul_lunge");
		options.remove(lastAttack);
		if (!options.isEmpty()) startAttack(options.get(random.nextInt(options.size())), target);
	}

	private void startAttack(String attack, LivingEntity target) {
		begin(attack, target.position());
		setAim(target.position());
		lastAttack = attack;
		cleaveLength = 0;
		switch (attack) {
			case "combo" -> playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2.0f, 0.6f);
			case "throne_slam" -> playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0f, 0.5f);
			case "guard" -> playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2.0f, 0.45f);
			case "rift_cleave" -> playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0f, 0.6f);
			case "summon" -> playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.6f);
			case "spectral_slash" -> playSound(SoundEvents.SOUL_ESCAPE.value(), 2.5f, 0.6f);
			case "soul_lunge" -> playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.2f, 1.4f);
			default -> {
			}
		}
	}

	private void resolve(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "combo" -> {
				if (target != null && (age < 12 || (age > 16 && age < 24) || (age > 28 && age < 36))) faceToward(target.position(), 8);
				if (age == 14 || age == 26 || age == 38) {
					struck.clear();
					double damage = cfg("combo_damage", 0, 1000) * (age == 38 ? cfg("combo_finisher_multiplier", 0, 10) : 1);
					for (LivingEntity hit : HollowLegion.arc(this, 4, 100, this::canHit)) strike(hit, (float) damage, age == 38 ? 0.9 : 0.4);
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, age == 38 ? 0.5f : 0.7f);
				}
			}
			case "throne_slam" -> {
				if (age == 22) playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.6f);
				if (age >= 22 && age <= 34) {
					double radius = slamRadius(age);
					double ground = cfg("slam_ground_height", 0, 4);
					AABB box = getBoundingBox().inflate(radius + 1.5, 2, radius + 1.5);
					for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
						double d = horizontalDistance(victim);
						double lift = victim.getY() - getY();
						if (Math.abs(d - radius) < 1.0 && lift < ground && lift > -1.5) {
							strike(victim, (float) cfg("slam_damage", 0, 1000), 0.6);
						}
					}
				}
			}
			case "riposte" -> {
				if (age == 6) {
					Vec3 forward = Vec3.directionFromRotation(0, getYRot());
					for (LivingEntity hit : HollowLegion.line(this, position(), forward, 3 + getBbWidth() / 2, 0.8, this::canHit)) {
						strike(hit, (float) cfg("riposte_damage", 0, 1000), 0.8);
					}
					playSound(SoundEvents.PLAYER_ATTACK_STRONG, 2.0f, 0.6f);
				}
			}
			case "rift_cleave" -> {
				if (age < 10 && target != null) {
					faceToward(target.position(), 12);
					setAim(target.position());
				}
				if (age >= 18 && age < 18 + (int) cfg("cleave_length", 1, 32)) cleaveStep(age - 17);
			}
			case "summon" -> {
				if (age == 24) {
					spawnGuards();
					summonCooldown = (int) cfg("summon_cooldown_ticks", 0, 24000);
				}
			}
			case "spectral_slash" -> {
				if (age < 14 && target != null) {
					faceToward(target.position(), 15);
					setAim(target.position());
				}
				if (age == 16) releaseCrescents();
			}
			case "soul_lunge" -> soulLunge(age, target);
			case "last_command" -> {
				if (age == 30 || age == 50 || age == 70) markWave();
			}
			case "phase_shatter" -> {
				if (age == 30) pulse();
				if (age == SHATTER_SPAWN_TICK) {
					spawnPauldrons();
					spawnGuards();
					summonCooldown = (int) cfg("summon_cooldown_ticks", 0, 24000);
					playSound(ModSounds.PAULDRON_HUM.get(), 2.0f, 0.8f);
				}
			}
			case "phase_soul" -> {
				if (age == SOUL_BREAK_TICK && level() instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 2.6, getZ(), 40, 0.5, 0.5, 0.5, 0.08);
					playSound(SoundEvents.ANVIL_DESTROY, 1.5f, 0.6f);
				}
			}
			default -> {
			}
		}
	}

	private static double slamRadius(int age, double max) {
		return Math.max(0, age - 22) * max / 12.0;
	}

	private double slamRadius(int age) {
		return slamRadius(age, cfg("slam_radius", 1, 24));
	}

	/** Wave front advances one block per tick; it stops at the first wall. */
	private void cleaveStep(int front) {
		if (cleaveLength < 0) return;
		Vec3 dir = aim().subtract(position()).multiply(1, 0, 1);
		if (dir.lengthSqr() < 1.0E-4) dir = Vec3.directionFromRotation(0, getYRot());
		dir = dir.normalize();
		Vec3 origin = position().add(dir.scale(getBbWidth() / 2));
		Vec3 point = origin.add(dir.scale(front));
		BlockPos at = BlockPos.containing(point.x, getY() + 0.5, point.z);
		if (!level().getBlockState(at).getCollisionShape(level(), at).isEmpty()) {
			cleaveLength = -1;
			return;
		}
		cleaveLength = front;
		if (front == 1) playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.3f);
		for (LivingEntity hit : HollowLegion.line(this, origin.add(dir.scale(front - 1)), dir, 1.0, 0.75, this::canHit)) {
			strike(hit, (float) cfg("cleave_damage", 0, 1000), 0.5);
		}
	}

	private void releaseCrescents() {
		if (!(level() instanceof ServerLevel server)) return;
		Vec3 toward = aim().subtract(position()).multiply(1, 0, 1);
		float base = toward.lengthSqr() < 1.0E-4 ? getYRot() : HollowLegion.yawToward(Vec3.ZERO, toward);
		Vec3 origin = position().add(0, 1.2, 0);
		float damage = (float) cfg("spectral_slash_damage", 0, 1000);
		double speed = cfg("crescent_speed", 0.2, 4);
		for (int i = -1; i <= 1; i++) {
			Vec3 dir = Vec3.directionFromRotation(0, base + i * 15);
			SpectralCrescentEntity.fire(server, this, origin.add(dir.scale(1.4)), dir, speed, damage, 8, 20);
		}
		playSound(ModSounds.SPECTRAL_SLASH.get(), 2.5f, 0.8f);
	}

	private void soulLunge(int age, LivingEntity target) {
		if (age < 12) {
			if (target != null) {
				faceToward(target.position(), 15);
				setAim(target.position());
			}
			return;
		}
		if (age == 12) {
			Vec3 goal = target != null ? target.position() : aim();
			setAim(goal);
			Vec3 path = goal.subtract(position()).multiply(1, 0, 1);
			double reach = Math.min(cfg("soul_lunge_distance", 0, 24), Math.max(0, path.length() - 1.5));
			Vec3 dir = path.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : path.normalize();
			double clear = 0;
			for (double s = 0.25; s <= reach; s += 0.25) {
				if (!level().noCollision(this, getBoundingBox().move(dir.scale(s)))) break;
				clear = s;
			}
			lungeFrom = position();
			lungeTo = position().add(dir.scale(clear));
			faceToward(lungeTo.add(dir), 360);
			playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.6f);
		}
		int step = age - 12;
		if (step >= 0 && step < LUNGE_EASE.length) {
			Vec3 p = lungeFrom.lerp(lungeTo, LUNGE_EASE[step]);
			setPos(p.x, getY(), p.z);
			setDeltaMovement(0, getDeltaMovement().y, 0);
		}
		if (age == 16) {
			AABB box = getBoundingBox().inflate(2, 1, 2);
			for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
				if (horizontalDistance(victim) - getBbWidth() / 2 <= 2) strike(victim, (float) cfg("soul_lunge_damage", 0, 1000), 1.0);
			}
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 2.0f, 0.5f);
		}
	}

	private void markWave() {
		if (!(level() instanceof ServerLevel server)) return;
		float radius = (float) cfg("last_command_radius", 0.5, 8);
		float damage = (float) cfg("last_command_damage", 0, 1000);
		for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32), HollowLegion::sovereignVictim)) {
			if (player.distanceToSqr(this) > 32 * 32) continue;
			place(server, floorBelow(player.position()), radius, damage);
			for (int i = 0; i < 2; i++) {
				double angle = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 4;
				place(server, floorBelow(player.position().add(Math.cos(angle) * r, 2, Math.sin(angle) * r)), radius, damage);
			}
		}
		playSound(ModSounds.SOVEREIGN_TOLL.get(), 3.0f, 1.0f);
	}

	private void place(ServerLevel server, Vec3 at, float radius, float damage) {
		if (at != null) RoyalBladeMarkEntity.place(server, this, at, radius, damage);
	}

	/** First standing surface at or below {@code from}, within 8 blocks. */
	private Vec3 floorBelow(Vec3 from) {
		BlockPos.MutableBlockPos pos = BlockPos.containing(from).mutable();
		for (int i = 0; i < 9; i++) {
			BlockPos below = pos.below();
			if (level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()
					&& !level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
				return new Vec3(from.x, pos.getY(), from.z);
			}
			pos.move(0, -1, 0);
		}
		return null;
	}

	/** Phase-two shockwave: pushes everyone back and deals no damage. */
	private void pulse() {
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7), this::canHit)) {
			Vec3 away = victim.position().subtract(position());
			victim.knockback(1.4, -away.x, -away.z);
			victim.hurtMarked = true;
		}
		playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.5f);
	}

	private void spawnPauldrons() {
		if (!(level() instanceof ServerLevel server)) return;
		for (int side = 0; side < 2; side++) {
			Vec3 shoulder = position().add(Vec3.directionFromRotation(0, getYRot() + (side == 0 ? -90 : 90)).scale(1.2)).add(0, 3.2, 0);
			SovereignPauldronEntity.spawn(server, this, side, shoulder);
		}
		pauldronTimer = 60;
	}

	private void spawnGuards() {
		if (!(level() instanceof ServerLevel server)) return;
		for (int side = -1; side <= 1; side += 2) {
			Vec3 spot = position().add(Vec3.directionFromRotation(0, getYRot() + side * 90).scale(2.6));
			Vec3 ground = guardSpot(spot);
			SpectralRoyalGuardEntity.forBoss(server, this, ground == null ? position() : ground);
		}
	}

	private Vec3 guardSpot(Vec3 around) {
		var type = com.nightbeam.remnants.init.ModEntities.SPECTRAL_ROYAL_GUARD.get();
		for (int dy = 2; dy >= -3; dy--) {
			Vec3 p = new Vec3(around.x, Math.floor(getY()) + dy, around.z);
			BlockPos below = BlockPos.containing(p).below();
			if (!level().getBlockState(below).getCollisionShape(level(), below).isEmpty() && level().noCollision(type.getSpawnAABB(p.x, p.y, p.z))) {
				return p;
			}
		}
		return null;
	}

	private List<SpectralRoyalGuardEntity> guards() {
		return level().getEntitiesOfClass(SpectralRoyalGuardEntity.class, getBoundingBox().inflate(40),
				g -> g.isAlive() && getUUID().equals(g.ownerId()));
	}

	private List<SovereignPauldronEntity> pauldrons() {
		return level().getEntitiesOfClass(SovereignPauldronEntity.class, getBoundingBox().inflate(40),
				p -> p.isAlive() && p.ownedBy(this));
	}

	private void discardPauldrons() {
		for (SovereignPauldronEntity pauldron : pauldrons()) {
			if (level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.SOUL, pauldron.getX(), pauldron.getY() + 0.4, pauldron.getZ(), 12, 0.3, 0.3, 0.3, 0.03);
			}
			pauldron.discard();
		}
	}

	/** Dives are scheduled here so that only one pauldron is ever telegraphing or diving. */
	private void tickPauldrons() {
		if (phase() != 2 || TRANSITIONS.contains(action()) || --pauldronTimer > 0) return;
		List<SovereignPauldronEntity> pauldrons = pauldrons();
		LivingEntity target = getTarget();
		if (pauldrons.isEmpty() || target == null || !target.isAlive() || !canHit(target)) {
			pauldronTimer = 20;
			return;
		}
		if (pauldrons.stream().anyMatch(SovereignPauldronEntity::busy)) {
			pauldronTimer = 10;
			return;
		}
		SovereignPauldronEntity next = pauldrons.get(nextPauldron++ % pauldrons.size());
		next.beginDive(target);
		int interval = 140 + random.nextInt(41);
		pauldronTimer = pauldrons.size() >= 2 ? interval / 2 : interval;
	}

	public void onPauldronDestroyed(SovereignPauldronEntity pauldron) {
		String action = action();
		if (!isAlive() || TRANSITIONS.contains(action) || "last_command".equals(action) || "stagger".equals(action)) return;
		begin("stagger", null);
		playSound(SoundEvents.IRON_GOLEM_DAMAGE, 2.0f, 0.5f);
	}

	public boolean canHit(LivingEntity entity) {
		return entity != this && HollowLegion.sovereignVictim(entity);
	}

	private void strike(LivingEntity target, float damage, double knockback) {
		if (!struck.add(target.getUUID())) return;
		if (target.hurt(damageSources().mobAttack(this), damage) && knockback > 0) {
			target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ());
		}
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return super.isAlliedTo(other) || (other != this && HollowLegion.isSovereignSide(other));
	}

	private static boolean isMelee(DamageSource source) {
		return !source.is(DamageTypeTags.IS_PROJECTILE) && source.getEntity() instanceof LivingEntity
				&& source.getDirectEntity() == source.getEntity();
	}

	private void snapshotAttackStrength() {
		attackStrength.clear();
		for (Player player : level().players()) {
			// Packets run between ticks, after the player's own tick: 1.5 mirrors Player.attack's 0.5.
			if (player.distanceToSqr(this) < 144) attackStrength.put(player.getUUID(), player.getAttackStrengthScale(1.5f));
		}
	}

	private int recentHits(LivingEntity attacker) {
		ArrayDeque<Long> hits = hitLog.get(attacker.getUUID());
		if (hits == null) return 0;
		long now = level().getGameTime();
		while (!hits.isEmpty() && now - hits.peekFirst() > 60) hits.pollFirst();
		return hits.size();
	}

	private boolean breaksGuard(LivingEntity attacker) {
		if (!attacker.getMainHandItem().is(ItemTags.AXES)) return false;
		return !(attacker instanceof Player player) || attackStrength.getOrDefault(player.getUUID(), 0f) >= 0.9f;
	}

	private void guardFlash(Vec3 toward) {
		playSound(ModSounds.SOVEREIGN_GUARD.get(), 2.0f, 1.0f);
		if (level() instanceof ServerLevel server) {
			Vec3 p = position().add(toward.subtract(position()).multiply(1, 0, 1).normalize().scale(1.1)).add(0, 2.2, 0);
			server.sendParticles(GILT, p.x, p.y, p.z, 10, 0.3, 0.3, 0.3, 0);
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 8, 0.25, 0.25, 0.25, 0.2);
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) return super.hurt(source, amount);
		String action = action();
		boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (!bypass && TRANSITIONS.contains(action)) return false;
		Entity attacker = source.getEntity();
		if (attacker != null && attacker != this && HollowLegion.isSovereignSide(attacker)) return false;
		int age = actionAge();
		if (!bypass && "guard".equals(action) && age >= 4 && age <= 32) {
			Vec3 from = source.getSourcePosition() == null ? getEyePosition() : source.getSourcePosition();
			if (source.is(DamageTypeTags.IS_PROJECTILE)) {
				guardFlash(from);
				return false;
			}
			if (isMelee(source) && attacker instanceof LivingEntity living) {
				if (breaksGuard(living)) {
					guardBroken = 40;
					begin("stagger", null);
					playSound(SoundEvents.SHIELD_BREAK, 2.0f, 0.5f);
					guardFlash(from);
				} else {
					guardFlash(from);
					begin("riposte", living.position());
					playSound(ModSounds.SOVEREIGN_GUARD.get(), 2.0f, 0.8f);
					return false;
				}
			}
		}
		if (attacker instanceof LivingEntity) {
			hitLog.computeIfAbsent(attacker.getUUID(), id -> new ArrayDeque<>()).addLast(level().getGameTime());
		}
		float scale = 1;
		if (guardBroken > 0) scale *= (float) cfg("guard_break_multiplier", 1, 5);
		if ("last_command".equals(action)) scale *= (float) cfg("kneel_damage_multiplier", 1, 5);
		return super.hurt(source, amount * scale);
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
		int n = Math.max(8, (int) (40 * density));
		int age = actionAge();
		Vec3 feet = position().add(0, 0.1, 0);
		if (!isAlive()) {
			if (deathTime < 70 && tickCount % 2 == 0) particle(ParticleTypes.SOUL, position().add(random.nextGaussian() * 0.4, 2.4 + deathTime * 0.03, random.nextGaussian() * 0.4), 0.05);
			return;
		}
		if (tickCount % (phase() >= 3 ? 2 : 6) == 0) {
			particle(phase() >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, position().add(random.nextGaussian() * 0.2, 2.6, random.nextGaussian() * 0.2), 0.02);
		}
		Vec3 forward = Vec3.directionFromRotation(0, yBodyRot);
		switch (action()) {
			case "awaken", "phase_soul", "phase_shatter" -> {
				if (tickCount % 3 == 0) for (int h = 0; h < 3; h++) ring(position().add(0, h * 1.2 + 0.2, 0), 2.2 + h * 0.2, n / 2, age * 0.08 + h, SOUL_LIGHT);
				if ("phase_shatter".equals(action()) && age == 30) ring(feet, 4, n * 2, 0, ParticleTypes.SOUL_FIRE_FLAME);
			}
			case "combo" -> {
				if ((age >= 10 && age <= 14) || (age >= 22 && age <= 26) || (age >= 34 && age <= 38)) {
					for (int i = 0; i < n / 2; i++) {
						double a = Math.toRadians(yBodyRot) + (i / (double) (n / 2) - 0.5) * Math.toRadians(100);
						particle(i % 3 == 0 ? GILT : SOUL_LIGHT, position().add(-Math.sin(a) * 3.2, 1.6 + i * 0.02, Math.cos(a) * 3.2), 0);
					}
				}
			}
			case "throne_slam" -> {
				double max = SovereignConfig.value("slam_radius", 1, 24);
				if (age < 22) {
					if (tickCount % 3 == 0) ring(feet, max, n * 2, tickCount * 0.01, ParticleTypes.SOUL);
					ring(feet, Math.max(0.2, (22 - age) * 0.15), n / 3, age * 0.2, SOUL_LIGHT);
				} else if (age <= 34) {
					double r = slamRadius(age, max);
					ring(feet, r, n * 2, age * 0.1, ParticleTypes.SOUL_FIRE_FLAME);
					ring(feet.add(0, 0.6, 0), r, n, age * 0.1, SOUL_LIGHT);
				}
			}
			case "guard" -> {
				if (age >= 4 && age <= 32 && tickCount % 2 == 0) {
					Vec3 p = position().add(forward.scale(1.1)).add(0, 2.2, 0);
					particle(GILT, p.add(random.nextGaussian() * 0.3, random.nextGaussian() * 0.3, random.nextGaussian() * 0.3), 0);
				}
			}
			case "rift_cleave" -> {
				Vec3 dir = aim().subtract(position()).multiply(1, 0, 1);
				if (dir.lengthSqr() < 1.0E-4) dir = forward;
				dir = dir.normalize();
				Vec3 origin = feet.add(dir.scale(getBbWidth() / 2));
				int length = (int) SovereignConfig.value("cleave_length", 1, 32);
				if (age < 18 && tickCount % 2 == 0) {
					for (int i = 1; i <= length; i++) particle(i % 2 == 0 ? SOUL_LIGHT : ParticleTypes.SOUL, origin.add(dir.scale(i)), 0);
				} else if (age >= 18 && age < 18 + length) {
					Vec3 front = origin.add(dir.scale(age - 17));
					Vec3 side = new Vec3(-dir.z, 0, dir.x);
					BlockPos at = BlockPos.containing(front.x, getY() + 0.5, front.z);
					if (level().getBlockState(at).getCollisionShape(level(), at).isEmpty()) {
						for (int i = -2; i <= 2; i++) {
							particle(ParticleTypes.SOUL_FIRE_FLAME, front.add(side.scale(i * 0.35)), 0.15);
							particle(SOUL_LIGHT, front.add(side.scale(i * 0.35)).add(0, 0.5, 0), 0.05);
						}
					}
				}
			}
			case "summon" -> {
				if (age < 24 && tickCount % 2 == 0) {
					for (int side = -1; side <= 1; side += 2) {
						Vec3 spot = position().add(Vec3.directionFromRotation(0, yBodyRot + side * 90).scale(2.6));
						ring(spot.add(0, 0.1, 0), 0.7, n / 4, age * 0.2, ParticleTypes.SOUL);
					}
				}
			}
			case "spectral_slash" -> {
				if (age < 16) particle(SOUL_LIGHT, position().add(forward.scale(1.2)).add(random.nextGaussian() * 0.4, 1.4 + random.nextDouble() * 1.5, random.nextGaussian() * 0.4), 0);
			}
			case "soul_lunge" -> {
				if (age < 12) ring(position().add(0, 1.8, 0), (12 - age) * 0.18, n / 3, age * 0.3, SOUL_LIGHT);
				else if (age <= 18) {
					for (int i = 0; i < Math.max(3, n / 4); i++) {
						particle(i % 2 == 0 ? ParticleTypes.SOUL : SOUL_LIGHT, position().add(random.nextGaussian() * 0.4, random.nextDouble() * 3.6, random.nextGaussian() * 0.4), 0);
					}
				}
			}
			case "last_command" -> {
				if (tickCount % 4 == 0) ring(feet, 3.2, n, -tickCount * 0.03, GILT);
			}
			case "stagger" -> {
				if (tickCount % 3 == 0) particle(ParticleTypes.CRIT, position().add(random.nextGaussian() * 0.5, 3.2, random.nextGaussian() * 0.5), 0.05);
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
			discardPauldrons();
			playSound(ModSounds.SOUL_RELEASE.get(), 3.0f, 0.8f);
			stopBossMusic();
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		getNavigation().stop();
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

	/** Never despawns, Peaceful included (it cannot hurt players there). */
	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
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
			if (reason.shouldDestroy()) discardPauldrons();
		}
		bar.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.IRON_GOLEM_STEP;
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
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return isDeadOrDying() ? 0.7f : 0.6f;
	}

	@Override
	protected float getSoundVolume() {
		return 1.6f;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		// Mid-shatter saves keep phase 1 so the transition (and its pauldrons) replays on load.
		int phase = "phase_shatter".equals(action()) && actionAge() < SHATTER_SPAWN_TICK ? 1 : phase();
		tag.putInt("SovereignPhase", phase);
		tag.putBoolean("SovereignLastCommand", lastCommandUsed);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		loaded = true;
		configured = false;
		// 2.x saves only knew SovereignEnraged; enraged bosses resume in phase 2.
		setPhase(tag.contains("SovereignPhase") ? tag.getInt("SovereignPhase") : tag.getBoolean("SovereignEnraged") ? 2 : 1);
		lastCommandUsed = tag.getBoolean("SovereignLastCommand");
		entityData.set(ACTION, "");
		cooldown = 40;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "sovereign", 3, state -> {
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
			String move = state.isMoving() ? (phase() >= 2 ? "run" : "walk") : "idle";
			return state.setAndContinue(RemnantAnims.of(CLIPS, move));
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
