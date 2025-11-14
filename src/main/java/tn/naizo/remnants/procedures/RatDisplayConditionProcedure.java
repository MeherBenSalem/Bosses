package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RatEntity;

import net.minecraft.world.entity.Entity;

public class RatDisplayConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity instanceof RatEntity _datEntI ? _datEntI.getEntityData().get(RatEntity.DATA_skin) : 0) == 0;
	}
}