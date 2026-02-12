package tn.naizo.remnants.event;

import tn.naizo.remnants.block.AncientAltarBlock;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Handles player interactions with blocks.
 * Replaces the deleted AncientRuinBlockOnBlockRightClickedProcedure.
 */
public class BlockInteractionEvents {

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		Level level = player.level();
		Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

		// Only run on server
		if (level.isClientSide) {
			return;
		}

		// Handle Ancient Altar right-click
		if (block instanceof AncientAltarBlock) {
			handleAncientAltarClick(player, event.getPos(), level);
			event.setCanceled(true);
		}
	}

	/**
	 * Handle Ancient Altar block right-click.
	 * Original procedure performed ritual or entity summoning.
	 */
	private static void handleAncientAltarClick(Player player, net.minecraft.core.BlockPos pos, Level level) {
		// Placeholder for altar interaction logic
		// This could summon entities, trigger events, or modify world state
	}
}
