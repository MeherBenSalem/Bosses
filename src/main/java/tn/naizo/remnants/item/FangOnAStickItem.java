package tn.naizo.remnants.item;

import tn.naizo.remnants.init.RemnantBossesModItems;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

public class FangOnAStickItem extends SwordItem {
	public FangOnAStickItem() {
		super(new Tier() {
			public int getUses() {
				return 50;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 1f;
			}

			public int getLevel() {
				return 2;
			}

			public int getEnchantmentValue() {
				return 5;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(RemnantBossesModItems.RAT_FANG.get()));
			}
		}, 3, -2f, new Item.Properties());
	}
}