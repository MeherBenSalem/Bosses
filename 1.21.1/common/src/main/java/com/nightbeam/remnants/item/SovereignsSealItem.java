package com.nightbeam.remnants.item;

import com.nightbeam.remnants.journal.RitualInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Altar activation item for the Hollow Sovereign. The tooltip names the configured pedestal offerings. */
public class SovereignsSealItem extends Item {
	public SovereignsSealItem(Properties properties) {
		super(properties);
	}

	/** The ritual file (remnant/bosses/&lt;file&gt;) whose offerings the tooltip names. */
	protected String summonFile() {
		return "hollow_sovereign_summon";
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.0").withStyle(ChatFormatting.GRAY));
		RitualInfo ritual = RitualInfo.resolve(summonFile());
		if (!ritual.valid()) return;
		// Offerings are ordered east, west, south, north.
		List<String> offerings = ritual.offerings();
		tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.offerings",
				name(offerings.get(0)), name(offerings.get(1)), name(offerings.get(2)), name(offerings.get(3)))
				.withStyle(ChatFormatting.DARK_AQUA));
	}

	private static Component name(String block) {
		ResourceLocation id = ResourceLocation.tryParse(block);
		return id == null ? Component.literal(block) : BuiltInRegistries.BLOCK.get(id).getName();
	}
}
