package com.nightbeam.remnants.fabric;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.config.JaumlConfigBootstrap;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
import com.nightbeam.remnants.entity.SickleghastEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.FallenStandardBearerEntity;
import com.nightbeam.remnants.entity.HollowArbalistEntity;
import com.nightbeam.remnants.entity.HollowSentinelEntity;
import com.nightbeam.remnants.entity.SoulboundKnightEntity;
import com.nightbeam.remnants.entity.SovereignPauldronEntity;
import com.nightbeam.remnants.entity.SpectralRoyalGuardEntity;
import com.nightbeam.remnants.entity.BurialGuardianEntity;
import com.nightbeam.remnants.entity.CatacombRatEntity;
import com.nightbeam.remnants.entity.ChainAnchorEntity;
import com.nightbeam.remnants.entity.CryptLurkerEntity;
import com.nightbeam.remnants.entity.GraveboundSoldierEntity;
import com.nightbeam.remnants.entity.GraveboundWardenEntity;
import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import com.nightbeam.remnants.entity.LostPilgrimEntity;
import com.nightbeam.remnants.entity.MemoryWraithEntity;
import com.nightbeam.remnants.entity.SoulCollectorEntity;
import com.nightbeam.remnants.entity.SoulWispEntity;
import com.nightbeam.remnants.entity.WatcherIllusionEntity;
import com.nightbeam.remnants.event.WatcherRelics;
import com.nightbeam.remnants.event.BlockInteractionEvents;
import com.nightbeam.remnants.event.EntityDeathEvents;
import com.nightbeam.remnants.event.EntitySpawnEvents;
import com.nightbeam.remnants.event.EntityTickEvents;
import com.nightbeam.remnants.event.PlayerInteractionEvents;
import com.nightbeam.remnants.event.SovereignRelics;
import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.fabric.platform.FabricNetwork;
import com.nightbeam.remnants.fabric.registry.FabricModRegistry;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.realm.RealmTravel;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.InteractionResult;
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
		registerEvents();
		registerNetworking();
		JaumlConfigBootstrap.initConfigs();
		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on Fabric", Constants.MOD_NAME);
	}

	private static void registerAttributes() {
		FabricDefaultAttributeRegistry.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SICKLEGHAST.get(), SickleghastEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SOVEREIGN_PAULDRON.get(), SovereignPauldronEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.HOLLOW_SENTINEL.get(), HollowSentinelEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SPECTRAL_ROYAL_GUARD.get(), SpectralRoyalGuardEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.HOLLOW_ARBALIST.get(), HollowArbalistEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.FALLEN_STANDARD_BEARER.get(), FallenStandardBearerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SOULBOUND_KNIGHT.get(), SoulboundKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.GRAVEBOUND_WARDEN.get(), GraveboundWardenEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.GRAVEBOUND_SOLDIER.get(), GraveboundSoldierEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.CRYPT_LURKER.get(), CryptLurkerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.CATACOMB_RAT.get(), CatacombRatEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.BURIAL_GUARDIAN.get(), BurialGuardianEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.FORGOTTEN_WATCHER.get(), ForgottenWatcherEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.WATCHER_ILLUSION.get(), WatcherIllusionEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.LOST_PILGRIM.get(), LostPilgrimEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.MEMORY_WRAITH.get(), MemoryWraithEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SOUL_COLLECTOR.get(), SoulCollectorEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SOUL_WISP.get(), SoulWispEntity.createAttributes());
	}

	private static void registerSpawnPlacements() {
		// The Hollow Legion has no biome spawns; placements let the Hollow Citadel's spawn_overrides work.
		SpawnPlacements.register(ModEntities.HOLLOW_SENTINEL.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.HOLLOW_ARBALIST.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.FALLEN_STANDARD_BEARER.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.SOULBOUND_KNIGHT.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		// The Cryptborn have no overworld biome spawns; placements let the Gravebound Catacombs'
		// spawn_overrides and the Eclipsed Realm's biome spawners place them.
		SpawnPlacements.register(ModEntities.GRAVEBOUND_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.CRYPT_LURKER.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.CATACOMB_RAT.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.BURIAL_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		// The Forgotten Souls: the Lantern Heights' and Gracewood's spawners and the Forgotten Observatory.
		SpawnPlacements.register(ModEntities.LOST_PILGRIM.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.MEMORY_WRAITH.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.SOUL_COLLECTOR.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
	}

	private static void registerEvents() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			RemnantBosses.tickWorkQueue();
			BossHunts.tick(server);
			SovereignRelics.serverTick(server);
			RealmTravel.tick(server);
			WardenRelics.serverTick(server);
			WatcherRelics.serverTick(server);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SovereignRelics.onLogin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			BossHunts.logout(handler.player);
			SovereignRelics.onLogout(handler.player);
			RealmTravel.logout(handler.player);
			WardenRelics.onLogout(handler.player);
			WatcherRelics.onLogout(handler.player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> SovereignRelics.onRespawn(newPlayer));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BossHunts.stop(server);
			SovereignRelics.stop();
			RealmTravel.stop();
			WardenRelics.stop();
			WatcherRelics.stop();
		});
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
		ServerLivingEntityEvents.AFTER_DEATH.register(EntityDeathEvents::onLivingDeath);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			SovereignRelics.afterDamage(entity, source, damageTaken);
			WardenRelics.afterDamage(entity, source, damageTaken);
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			EntityTickEvents.onLivingHurt(entity);
			PlayerInteractionEvents.onLivingHurt(entity, source.getEntity());
			return true;
		});
	}

	private static void registerNetworking() {
		PayloadTypeRegistry.playS2C().register(ClientboundBossMusicPacket.TYPE, ClientboundBossMusicPacket.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClientboundJournalPacket.TYPE, ClientboundJournalPacket.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ServerboundJournalRequestPacket.TYPE, ServerboundJournalRequestPacket.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClientboundRealmFxPacket.TYPE, ClientboundRealmFxPacket.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClientboundGraceMenuPacket.TYPE, ClientboundGraceMenuPacket.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ServerboundGraceActionPacket.TYPE, ServerboundGraceActionPacket.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ServerboundGraceActionPacket.TYPE, (packet, context) ->
				context.server().execute(() -> ServerboundGraceActionPacket.handleServer(packet, context.player())));
		ServerPlayNetworking.registerGlobalReceiver(ServerboundJournalRequestPacket.TYPE, (packet, context) ->
				context.server().execute(() -> BossHunts.request(context.player(), packet.schemaVersion())));
	}
}
