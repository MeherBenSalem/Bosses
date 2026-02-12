import tn.naizo.remnants.item.RatFangItem;
import tn.naizo.remnants.item.OssukageSwordItem;
import tn.naizo.remnants.item.OldSkeletonHeadItem;
import tn.naizo.remnants.item.OldSkeletonBoneItem;
import tn.naizo.remnants.item.FangOnAStickItem;
import tn.naizo.remnants.RemnantBossesMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEMS, RemnantBossesMod.MODID);

	// Custom items
	public static final DeferredHolder<Item> OSSUKAGE_SWORD = ITEMS.register("ossukage_sword", () -> new OssukageSwordItem());
	public static final DeferredHolder<Item> RAT_FANG = ITEMS.register("rat_fang", () -> new RatFangItem());
	public static final DeferredHolder<Item> FANG_ON_A_STICK = ITEMS.register("fang_on_a_stick", () -> new FangOnAStickItem());
	public static final DeferredHolder<Item> OLD_SKELETON_BONE = ITEMS.register("old_skeleton_bone", () -> new OldSkeletonBoneItem());
	public static final DeferredHolder<Item> OLD_SKELETON_HEAD = ITEMS.register("old_skeleton_head", () -> new OldSkeletonHeadItem());

	// Block items (held in blocks, but also need item forms)
	public static final DeferredHolder<Item> ANCIENT_ALTAR = blockItem(ModBlocks.ANCIENT_ALTAR);
	public static final DeferredHolder<Item> ANCIENT_PEDESTAL = blockItem(ModBlocks.ANCIENT_PEDESTAL);

	// Spawn eggs are registered in ModEntities.SPAWN_EGGS

	private static DeferredHolder<Item> blockItem(DeferredHolder<Block> block) {
		return blockItem(block, new Item.Properties());
	}

	private static DeferredHolder<Item> blockItem(DeferredHolder<Block> block, Item.Properties properties) {
		return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}
