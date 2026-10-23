package com.nightbeam.remnants.item;

import net.minecraft.world.InteractionResult;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.nightbeam.remnants.registry.RegistryKeys;
import com.nightbeam.remnants.init.ModToolMaterials;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import java.util.List;

public class OssukageSwordItem extends Item {
	public OssukageSwordItem() {
		// 3 + 8 (material) attack damage, 4 - 2 = 2 attack speed, no durability.
		super(ModToolMaterials.unbreakableSword(RegistryKeys.item().fireResistant(), ModToolMaterials.OSSUKAGE, 3, -2f));
	}

	@Override
	public InteractionResult use(net.minecraft.world.level.Level level,
			Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		// Check cooldown to prevent offhand exploit
		if (player.getCooldowns().isOnCooldown(itemstack)) {
			return InteractionResult.FAIL;
		}

		if (!level.isClientSide()) {
			// Execute logic
			com.nightbeam.remnants.procedures.ThrowKunaisProcedureProcedure.execute(player);

			// Play sound
			level.playSound(null, player.blockPosition(),
					net.minecraft.sounds.SoundEvents.ARROW_SHOOT,
					net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);

			// Add Cooldown (20 ticks)
			player.getCooldowns().addCooldown(itemstack, 20);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, display, list, flag);
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_0"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_1"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_2"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_3"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_4"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_5"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_6"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_7"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_8"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_9"));
		list.accept(Component.translatable("item.remnant_bosses.ossukage_sword.description_10"));
	}
}