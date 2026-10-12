package com.nightbeam.remnants.item;

import com.nightbeam.remnants.journal.BossHunts;
import net.minecraft.ChatFormatting;
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

/** Access to personal world progress, never a container for that progress. */
public final class BossJournalItem extends Item {
    public BossJournalItem() { super(new Item.Properties().stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) BossHunts.open(serverPlayer);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.remnant_bosses.boss_journal.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.remnant_bosses.boss_journal.progress").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
