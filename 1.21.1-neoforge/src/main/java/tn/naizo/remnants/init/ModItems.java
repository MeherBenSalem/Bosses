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

public class ModItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RemnantBossesMod.MODID);

	// Custom items
	public static final RegistryObject<Item> OSSUKAGE_SWORD = ITEMS.register("ossukage_sword", () -> new OssukageSwordItem());
	public static final RegistryObject<Item> RAT_FANG = ITEMS.register("rat_fang", () -> new RatFangItem());
	public static final RegistryObject<Item> FANG_ON_A_STICK = ITEMS.register("fang_on_a_stick", () -> new FangOnAStickItem());
	public static final RegistryObject<Item> OLD_SKELETON_BONE = ITEMS.register("old_skeleton_bone", () -> new OldSkeletonBoneItem());
	public static final RegistryObject<Item> OLD_SKELETON_HEAD = ITEMS.register("old_skeleton_head", () -> new OldSkeletonHeadItem());

	// Block items (held in blocks, but also need item forms)
	public static final RegistryObject<Item> ANCIENT_ALTAR = blockItem(ModBlocks.ANCIENT_ALTAR);
	public static final RegistryObject<Item> ANCIENT_PEDESTAL = blockItem(ModBlocks.ANCIENT_PEDESTAL);

	// Spawn eggs are registered in ModEntities.SPAWN_EGGS

	private static RegistryObject<Item> blockItem(RegistryObject<Block> block) {
		return blockItem(block, new Item.Properties());
	}

	private static RegistryObject<Item> blockItem(RegistryObject<Block> block, Item.Properties properties) {
		return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}