package com.nightbeam.remnants.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class OldSkeletonHeadItem extends Item {
	public OldSkeletonHeadItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}