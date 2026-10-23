package com.nightbeam.remnants.item;

import com.nightbeam.remnants.event.WardenRelics;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.nightbeam.remnants.registry.RegistryKeys;
import com.nightbeam.remnants.init.ModToolMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Gravebound Executioner: the Warden's headsman's axe. 10 attack damage, 0.9 attack
 * speed, and +40% damage against slowed or chained targets.
 */
public class GraveboundExecutionerItem extends Item {
	public static final float CONDEMNED_BONUS = 0.40f;

	public GraveboundExecutionerItem() {
		// 1 (player base) + 7 + 2 (tier) = 10 attack damage; 4 - 3.1 = 0.9 attack speed.
		super(RegistryKeys.item().axe(ModToolMaterials.EXECUTIONER, 7f, -3.1f).rarity(Rarity.RARE));
	}

	/** Slowed or chained targets take 40% more from the blade (added to the weapon's base damage). */
	@Override
	public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
		if (target instanceof LivingEntity living && (living.hasEffect(MobEffects.SLOWNESS) || WardenRelics.isChained(living))) {
			return damage * CONDEMNED_BONUS;
		}
		return 0;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 2, tooltip);
	}
}
