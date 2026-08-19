package com.nightbeam.remnants.event;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
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

		// Natural-spawn dimension limits apply only to rats/wraiths.
		// Bosses and ritual minions must spawn from commands, eggs, altars, and
		// other mods (e.g. Craft to Exile 2) in any dimension.
		if (entity instanceof RatEntity || entity instanceof WraithEntity) {
			if (!isDimensionAllowed(level)) {
				return false;
			}
		}

		if (entity instanceof RemnantOssukageEntity ossukage) {
			initializeOssukageSpawn(ossukage);
		}
		if (entity instanceof RatEntity rat) {
			initializeRatSpawn(rat);
		}
		if (entity instanceof SkeletonMinionEntity skeleton) {
			initializeSkeletonMinionSpawn(skeleton);
		}
		if (entity instanceof WraithEntity wraith) {
			initializeWraithSpawn(wraith);
		}
		return true;
	}

	private static boolean isDimensionAllowed(Level level) {
		String dimensionKey = level.dimension().location().toString();

		java.util.List<String> whitelist = JaumlConfigLib.getStringListValue("remnant/spawning", "rat_spawns",
				"dimension_whitelist");
		if (!whitelist.isEmpty() && !whitelist.contains(dimensionKey)) {
			return false;
		}

		java.util.List<String> blacklist = JaumlConfigLib.getStringListValue("remnant/spawning", "rat_spawns",
				"dimension_blacklist");
		return !blacklist.contains(dimensionKey);
	}

	private static void initializeOssukageSpawn(RemnantOssukageEntity entity) {
		OssukageOnInitialEntitySpawnProcedure.execute((LevelAccessor) entity.level(), entity);
	}

	private static void initializeRatSpawn(RatEntity entity) {
		entity.setSkinVariant(entity.getRandom().nextInt(4));
	}

	private static void initializeSkeletonMinionSpawn(SkeletonMinionEntity entity) {
		entity.setSpawned(true);
	}

	private static void initializeWraithSpawn(WraithEntity entity) {
	}
}
