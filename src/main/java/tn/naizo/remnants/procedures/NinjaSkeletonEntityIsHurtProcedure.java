package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.OssukageEntity;
import tn.naizo.remnants.configuration.MainConfigConfiguration;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

public class NinjaSkeletonEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity toSpawn = null;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= ((entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) / 100) * (double) MainConfigConfiguration.HP_THRESHOLD.get()) {
			if (!(entity instanceof OssukageEntity _datEntL3 && _datEntL3.getEntityData().get(OssukageEntity.DATA_transform))) {
				RemnantBossesMod.queueServerWork((int) (double) MainConfigConfiguration.TRANSFORM_DELAY.get(), () -> {
					if (world instanceof ServerLevel _level) {
						LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
						entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y, z)));
						entityToSpawn.setVisualOnly(true);
						_level.addFreshEntity(entityToSpawn);
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 5, 3, 3, 3, 1);
					if (entity instanceof OssukageEntity animatable)
						animatable.setTexture("ninja_skeleton_phase_2");
					for (int index0 = 0; index0 < (int) (double) MainConfigConfiguration.SKELETONS_ON_TRANSFORM.get(); index0++) {
						if (world instanceof ServerLevel _level) {
							Entity entityToSpawn = EntityType.SKELETON.spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
							if (entityToSpawn != null) {
								entityToSpawn.setDeltaMovement(0, 0, 0);
							}
						}
					}
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) (double) MainConfigConfiguration.INVISIBILITY_TIMER.get(), 3, false, true));
					if (entity instanceof LivingEntity _livingEntity12 && _livingEntity12.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
						_livingEntity12.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
					if (entity instanceof LivingEntity _livingEntity14 && _livingEntity14.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
						_livingEntity14.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(((double) MainConfigConfiguration.MOVEMENT_SPEED_P2.get()));
					if (entity instanceof LivingEntity _livingEntity16 && _livingEntity16.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
						_livingEntity16.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(((double) MainConfigConfiguration.ATTACK_DAMAGE_P2.get()));
				});
				if (entity instanceof OssukageEntity _datEntSetL)
					_datEntSetL.getEntityData().set(OssukageEntity.DATA_transform, true);
			}
		}
	}
}
