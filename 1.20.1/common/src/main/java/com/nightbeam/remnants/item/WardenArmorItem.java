package com.nightbeam.remnants.item;

import com.nightbeam.remnants.init.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.List;

/**
 * One piece of the Warden set (Helm, Hauberk, Tassets, Boots). Set bonuses live in
 * {@code WardenRelics}: two pieces deal +10% damage to undead, four pieces (Jailer's Hold) can chain a
 * struck target.
 */
public class WardenArmorItem extends ArmorItem {
	public WardenArmorItem(Type type, Rarity rarity) {
		// ArmorItem derives the durability from the material (getDurabilityForType).
		super(ModArmorMaterials.WARDEN, type, new Properties().rarity(rarity));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.0").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.remnant_bosses.warden.set.two").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("item.remnant_bosses.warden.set.four").withStyle(ChatFormatting.GOLD));
	}
}
