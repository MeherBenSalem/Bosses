package com.nightbeam.remnants.item;

import com.nightbeam.remnants.init.ModArmorMaterials;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.nightbeam.remnants.registry.RegistryKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One piece of the Regalia set (Hollow Crown, Cuirass, Greaves, Sabatons). Set bonuses: {@code SovereignRelics}. */
public class RegaliaArmorItem extends Item {
	public RegaliaArmorItem(ArmorType type, Rarity rarity) {
		super(RegistryKeys.item().rarity(rarity).humanoidArmor(ModArmorMaterials.REGALIA, type));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip.0").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.remnant_bosses.regalia.set.two").withStyle(ChatFormatting.GOLD));
		tooltip.accept(Component.translatable("item.remnant_bosses.regalia.set.four").withStyle(ChatFormatting.GOLD));
	}
}
