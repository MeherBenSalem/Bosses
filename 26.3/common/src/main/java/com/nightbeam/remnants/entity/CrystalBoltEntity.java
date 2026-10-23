package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.util.ARGB;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** remnants:crystal_bolt: the Shardling's splinter of rose-violet soul glass. Never saved. */
public class CrystalBoltEntity extends RemnantBoltEntity {
	/** Oracle corruption, #D77BC0 and #F6C9EC. */
	public static final DustParticleOptions ROSE = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.84f, 0.48f, 0.75f), 0.9f);
	public static final DustParticleOptions PALE_ROSE = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.96f, 0.79f, 0.93f), 0.7f);

	public CrystalBoltEntity(EntityType<? extends CrystalBoltEntity> type, Level level) {
		super(type, level);
	}

	public static CrystalBoltEntity fire(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 direction, double speed, float damage,
			double range) {
		CrystalBoltEntity bolt = ModEntities.CRYSTAL_BOLT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (bolt == null) return null;
		return launch(level, bolt, owner, origin, direction, speed, damage, range);
	}

	@Override
	protected boolean familyVictim(LivingEntity entity) {
		return CorruptedRelics.victim(entity);
	}

	@Override
	protected void trail(Vec3 at) {
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || random.nextDouble() > density) return;
		level().addParticle(tickCount % 2 == 0 ? ROSE : PALE_ROSE, at.x, at.y, at.z, 0, 0, 0);
	}

	@Override
	protected void shatter(ServerLevel level, Vec3 at) {
		level.sendParticles(ROSE, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.0);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.03);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.6f, 1.6f);
	}
}
