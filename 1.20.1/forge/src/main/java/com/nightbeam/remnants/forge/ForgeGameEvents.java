package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.event.GameEvents;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeGameEvents {
	private ForgeGameEvents() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!GameEvents.onEntityJoin(event.getEntity(), event.getLevel())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		GameEvents.onLivingDeath(event.getEntity(), event.getSource());
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		GameEvents.onLivingHurt(event.getEntity(), event.getSource(), event.getAmount());
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
		if (GameEvents.onRightClickBlock(event.getEntity(), event.getLevel(), event.getPos(), block)) {
			event.setCanceled(true);
		}
	}
}
