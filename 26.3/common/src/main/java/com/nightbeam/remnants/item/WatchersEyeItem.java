package com.nightbeam.remnants.item;

import com.nightbeam.remnants.event.WatcherRelics;
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
 * Watcher's Eye, an off-hand relic. Passive (off-hand): every 200 ticks hostile
 * creatures within 24 blocks glow for 60 ticks. Use (off-hand): blink 8 blocks toward where you look,
 * stopping short of walls, for 2 hunger points; cooldown 200 ticks. Rules live in {@link WatcherRelics}.
 */
public class WatchersEyeItem extends Item {
	public static final int COOLDOWN = 200;

	public WatchersEyeItem(Properties properties) {
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
			if (!serverPlayer.getAbilities().instabuild && serverPlayer.getFoodData().getFoodLevel() < WatcherRelics.BLINK_HUNGER) {
				player.sendOverlayMessage(Component.translatable(getDescriptionId() + ".hungry"));
				return InteractionResult.FAIL;
			}
			if (!WatcherRelics.blink(serverPlayer)) {
				player.sendOverlayMessage(Component.translatable(getDescriptionId() + ".blocked"));
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
