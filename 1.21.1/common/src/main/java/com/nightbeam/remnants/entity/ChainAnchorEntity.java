package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * remnants:chain_anchor: one of the four Chain Wall posts. A chain runs from each
 * anchor to its neighbour at waist height (it can be jumped); touching it deals 6 damage and throws the
 * victim back. Anchors have 30 health, expire after 400 ticks and are never saved. Breaking every
 * anchor of a wall staggers the Warden.
 */
public class ChainAnchorEntity extends Mob implements Enemy {
	/** The chain band, measured from the anchors' feet: low enough to be jumped over. */
	public static final double CHAIN_LOW = 0.75, CHAIN_HIGH = 1.15, CHAIN_HEIGHT = 0.95;
	private static final EntityDataAccessor<Integer> NEXT = SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.INT);
	private final Map<UUID, Long> contact = new HashMap<>();
	private UUID ownerId;
	private GraveboundWardenEntity cachedOwner;
	private ChainAnchorEntity next;
	private int wallId, life = 400, orphanTicks;
	private float chainDamage = 6;

	public ChainAnchorEntity(EntityType<? extends ChainAnchorEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.MOVEMENT_SPEED, 0);
	}

	public static ChainAnchorEntity place(ServerLevel level, GraveboundWardenEntity owner, Vec3 at, int wallId, int life,
			float health, float chainDamage) {
		ChainAnchorEntity anchor = ModEntities.CHAIN_ANCHOR.get().create(level);
		if (anchor == null) return null;
		anchor.ownerId = owner.getUUID();
		anchor.cachedOwner = owner;
		anchor.wallId = wallId;
		anchor.life = life;
		anchor.chainDamage = chainDamage;
		anchor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		anchor.setHealth(health);
		anchor.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
		anchor.setPersistenceRequired();
		level.addFreshEntity(anchor);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1.0, at.z, 12, 0.2, 0.6, 0.2, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, anchor.getSoundSource(), 1.5f, 0.6f);
		return anchor;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(NEXT, -1);
	}

	public void linkTo(ChainAnchorEntity neighbour) {
		next = neighbour;
		entityData.set(NEXT, neighbour.getId());
	}

	/** The neighbour this anchor's chain runs to (client: by synced id). */
	public ChainAnchorEntity neighbour() {
		if (!level().isClientSide) return next != null && next.isAlive() ? next : null;
		int id = entityData.get(NEXT);
		return id >= 0 && level().getEntity(id) instanceof ChainAnchorEntity anchor && anchor.isAlive() ? anchor : null;
	}

	public int wallId() {
		return wallId;
	}

	public boolean ownedBy(GraveboundWardenEntity warden) {
		return warden.getUUID().equals(ownerId);
	}

	private GraveboundWardenEntity owner() {
		if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
		cachedOwner = null;
		if (ownerId != null && level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof GraveboundWardenEntity warden) {
			cachedOwner = warden;
		}
		return cachedOwner;
	}

	@Override
	protected void registerGoals() {
	}

	@Override
	public void tick() {
		setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
		super.tick();
		if (level().isClientSide) {
			clientVisuals();
			return;
		}
		if (!isAlive()) return;
		GraveboundWardenEntity owner = owner();
		if (owner == null || !owner.isAlive()) {
			if (owner != null || ++orphanTicks > 40) crumble();
			return;
		}
		orphanTicks = 0;
		if (--life <= 0) {
			crumble();
			return;
		}
		ChainAnchorEntity neighbour = neighbour();
		if (neighbour != null) chainContact(owner, neighbour);
	}

	/** Contact with the chain to the neighbour: 6 damage and a shove away from the chain, every 10 ticks at most. */
	private void chainContact(GraveboundWardenEntity owner, ChainAnchorEntity neighbour) {
		Vec3 a = position(), b = neighbour.position();
		double baseY = Math.min(a.y, b.y);
		AABB box = new AABB(a, b).inflate(1.0, 0, 1.0).expandTowards(0, 2.5, 0).move(0, -1, 0);
		long now = level().getGameTime();
		for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box, owner::canHit)) {
			double feet = victim.getY(), head = feet + victim.getBbHeight();
			double chainY = baseY + CHAIN_HEIGHT;
			if (head < chainY - (CHAIN_HEIGHT - CHAIN_LOW) || feet > baseY + CHAIN_HIGH) continue;
			Vec3 closest = closestOnSegment(a, b, victim.position());
			Vec3 offset = victim.position().subtract(closest).multiply(1, 0, 1);
			if (offset.length() > victim.getBbWidth() / 2 + 0.25) continue;
			Long last = contact.get(victim.getUUID());
			if (last != null && now - last < 10) continue;
			contact.put(victim.getUUID(), now);
			if (victim.hurt(damageSources().mobAttack(owner), chainDamage)) {
				Vec3 away = offset.lengthSqr() < 1.0E-4 ? victim.position().subtract(owner.position()) : offset;
				victim.knockback(0.8, -away.x, -away.z);
				victim.hurtMarked = true;
				playSound(SoundEvents.CHAIN_HIT, 1.0f, 0.7f);
			}
		}
		if (tickCount % 40 == 0) contact.entrySet().removeIf(e -> now - e.getValue() > 40);
	}

	/** Closest point to {@code p} on segment a-b, ignoring height. */
	public static Vec3 closestOnSegment(Vec3 a, Vec3 b, Vec3 p) {
		Vec3 ab = b.subtract(a).multiply(1, 0, 1);
		double length = ab.lengthSqr();
		if (length < 1.0E-6) return a;
		double t = Math.max(0, Math.min(1, p.subtract(a).multiply(1, 0, 1).dot(ab) / length));
		return new Vec3(a.x + ab.x * t, a.y, a.z + ab.z * t);
	}

	private void clientVisuals() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || !isAlive()) return;
		if (tickCount % 5 == 0) level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.15, getZ(), 0, 0.01, 0);
		ChainAnchorEntity neighbour = neighbour();
		if (neighbour != null && tickCount % 6 == 0 && random.nextDouble() < density) {
			Vec3 p = position().lerp(neighbour.position(), random.nextDouble()).add(0, CHAIN_HEIGHT, 0);
			level().addParticle(ParticleTypes.SOUL, p.x, p.y, p.z, 0, 0.01, 0);
		}
	}

	/** Expiry, or the Warden is gone: the anchor falls apart without counting as destroyed. */
	public void crumble() {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 10, 0.2, 0.6, 0.2, 0.02);
			server.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_BREAK, getSoundSource(), 1.0f, 0.9f);
		}
		discard();
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && Cryptborn.isWardenSide(attacker)) return false;
		return super.hurt(source, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide) {
			GraveboundWardenEntity owner = owner();
			if (owner != null) owner.onAnchorDestroyed(this);
			playSound(SoundEvents.CHAIN_BREAK, 1.5f, 0.6f);
			playSound(SoundEvents.ANVIL_LAND, 0.6f, 1.4f);
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (deathTime >= 10 && !level().isClientSide && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return super.isAlliedTo(other) || (other != this && Cryptborn.isWardenSide(other));
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		return false;
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
	protected boolean shouldDropLoot() {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.CHAIN_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.CHAIN_BREAK;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		ChainAnchorEntity neighbour = neighbour();
		AABB box = super.getBoundingBoxForCulling();
		return neighbour == null ? box : box.minmax(neighbour.getBoundingBox());
	}
}
