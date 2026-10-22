package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * remnants:warden_chain: a thrown chain that latches onto the first creature it touches and then
 * pulls. The Warden's Chain Grab latches for 8 ticks and pulls the target 70% of
 * the way in without damage; a shield raised toward the Warden or a broken line of sight cancels the
 * latch. Warden's Grasp throws the same chain: it pulls a creature to the player, or the
 * player to a block or to a creature too heavy to move. Never saved.
 */
public class WardenChainEntity extends Entity implements TraceableEntity {
	public static final int FLYING = 0, LATCHED = 1, PULLING = 2;
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(WardenChainEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HOOKED = SynchedEntityData.defineId(WardenChainEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(WardenChainEntity.class, EntityDataSerializers.INT);

	private UUID ownerId;
	private Entity cachedOwner;
	private LivingEntity hooked;
	private double range = 14, travelled, pullFraction = 0.7;
	private int latchTicks = 8, pullTicks = 8, stateTicks;
	private boolean grasp, pullOwner;
	private Vec3 anchorPoint, destination;

	public WardenChainEntity(EntityType<? extends WardenChainEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
	}

	/** Chain Grab: the Warden throws from its shackle toward {@code direction}. */
	public static WardenChainEntity forWarden(ServerLevel level, GraveboundWardenEntity warden, Vec3 from, Vec3 direction,
			double speed, double range, int latchTicks, double pullFraction) {
		WardenChainEntity chain = create(level, warden, from, direction, speed, range);
		if (chain == null) return null;
		chain.latchTicks = latchTicks;
		chain.pullTicks = 8;
		chain.pullFraction = pullFraction;
		level.addFreshEntity(chain);
		return chain;
	}

	/** Warden's Grasp: one hook per player; a new throw replaces the old one. */
	public static WardenChainEntity forGrasp(ServerLevel level, Player player, Vec3 from, Vec3 direction, double speed, double range) {
		for (WardenChainEntity old : level.getEntitiesOfClass(WardenChainEntity.class, player.getBoundingBox().inflate(range + 8),
				c -> c.getOwner() == player)) {
			old.discard();
		}
		WardenChainEntity chain = create(level, player, from, direction, speed, range);
		if (chain == null) return null;
		chain.grasp = true;
		chain.latchTicks = 2;
		chain.pullTicks = 6;
		chain.pullFraction = 0.8;
		level.addFreshEntity(chain);
		return chain;
	}

	private static WardenChainEntity create(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 direction, double speed, double range) {
		WardenChainEntity chain = ModEntities.WARDEN_CHAIN.get().create(level);
		if (chain == null || direction.lengthSqr() < 1.0E-6) return null;
		chain.ownerId = owner.getUUID();
		chain.cachedOwner = owner;
		chain.entityData.set(OWNER, owner.getId());
		chain.range = range;
		chain.setPos(from.x, from.y - chain.getBbHeight() / 2, from.z);
		chain.setDeltaMovement(direction.normalize().scale(speed));
		return chain;
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(OWNER, -1);
		entityData.define(HOOKED, -1);
		entityData.define(STATE, FLYING);
	}

	public int state() {
		return entityData.get(STATE);
	}

	/** Client and server: the owner by synced id (server: by UUID). */
	@Override
	public Entity getOwner() {
		if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
		if (level() instanceof ServerLevel server) {
			if (ownerId != null) cachedOwner = server.getEntity(ownerId);
		} else {
			cachedOwner = level().getEntity(entityData.get(OWNER));
		}
		return cachedOwner;
	}

	/** Client: the latched creature, if any. */
	public Entity hookedEntity() {
		int id = entityData.get(HOOKED);
		return id < 0 ? null : level().getEntity(id);
	}

	private void setState(int state) {
		entityData.set(STATE, state);
		stateTicks = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (state() == FLYING) {
				Vec3 to = position().add(getDeltaMovement());
				setPos(to.x, to.y, to.z);
			}
			return;
		}
		Entity owner = getOwner();
		if (!(owner instanceof LivingEntity living) || !owner.isAlive() || owner.level() != level() || tickCount > 200) {
			discard();
			return;
		}
		switch (state()) {
			case FLYING -> fly(living);
			case LATCHED -> hold(living);
			case PULLING -> pull(living);
			default -> discard();
		}
	}

	private void fly(LivingEntity owner) {
		Vec3 motion = getDeltaMovement();
		Vec3 from = position().add(0, getBbHeight() / 2, 0);
		Vec3 to = from.add(motion);
		BlockHitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		boolean blocked = block.getType() != HitResult.Type.MISS;
		if (blocked) to = block.getLocation();
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, from, to, getBoundingBox().expandTowards(to.subtract(from)).inflate(1.0),
				e -> e instanceof LivingEntity victim && canHook(owner, victim), 0.3f);
		if (hit != null && hit.getEntity() instanceof LivingEntity victim) {
			latch(owner, victim);
			return;
		}
		if (blocked) {
			if (grasp) {
				// Grapple: the player is pulled most of the way to the block.
				anchorPoint = block.getLocation();
				setPos(anchorPoint.x, anchorPoint.y - getBbHeight() / 2, anchorPoint.z);
				pullOwner = true;
				startPull(owner, owner, anchorPoint, 0.85, Math.max(4, Math.min(12, (int) (owner.position().distanceTo(anchorPoint) / 1.2))));
				playSound(SoundEvents.CHAIN_HIT, 1.0f, 1.2f);
				return;
			}
			playSound(SoundEvents.CHAIN_HIT, 1.0f, 0.8f);
			discard();
			return;
		}
		setPos(to.x, to.y - getBbHeight() / 2, to.z);
		travelled += motion.length();
		if (travelled >= range) discard();
	}

