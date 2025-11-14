package tn.naizo.remnants.procedures;

import net.minecraft.world.entity.Entity;

public class CheckIsIdleAnimProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return Math.hypot(entity.getDeltaMovement().x(), entity.getDeltaMovement().z()) == 0;
	}
}