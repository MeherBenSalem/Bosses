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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * remnants:memory_bolt: a splinter of violet crystal flying in a straight line. The Forgotten
 * Watcher's Ring Volleys, its illusions' weaker volleys, the Memory Wraith's soul bolt and the Memory Staff
 * all fire it. A homing bolt turns toward a chosen target, or (the staff's) toward the nearest hostile
 * creature near its path. It breaks on the first block or creature it hits. Never saved.
 */
public class MemoryBoltEntity extends Entity implements GeoEntity, TraceableEntity {
	private static final DustParticleOptions VIOLET = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.62f, 0.53f, 0.88f), 0.9f);
	private static final DustParticleOptions PALE = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.82f, 0.78f, 1.0f), 0.7f);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private UUID ownerId;
	private Entity cachedOwner;
	private LivingEntity homingTarget;
	private float damage = 6;
	private double range = 24, travelled, turnDegrees;
	private boolean seekHostiles;

	public MemoryBoltEntity(EntityType<? extends MemoryBoltEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
	}

	/** A straight bolt from {@code origin} (its centre) along {@code direction}. */
	public static MemoryBoltEntity fire(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 direction, double speed,
			float damage, double range) {
		MemoryBoltEntity bolt = ModEntities.MEMORY_BOLT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (bolt == null) return null;
		bolt.ownerId = owner.getUUID();
		bolt.cachedOwner = owner;
		bolt.damage = damage;
		bolt.range = range;
		Vec3 motion = direction.normalize().scale(speed);
		bolt.setPos(origin.x, origin.y - bolt.getBbHeight() / 2, origin.z);
		bolt.setDeltaMovement(motion);
		bolt.face(motion);
		bolt.yRotO = bolt.getYRot();
		bolt.xRotO = bolt.getXRot();
		level.addFreshEntity(bolt);
		return bolt;
	}

	/** Turns toward {@code target} by at most {@code turnDegrees} per tick. */
	public MemoryBoltEntity homing(LivingEntity target, double turnDegrees) {
		this.homingTarget = target;
		this.turnDegrees = turnDegrees;
		return this;
	}

	/** Memory Staff: turns toward the nearest hostile creature within 20 degrees of its path. */
	public MemoryBoltEntity seekHostiles(double turnDegrees) {
		this.seekHostiles = true;
		this.turnDegrees = turnDegrees;
		return this;
	}

	private void face(Vec3 motion) {
		if (motion.lengthSqr() < 1.0E-6) return;
		setYRot(HollowLegion.yawToward(Vec3.ZERO, motion));
		setXRot((float) -Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public Entity getOwner() {
		if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
		if (ownerId != null && level() instanceof ServerLevel server) cachedOwner = server.getEntity(ownerId);
		return cachedOwner;
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 motion = getDeltaMovement();
		Vec3 from = position();
		yRotO = getYRot();
		xRotO = getXRot();
		if (level().isClientSide()) {
			Vec3 to = from.add(motion);
			setPos(to.x, to.y, to.z);
			face(motion);
			trail(from);
			return;
		}
		if (motion.lengthSqr() < 1.0E-6 || tickCount > 300) {
			discard();
			return;
		}
		motion = steer(motion);
		setDeltaMovement(motion);
		face(motion);
		Vec3 to = from.add(motion);
		Vec3 centre = from.add(0, getBbHeight() / 2, 0);
		var block = level().clip(new ClipContext(centre, centre.add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		boolean blocked = block.getType() != HitResult.Type.MISS;
		if (blocked) to = block.getLocation().subtract(0, getBbHeight() / 2, 0);
		AABB sweep = getBoundingBox().expandTowards(to.subtract(from)).inflate(0.15);
		List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, sweep, this::canHit);
		if (!victims.isEmpty()) {
			victims.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
			LivingEntity victim = victims.get(0);
			Damage.hurt(victim, source(), damage);
			shatter(victim.position().add(0, victim.getBbHeight() / 2, 0));
			return;
		}
		setPos(to.x, to.y, to.z);
		travelled += from.distanceTo(to);
		if (blocked || travelled >= range) shatter(to.add(0, getBbHeight() / 2, 0));
	}

	/** Homing: rotate the velocity toward the target by at most {@link #turnDegrees}, keeping the speed. */
	private Vec3 steer(Vec3 motion) {
		if (turnDegrees <= 0) return motion;
		if (seekHostiles && (homingTarget == null || !homingTarget.isAlive()) && tickCount % 4 == 1) homingTarget = seek(motion);
		if (homingTarget == null || !homingTarget.isAlive() || homingTarget.isRemoved()) return motion;
		Vec3 want = homingTarget.position().add(0, homingTarget.getBbHeight() * 0.55, 0).subtract(position().add(0, getBbHeight() / 2, 0));
		if (want.lengthSqr() < 1.0E-4) return motion;
		double speed = motion.length();
		Vec3 a = motion.normalize(), b = want.normalize();
		double angle = Math.acos(Mth.clamp(a.dot(b), -1, 1));
		double max = Math.toRadians(turnDegrees);
		if (angle <= max) return b.scale(speed);
		double t = max / angle;
		return a.scale(1 - t).add(b.scale(t)).normalize().scale(speed);
	}

	private LivingEntity seek(Vec3 motion) {
		Vec3 dir = motion.normalize();
		double cos = Math.cos(Math.toRadians(20));
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
				e -> canHit(e) && (e instanceof Enemy || (e instanceof Mob mob && mob.getTarget() == getOwner())))) {
			Vec3 offset = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(position());
			double distance = offset.length();
			if (distance < 1.0E-3 || offset.scale(1 / distance).dot(dir) < cos || distance >= bestDistance) continue;
			best = entity;
			bestDistance = distance;
		}
		return best;
	}

	private DamageSource source() {
		Entity owner = getOwner();
		return owner instanceof LivingEntity living ? damageSources().mobProjectile(this, living) : damageSources().magic();
	}

	private boolean canHit(LivingEntity entity) {
		if (!entity.isAlive() || entity.isSpectator() || !entity.isAttackable()) return false;
		Entity owner = getOwner();
		if (owner == null || ForgottenSouls.isWatcherSide(owner)) return ForgottenSouls.victim(entity);
		if (entity == owner) return false;
		if (entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null && owner.getUUID().equals(ownable.getOwnerReference().getUUID())) return false;
		return !(owner instanceof Player player && entity instanceof Player other && !player.canHarmPlayer(other));
	}

	private void shatter(Vec3 at) {
		if (level() instanceof ServerLevel server) {
			if (SovereignConfig.visualDensity() > 0) {
				server.sendParticles(VIOLET, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.0);
				server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.04);
			}
			server.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.6f, 1.4f);
		}
		discard();
	}

	private void trail(Vec3 from) {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || random.nextDouble() > density) return;
		Vec3 p = from.add(0, getBbHeight() / 2, 0);
		level().addParticle(tickCount % 2 == 0 ? VIOLET : PALE, p.x, p.y, p.z, 0, 0, 0);
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 64 * 64;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput tag) {
		ownerId = tag.read("Owner", UUIDUtil.CODEC).orElse(ownerId);
		damage = tag.getFloatOr("Damage", 0f);
		range = tag.getDoubleOr("Range", 0d);
		travelled = tag.getDoubleOr("Travelled", 0d);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput tag) {
		if (ownerId != null) tag.store("Owner", UUIDUtil.CODEC, ownerId);
		tag.putFloat("Damage", damage);
		tag.putDouble("Range", range);
		tag.putDouble("Travelled", travelled);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>("spin", 0,
				state -> state.setAndContinue(RemnantAnims.of(RemnantClips.MEMORY_BOLT, "spin"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
