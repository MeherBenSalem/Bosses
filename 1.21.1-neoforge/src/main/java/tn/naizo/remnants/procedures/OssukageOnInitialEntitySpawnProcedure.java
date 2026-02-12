package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.remnants.config.JaumlConfigLib;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class OssukageOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
			_livingEntity0.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
		if (entity instanceof LivingEntity _livingEntity2 && _livingEntity2.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
			_livingEntity2.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_1"));
		if (entity instanceof LivingEntity _livingEntity4 && _livingEntity4.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
			_livingEntity4.getAttribute(Attributes.MAX_HEALTH).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "max_health_phase_1"));
		if (entity instanceof LivingEntity _entity)
			_entity.setHealth((float) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "max_health_phase_1"));
		if (entity instanceof RemnantOssukageEntity _datEntSetS)
			_datEntSetS.getEntityData().set(RemnantOssukageEntity.DATA_state, "spawn");
		RemnantBossesMod.queueServerWork(70, () -> {
			if (entity instanceof RemnantOssukageEntity _datEntSetS)
				_datEntSetS.getEntityData().set(RemnantOssukageEntity.DATA_state, "");
			if (entity instanceof LivingEntity _livingEntity10 && _livingEntity10.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
				_livingEntity10.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_1"));
		});
	}
}
