package tn.naizo.remnants.item;

import tn.naizo.remnants.init.ModItems;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class FangOnAStickItem extends SwordItem {
	private static final Tier FANG_TIER = new Tier() {
		public int getUses() {
			return 50;
		}

		public float getSpeed() {
			return 4f;
		}

		public float getAttackDamageBonus() {
			return 1f;
		}

		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_IRON_TOOL;
		}

		public int getEnchantmentValue() {
			return 5;
		}

		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(ModItems.RAT_FANG.get()));
		}
	};

	public FangOnAStickItem() {
		super(FANG_TIER, new Item.Properties().attributes(SwordItem.createAttributes(FANG_TIER, 3, -2f)));
	}
}