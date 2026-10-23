package com.nightbeam.remnants.procedures;

import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.JaumlConfigLib;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class OssukageOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof RemnantOssukageEntity ossukage)
			ossukage.initializeEncounter();
	}
}
