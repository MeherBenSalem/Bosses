package com.nightbeam.remnants.item;

import com.nightbeam.remnants.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.List;

/**
 * Gravebound Executioner: the Warden's headsman's axe. 10 attack damage, 0.9 attack
 * speed, and +40% damage against slowed or chained targets.
 *
 * <p>Minecraft 1.20.1 has no {@code Item.getAttackDamageBonus(Entity, float, DamageSource)}: the bonus is
 * applied by {@code WardenRelics.scaleIncoming} (RelicDamageMixin, every loader) to the melee hit's
 * amount, and the iron mining level stands in for 1.21's incorrect-for-iron block tag.
 */
public class GraveboundExecutionerItem extends AxeItem {
	public static final float CONDEMNED_BONUS = 0.40f;
	public static final Tier TIER = new Tier() {
		@Override public int getUses() { return 1000; }
		@Override public float getSpeed() { return 7f; }
		@Override public float getAttackDamageBonus() { return 2f; }
		@Override public int getLevel() { return 2; }
		@Override public int getEnchantmentValue() { return 14; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.GRAVE_IRON.get()); }
	};

	public GraveboundExecutionerItem() {
		// 1 (player base) + 7 + 2 (tier) = 10 attack damage; 4 - 3.1 = 0.9 attack speed.
		super(TIER, 7f, -3.1f, new Properties().rarity(Rarity.RARE));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 2, tooltip);
	}
}
