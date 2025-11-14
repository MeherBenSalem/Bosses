package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.SkeletonMinionEntity;

import net.minecraft.world.entity.Entity;

public class SkeletonSpawnConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return entity instanceof SkeletonMinionEntity _datEntL0 && _datEntL0.getEntityData().get(SkeletonMinionEntity.DATA_Spawned);
	}
}