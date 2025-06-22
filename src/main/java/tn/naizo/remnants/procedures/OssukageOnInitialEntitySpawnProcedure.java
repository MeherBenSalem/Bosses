package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.OssukageEntity;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class OssukageOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof OssukageEntity) {
			((OssukageEntity) entity).setAnimation("spawn");
		}
		if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
			_livingEntity1.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
		RemnantBossesMod.queueServerWork(70, () -> {
			if (entity instanceof LivingEntity _livingEntity2 && _livingEntity2.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
				_livingEntity2.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
		});
	}
}
