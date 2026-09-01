package com.nightbeam.remnants.fabric.registry;

import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class FabricModRegistry {
	private FabricModRegistry() {
	}

	public static void register() {
		registerBlock(ModBlocks.ANCIENT_ALTAR);
		registerBlock(ModBlocks.ANCIENT_PEDESTAL);

		registerItem(ModItems.RAT_FANG);
		registerItem(ModItems.OLD_SKELETON_BONE);
		registerItem(ModItems.OLD_SKELETON_HEAD);
		registerItem(ModItems.OSSUKAGE_SWORD);
		registerItem(ModItems.FANG_ON_A_STICK);
		registerItem(ModItems.ANCIENT_ALTAR);
		registerItem(ModItems.ANCIENT_PEDESTAL);

		registerEntity(ModEntities.KUNAI);
		registerEntity(ModEntities.RAT);
		registerEntity(ModEntities.SKELETON_MINION);
		registerEntity(ModEntities.REMNANT_OSSUKAGE);
		registerEntity(ModEntities.OSSUKAGE_RUNE_EFFECT);
		registerEntity(ModEntities.WRAITH);
		registerEntity(ModEntities.ARMORED_GRUB);
		registerEntity(ModEntities.UMBRAKAR);
		registerEntity(ModEntities.UMBRAKAR_ORB);
		registerEntity(ModEntities.KOTSUKAGE);
		registerEntity(ModEntities.KOTSUKAGE_TRAP);

		registerSpawnEgg(ModEntities.RAT_SPAWN_EGG, ModEntities.RAT.get(), 0xCC666B, 0xFF0000);
		registerSpawnEgg(ModEntities.SKELETON_MINION_SPAWN_EGG, ModEntities.SKELETON_MINION.get(), 0xFF8C8C, 0xFF0000);
		registerSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, ModEntities.REMNANT_OSSUKAGE.get(), 0xCC0000, 0xFF0000);
		registerSpawnEgg(ModEntities.WRAITH_SPAWN_EGG, ModEntities.WRAITH.get(), 0x000000, 0xFFFFFF);
		registerSpawnEgg(ModEntities.ARMORED_GRUB_SPAWN_EGG, ModEntities.ARMORED_GRUB.get(), 0x4A7C00, 0x8B5E00);
		registerSpawnEgg(ModEntities.UMBRAKAR_SPAWN_EGG, ModEntities.UMBRAKAR.get(), 0x3A1A4A, 0xC48CFF);
		registerSpawnEgg(ModEntities.KOTSUKAGE_SPAWN_EGG, ModEntities.KOTSUKAGE.get(), 0xC4B59A, 0x3A7A3A);

		registerSound(ModSounds.SKELETONFIGHT_THEME);
		registerSound(ModSounds.DASH_SFX);
		registerSound(ModSounds.ARMORED_GRUB_AMBIENT);
		registerSound(ModSounds.ARMORED_GRUB_DEATH);

		CreativeModeTab tab = ModTabs.createTab();
		ModTabs.REMNANT_BOSSES_TAB.bind(Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModTabs.REMNANT_BOSSES_TAB.id(), tab));
	}

	private static void registerBlock(RegistryHolder<Block> holder) {
		holder.bind(Registry.register(BuiltInRegistries.BLOCK, holder.id(), holder.get()));
	}

	private static void registerItem(RegistryHolder<Item> holder) {
		holder.bind(Registry.register(BuiltInRegistries.ITEM, holder.id(), holder.get()));
	}

	private static <T extends net.minecraft.world.entity.Entity> void registerEntity(RegistryHolder<EntityType<T>> holder) {
		holder.bind(Registry.register(BuiltInRegistries.ENTITY_TYPE, holder.id(), holder.get()));
	}

	private static void registerSpawnEgg(RegistryHolder<Item> holder, EntityType<? extends Mob> type, int background,
			int highlight) {
		holder.bind(Registry.register(BuiltInRegistries.ITEM, holder.id(),
				new SpawnEggItem(type, background, highlight, new Item.Properties())));
	}

	private static void registerSound(RegistryHolder<SoundEvent> holder) {
		holder.bind(Registry.register(BuiltInRegistries.SOUND_EVENT, holder.id(), holder.get()));
	}
}
