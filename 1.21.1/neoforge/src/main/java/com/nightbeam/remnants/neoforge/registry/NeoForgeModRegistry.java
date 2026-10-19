package com.nightbeam.remnants.neoforge.registry;

import com.nightbeam.remnants.Constants;
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
import com.nightbeam.remnants.init.ModArmorMaterials;
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
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
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
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeModRegistry {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Constants.ENTITY_NAMESPACE);
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Constants.MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Constants.MOD_ID);
	public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Constants.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

	private NeoForgeModRegistry() {
	}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ARMOR_MATERIALS.register(modBus);
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
			BlockEntityType<EclipseWaygateBlockEntity> type = BlockEntityType.Builder.of(EclipseWaygateBlockEntity::new,
					ModBlocks.ECLIPSE_WAYGATE.get()).build(null);
			ModBlockEntities.ECLIPSE_WAYGATE.bind(type);
			return type;
		});
		for (RegistryHolder<SimpleParticleType> particle : ModParticles.ALL) {
			PARTICLES.register(particle.path(), () -> {
				SimpleParticleType type = ModParticles.create();
				particle.bind(type);
				return type;
			});
		}

		// The DeferredHolder exists now and binds when ARMOR_MATERIAL registers; armor items only store it.
		DeferredHolder<ArmorMaterial, ArmorMaterial> regalia = ARMOR_MATERIALS.register(ModArmorMaterials.REGALIA.path(), ModArmorMaterials.REGALIA::get);
		ModArmorMaterials.REGALIA.bindHolder(regalia);
		DeferredHolder<ArmorMaterial, ArmorMaterial> warden = ARMOR_MATERIALS.register(ModArmorMaterials.WARDEN.path(), ModArmorMaterials.WARDEN::get);
		ModArmorMaterials.WARDEN.bindHolder(warden);
		DeferredHolder<ArmorMaterial, ArmorMaterial> archivist = ARMOR_MATERIALS.register(ModArmorMaterials.ARCHIVIST.path(), ModArmorMaterials.ARCHIVIST::get);
		ModArmorMaterials.ARCHIVIST.bindHolder(archivist);

		for (RegistryHolder<Item> item : ModItems.ALL) bindItem(item);
		for (RegistryHolder<Item> item : ModItems.REALM_ITEMS) bindItem(item);

		bindEntity(ModEntities.KUNAI);
		bindEntity(ModEntities.REMNANT_OSSUKAGE);
		bindEntity(ModEntities.OSSUKAGE_RUNE_EFFECT);
		bindEntity(ModEntities.ARMORED_GRUB);
		bindEntity(ModEntities.HOLLOW_SOVEREIGN);
		bindEntity(ModEntities.SICKLEGHAST);
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

		bindSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.REMNANT_OSSUKAGE::get, 0xCC0000, 0xFF0000, new Item.Properties()));
		bindSpawnEgg(ModEntities.ARMORED_GRUB_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.ARMORED_GRUB::get, 0x4A7C00, 0x8B5E00, new Item.Properties()));
		bindSpawnEgg(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.HOLLOW_SOVEREIGN::get, ModEntities.SOVEREIGN_EGG_BASE, ModEntities.SOVEREIGN_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.SICKLEGHAST_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.SICKLEGHAST::get, 0x947360, 0x64CE8A, new Item.Properties()));
		bindSpawnEgg(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.HOLLOW_SENTINEL::get, ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.HOLLOW_ARBALIST::get, ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.FALLEN_STANDARD_BEARER::get, ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.SOULBOUND_KNIGHT::get, ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.GRAVEBOUND_WARDEN::get, ModEntities.CRYPTBORN_EGG_BASE, ModEntities.WARDEN_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.GRAVEBOUND_SOLDIER::get, ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.CRYPT_LURKER_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.CRYPT_LURKER::get, ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.CATACOMB_RAT_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.CATACOMB_RAT::get, ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.BURIAL_GUARDIAN::get, ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.FORGOTTEN_WATCHER::get, ModEntities.WATCHER_EGG_BASE, ModEntities.WATCHER_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.LOST_PILGRIM_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.LOST_PILGRIM::get, ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.MEMORY_WRAITH_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.MEMORY_WRAITH::get, ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS, new Item.Properties()));
		bindSpawnEgg(ModEntities.SOUL_COLLECTOR_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.SOUL_COLLECTOR::get, ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS, new Item.Properties()));

		for (RegistryHolder<SoundEvent> sound : ModSounds.ALL) bindSound(sound);

		TABS.register(ModTabs.REMNANT_BOSSES_TAB.path(), () -> {
			CreativeModeTab tab = ModTabs.createTab();
			ModTabs.REMNANT_BOSSES_TAB.bind(tab);
			return tab;
		});
	}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignEntity.createAttributes().build());
		event.put(ModEntities.SICKLEGHAST.get(), SickleghastEntity.createAttributes().build());
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
