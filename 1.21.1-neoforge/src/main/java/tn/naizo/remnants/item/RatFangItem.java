package tn.naizo.remnants.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class RatFangItem extends Item {
	public RatFangItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}