package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A spectral sword arc flying in a straight line. Fired by the Hollow Sovereign (phase 3) and by
 * Sovereign's Ruin; it passes through {@code pierce} targets and fades after its range.
 */
public class SpectralCrescentEntity extends Entity implements GeoEntity, TraceableEntity {
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(new Vector3f(0.61f, 0.95f, 0.9f), 1.0f);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final Set<UUID> struck = new HashSet<>();
	private UUID ownerId;
	private Entity cachedOwner;
	private float damage = 10;
	private int pierce = 3;
	private double range = 20, travelled;

	public SpectralCrescentEntity(EntityType<? extends SpectralCrescentEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
	}

	/**
	 * Launches a crescent from {@code origin} (its centre) along {@code direction}.
	 *
	 * @param pierce how many targets it may pass through; it stops on the one after
	 */
	public static SpectralCrescentEntity fire(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 direction,
			double speed, float damage, int pierce, double range) {
		SpectralCrescentEntity crescent = ModEntities.SPECTRAL_CRESCENT.get().create(level);
		if (crescent == null) return null;
		crescent.ownerId = owner.getUUID();
		crescent.cachedOwner = owner;
		crescent.damage = damage;
		crescent.pierce = pierce;
		crescent.range = range;
		Vec3 motion = direction.normalize().scale(speed);
		crescent.setPos(origin.x, origin.y - crescent.getBbHeight() / 2, origin.z);
		crescent.setDeltaMovement(motion);
		crescent.face(motion);
		crescent.yRotO = crescent.getYRot();
		level.addFreshEntity(crescent);
		return crescent;
	}

	private void face(Vec3 motion) {
		if (motion.horizontalDistanceSqr() > 1.0E-6) setYRot(HollowLegion.yawToward(Vec3.ZERO, motion));
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
		Vec3 to = from.add(motion);
		yRotO = getYRot();
		face(motion);
		if (level().isClientSide) {
			setPos(to.x, to.y, to.z);
			trail(from);
			return;
		}
		if (motion.lengthSqr() < 1.0E-6 || tickCount > 400) {
			discard();
			return;
		}
		Vec3 centre = from.add(0, getBbHeight() / 2, 0);
		var block = level().clip(new ClipContext(centre, centre.add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		boolean blocked = block.getType() != HitResult.Type.MISS;
		if (blocked) to = block.getLocation().subtract(0, getBbHeight() / 2, 0);
		AABB sweep = getBoundingBox().expandTowards(to.subtract(from)).inflate(0.2);
		List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, sweep, this::canHit);
		victims.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
		for (LivingEntity victim : victims) {
			if (!struck.add(victim.getUUID())) continue;
			victim.hurt(source(), damage);
			if (pierce-- <= 0) {
				fade(victim.position().add(0, victim.getBbHeight() / 2, 0));
				return;
			}
		}
		setPos(to.x, to.y, to.z);
		travelled += from.distanceTo(to);
		if (blocked || travelled >= range) fade(to.add(0, getBbHeight() / 2, 0));
	}

	private DamageSource source() {
		Entity owner = getOwner();
		return owner instanceof LivingEntity living ? damageSources().mobProjectile(this, living) : damageSources().magic();
	}

	private boolean canHit(LivingEntity entity) {
		if (!entity.isAlive() || entity.isSpectator() || !entity.isAttackable()) return false;
		Entity owner = getOwner();
		if (owner == null || HollowLegion.isSovereignSide(owner)) return HollowLegion.sovereignVictim(entity);
		if (entity == owner) return false;
		if (entity instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) return false;
		if (entity instanceof SpectralRoyalGuardEntity guard && owner.getUUID().equals(guard.ownerId())) return false;
		return !(owner instanceof Player player && entity instanceof Player other && !player.canHarmPlayer(other));
	}

	private void fade(Vec3 at) {
		if (level() instanceof ServerLevel server && SovereignConfig.visualDensity() > 0) {
			server.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 10, 0.4, 0.15, 0.4, 0.03);
			server.sendParticles(SOUL_LIGHT, at.x, at.y, at.z, 8, 0.5, 0.1, 0.5, 0.0);
		}
		discard();
	}

	private void trail(Vec3 from) {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90);
		int count = Math.max(1, (int) (3 * density));
		for (int i = 0; i < count; i++) {
			double s = (random.nextDouble() - 0.5) * getBbWidth();
			Vec3 p = from.add(side.scale(s)).add(0, getBbHeight() / 2, 0);
			level().addParticle(i == 0 ? ParticleTypes.SOUL_FIRE_FLAME : SOUL_LIGHT, p.x, p.y, p.z, 0, 0, 0);
		}
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
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
		damage = tag.getFloat("Damage");
		pierce = tag.getInt("Pierce");
		range = tag.getDouble("Range");
		travelled = tag.getDouble("Travelled");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (ownerId != null) tag.putUUID("Owner", ownerId);
		tag.putFloat("Damage", damage);
		tag.putInt("Pierce", pierce);
		tag.putDouble("Range", range);
		tag.putDouble("Travelled", travelled);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "spin", 0,
				state -> state.setAndContinue(RemnantAnims.of(RemnantClips.SPECTRAL_CRESCENT, "spin"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
