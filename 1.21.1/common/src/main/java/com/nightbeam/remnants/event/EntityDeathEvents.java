package com.nightbeam.remnants.event;

import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class EntityDeathEvents {
	private EntityDeathEvents() {
	}

	public static void onLivingDeath(LivingEntity entity) {
		Level level = entity.level();
		if (level.isClientSide) {
			return;
		}
		if (entity instanceof RemnantOssukageEntity ossukage) {
			handleOssukageDeath(ossukage, level);
		}
	}

	private static void handleOssukageDeath(RemnantOssukageEntity entity, Level level) {
		// Boss music is stopped from RemnantOssukageEntity.die().
	}
}
