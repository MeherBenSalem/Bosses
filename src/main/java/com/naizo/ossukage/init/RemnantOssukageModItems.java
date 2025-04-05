
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import com.naizo.ossukage.item.OssukageSwordItem;
import com.naizo.ossukage.RemnantOssukageMod;

public class RemnantOssukageModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(RemnantOssukageMod.MODID);
	public static final DeferredItem<Item> OSSUKAGE_SPAWN_EGG = REGISTRY.register("ossukage_spawn_egg", () -> new DeferredSpawnEggItem(RemnantOssukageModEntities.OSSUKAGE, -13421773, -1, new Item.Properties()));
	public static final DeferredItem<Item> ANCIENT_RUIN_BLOCK = block(RemnantOssukageModBlocks.ANCIENT_RUIN_BLOCK);
	public static final DeferredItem<Item> OSSUKAGE_SWORD = REGISTRY.register("ossukage_sword", OssukageSwordItem::new);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
