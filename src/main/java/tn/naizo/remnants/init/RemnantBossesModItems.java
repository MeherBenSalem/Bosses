
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.item.OssukageSwordItem;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

public class RemnantBossesModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, RemnantBossesMod.MODID);
	public static final RegistryObject<Item> OSSUKAGE_SPAWN_EGG = REGISTRY.register("ossukage_spawn_egg", () -> new ForgeSpawnEggItem(RemnantBossesModEntities.OSSUKAGE, -13421773, -1, new Item.Properties()));
	public static final RegistryObject<Item> OSSUKAGE_SWORD = REGISTRY.register("ossukage_sword", () -> new OssukageSwordItem());
	public static final RegistryObject<Item> SKELETON_MINIONS_SPAWN_EGG = REGISTRY.register("skeleton_minions_spawn_egg", () -> new ForgeSpawnEggItem(RemnantBossesModEntities.SKELETON_MINIONS, -1, -3355444, new Item.Properties()));
	public static final RegistryObject<Item> RAT_SPAWN_EGG = REGISTRY.register("rat_spawn_egg", () -> new ForgeSpawnEggItem(RemnantBossesModEntities.RAT, -13421824, -10066330, new Item.Properties()));
	public static final RegistryObject<Item> ANCIENT_ALTAR = block(RemnantBossesModBlocks.ANCIENT_ALTAR);
	public static final RegistryObject<Item> ANCIENT_PEDESTAL = block(RemnantBossesModBlocks.ANCIENT_PEDESTAL);

	// Start of user code block custom items
	// End of user code block custom items
	private static RegistryObject<Item> block(RegistryObject<Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
