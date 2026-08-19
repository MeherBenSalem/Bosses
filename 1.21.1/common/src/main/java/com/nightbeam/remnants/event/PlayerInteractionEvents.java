package com.nightbeam.remnants.event;

import com.nightbeam.remnants.item.OssukageSwordItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PlayerInteractionEvents {
	private PlayerInteractionEvents() {
	}

	public static void onLivingHurt(LivingEntity target, net.minecraft.world.entity.Entity attacker) {
		Level level = target.level();
		if (level.isClientSide) {
			return;
		}
		if (!(attacker instanceof Player player)) {
			return;
		}
		ItemStack itemStack = player.getMainHandItem();
		if (itemStack.getItem() instanceof OssukageSwordItem) {
			handleOssukageSwordHit(target, player, itemStack, level);
		}
	}

	private static void handleOssukageSwordHit(LivingEntity target, Player attacker, ItemStack itemStack, Level level) {
	}
}
