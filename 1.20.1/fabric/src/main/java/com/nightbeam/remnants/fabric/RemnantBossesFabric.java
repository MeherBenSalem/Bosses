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
import com.nightbeam.remnants.event.GameEvents;
import com.nightbeam.remnants.event.SovereignRelics;
import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import com.nightbeam.remnants.entity.LostPilgrimEntity;
import com.nightbeam.remnants.entity.MemoryWraithEntity;
import com.nightbeam.remnants.entity.SoulCollectorEntity;
import com.nightbeam.remnants.entity.SoulWispEntity;
import com.nightbeam.remnants.entity.WatcherIllusionEntity;
import com.nightbeam.remnants.event.WatcherRelics;
import com.nightbeam.remnants.fabric.network.FabricNetwork;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.realm.RealmTravel;
import net.minecraft.core.particles.SimpleParticleType;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.platform.Services;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvent;
import software.bernie.geckolib.GeckoLib;

public class RemnantBossesFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		GeckoLib.initialize();
		Services.NETWORK = new FabricNetwork();
		FabricNetwork.registerServer();

		registerContent();
		registerAttributes();
		registerSpawnPlacements();
		registerEvents();

		ModEntities.initEntities();
		JaumlConfigBootstrap.initConfigs();
		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on Fabric", Constants.MOD_NAME);
	}

	private void registerContent() {
		registerBlock(ModBlocks.ANCIENT_ALTAR, ModBlocks.createAltar());
		registerBlock(ModBlocks.ANCIENT_PEDESTAL, ModBlocks.createPedestal());
		for (ModBlocks.Entry entry : ModBlocks.REALM_BLOCKS) registerBlock(entry.holder(), entry.factory().get());
		ModBlockEntities.ECLIPSE_WAYGATE.bind(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
				ModBlockEntities.ECLIPSE_WAYGATE.id(),
				FabricBlockEntityTypeBuilder.create(EclipseWaygateBlockEntity::new, ModBlocks.ECLIPSE_WAYGATE.get()).build()));
		for (RegistryHolder<SimpleParticleType> particle : ModParticles.ALL) {
			particle.bind(Registry.register(BuiltInRegistries.PARTICLE_TYPE, particle.id(), ModParticles.create()));
		}

		registerItem(ModItems.OSSUKAGE_SWORD, ModItems.createOssukageSword());
		registerItem(ModItems.BOSS_JOURNAL, ModItems.createBossJournal());
		registerItem(ModItems.ANCIENT_ALTAR, ModItems.createBlockItem(ModBlocks.ANCIENT_ALTAR));
		registerItem(ModItems.ANCIENT_PEDESTAL, ModItems.createBlockItem(ModBlocks.ANCIENT_PEDESTAL));
		for (ModItems.Entry entry : ModItems.SOVEREIGN_ITEMS) registerItem(entry.holder(), entry.factory().get());
		for (ModItems.Entry entry : ModItems.WARDEN_ITEMS) registerItem(entry.holder(), entry.factory().get());
		for (ModItems.Entry entry : ModItems.WATCHER_ITEMS) registerItem(entry.holder(), entry.factory().get());
		for (ModItems.Entry entry : ModItems.REALM_ITEMS) registerItem(entry.holder(), entry.factory().get());

		registerEntity(ModEntities.KUNAI, ModEntities.createKunai().build(ModEntities.KUNAI.path()));
		registerEntity(ModEntities.REMNANT_OSSUKAGE, ModEntities.createRemnantOssukage().build(ModEntities.REMNANT_OSSUKAGE.path()));
		registerEntity(ModEntities.OSSUKAGE_RUNE_EFFECT, ModEntities.createOssukageRuneEffect().build(ModEntities.OSSUKAGE_RUNE_EFFECT.path()));
		registerEntity(ModEntities.ARMORED_GRUB, ModEntities.createArmoredGrub().build(ModEntities.ARMORED_GRUB.path()));
		registerEntity(ModEntities.HOLLOW_SOVEREIGN, ModEntities.createHollowSovereign().build(ModEntities.HOLLOW_SOVEREIGN.path()));
		registerEntity(ModEntities.SICKLEGHAST, ModEntities.createSickleghast().build(ModEntities.SICKLEGHAST.path()));
		registerEntity(ModEntities.SOVEREIGN_PAULDRON, ModEntities.createSovereignPauldron().build(ModEntities.SOVEREIGN_PAULDRON.path()));
		registerEntity(ModEntities.SPECTRAL_CRESCENT, ModEntities.createSpectralCrescent().build(ModEntities.SPECTRAL_CRESCENT.path()));
		registerEntity(ModEntities.ROYAL_BLADE_MARK, ModEntities.createRoyalBladeMark().build(ModEntities.ROYAL_BLADE_MARK.path()));
		registerEntity(ModEntities.HOLLOW_SENTINEL, ModEntities.createHollowSentinel().build(ModEntities.HOLLOW_SENTINEL.path()));
		registerEntity(ModEntities.SPECTRAL_ROYAL_GUARD, ModEntities.createSpectralRoyalGuard().build(ModEntities.SPECTRAL_ROYAL_GUARD.path()));
		registerEntity(ModEntities.HOLLOW_ARBALIST, ModEntities.createHollowArbalist().build(ModEntities.HOLLOW_ARBALIST.path()));
		registerEntity(ModEntities.FALLEN_STANDARD_BEARER, ModEntities.createFallenStandardBearer().build(ModEntities.FALLEN_STANDARD_BEARER.path()));
		registerEntity(ModEntities.SOULBOUND_KNIGHT, ModEntities.createSoulboundKnight().build(ModEntities.SOULBOUND_KNIGHT.path()));
		registerEntity(ModEntities.GRAVEBOUND_WARDEN, ModEntities.createGraveboundWarden().build(ModEntities.GRAVEBOUND_WARDEN.path()));
		registerEntity(ModEntities.WARDEN_CHAIN, ModEntities.createWardenChain().build(ModEntities.WARDEN_CHAIN.path()));
		registerEntity(ModEntities.CHAIN_ANCHOR, ModEntities.createChainAnchor().build(ModEntities.CHAIN_ANCHOR.path()));
		registerEntity(ModEntities.GRAVEBOUND_SOLDIER, ModEntities.createGraveboundSoldier().build(ModEntities.GRAVEBOUND_SOLDIER.path()));
		registerEntity(ModEntities.CRYPT_LURKER, ModEntities.createCryptLurker().build(ModEntities.CRYPT_LURKER.path()));
		registerEntity(ModEntities.CATACOMB_RAT, ModEntities.createCatacombRat().build(ModEntities.CATACOMB_RAT.path()));
		registerEntity(ModEntities.BURIAL_GUARDIAN, ModEntities.createBurialGuardian().build(ModEntities.BURIAL_GUARDIAN.path()));
		registerEntity(ModEntities.FORGOTTEN_WATCHER, ModEntities.createForgottenWatcher().build(ModEntities.FORGOTTEN_WATCHER.path()));
		registerEntity(ModEntities.WATCHER_ILLUSION, ModEntities.createWatcherIllusion().build(ModEntities.WATCHER_ILLUSION.path()));
		registerEntity(ModEntities.MEMORY_BOLT, ModEntities.createMemoryBolt().build(ModEntities.MEMORY_BOLT.path()));
		registerEntity(ModEntities.LOST_PILGRIM, ModEntities.createLostPilgrim().build(ModEntities.LOST_PILGRIM.path()));
		registerEntity(ModEntities.MEMORY_WRAITH, ModEntities.createMemoryWraith().build(ModEntities.MEMORY_WRAITH.path()));
		registerEntity(ModEntities.SOUL_COLLECTOR, ModEntities.createSoulCollector().build(ModEntities.SOUL_COLLECTOR.path()));
		registerEntity(ModEntities.SOUL_WISP, ModEntities.createSoulWisp().build(ModEntities.SOUL_WISP.path()));

		registerItem(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, new SpawnEggItem(ModEntities.REMNANT_OSSUKAGE.get(), 0xCC0000, 0xFF0000, new Item.Properties()));
		registerItem(ModEntities.ARMORED_GRUB_SPAWN_EGG, new SpawnEggItem(ModEntities.ARMORED_GRUB.get(), 0x4A7C00, 0x8B5E00, new Item.Properties()));
		registerItem(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG, new SpawnEggItem(ModEntities.HOLLOW_SOVEREIGN.get(), ModEntities.SOVEREIGN_EGG_BASE, ModEntities.SOVEREIGN_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.SICKLEGHAST_SPAWN_EGG, new SpawnEggItem(ModEntities.SICKLEGHAST.get(), 0x947360, 0x64CE8A, new Item.Properties()));
		registerItem(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG, new SpawnEggItem(ModEntities.HOLLOW_SENTINEL.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG, new SpawnEggItem(ModEntities.HOLLOW_ARBALIST.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG, new SpawnEggItem(ModEntities.FALLEN_STANDARD_BEARER.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG, new SpawnEggItem(ModEntities.SOULBOUND_KNIGHT.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG, new SpawnEggItem(ModEntities.GRAVEBOUND_WARDEN.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.WARDEN_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG, new SpawnEggItem(ModEntities.GRAVEBOUND_SOLDIER.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.CRYPT_LURKER_SPAWN_EGG, new SpawnEggItem(ModEntities.CRYPT_LURKER.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.CATACOMB_RAT_SPAWN_EGG, new SpawnEggItem(ModEntities.CATACOMB_RAT.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG, new SpawnEggItem(ModEntities.BURIAL_GUARDIAN.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG, new SpawnEggItem(ModEntities.FORGOTTEN_WATCHER.get(), ModEntities.WATCHER_EGG_BASE, ModEntities.WATCHER_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.LOST_PILGRIM_SPAWN_EGG, new SpawnEggItem(ModEntities.LOST_PILGRIM.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.MEMORY_WRAITH_SPAWN_EGG, new SpawnEggItem(ModEntities.MEMORY_WRAITH.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS, new Item.Properties()));
		registerItem(ModEntities.SOUL_COLLECTOR_SPAWN_EGG, new SpawnEggItem(ModEntities.SOUL_COLLECTOR.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS, new Item.Properties()));

		registerSound(ModSounds.SKELETONFIGHT_THEME, ModSounds.createSkeletonFightTheme());
		registerSound(ModSounds.DASH_SFX, ModSounds.createDashSfx());
		registerSound(ModSounds.ARMORED_GRUB_AMBIENT, ModSounds.createArmoredGrubAmbient());
		registerSound(ModSounds.ARMORED_GRUB_DEATH, ModSounds.createArmoredGrubDeath());
		for (RegistryHolder<SoundEvent> sound : ModSounds.SOVEREIGN_SOUNDS) registerSound(sound, ModSounds.create(sound));
		for (RegistryHolder<SoundEvent> sound : ModSounds.REALM_SOUNDS) registerSound(sound, ModSounds.create(sound));

		CreativeModeTab tab = ModTabs.createTab();
		ModTabs.REMNANT_BOSSES_TAB.bind(Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModTabs.REMNANT_BOSSES_TAB.id(), tab));
	}

	private void registerAttributes() {
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

	private void registerSpawnPlacements() {
		// The Hollow Legion has no biome spawns; placements let the Hollow Citadel's spawn_overrides work.
		SpawnPlacements.register(ModEntities.HOLLOW_SENTINEL.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.HOLLOW_ARBALIST.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.FALLEN_STANDARD_BEARER.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.SOULBOUND_KNIGHT.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		// The Cryptborn have no overworld biome spawns; placements let the Gravebound Catacombs'
		// spawn_overrides and the Eclipsed Realm's biome spawners place them.
		SpawnPlacements.register(ModEntities.GRAVEBOUND_SOLDIER.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.CRYPT_LURKER.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.CATACOMB_RAT.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.BURIAL_GUARDIAN.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		// The Forgotten Souls: the Lantern Heights' and Gracewood's spawners and the Forgotten Observatory.
		SpawnPlacements.register(ModEntities.LOST_PILGRIM.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.MEMORY_WRAITH.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
		SpawnPlacements.register(ModEntities.SOUL_COLLECTOR.get(), SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::canMonsterSpawn);
	}

	private void registerEvents() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			RemnantBosses.tickWorkQueue();
			BossHunts.tick(server);
			SovereignRelics.serverTick(server);
			WardenRelics.serverTick(server);
			WatcherRelics.serverTick(server);
			RealmTravel.tick(server);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SovereignRelics.onLogin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			BossHunts.logout(handler.player);
			SovereignRelics.onLogout(handler.player);
			WardenRelics.onLogout(handler.player);
			WatcherRelics.onLogout(handler.player);
			RealmTravel.logout(handler.player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> SovereignRelics.onRespawn(newPlayer));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BossHunts.stop(server);
			SovereignRelics.stop();
			WardenRelics.stop();
			WatcherRelics.stop();
			RealmTravel.stop();
		});
		// Unbowed (Regalia 4-piece) and Jailer's Hold (Warden 4-piece): Fabric API 0.92 has no AFTER_DAMAGE event,
		// so LivingEntityDamageMixin calls SovereignRelics.afterDamage and WardenRelics.afterDamage with the same
		// amount Fabric's 1.21 AFTER_DAMAGE reports.

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!GameEvents.onEntityJoin(entity, world)) {
				entity.discard();
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register(GameEvents::onLivingDeath);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			GameEvents.onLivingHurt(entity, source, amount);
			return true;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (GameEvents.onRightClickBlock(player, world, hitResult.getBlockPos(), world.getBlockState(hitResult.getBlockPos()).getBlock())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static void registerBlock(RegistryHolder<Block> holder, Block value) {
		holder.bind(Registry.register(BuiltInRegistries.BLOCK, holder.id(), value));
	}

	private static void registerItem(RegistryHolder<Item> holder, Item value) {
		holder.bind(Registry.register(BuiltInRegistries.ITEM, holder.id(), value));
	}

	private static <T extends net.minecraft.world.entity.Entity> void registerEntity(RegistryHolder<EntityType<T>> holder, EntityType<T> value) {
		holder.bind(Registry.register(BuiltInRegistries.ENTITY_TYPE, holder.id(), value));
	}

	private static void registerSound(RegistryHolder<SoundEvent> holder, SoundEvent value) {
		holder.bind(Registry.register(BuiltInRegistries.SOUND_EVENT, holder.id(), value));
	}
}
