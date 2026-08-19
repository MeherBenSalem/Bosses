package com.nightbeam.remnants.init;

import com.nightbeam.remnants.item.FangOnAStickItem;
import com.nightbeam.remnants.item.OldSkeletonBoneItem;
import com.nightbeam.remnants.item.OldSkeletonHeadItem;
import com.nightbeam.remnants.item.OssukageSwordItem;
import com.nightbeam.remnants.item.RatFangItem;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	public static final RegistryHolder<Item> OSSUKAGE_SWORD = new RegistryHolder<>("ossukage_sword");
	public static final RegistryHolder<Item> RAT_FANG = new RegistryHolder<>("rat_fang");
	public static final RegistryHolder<Item> FANG_ON_A_STICK = new RegistryHolder<>("fang_on_a_stick");
	public static final RegistryHolder<Item> OLD_SKELETON_BONE = new RegistryHolder<>("old_skeleton_bone");
	public static final RegistryHolder<Item> OLD_SKELETON_HEAD = new RegistryHolder<>("old_skeleton_head");
	public static final RegistryHolder<Item> ANCIENT_ALTAR = new RegistryHolder<>("ancient_altar");
	public static final RegistryHolder<Item> ANCIENT_PEDESTAL = new RegistryHolder<>("ancient_pedestal");

	private ModItems() {
	}

	public static OssukageSwordItem createOssukageSword() {
		return new OssukageSwordItem();
	}

	public static RatFangItem createRatFang() {
		return new RatFangItem();
	}

	public static FangOnAStickItem createFangOnAStick() {
		return new FangOnAStickItem();
	}

	public static OldSkeletonBoneItem createOldSkeletonBone() {
		return new OldSkeletonBoneItem();
	}

	public static OldSkeletonHeadItem createOldSkeletonHead() {
		return new OldSkeletonHeadItem();
	}

	public static BlockItem createBlockItem(RegistryHolder<Block> block) {
		return new BlockItem(block.get(), new Item.Properties());
	}
}
