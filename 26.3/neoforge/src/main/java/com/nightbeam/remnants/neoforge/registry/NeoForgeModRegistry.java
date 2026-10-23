package com.nightbeam.remnants.neoforge.registry;

import java.util.Set;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
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
import com.nightbeam.remnants.entity.AncientVesselEntity;
import com.nightbeam.remnants.entity.AshboundCrawlerEntity;
import com.nightbeam.remnants.entity.EmberServitorEntity;
import com.nightbeam.remnants.entity.RelicStalkerEntity;
import com.nightbeam.remnants.entity.RustedGuardianEntity;
import com.nightbeam.remnants.entity.ShardlingEntity;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import com.nightbeam.remnants.block.realm.GraceEmberBlockEntity;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeModRegistry {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Constants.ENTITY_NAMESPACE);
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Constants.MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Constants.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

	private NeoForgeModRegistry() {
	}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		ENTITIES.register(modBus);
		SOUNDS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		PARTICLES.register(modBus);
		TABS.register(modBus);

		bindBlock(ModBlocks.ANCIENT_ALTAR);
		bindBlock(ModBlocks.ANCIENT_PEDESTAL);
		for (ModBlocks.Entry entry : ModBlocks.REALM_BLOCKS) bindBlock(entry.holder());
		BLOCK_ENTITIES.register(ModBlockEntities.ECLIPSE_WAYGATE.path(), () -> {
			BlockEntityType<EclipseWaygateBlockEntity> type = new BlockEntityType<>(EclipseWaygateBlockEntity::new, Set.of(ModBlocks.ECLIPSE_WAYGATE.get()));
			ModBlockEntities.ECLIPSE_WAYGATE.bind(type);
			return type;
		});
		BLOCK_ENTITIES.register(ModBlockEntities.GRACE_EMBER.path(), () -> {
			BlockEntityType<GraceEmberBlockEntity> type = new BlockEntityType<>(GraceEmberBlockEntity::new, Set.of(ModBlocks.GRACE_EMBER.get()));
			ModBlockEntities.GRACE_EMBER.bind(type);
			return type;
		});
		for (RegistryHolder<SimpleParticleType> particle : ModParticles.ALL) {
			PARTICLES.register(particle.path(), () -> {
				SimpleParticleType type = ModParticles.create();
				particle.bind(type);
				return type;
			});
		}

		for (RegistryHolder<Item> item : ModItems.ALL) bindItem(item);
		for (RegistryHolder<Item> item : ModItems.REALM_ITEMS) bindItem(item);

		bindEntity(ModEntities.KUNAI);
		bindEntity(ModEntities.REMNANT_OSSUKAGE);
		bindEntity(ModEntities.OSSUKAGE_RUNE_EFFECT);
		bindEntity(ModEntities.HOLLOW_SOVEREIGN);
		bindEntity(ModEntities.SOVEREIGN_PAULDRON);
		bindEntity(ModEntities.SPECTRAL_CRESCENT);
		bindEntity(ModEntities.ROYAL_BLADE_MARK);
		bindEntity(ModEntities.HOLLOW_SENTINEL);
		bindEntity(ModEntities.SPECTRAL_ROYAL_GUARD);
		bindEntity(ModEntities.HOLLOW_ARBALIST);
		bindEntity(ModEntities.FALLEN_STANDARD_BEARER);
		bindEntity(ModEntities.SOULBOUND_KNIGHT);
		bindEntity(ModEntities.GRAVEBOUND_WARDEN);
		bindEntity(ModEntities.WARDEN_CHAIN);
		bindEntity(ModEntities.CHAIN_ANCHOR);
		bindEntity(ModEntities.GRAVEBOUND_SOLDIER);
		bindEntity(ModEntities.CRYPT_LURKER);
		bindEntity(ModEntities.CATACOMB_RAT);
		bindEntity(ModEntities.BURIAL_GUARDIAN);
		bindEntity(ModEntities.FORGOTTEN_WATCHER);
		bindEntity(ModEntities.WATCHER_ILLUSION);
		bindEntity(ModEntities.MEMORY_BOLT);
		bindEntity(ModEntities.LOST_PILGRIM);
		bindEntity(ModEntities.MEMORY_WRAITH);
		bindEntity(ModEntities.SOUL_COLLECTOR);
		bindEntity(ModEntities.SOUL_WISP);
		bindEntity(ModEntities.ASHBOUND_CRAWLER);
		bindEntity(ModEntities.EMBER_SERVITOR);
		bindEntity(ModEntities.RUSTED_GUARDIAN);
		bindEntity(ModEntities.EMBER_BOLT);
		bindEntity(ModEntities.SHARDLING);
		bindEntity(ModEntities.ANCIENT_VESSEL);
		bindEntity(ModEntities.RELIC_STALKER);
		bindEntity(ModEntities.CRYSTAL_BOLT);

		bindSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, ModEntities.REMNANT_OSSUKAGE.get()));
		bindSpawnEgg(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG, ModEntities.HOLLOW_SOVEREIGN.get()));
		bindSpawnEgg(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG, ModEntities.HOLLOW_SENTINEL.get()));
		bindSpawnEgg(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG, ModEntities.HOLLOW_ARBALIST.get()));
		bindSpawnEgg(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG, ModEntities.FALLEN_STANDARD_BEARER.get()));
		bindSpawnEgg(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG, ModEntities.SOULBOUND_KNIGHT.get()));
		bindSpawnEgg(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG, ModEntities.GRAVEBOUND_WARDEN.get()));
		bindSpawnEgg(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG, ModEntities.GRAVEBOUND_SOLDIER.get()));
		bindSpawnEgg(ModEntities.CRYPT_LURKER_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.CRYPT_LURKER_SPAWN_EGG, ModEntities.CRYPT_LURKER.get()));
		bindSpawnEgg(ModEntities.CATACOMB_RAT_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.CATACOMB_RAT_SPAWN_EGG, ModEntities.CATACOMB_RAT.get()));
		bindSpawnEgg(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG, ModEntities.BURIAL_GUARDIAN.get()));
		bindSpawnEgg(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG, ModEntities.FORGOTTEN_WATCHER.get()));
		bindSpawnEgg(ModEntities.LOST_PILGRIM_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.LOST_PILGRIM_SPAWN_EGG, ModEntities.LOST_PILGRIM.get()));
		bindSpawnEgg(ModEntities.MEMORY_WRAITH_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.MEMORY_WRAITH_SPAWN_EGG, ModEntities.MEMORY_WRAITH.get()));
		bindSpawnEgg(ModEntities.SOUL_COLLECTOR_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.SOUL_COLLECTOR_SPAWN_EGG, ModEntities.SOUL_COLLECTOR.get()));
		bindSpawnEgg(ModEntities.ASHBOUND_CRAWLER_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.ASHBOUND_CRAWLER_SPAWN_EGG, ModEntities.ASHBOUND_CRAWLER.get()));
		bindSpawnEgg(ModEntities.EMBER_SERVITOR_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.EMBER_SERVITOR_SPAWN_EGG, ModEntities.EMBER_SERVITOR.get()));
		bindSpawnEgg(ModEntities.RUSTED_GUARDIAN_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.RUSTED_GUARDIAN_SPAWN_EGG, ModEntities.RUSTED_GUARDIAN.get()));
		bindSpawnEgg(ModEntities.SHARDLING_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.SHARDLING_SPAWN_EGG, ModEntities.SHARDLING.get()));
		bindSpawnEgg(ModEntities.ANCIENT_VESSEL_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.ANCIENT_VESSEL_SPAWN_EGG, ModEntities.ANCIENT_VESSEL.get()));
		bindSpawnEgg(ModEntities.RELIC_STALKER_SPAWN_EGG, () -> ModEntities.spawnEgg(ModEntities.RELIC_STALKER_SPAWN_EGG, ModEntities.RELIC_STALKER.get()));

		for (RegistryHolder<SoundEvent> sound : ModSounds.ALL) bindSound(sound);

		TABS.register(ModTabs.REMNANT_BOSSES_TAB.path(), () -> {
			CreativeModeTab tab = ModTabs.createTab();
			ModTabs.REMNANT_BOSSES_TAB.bind(tab);
			return tab;
		});
	}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignEntity.createAttributes().build());
		event.put(ModEntities.SOVEREIGN_PAULDRON.get(), SovereignPauldronEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_SENTINEL.get(), HollowSentinelEntity.createAttributes().build());
		event.put(ModEntities.SPECTRAL_ROYAL_GUARD.get(), SpectralRoyalGuardEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_ARBALIST.get(), HollowArbalistEntity.createAttributes().build());
		event.put(ModEntities.FALLEN_STANDARD_BEARER.get(), FallenStandardBearerEntity.createAttributes().build());
		event.put(ModEntities.SOULBOUND_KNIGHT.get(), SoulboundKnightEntity.createAttributes().build());
		event.put(ModEntities.GRAVEBOUND_WARDEN.get(), GraveboundWardenEntity.createAttributes().build());
		event.put(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorEntity.createAttributes().build());
		event.put(ModEntities.GRAVEBOUND_SOLDIER.get(), GraveboundSoldierEntity.createAttributes().build());
		event.put(ModEntities.CRYPT_LURKER.get(), CryptLurkerEntity.createAttributes().build());
		event.put(ModEntities.CATACOMB_RAT.get(), CatacombRatEntity.createAttributes().build());
		event.put(ModEntities.BURIAL_GUARDIAN.get(), BurialGuardianEntity.createAttributes().build());
		event.put(ModEntities.FORGOTTEN_WATCHER.get(), ForgottenWatcherEntity.createAttributes().build());
		event.put(ModEntities.WATCHER_ILLUSION.get(), WatcherIllusionEntity.createAttributes().build());
		event.put(ModEntities.LOST_PILGRIM.get(), LostPilgrimEntity.createAttributes().build());
		event.put(ModEntities.MEMORY_WRAITH.get(), MemoryWraithEntity.createAttributes().build());
		event.put(ModEntities.SOUL_COLLECTOR.get(), SoulCollectorEntity.createAttributes().build());
		event.put(ModEntities.SOUL_WISP.get(), SoulWispEntity.createAttributes().build());
		event.put(ModEntities.ASHBOUND_CRAWLER.get(), AshboundCrawlerEntity.createAttributes().build());
		event.put(ModEntities.EMBER_SERVITOR.get(), EmberServitorEntity.createAttributes().build());
		event.put(ModEntities.RUSTED_GUARDIAN.get(), RustedGuardianEntity.createAttributes().build());
		event.put(ModEntities.SHARDLING.get(), ShardlingEntity.createAttributes().build());
		event.put(ModEntities.ANCIENT_VESSEL.get(), AncientVesselEntity.createAttributes().build());
		event.put(ModEntities.RELIC_STALKER.get(), RelicStalkerEntity.createAttributes().build());
	}

	public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		// The Hollow Legion has no biome spawns; these placements let the Hollow Citadel spawn_overrides work.
		event.register(ModEntities.HOLLOW_SENTINEL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.HOLLOW_ARBALIST.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.FALLEN_STANDARD_BEARER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.SOULBOUND_KNIGHT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		// The Cryptborn have no overworld biome spawns; placements let the Gravebound Catacombs'
		// spawn_overrides and the Eclipsed Realm's biome spawners place them.
		event.register(ModEntities.GRAVEBOUND_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.CRYPT_LURKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.CATACOMB_RAT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.BURIAL_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		// The Forgotten Souls: the Lantern Heights' and Gracewood's spawners and the Forgotten Observatory.
		event.register(ModEntities.LOST_PILGRIM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.MEMORY_WRAITH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.SOUL_COLLECTOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		// The Ashbound Constructs and the Corrupted Relics: the Eclipsed Realm's biome spawners only.
		event.register(ModEntities.ASHBOUND_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.EMBER_SERVITOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.RUSTED_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.SHARDLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.ANCIENT_VESSEL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.RELIC_STALKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
	}

	private static void bindBlock(RegistryHolder<Block> holder) {
		BLOCKS.register(holder.path(), holder::get);
	}

	private static void bindItem(RegistryHolder<Item> holder) {
		ITEMS.register(holder.path(), holder::get);
	}

	private static <T extends net.minecraft.world.entity.Entity> void bindEntity(RegistryHolder<EntityType<T>> holder) {
		ENTITIES.register(holder.path(), holder::get);
	}

	private static void bindSpawnEgg(RegistryHolder<Item> holder, java.util.function.Supplier<Item> factory) {
		ITEMS.register(holder.path(), () -> {
			Item item = factory.get();
			holder.bind(item);
			return item;
		});
	}

	private static void bindSound(RegistryHolder<SoundEvent> holder) {
		SOUNDS.register(holder.path(), holder::get);
	}
}
