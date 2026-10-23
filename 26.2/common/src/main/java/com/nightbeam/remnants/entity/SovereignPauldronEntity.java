package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.util.ARGB;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One of the Sovereign's two flying pauldrons (phase 2). It orbits the boss, and when the boss
 * schedules it, telegraphs for 20 ticks, dives in a straight line at a locked position and returns.
 */
public class SovereignPauldronEntity extends Mob implements GeoEntity, Enemy {
	public static final int ORBIT = 0, TELEGRAPH = 1, DIVE = 2, RETURN = 3;
	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(SovereignPauldronEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SIDE = SynchedEntityData.defineId(SovereignPauldronEntity.class, EntityDataSerializers.INT);
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.31f, 0.79f, 0.76f), 1.0f);
	private static final double ORBIT_RADIUS = 3.5, ORBIT_HEIGHT = 3.2, ORBIT_PERIOD = 160, DIVE_SPEED = 1.2, DIVE_RANGE = 14;
	private static final int TELEGRAPH_TICKS = RemnantClips.SOVEREIGN_PAULDRON.ticks("telegraph");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final Set<UUID> struck = new HashSet<>();
	private UUID ownerId;
	private HollowSovereignEntity cachedOwner;
	private LivingEntity diveTarget;
	private Vec3 diveDirection = Vec3.ZERO;
	private double travelled;
	private int stateTicks, orphanTicks;
	private boolean configured;

	public SovereignPauldronEntity(EntityType<? extends SovereignPauldronEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.noPhysics = true;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ARMOR, 6)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 32);
	}

	public static SovereignPauldronEntity spawn(ServerLevel level, HollowSovereignEntity owner, int side, Vec3 at) {
		SovereignPauldronEntity pauldron = ModEntities.SOVEREIGN_PAULDRON.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (pauldron == null) return null;
		pauldron.ownerId = owner.getUUID();
		pauldron.cachedOwner = owner;
		pauldron.entityData.set(SIDE, side & 1);
		pauldron.snapTo(at.x, at.y, at.z, owner.getYRot(), 0);
		pauldron.setPersistenceRequired();
		level.addFreshEntity(pauldron);
		return pauldron;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATE, ORBIT);
		builder.define(SIDE, 0);
	}

	public int state() {
		return entityData.get(STATE);
	}

	public boolean busy() {
		return state() != ORBIT;
	}

	public boolean ownedBy(HollowSovereignEntity boss) {
		return boss.getUUID().equals(ownerId);
	}

	private void setState(int state) {
		entityData.set(STATE, state);
		stateTicks = 0;
	}

	private HollowSovereignEntity owner() {
		if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
		cachedOwner = null;
		if (ownerId != null && level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof HollowSovereignEntity boss) {
			cachedOwner = boss;
		}
		return cachedOwner;
	}

	/** Called by the boss; true when the dive telegraph started. */
	public boolean beginDive(LivingEntity target) {
		if (busy() || !isAlive()) return false;
		diveTarget = target;
		setState(TELEGRAPH);
		playSound(SoundEvents.TRIDENT_RETURN, 1.4f, 0.6f);
		playSound(ModSounds.PAULDRON_HUM.get(), 1.2f, 1.0f);
		return true;
	}

	private Vec3 orbitPoint(HollowSovereignEntity boss) {
		double angle = level().getGameTime() * (Math.PI * 2 / ORBIT_PERIOD) + entityData.get(SIDE) * Math.PI;
		return boss.position().add(Math.cos(angle) * ORBIT_RADIUS, ORBIT_HEIGHT, Math.sin(angle) * ORBIT_RADIUS);
	}

	@Override
	public void tick() {
		setDeltaMovement(Vec3.ZERO);
		super.tick();
		if (level().isClientSide()) {
			clientVisuals();
			return;
		}
		if (!configured) {
			configured = true;
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(SovereignConfig.value("pauldron_health", 1, 1000));
			if (tickCount <= 1) setHealth(getMaxHealth());
		}
		if (!isAlive()) return;
		HollowSovereignEntity boss = owner();
		if (boss == null) {
			if (++orphanTicks > 60) discard();
			return;
		}
		orphanTicks = 0;
		if (!boss.isAlive() || boss.phase() >= 3) {
			shatter();
			return;
		}
		stateTicks++;
		switch (state()) {
			case ORBIT -> glide(orbitPoint(boss), 0.9);
			case TELEGRAPH -> {
				glide(orbitPoint(boss), 0.9);
				if (stateTicks >= TELEGRAPH_TICKS) {
					LivingEntity target = diveTarget;
					if (target == null || !target.isAlive()) {
						setState(RETURN);
						return;
					}
					Vec3 lock = target.position().add(0, target.getBbHeight() * 0.5, 0);
					Vec3 path = lock.subtract(position());
					diveDirection = path.lengthSqr() < 1.0E-4 ? Vec3.ZERO : path.normalize();
					travelled = 0;
					struck.clear();
					setState(DIVE);
				}
			}
			case DIVE -> dive();
			case RETURN -> {
				if (glide(orbitPoint(boss), 0.9) < 0.8 || stateTicks > 100) setState(ORBIT);
			}
			default -> setState(ORBIT);
		}
		if (state() != DIVE) setYRot(boss.getYRot());
		setYBodyRot(getYRot());
		setYHeadRot(getYRot());
	}

	/** Moves toward {@code point} at most {@code speed} blocks; returns the distance left. */
	private double glide(Vec3 point, double speed) {
		Vec3 offset = point.subtract(position());
		double distance = offset.length();
		if (distance <= speed) setPos(point.x, point.y, point.z);
		else {
			Vec3 step = offset.scale(speed / distance);
			setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
		}
		return Math.max(0, distance - speed);
	}

	private void dive() {
		Vec3 from = position().add(0, getBbHeight() / 2, 0);
		Vec3 to = from.add(diveDirection.scale(DIVE_SPEED));
		var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		boolean blocked = hit.getType() != HitResult.Type.MISS;
		if (blocked) to = hit.getLocation().subtract(diveDirection.scale(0.3));
		AABB sweep = getBoundingBox().expandTowards(to.subtract(from)).inflate(0.35);
		float damage = (float) SovereignConfig.value("pauldron_dive_damage", 0, 1000);
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, sweep, e -> e != this && HollowLegion.sovereignVictim(e))) {
			if (struck.add(victim.getUUID()) && Damage.hurt(victim, damageSources().mobAttack(this), damage)) {
				victim.knockback(0.7, -diveDirection.x, -diveDirection.z, damageSources().generic(), 0f);
			}
		}
		setPos(to.x, to.y - getBbHeight() / 2, to.z);
		setYRot(HollowLegion.yawToward(Vec3.ZERO, diveDirection));
		travelled += DIVE_SPEED;
		if (blocked || travelled >= DIVE_RANGE || diveDirection == Vec3.ZERO) {
			if (blocked && level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 10, 0.2, 0.2, 0.2, 0.3);
				playSound(SoundEvents.ANVIL_LAND, 0.7f, 1.4f);
			}
			setState(RETURN);
		}
	}

	private void shatter() {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.4, getZ(), 12, 0.3, 0.3, 0.3, 0.03);
		}
		discard();
	}

	private void clientVisuals() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || !isAlive()) return;
		int state = state();
		if (state == TELEGRAPH) {
			for (int i = 0; i < Math.max(1, (int) (3 * density)); i++) {
				level().addParticle(i % 2 == 0 ? ParticleTypes.SOUL : SOUL_LIGHT, getRandomX(0.8), getY() + random.nextDouble() * 0.8, getRandomZ(0.8), 0, 0.02, 0);
			}
		} else if (state == DIVE) {
			level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.4, getZ(), 0, 0, 0);
			level().addParticle(SOUL_LIGHT, getRandomX(0.5), getY() + 0.4, getRandomZ(0.5), 0, 0, 0);
		} else if (tickCount % 4 == 0 && random.nextDouble() < density) {
			level().addParticle(ParticleTypes.SOUL, getX(), getY() + 0.3, getZ(), 0, -0.02, 0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && HollowLegion.isSovereignSide(attacker)) return false;
		return super.hurtServer(level, source, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide()) {
			HollowSovereignEntity boss = owner();
			if (boss != null) boss.onPauldronDestroyed(this);
			playSound(SoundEvents.ANVIL_DESTROY, 1.0f, 0.8f);
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (deathTime >= 8 && !level().isClientSide() && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public boolean canTeleport(Level oldLevel, Level newLevel) {
		return false;
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
	public float getVoicePitch() {
		return 1.2f;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		if (ownerId != null) tag.store("SovereignOwner", UUIDUtil.CODEC, ownerId);
		tag.putInt("PauldronSide", entityData.get(SIDE));
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		ownerId = tag.read("SovereignOwner", UUIDUtil.CODEC).orElse(ownerId);
		entityData.set(SIDE, tag.getIntOr("PauldronSide", 0) & 1);
		entityData.set(STATE, RETURN);
		configured = true;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>("pauldron", 3, state -> {
			RemnantClips.Sheet sheet = RemnantClips.SOVEREIGN_PAULDRON;
			return switch (state()) {
				case TELEGRAPH -> state.setAndContinue(RemnantAnims.of(sheet, "telegraph"));
				case DIVE -> state.setAndContinue(RemnantAnims.of(sheet, "dive"));
				default -> state.setAndContinue(RemnantAnims.of(sheet, "idle"));
			};
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
