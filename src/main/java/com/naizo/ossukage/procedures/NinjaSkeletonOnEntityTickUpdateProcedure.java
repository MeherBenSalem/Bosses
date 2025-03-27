package com.naizo.ossukage.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

import com.naizo.ossukage.entity.OssukageEntity;
import com.naizo.ossukage.configuration.RemnantConfigConfiguration;

public class NinjaSkeletonOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double particleRadius = 0;
		double particleAmount = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if (entity instanceof OssukageEntity _datEntSetI)
				_datEntSetI.getEntityData().set(OssukageEntity.DATA_AI, (int) ((entity instanceof OssukageEntity _datEntI ? _datEntI.getEntityData().get(OssukageEntity.DATA_AI) : 0) + 1));
			if ((entity instanceof OssukageEntity _datEntI ? _datEntI.getEntityData().get(OssukageEntity.DATA_AI) : 0) == (double) RemnantConfigConfiguration.SHURIKEN_TIMER.get()) {
				ThrowKunaisProcedureProcedure.execute(entity);
			}
			if ((entity instanceof OssukageEntity _datEntI ? _datEntI.getEntityData().get(OssukageEntity.DATA_AI) : 0) == (double) RemnantConfigConfiguration.DASH_TIMER.get()) {
				DashAttackProcedureProcedure.execute(world, x, y, z, entity);
			}
		}
	}
}
