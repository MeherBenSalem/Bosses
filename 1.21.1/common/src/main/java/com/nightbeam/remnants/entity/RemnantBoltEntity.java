package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A family creature's straight bolt: it flies in a line at a fixed speed, hits the first creature its owner's
 * family may damage ({@code mobProjectile}, so armor and difficulty scaling apply) and breaks on the first
 * block or after its range. It never changes a block. Drawn with the Memory Bolt model and spin clip in the
 * family's own colours. Never saved.
 */
public abstract class RemnantBoltEntity extends Entity implements GeoEntity, TraceableEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private UUID ownerId;
	private Entity cachedOwner;
	private float damage = 5;
	private double range = 16, travelled;

	protected RemnantBoltEntity(EntityType<? extends RemnantBoltEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
	}

	/** Sends {@code bolt} from {@code origin} (its centre) along {@code direction}. */
	protected static <T extends RemnantBoltEntity> T launch(ServerLevel level, T bolt, LivingEntity owner, Vec3 origin, Vec3 direction,
			double speed, float damage, double range) {
		RemnantBoltEntity base = bolt;
		base.ownerId = owner.getUUID();
		base.cachedOwner = owner;
		base.damage = damage;
		base.range = range;
		Vec3 motion = direction.normalize().scale(speed);
		base.setPos(origin.x, origin.y - base.getBbHeight() / 2, origin.z);
		base.setDeltaMovement(motion);
		base.face(motion);
		base.yRotO = base.getYRot();
		base.xRotO = base.getXRot();
		level.addFreshEntity(base);
		return bolt;
	}

	/** A living target the owner's family may damage (also used once the owner is gone). */
	protected abstract boolean familyVictim(LivingEntity entity);

	/** After the damage landed on {@code victim}. */
	protected void onHit(LivingEntity victim) {
	}

	/** Client trail particle at {@code at}, the bolt's centre. */
	protected abstract void trail(Vec3 at);

	/** Server burst where the bolt broke, at {@code at}. */
	protected abstract void shatter(ServerLevel level, Vec3 at);

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
		if (level().isClientSide) {
			Vec3 to = from.add(motion);
			setPos(to.x, to.y, to.z);
			face(motion);
			trail(from.add(0, getBbHeight() / 2, 0));
			return;
		}
		if (motion.lengthSqr() < 1.0E-6 || tickCount > 300) {
			discard();
			return;
		}
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
			if (victim.hurt(source(), damage)) onHit(victim);
			breakAt(victim.position().add(0, victim.getBbHeight() / 2, 0));
			return;
		}
		setPos(to.x, to.y, to.z);
		travelled += from.distanceTo(to);
		if (blocked || travelled >= range) breakAt(to.add(0, getBbHeight() / 2, 0));
	}

	private DamageSource source() {
		Entity owner = getOwner();
		return owner instanceof LivingEntity living ? damageSources().mobProjectile(this, living) : damageSources().magic();
	}

	private boolean canHit(LivingEntity entity) {
		if (!entity.isAlive() || entity.isSpectator() || !entity.isAttackable()) return false;
		return entity != getOwner() && familyVictim(entity);
	}

	private void breakAt(Vec3 at) {
		if (level() instanceof ServerLevel server) shatter(server, at);
		discard();
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
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 64 * 64;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
		damage = tag.getFloat("Damage");
		range = tag.getDouble("Range");
		travelled = tag.getDouble("Travelled");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (ownerId != null) tag.putUUID("Owner", ownerId);
		tag.putFloat("Damage", damage);
		tag.putDouble("Range", range);
		tag.putDouble("Travelled", travelled);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>(this, "spin", 0,
				state -> state.setAndContinue(RemnantAnims.of(RemnantClips.MEMORY_BOLT, "spin"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
