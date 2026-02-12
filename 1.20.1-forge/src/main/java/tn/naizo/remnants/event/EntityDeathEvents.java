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
@Mod.EventBusSubscriber(modid = "remnant_bosses", bus = Mod.EventBusSubscriber.Bus.FORGE)
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
		// Stop the boss fight music when the Ossukage dies (use server command like previous implementation)
		if (!level.isClientSide() && level.getServer() != null) {
			level.getServer().getCommands().performPrefixedCommand(
				new net.minecraft.commands.CommandSourceStack(net.minecraft.commands.CommandSource.NULL, entity.position(), entity.getRotationVector(),
					level instanceof net.minecraft.server.level.ServerLevel ? (net.minecraft.server.level.ServerLevel) level : null, 4, entity.getName().getString(), entity.getDisplayName(), level.getServer(), entity),
				"stopsound @p music remnant_bosses:skeletonfight_theme"
			);
		}
	}
}
