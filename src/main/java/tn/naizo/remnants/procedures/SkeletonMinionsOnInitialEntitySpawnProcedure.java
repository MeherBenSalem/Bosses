package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.SkeletonMinionsEntity;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class SkeletonMinionsOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
			_livingEntity0.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
		if (entity instanceof SkeletonMinionsEntity) {
			((SkeletonMinionsEntity) entity).setAnimation("spawn");
		}
		RemnantBossesMod.queueServerWork(90, () -> {
			if (entity instanceof LivingEntity _livingEntity2 && _livingEntity2.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
				_livingEntity2.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
		});
	}
}
