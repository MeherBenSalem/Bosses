package com.nightbeam.remnants.item;

import com.nightbeam.remnants.init.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * One piece of the Archivist set (Hood, Robe, Leggings, Boots). Set bonuses live in
 * {@code WatcherRelics}: two pieces clear Blindness and Darkness, four pieces (Archive Ward) negate one
 * projectile hit every 600 ticks.
 */
public class ArchivistArmorItem extends ArmorItem {
	public ArchivistArmorItem(Type type, Rarity rarity) {
		super(ModArmorMaterials.ARCHIVIST.holder(), type, new Properties().rarity(rarity)
				.durability(type.getDurability(ModArmorMaterials.ARCHIVIST_DURABILITY_MULTIPLIER)));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.0").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.remnant_bosses.archivist.set.two").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("item.remnant_bosses.archivist.set.four").withStyle(ChatFormatting.GOLD));
	}
}
