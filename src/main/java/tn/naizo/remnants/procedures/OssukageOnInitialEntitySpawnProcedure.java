package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.OssukageEntity;
import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.jauml.JaumlConfigLib;

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
		if (entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
			_livingEntity3.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_1"));
		if (entity instanceof LivingEntity _livingEntity5 && _livingEntity5.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
			_livingEntity5.getAttribute(Attributes.MAX_HEALTH).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "max_health_phase_1"));
		if (entity instanceof LivingEntity _entity)
			_entity.setHealth((float) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "max_health_phase_1"));
		RemnantBossesMod.queueServerWork(70, () -> {
			if (entity instanceof LivingEntity _livingEntity9 && _livingEntity9.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
				_livingEntity9.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_1"));
		});
	}
}
