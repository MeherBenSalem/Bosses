
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import com.naizo.ossukage.item.OssukageSwordItem;
import com.naizo.ossukage.RemnantOssukageMod;

public class RemnantOssukageModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, RemnantOssukageMod.MODID);
	public static final RegistryObject<Item> OSSUKAGE_SPAWN_EGG = REGISTRY.register("ossukage_spawn_egg", () -> new ForgeSpawnEggItem(RemnantOssukageModEntities.OSSUKAGE, -13421773, -1, new Item.Properties()));
	public static final RegistryObject<Item> ANCIENT_RUIN_BLOCK = block(RemnantOssukageModBlocks.ANCIENT_RUIN_BLOCK);
	public static final RegistryObject<Item> OSSUKAGE_SWORD = REGISTRY.register("ossukage_sword", () -> new OssukageSwordItem());

	// Start of user code block custom items
	// End of user code block custom items
	private static RegistryObject<Item> block(RegistryObject<Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
