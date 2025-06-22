package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.OssukageEntity;
import tn.naizo.remnants.configuration.MainConfigConfiguration;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class DashAttackProcedureProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double particleRadius = 0;
		double particleAmount = 0;
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
				((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
		if (entity instanceof OssukageEntity _datEntL7 && _datEntL7.getEntityData().get(OssukageEntity.DATA_transform)) {
			if (Mth.nextInt(RandomSource.create(), 0, 100) <= (double) MainConfigConfiguration.SPECIAL_ATTACK_CHANCE.get()) {
				if (world instanceof ServerLevel _level) {
					LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
					entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX(), (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY(),
							(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));;
					_level.addFreshEntity(entityToSpawn);
				}
			} else if (Mth.nextInt(RandomSource.create(), 0, 100) <= (double) MainConfigConfiguration.SPECIAL_ATTACK_CHANCE.get()) {
				if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 150, 1, false, false));
			} else if (Mth.nextInt(RandomSource.create(), 0, 100) <= (double) MainConfigConfiguration.SPECIAL_ATTACK_CHANCE.get()) {
				for (int index0 = 0; index0 < (int) (double) MainConfigConfiguration.SKELETONS_ON_DASH.get(); index0++) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = EntityType.SKELETON.spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setDeltaMovement(0, 0, 0);
						}
					}
				}
			}
		}
		if (entity instanceof OssukageEntity) {
			((OssukageEntity) entity).setAnimation("leap_attack");
		}
		entity.setDeltaMovement(new Vec3(((entity.getDeltaMovement().x() + entity.getLookAngle().x) * (double) MainConfigConfiguration.DASH_DISTANCE.get()),
				((entity.getDeltaMovement().y() + entity.getLookAngle().y) * (double) MainConfigConfiguration.DASH_DISTANCE.get()), ((entity.getDeltaMovement().z() + entity.getLookAngle().z) * (double) MainConfigConfiguration.DASH_DISTANCE.get())));
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("remnant_bosses:dash_sfx")), SoundSource.HOSTILE, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("remnant_bosses:dash_sfx")), SoundSource.HOSTILE, 1, 1, false);
			}
		}
		particleAmount = 15;
		particleRadius = 2;
		for (int index1 = 0; index1 < 5; index1++) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 5, 3, 3, 3, 1);
		}
		if (entity instanceof OssukageEntity _datEntSetI)
			_datEntSetI.getEntityData().set(OssukageEntity.DATA_AI, 0);
	}
}
