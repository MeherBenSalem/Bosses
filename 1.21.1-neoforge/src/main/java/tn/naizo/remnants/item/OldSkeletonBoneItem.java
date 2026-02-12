package tn.naizo.remnants.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class OldSkeletonBoneItem extends Item {
	public OldSkeletonBoneItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}