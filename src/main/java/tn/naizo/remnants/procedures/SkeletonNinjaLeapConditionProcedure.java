package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RemnantOssukageEntity;

import net.minecraft.world.entity.Entity;

public class SkeletonNinjaLeapConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity instanceof RemnantOssukageEntity _datEntS ? _datEntS.getEntityData().get(RemnantOssukageEntity.DATA_state) : "").equals("leap");
	}
}