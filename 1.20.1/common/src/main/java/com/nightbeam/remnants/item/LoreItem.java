package com.nightbeam.remnants.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.List;

/** A plain item with {@code lines} translated tooltip lines: item.remnant_bosses.&lt;id&gt;.tooltip.&lt;n&gt;. */
public class LoreItem extends Item {
	private final int lines;

	public LoreItem(Properties properties, int lines) {
		super(properties);
		this.lines = lines;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		appendLore(this, lines, tooltip);
	}

	static void appendLore(Item item, int lines, List<Component> tooltip) {
		String key = item.getDescriptionId() + ".tooltip.";
		for (int i = 0; i < lines; i++) {
			tooltip.add(Component.translatable(key + i).withStyle(i == 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_AQUA));
		}
	}
}
