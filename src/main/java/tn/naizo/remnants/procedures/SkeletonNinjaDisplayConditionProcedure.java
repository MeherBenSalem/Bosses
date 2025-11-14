package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RemnantOssukageEntity;

import net.minecraft.world.entity.Entity;

public class SkeletonNinjaDisplayConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof RemnantOssukageEntity _datEntL0 && _datEntL0.getEntityData().get(RemnantOssukageEntity.DATA_transform);
	}
}