package tn.naizo.remnants.event;

import tn.naizo.remnants.entity.RatEntity;
import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.entity.SkeletonMinionEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Handles entity spawn initialization for custom entities.
 * Replaces the deleted OssukageOnInitialEntitySpawnProcedure,
 * RatOnInitialEntitySpawnProcedure, and SkeletonMinionOnInitialEntitySpawnProcedure.
 */
@Mod.EventBusSubscriber(modid = "remnant_bosses")
public class EntitySpawnEvents {

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		Level level = event.getLevel();

		// Only run on server
		if (level.isClientSide) {
			return;
		}

		// Initialize Ossukage entity
		if (entity instanceof RemnantOssukageEntity ossukage) {
			initializeOssukageSpawn(ossukage);
		}

		// Initialize Rat entity
		if (entity instanceof RatEntity rat) {
			initializeRatSpawn(rat);
		}

		// Initialize Skeleton Minion entity
		if (entity instanceof SkeletonMinionEntity skeleton) {
			initializeSkeletonMinionSpawn(skeleton);
		}
	}

	/**
	 * Initialize Ossukage entity on spawn.
	 * Sets up initial state, AI behavior, and data values.
	 */
	private static void initializeOssukageSpawn(RemnantOssukageEntity entity) {
		// Initialize data accessors
		entity.setTransformed(false);
		entity.setAIState(0);
		entity.setEntityState("idle");

		// Boss bar setup is handled in the entity class itself via startSeenByPlayer()
	}

	/**
	 * Initialize Rat entity on spawn.
	 * Sets up initial skin variant and state.
	 */
	private static void initializeRatSpawn(RatEntity entity) {
		// Set random skin variant (0-3)
		int skinVariant = entity.getRandom().nextInt(4);
		entity.setSkinVariant(skinVariant);
	}

	/**
	 * Initialize Skeleton Minion entity on spawn.
	 * Sets up initial spawn state.
	 */
	private static void initializeSkeletonMinionSpawn(SkeletonMinionEntity entity) {
		// Mark as spawned
		entity.setSpawned(true);
	}
}
