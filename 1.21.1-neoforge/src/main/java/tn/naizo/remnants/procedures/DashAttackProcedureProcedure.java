package tn.naizo.remnants.procedures;

import tn.naizo.remnants.init.ModEntities;
import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.remnants.config.JaumlConfigLib;

import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
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
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
				((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
		if (entity instanceof RemnantOssukageEntity _datEntL7 && _datEntL7.getEntityData().get(RemnantOssukageEntity.DATA_transform)) {
			if (Mth.nextInt(RandomSource.create(), 0, 100) <= JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "special_attack_chance_phase_2")) {
				if (world instanceof Level _level && !_level.isClientSide())
					_level.explode(null, ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 4, Level.ExplosionInteraction.MOB);
			} else if (Mth.nextInt(RandomSource.create(), 0, 100) <= JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "special_attack_chance_phase_2")) {
				if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 150, 1, false, false));
			} else if (Mth.nextInt(RandomSource.create(), 0, 100) <= JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "special_attack_chance_phase_2")) {
				for (int index0 = 0; index0 < (int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "skeletons_on_dash_phase_2"); index0++) {
					if (world instanceof ServerLevel _level) {
						Entity entityToSpawn = ModEntities.SKELETON_MINION.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
						if (entityToSpawn != null) {
							entityToSpawn.setDeltaMovement(0, 0, 0);
						}
					}
				}
			}
		}
		if (entity instanceof RemnantOssukageEntity _datEntSetS)
			_datEntSetS.getEntityData().set(RemnantOssukageEntity.DATA_state, "leap");
		RemnantBossesMod.queueServerWork(60, () -> {
			if (entity instanceof RemnantOssukageEntity _datEntSetS)
				_datEntSetS.getEntityData().set(RemnantOssukageEntity.DATA_state, "");
		});
		entity.setDeltaMovement(new Vec3(((entity.getDeltaMovement().x() + entity.getLookAngle().x) * JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "dash_distance")),
				((entity.getDeltaMovement().y() + entity.getLookAngle().y) * JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "dash_distance")),
				((entity.getDeltaMovement().z() + entity.getLookAngle().z) * JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "dash_distance"))));
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), NeoForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("remnant_bosses:dash_sfx")), SoundSource.HOSTILE, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, NeoForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("remnant_bosses:dash_sfx")), SoundSource.HOSTILE, 1, 1, false);
			}
		}
		for (int index1 = 0; index1 < 5; index1++) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 5, 3, 3, 3, 1);
		}
		if (entity instanceof RemnantOssukageEntity _datEntSetI)
			_datEntSetI.getEntityData().set(RemnantOssukageEntity.DATA_AI, 0);
	}
}
