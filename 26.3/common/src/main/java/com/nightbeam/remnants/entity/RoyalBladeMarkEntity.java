package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * remnants:royal_blade_mark: one Last Command blade. A ring fills the mark for {@link #FUSE} ticks,
 * then a spectral blade falls and damages everything inside it. Never saved.
 */
public class RoyalBladeMarkEntity extends Entity {
	public static final int FUSE = 30;
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(RoyalBladeMarkEntity.class, EntityDataSerializers.FLOAT);
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.31f, 0.79f, 0.76f), 1.2f);
	private static final DustParticleOptions GILT = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.85f, 0.72f, 0.38f), 1.0f);
	private UUID ownerId;
	private float damage = 12;

	public RoyalBladeMarkEntity(EntityType<? extends RoyalBladeMarkEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
	}

	public static RoyalBladeMarkEntity place(ServerLevel level, HollowSovereignEntity owner, Vec3 at, float radius, float damage) {
		RoyalBladeMarkEntity mark = ModEntities.ROYAL_BLADE_MARK.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (mark == null) return null;
		mark.ownerId = owner.getUUID();
		mark.damage = damage;
		mark.entityData.set(RADIUS, radius);
		mark.setPos(at.x, at.y, at.z);
		level.addFreshEntity(mark);
		return mark;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(RADIUS, 2.5f);
	}

	public float radius() {
		return entityData.get(RADIUS);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			visuals();
			return;
		}
		if (tickCount == FUSE) strike();
		if (tickCount >= FUSE + 6) discard();
	}

	private void strike() {
		ServerLevel server = (ServerLevel) level();
		float radius = radius();
		Entity owner = ownerId == null ? null : server.getEntity(ownerId);
		DamageSource source = owner instanceof LivingEntity living && living.isAlive()
				? damageSources().mobAttack(living) : damageSources().magic();
		AABB area = new AABB(getX() - radius, getY() - 1, getZ() - radius, getX() + radius, getY() + 3, getZ() + radius);
		for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area, HollowLegion::sovereignVictim)) {
			double dx = victim.getX() - getX(), dz = victim.getZ() - getZ();
			if (dx * dx + dz * dz <= (radius + victim.getBbWidth() / 2) * (radius + victim.getBbWidth() / 2)) {
				Damage.hurt(victim, source, damage);
			}
		}
		server.playSound(null, getX(), getY(), getZ(), ModSounds.SPECTRAL_SLASH.get(), SoundSource.HOSTILE, 1.2f, 0.7f);
		server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 24, radius * 0.4, 0.05, radius * 0.4, 0.04);
		server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 10, 0.15, 0.4, 0.15, 0.05);
	}

	private void visuals() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		float radius = radius();
		int t = tickCount;
		int count = Math.max(8, (int) (28 * density));
		if (t < FUSE) {
			if (t % 2 == 0) ring(radius, count, t * 0.03, ParticleTypes.SOUL_FIRE_FLAME, 0.05);
			double fill = radius * (1 - t / (double) FUSE);
			ring(fill, Math.max(6, (int) (count * fill / radius)), -t * 0.08, SOUL_LIGHT, 0.08);
			if (t % 5 == 0) ring(radius * 0.5, count / 3, t * 0.1, GILT, 0.06);
		}
		if (t >= FUSE - 5 && t <= FUSE) {
			// The blade falls: a bright column dropping onto the mark's centre.
			double top = (FUSE - t) * 1.6 + 0.4;
			for (int i = 0; i < 6; i++) {
				level().addParticle(ParticleTypes.END_ROD, getX(), getY() + top + i * 0.35, getZ(), 0, -0.6, 0);
			}
		}
	}

	private void ring(double radius, int count, double twist, net.minecraft.core.particles.ParticleOptions particle, double height) {
		for (int i = 0; i < count; i++) {
			double angle = i * Math.PI * 2 / count + twist;
			level().addParticle(particle, getX() + Math.cos(angle) * radius, getY() + height, getZ() + Math.sin(angle) * radius, 0, 0.005, 0);
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
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput tag) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput tag) {
	}
}
