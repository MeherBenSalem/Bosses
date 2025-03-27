package com.naizo.ossukage.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import com.naizo.ossukage.configuration.RemnantConfigConfiguration;

public class OssukageOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
			_livingEntity1.getAttribute(Attributes.MAX_HEALTH).setBaseValue(((double) RemnantConfigConfiguration.MAX_HEALTH_PHASE_1.get()));
		if (entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
			_livingEntity3.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(((double) RemnantConfigConfiguration.MOVEMENT_SPEED_PHASE_1.get()));
		if (entity instanceof LivingEntity _livingEntity5 && _livingEntity5.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
			_livingEntity5.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(((double) RemnantConfigConfiguration.ATTACK_DAMAGE_PHASE_1.get()));
		if (entity instanceof LivingEntity _entity)
			_entity.setHealth((float) (entity instanceof LivingEntity _livingEntity6 && _livingEntity6.getAttributes().hasAttribute(Attributes.MAX_HEALTH) ? _livingEntity6.getAttribute(Attributes.MAX_HEALTH).getBaseValue() : 0));
	}
}
