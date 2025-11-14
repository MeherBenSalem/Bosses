package tn.naizo.remnants.procedures;

import net.minecraft.world.entity.Entity;

public class SkeletonNinjaPlaybackConditionProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return !entity.isAlive();
	}
}