	private boolean canHook(LivingEntity owner, LivingEntity victim) {
		if (victim == owner || !victim.isAlive() || victim.isSpectator() || !victim.isAttackable() || !victim.isPickable()) return false;
		if (owner instanceof GraveboundWardenEntity warden) return warden.canHit(victim);
		if (HollowLegion.isUntouchablePlayer(victim)) return false;
		if (victim instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) return false;
		if (victim instanceof SpectralRoyalGuardEntity guard && owner.getUUID().equals(guard.ownerId())) return false;
		return !(owner instanceof Player player && victim instanceof Player other && !player.canHarmPlayer(other));
	}

	private void latch(LivingEntity owner, LivingEntity victim) {
		if (shieldFacing(owner, victim)) {
			victim.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f);
			discard();
			return;
		}
		hooked = victim;
		entityData.set(HOOKED, victim.getId());
		setState(LATCHED);
		playSound(SoundEvents.CHAIN_PLACE, 1.4f, 0.7f);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
		}
		if (latchTicks <= 0) hold(owner);
	}

	/** A raised shield facing the thrower deflects or cancels the chain. */
	private static boolean shieldFacing(LivingEntity owner, LivingEntity victim) {
		if (!victim.isBlocking()) return false;
		// Entity.calculateViewVector is protected in 1.20.1; this is the same horizontal facing.
		Vec3 view = Vec3.directionFromRotation(0, victim.getYHeadRot());
		Vec3 toOwner = owner.position().subtract(victim.position()).multiply(1, 0, 1);
		return toOwner.lengthSqr() < 1.0E-4 || toOwner.normalize().dot(view.multiply(1, 0, 1).normalize()) > 0;
	}

	private void hold(LivingEntity owner) {
		if (hooked == null || !hooked.isAlive() || hooked.level() != level()) {
			discard();
			return;
		}
		follow(hooked);
		if (shieldFacing(owner, hooked) || !owner.hasLineOfSight(hooked)) {
			hooked.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);
			discard();
			return;
		}
		if (++stateTicks < latchTicks) return;
		boolean heavy = grasp && hooked.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0;
		pullOwner = heavy;
		if (heavy) startPull(owner, owner, hooked.position(), pullFraction, pullTicks);
		else startPull(owner, hooked, owner.position(), pullFraction, pullTicks);
		playSound(SoundEvents.CHAIN_FALL, 1.4f, 0.9f);
	}

	private void startPull(LivingEntity owner, LivingEntity mover, Vec3 toward, double fraction, int ticks) {
		Vec3 start = mover.position();
		destination = start.add(toward.subtract(start).scale(fraction));
		pullTicks = Math.max(1, ticks);
		setState(PULLING);
	}

	private void pull(LivingEntity owner) {
		LivingEntity mover = pullOwner ? owner : hooked;
		if (mover == null || !mover.isAlive() || destination == null) {
			discard();
			return;
		}
		if (hooked != null) follow(hooked);
		int left = pullTicks - stateTicks;
		stateTicks++;
		if (left <= 0) {
			if (!grasp && owner instanceof GraveboundWardenEntity warden && hooked != null) warden.onChainPulled(hooked);
			discard();
			return;
		}
		Vec3 remaining = destination.subtract(mover.position());
		Vec3 step = remaining.multiply(1, 0, 1).scale(1.0 / left);
		if (step.length() > 1.6) step = step.normalize().scale(1.6);
		double lift = Math.max(-0.4, Math.min(0.8, remaining.y / left + 0.1));
		mover.setDeltaMovement(step.x, lift, step.z);
		mover.hurtMarked = true;
		mover.fallDistance = 0;
	}

	private void follow(LivingEntity target) {
		setPos(target.getX(), target.getY() + target.getBbHeight() * 0.55 - getBbHeight() / 2, target.getZ());
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
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}
}
