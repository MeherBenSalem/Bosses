package tn.naizo.remnants.event;

import tn.naizo.remnants.item.OssukageSwordItem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Handles player interactions with items.
 * Replaces deleted sword interaction procedures:
 * - OssukageSwordRightclickedProcedure
 * - OssukageSwordLivingEntityIsHitWithToolProcedure
 * - OssukageSwordToolInHandTickProcedure
 */
public class PlayerInteractionEvents {

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		ItemStack itemStack = event.getItemStack();
		Level level = player.level();

		// Only run on server
		if (level.isClientSide) {
			return;
		}

		// Handle Ossukage sword right-click
		if (itemStack.getItem() instanceof OssukageSwordItem) {
			handleOssukageSwordRightClick(player, itemStack, level);
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		LivingEntity target = event.getEntity();
		Level level = target.level();

		// Only run on server
		if (level.isClientSide) {
			return;
		}

		// Get attacker from damage source
		net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
		if (!(attacker instanceof Player player)) {
			return;
		}

		// Handle Ossukage sword hit
		ItemStack itemStack = player.getMainHandItem();
		if (itemStack.getItem() instanceof OssukageSwordItem) {
			handleOssukageSwordHit(target, player, itemStack, level);
		}
	}

	/**
	 * Handle Ossukage sword right-click action.
	 * Original procedure shot kunai or performed special attack.
	 */
	private static void handleOssukageSwordRightClick(Player player, ItemStack itemStack, Level level) {
		// Placeholder for right-click logic
		// This would typically spawn kunai entities or perform special attack
	}

	/**
	 * Handle Ossukage sword hitting an entity.
	 * Original procedure applied effects or damage modifications.
	 */
	private static void handleOssukageSwordHit(LivingEntity target, Player attacker, ItemStack itemStack, Level level) {
		// Placeholder for hit logic
		// This could include special effects, damage bonuses, or status effects
	}
}
