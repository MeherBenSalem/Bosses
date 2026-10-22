package com.nightbeam.remnants.item;

import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Gravebound Executioner: the Warden's headsman's axe. 10 attack damage, 0.9 attack
 * speed, and +40% damage against slowed or chained targets.
 */
public class GraveboundExecutionerItem extends AxeItem {
	public static final float CONDEMNED_BONUS = 0.40f;
	public static final Tier TIER = new Tier() {
		@Override public int getUses() { return 1000; }
		@Override public float getSpeed() { return 7f; }
		@Override public float getAttackDamageBonus() { return 2f; }
		@Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
		@Override public int getEnchantmentValue() { return 14; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.GRAVE_IRON.get()); }
	};

	public GraveboundExecutionerItem() {
		// 1 (player base) + 7 + 2 (tier) = 10 attack damage; 4 - 3.1 = 0.9 attack speed.
		super(TIER, new Properties().rarity(Rarity.RARE).attributes(AxeItem.createAttributes(TIER, 7f, -3.1f)));
	}

	/** Slowed or chained targets take 40% more from the blade (added to the weapon's base damage). */
	@Override
	public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
		if (target instanceof LivingEntity living && (living.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || WardenRelics.isChained(living))) {
			return damage * CONDEMNED_BONUS;
		}
		return 0;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 2, tooltip);
	}
}
