package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.event.GameEvents;
import com.nightbeam.remnants.event.SovereignRelics;
import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.event.WatcherRelics;
import com.nightbeam.remnants.realm.RealmTravel;
import com.nightbeam.remnants.journal.BossHunts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeGameEvents {
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			BossHunts.logout(player);
			SovereignRelics.onLogout(player);
			WardenRelics.onLogout(player);
			WatcherRelics.onLogout(player);
			RealmTravel.logout(player);
		}
	}

	/** A Sigil toll saved with the player outlives its (unsaved) guards: clear it on login and respawn. */
	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) SovereignRelics.onLogin(player);
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) SovereignRelics.onRespawn(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		BossHunts.stop(event.getServer());
		SovereignRelics.stop();
		WardenRelics.stop();
		WatcherRelics.stop();
		RealmTravel.stop();
	}

	/**
	 * Unbowed (Regalia 4-piece) and Jailer's Hold (Warden 4-piece) read the damage after armor and enchantment
	 * reductions, like NeoForge's LivingDamageEvent.Post in 1.21.1. Lowest priority so other mods' changes and
	 * cancellations are final.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent event) {
		SovereignRelics.afterDamage(event.getEntity(), event.getSource(), event.getAmount());
		WardenRelics.afterDamage(event.getEntity(), event.getSource(), event.getAmount());
	}

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
