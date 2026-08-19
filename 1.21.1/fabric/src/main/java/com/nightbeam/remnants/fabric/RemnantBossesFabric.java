package com.nightbeam.remnants.fabric;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.JaumlConfigBootstrap;
import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.entity.UmbrakarEntity;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.event.BlockInteractionEvents;
import com.nightbeam.remnants.event.EntityDeathEvents;
import com.nightbeam.remnants.event.EntitySpawnEvents;
import com.nightbeam.remnants.event.EntityTickEvents;
import com.nightbeam.remnants.event.PlayerInteractionEvents;
import com.nightbeam.remnants.fabric.platform.FabricNetwork;
import com.nightbeam.remnants.fabric.registry.FabricModRegistry;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public class RemnantBossesFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Services.NETWORK = new FabricNetwork();
		FabricModRegistry.register();
		ModEntities.init();
		registerAttributes();
		registerSpawnPlacements();
		registerBiomeSpawns();
		registerEvents();
		registerNetworking();
		JaumlConfigBootstrap.initConfigs();
		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on Fabric", Constants.MOD_NAME);
	}

	private static void registerAttributes() {
		FabricDefaultAttributeRegistry.register(ModEntities.RAT.get(), RatEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.WRAITH.get(), WraithEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.UMBRAKAR.get(), UmbrakarEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbEntity.createAttributes());
	}

	private static void registerSpawnPlacements() {
		SpawnPlacements.register(ModEntities.RAT.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.SKELETON_MINION.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.WRAITH.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
	}

	private static void registerBiomeSpawns() {
		if (JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "enable_natural_spawning") <= 0) {
			return;
		}
		int weight = (int) JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "spawn_weight");
		int min = (int) JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "min_group_size");
		int max = (int) JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "max_group_size");
		BiomeModifications.addSpawn(context -> {
			if (!BiomeSelectors.foundInOverworld().test(context)) {
				return false;
			}
			String biomeKey = context.getBiomeKey().location().toString();
			return !JaumlConfigLib.getStringListValue("remnant/spawning", "rat_spawns", "biome_blacklist")
					.contains(biomeKey);
		}, MobCategory.MONSTER, ModEntities.RAT.get(), weight, min, max);
	}

	private static void registerEvents() {
		ServerTickEvents.END_SERVER_TICK.register(server -> RemnantBosses.tickWorkQueue());
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (BlockInteractionEvents.onRightClickBlock(player, world, hitResult.getBlockPos())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!EntitySpawnEvents.onEntityJoin(entity, world)) {
				entity.discard();
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> EntityDeathEvents.onLivingDeath(entity));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			EntityTickEvents.onLivingHurt(entity);
			PlayerInteractionEvents.onLivingHurt(entity, source.getEntity());
			return true;
		});
	}

	private static void registerNetworking() {
		PayloadTypeRegistry.playS2C().register(ClientboundBossMusicPacket.TYPE, ClientboundBossMusicPacket.STREAM_CODEC);
	}
}
