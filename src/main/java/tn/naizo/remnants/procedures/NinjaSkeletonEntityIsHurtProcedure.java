package tn.naizo.remnants.procedures;

import tn.naizo.remnants.init.RemnantBossesModEntities;
import tn.naizo.remnants.entity.OssukageEntity;
import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.jauml.JaumlConfigLib;

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
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

public class NinjaSkeletonEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity toSpawn = null;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) <= ((entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) / 100) * JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "hp_threshold_phase_2")) {
			if (!(entity instanceof OssukageEntity _datEntL3 && _datEntL3.getEntityData().get(OssukageEntity.DATA_transform))) {
				if (entity instanceof LivingEntity _livingEntity4 && _livingEntity4.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
					_livingEntity4.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
				if (entity instanceof OssukageEntity) {
					((OssukageEntity) entity).setAnimation("death");
				}
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 5));
				RemnantBossesMod.queueServerWork((int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "transform_delay_phase_2"), () -> {
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
					for (int index0 = 0; index0 < (int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "skeletons_on_transform_phase_2"); index0++) {
						if (world instanceof ServerLevel _level) {
							Entity entityToSpawn = RemnantBossesModEntities.SKELETON_MINIONS.get().spawn(_level, BlockPos.containing(x + Mth.nextInt(RandomSource.create(), -10, 10), y, z + Mth.nextInt(RandomSource.create(), -10, 10)),
									MobSpawnType.MOB_SUMMONED);
							if (entityToSpawn != null) {
								entityToSpawn.setDeltaMovement(0, 0, 0);
							}
						}
					}
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "invisibility_timer_phase_2"), 3, false, true));
					if (entity instanceof LivingEntity _livingEntity17 && _livingEntity17.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
						_livingEntity17.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
					if (entity instanceof LivingEntity _livingEntity19 && _livingEntity19.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
						_livingEntity19.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_2"));
					if (entity instanceof LivingEntity _livingEntity21 && _livingEntity21.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
						_livingEntity21.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_2"));
				});
				if (entity instanceof OssukageEntity _datEntSetL)
					_datEntSetL.getEntityData().set(OssukageEntity.DATA_transform, true);
			}
		}
	}
}
