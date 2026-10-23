package com.nightbeam.remnants.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A block item with {@code lines} translated tooltip lines, like {@link LoreItem}. */
public class LoreBlockItem extends BlockItem {
	private final int lines;

	public LoreBlockItem(Block block, Properties properties, int lines) {
		super(block, properties);
		this.lines = lines;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, lines, tooltip);
	}
}
