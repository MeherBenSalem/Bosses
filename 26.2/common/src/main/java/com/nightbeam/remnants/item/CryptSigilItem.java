package com.nightbeam.remnants.item;

import com.nightbeam.remnants.event.WardenRelics;
import net.minecraft.world.InteractionResult;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Crypt Sigil, an off-hand relic. Passive (off-hand): undead deal 10% less damage
 * to the holder. Use (off-hand): the nearest enemy within 12 blocks is chained in place for 60 ticks, at
 * the cost of Wither I for 60 ticks; cooldown 900 ticks. Rules live in {@link WardenRelics}.
 */
public class CryptSigilItem extends Item {
	public static final int COOLDOWN = 900;

	public CryptSigilItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.OFF_HAND) {
			if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(getDescriptionId() + ".offhand"));
			return InteractionResult.FAIL;
		}
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (player instanceof ServerPlayer serverPlayer) {
			if (!WardenRelics.sigilActive(serverPlayer)) {
				player.sendOverlayMessage(Component.translatable(getDescriptionId() + ".no_target"));
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(stack, COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 3, tooltip);
	}
}
