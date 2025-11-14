package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RatEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class RatOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof RatEntity _datEntSetI)
			_datEntSetI.getEntityData().set(RatEntity.DATA_skin, Mth.nextInt(RandomSource.create(), 0, 3));
	}
}