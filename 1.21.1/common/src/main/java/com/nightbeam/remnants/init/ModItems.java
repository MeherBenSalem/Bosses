package com.nightbeam.remnants.init;

import com.nightbeam.remnants.item.FangOnAStickItem;
import com.nightbeam.remnants.item.OldSkeletonBoneItem;
import com.nightbeam.remnants.item.OldSkeletonHeadItem;
import com.nightbeam.remnants.item.OssukageSwordItem;
import com.nightbeam.remnants.item.RatFangItem;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final RegistryHolder<Item> RAT_FANG = new RegistryHolder<>("rat_fang", RatFangItem::new);
	public static final RegistryHolder<Item> OLD_SKELETON_BONE = new RegistryHolder<>("old_skeleton_bone", OldSkeletonBoneItem::new);
	public static final RegistryHolder<Item> OLD_SKELETON_HEAD = new RegistryHolder<>("old_skeleton_head", OldSkeletonHeadItem::new);
	public static final RegistryHolder<Item> OSSUKAGE_SWORD = new RegistryHolder<>("ossukage_sword", OssukageSwordItem::new);
	public static final RegistryHolder<Item> FANG_ON_A_STICK = new RegistryHolder<>("fang_on_a_stick", FangOnAStickItem::new);
	public static final RegistryHolder<Item> ANCIENT_ALTAR = blockItem(ModBlocks.ANCIENT_ALTAR);
	public static final RegistryHolder<Item> ANCIENT_PEDESTAL = blockItem(ModBlocks.ANCIENT_PEDESTAL);

	private ModItems() {
	}

	private static RegistryHolder<Item> blockItem(RegistryHolder<net.minecraft.world.level.block.Block> block) {
		return new RegistryHolder<>(block.path(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
