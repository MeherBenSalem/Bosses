package com.nightbeam.remnants.neoforge.registry;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.entity.UmbrakarEntity;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.neoforge.worldgen.ConfigurableSpawnBiomeModifier;
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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.mojang.serialization.MapCodec;

public final class NeoForgeModRegistry {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Constants.MOD_ID);
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Constants.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);
	public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister
			.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Constants.MOD_ID);

	private NeoForgeModRegistry() {
	}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		ENTITIES.register(modBus);
		SOUNDS.register(modBus);
		TABS.register(modBus);
		BIOME_MODIFIER_SERIALIZERS.register(modBus);

		bindBlock(ModBlocks.ANCIENT_ALTAR);
		bindBlock(ModBlocks.ANCIENT_PEDESTAL);

		bindItem(ModItems.RAT_FANG);
		bindItem(ModItems.OLD_SKELETON_BONE);
		bindItem(ModItems.OLD_SKELETON_HEAD);
		bindItem(ModItems.OSSUKAGE_SWORD);
		bindItem(ModItems.FANG_ON_A_STICK);
		bindItem(ModItems.ANCIENT_ALTAR);
		bindItem(ModItems.ANCIENT_PEDESTAL);

		bindEntity(ModEntities.KUNAI);
		bindEntity(ModEntities.RAT);
		bindEntity(ModEntities.SKELETON_MINION);
		bindEntity(ModEntities.REMNANT_OSSUKAGE);
		bindEntity(ModEntities.OSSUKAGE_RUNE_EFFECT);
		bindEntity(ModEntities.WRAITH);
		bindEntity(ModEntities.ARMORED_GRUB);
		bindEntity(ModEntities.UMBRAKAR);
		bindEntity(ModEntities.UMBRAKAR_ORB);

		bindSpawnEgg(ModEntities.RAT_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.RAT::get, 0xCC666B, 0xFF0000, new Item.Properties()));
		bindSpawnEgg(ModEntities.SKELETON_MINION_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.SKELETON_MINION::get, 0xFF8C8C, 0xFF0000, new Item.Properties()));
		bindSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.REMNANT_OSSUKAGE::get, 0xCC0000, 0xFF0000, new Item.Properties()));
		bindSpawnEgg(ModEntities.WRAITH_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.WRAITH::get, 0x000000, 0xFFFFFF, new Item.Properties()));
		bindSpawnEgg(ModEntities.ARMORED_GRUB_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.ARMORED_GRUB::get, 0x4A7C00, 0x8B5E00, new Item.Properties()));
		bindSpawnEgg(ModEntities.UMBRAKAR_SPAWN_EGG, () -> new DeferredSpawnEggItem(ModEntities.UMBRAKAR::get, 0x3A1A4A, 0xC48CFF, new Item.Properties()));

		bindSound(ModSounds.SKELETONFIGHT_THEME);
		bindSound(ModSounds.DASH_SFX);
		bindSound(ModSounds.ARMORED_GRUB_AMBIENT);
		bindSound(ModSounds.ARMORED_GRUB_DEATH);

		TABS.register(ModTabs.REMNANT_BOSSES_TAB.path(), () -> {
			CreativeModeTab tab = ModTabs.createTab();
			ModTabs.REMNANT_BOSSES_TAB.bind(tab);
			return tab;
		});

		BIOME_MODIFIER_SERIALIZERS.register("configurable_spawn", () -> ConfigurableSpawnBiomeModifier.CODEC);
	}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ModEntities.RAT.get(), RatEntity.createAttributes().build());
		event.put(ModEntities.SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes().build());
		event.put(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(ModEntities.WRAITH.get(), WraithEntity.createAttributes().build());
		event.put(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes().build());
		event.put(ModEntities.UMBRAKAR.get(), UmbrakarEntity.createAttributes().build());
		event.put(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbEntity.createAttributes().build());
	}

	public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		event.register(ModEntities.RAT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.SKELETON_MINION.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, RegisterSpawnPlacementsEvent.Operation.AND);
		event.register(ModEntities.WRAITH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
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
