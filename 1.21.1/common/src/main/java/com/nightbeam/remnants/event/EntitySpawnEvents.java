package com.nightbeam.remnants.event;

import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.procedures.OssukageOnInitialEntitySpawnProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class EntitySpawnEvents {
	private EntitySpawnEvents() {
	}

	public static boolean onEntityJoin(Entity entity, Level level) {
		if (level.isClientSide) {
			return true;
		}

		if (entity instanceof RemnantOssukageEntity ossukage) {
			initializeOssukageSpawn(ossukage);
		}
		return true;
	}

	private static void initializeOssukageSpawn(RemnantOssukageEntity entity) {
		OssukageOnInitialEntitySpawnProcedure.execute((LevelAccessor) entity.level(), entity);
	}
}
