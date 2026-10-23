package com.nightbeam.remnants.item;

import com.nightbeam.remnants.journal.BossHunts;
import net.minecraft.world.InteractionResult;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.nightbeam.remnants.registry.RegistryKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Access to personal world progress, never a container for that progress. */
public final class BossJournalItem extends Item {
    public BossJournalItem() { super(RegistryKeys.item().stacksTo(1)); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) BossHunts.open(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.remnant_bosses.boss_journal.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.remnant_bosses.boss_journal.progress").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
