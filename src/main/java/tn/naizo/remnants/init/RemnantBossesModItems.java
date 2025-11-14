/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.item.RatFangItem;
import tn.naizo.remnants.item.OssukageSwordItem;
import tn.naizo.remnants.item.OldSkeletonHeadItem;
import tn.naizo.remnants.item.OldSkeletonBoneItem;
import tn.naizo.remnants.item.FangOnAStickItem;
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
	public static final RegistryObject<Item> OSSUKAGE_SWORD = REGISTRY.register("ossukage_sword", () -> new OssukageSwordItem());
	public static final RegistryObject<Item> ANCIENT_ALTAR = block(RemnantBossesModBlocks.ANCIENT_ALTAR);
	public static final RegistryObject<Item> ANCIENT_PEDESTAL = block(RemnantBossesModBlocks.ANCIENT_PEDESTAL);
	public static final RegistryObject<Item> RAT_SPAWN_EGG = REGISTRY.register("rat_spawn_egg", () -> new ForgeSpawnEggItem(RemnantBossesModEntities.RAT, -13421773, -16777216, new Item.Properties()));
	public static final RegistryObject<Item> SKELETON_MINION_SPAWN_EGG = REGISTRY.register("skeleton_minion_spawn_egg", () -> new ForgeSpawnEggItem(RemnantBossesModEntities.SKELETON_MINION, -6710887, -1, new Item.Properties()));
	public static final RegistryObject<Item> REMNANT_OSSUKAGE_SPAWN_EGG = REGISTRY.register("remnant_ossukage_spawn_egg", () -> new ForgeSpawnEggItem(RemnantBossesModEntities.REMNANT_OSSUKAGE, -13421824, -1, new Item.Properties()));
	public static final RegistryObject<Item> RAT_FANG = REGISTRY.register("rat_fang", () -> new RatFangItem());
	public static final RegistryObject<Item> FANG_ON_A_STICK = REGISTRY.register("fang_on_a_stick", () -> new FangOnAStickItem());
	public static final RegistryObject<Item> OLD_SKELETON_BONE = REGISTRY.register("old_skeleton_bone", () -> new OldSkeletonBoneItem());
	public static final RegistryObject<Item> OLD_SKELETON_HEAD = REGISTRY.register("old_skeleton_head", () -> new OldSkeletonHeadItem());

	// Start of user code block custom items
	// End of user code block custom items
	private static RegistryObject<Item> block(RegistryObject<Block> block) {
		return block(block, new Item.Properties());
	}

	private static RegistryObject<Item> block(RegistryObject<Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}