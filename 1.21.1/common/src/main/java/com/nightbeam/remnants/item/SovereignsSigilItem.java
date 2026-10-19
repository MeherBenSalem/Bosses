package com.nightbeam.remnants.item;

import com.nightbeam.remnants.event.SovereignRelics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Sovereign's Sigil, an off-hand relic. Passive (off-hand): owned allies within 16 blocks deal +10%
 * damage. Use (off-hand): two allied spectral royal guards for 400 ticks at the cost of 4 max health.
 */
public class SovereignsSigilItem extends Item {
	public static final int COOLDOWN = 1800;

	public SovereignsSigilItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.OFF_HAND) {
			if (!level.isClientSide) player.displayClientMessage(Component.translatable(getDescriptionId() + ".offhand"), true);
			return InteractionResultHolder.fail(stack);
		}
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		if (player instanceof ServerPlayer serverPlayer) {
			if (!SovereignRelics.summonSigilGuards(serverPlayer)) return InteractionResultHolder.fail(stack);
			player.getCooldowns().addCooldown(this, COOLDOWN);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 3, tooltip);
	}
}
