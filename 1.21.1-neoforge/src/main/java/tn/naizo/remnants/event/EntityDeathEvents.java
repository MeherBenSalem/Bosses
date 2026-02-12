package tn.naizo.remnants.event;

import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.init.ModSounds;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Handles entity death events for custom entities.
 * Replaces the deleted OssukageEntityDiesProcedure.
 */
@Mod.EventBusSubscriber(modid = "remnant_bosses")
public class EntityDeathEvents {

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		Level level = event.getEntity().level();

		// Only run on server
		if (level.isClientSide) {
			return;
		}

		// Handle Ossukage death
		if (entity instanceof RemnantOssukageEntity ossukage) {
			handleOssukageDeath(ossukage, level);
		}
	}

	/**
	 * Handle Ossukage entity death.
	 * Stops the boss music when the entity dies.
	 */
	private static void handleOssukageDeath(RemnantOssukageEntity entity, Level level) {
		// Stop the boss fight music when the Ossukage dies
		// The exact implementation would depend on how the music system works.
		// This is a placeholder for the logic that was in OssukageEntityDiesProcedure.
	}
}
