package com.nightbeam.remnants.item;

import com.nightbeam.remnants.init.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One piece of the Regalia set (Hollow Crown, Cuirass, Greaves, Sabatons). Set bonuses: {@code SovereignRelics}. */
public class RegaliaArmorItem extends ArmorItem {
	public RegaliaArmorItem(Type type, Rarity rarity) {
		super(ModArmorMaterials.REGALIA.holder(), type, new Properties().rarity(rarity)
				.durability(type.getDurability(ModArmorMaterials.REGALIA_DURABILITY_MULTIPLIER)));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.0").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.remnant_bosses.regalia.set.two").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("item.remnant_bosses.regalia.set.four").withStyle(ChatFormatting.GOLD));
	}
}
