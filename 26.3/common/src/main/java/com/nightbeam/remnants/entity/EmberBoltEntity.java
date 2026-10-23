package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.entity.EntitySpawnReason;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * remnants:ember_bolt: the Ember Servitor's shot, a spark of furnace ember. It sets the creature it hits
 * burning briefly and fizzles out on blocks without lighting them. Never saved.
 */
public class EmberBoltEntity extends RemnantBoltEntity {
	private int burnTicks;

	public EmberBoltEntity(EntityType<? extends EmberBoltEntity> type, Level level) {
		super(type, level);
	}

	/** An ember bolt from {@code origin} along {@code direction}; a hit burns for {@code burnTicks}. */
	public static EmberBoltEntity fire(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 direction, double speed, float damage,
			double range, int burnTicks) {
		EmberBoltEntity bolt = ModEntities.EMBER_BOLT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (bolt == null) return null;
		bolt.burnTicks = burnTicks;
		return launch(level, bolt, owner, origin, direction, speed, damage, range);
	}

	@Override
	protected boolean familyVictim(LivingEntity entity) {
		return Ashbound.victim(entity);
	}

	@Override
	protected void onHit(LivingEntity victim) {
		Ashbound.ignite(victim, burnTicks);
	}

	@Override
	protected void trail(Vec3 at) {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || random.nextDouble() > density) return;
		level().addParticle(tickCount % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 0, 0.005, 0);
		if (tickCount % 3 == 0) level().addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0.01, 0);
	}

	@Override
	protected void shatter(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.03);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.4f, 1.6f);
	}
}
