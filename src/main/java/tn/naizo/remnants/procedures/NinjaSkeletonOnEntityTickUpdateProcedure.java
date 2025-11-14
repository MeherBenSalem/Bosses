package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.jauml.JaumlConfigLib;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class NinjaSkeletonOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double particleRadius = 0;
		double particleAmount = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if (entity instanceof RemnantOssukageEntity _datEntSetI)
				_datEntSetI.getEntityData().set(RemnantOssukageEntity.DATA_AI, (int) ((entity instanceof RemnantOssukageEntity _datEntI ? _datEntI.getEntityData().get(RemnantOssukageEntity.DATA_AI) : 0) + 1));
			if ((entity instanceof RemnantOssukageEntity _datEntI ? _datEntI.getEntityData().get(RemnantOssukageEntity.DATA_AI) : 0) == JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_timer")) {
				ThrowKunaisProcedureProcedure.execute(entity);
			}
			if ((entity instanceof RemnantOssukageEntity _datEntI ? _datEntI.getEntityData().get(RemnantOssukageEntity.DATA_AI) : 0) == JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "dash_timer")) {
				DashAttackProcedureProcedure.execute(world, x, y, z, entity);
			}
		}
	}
}