package com.nightbeam.remnants.neoforge;

import com.nightbeam.remnants.event.BlockInteractionEvents;
import com.nightbeam.remnants.journal.BossHunts;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.nightbeam.remnants.event.EntityDeathEvents;
import com.nightbeam.remnants.event.EntitySpawnEvents;
import com.nightbeam.remnants.event.EntityTickEvents;
import com.nightbeam.remnants.event.PlayerInteractionEvents;
import com.nightbeam.remnants.event.SovereignRelics;
import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.event.WatcherRelics;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class NeoForgeModEvents {
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			BossHunts.logout(player);
			SovereignRelics.onLogout(player);
			com.nightbeam.remnants.realm.RealmTravel.logout(player);
			WardenRelics.onLogout(player);
			WatcherRelics.onLogout(player);
		}
	}

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
		com.nightbeam.remnants.realm.RealmTravel.stop();
	}

	@SubscribeEvent
	public static void onLivingDamaged(LivingDamageEvent.Post event) {
		SovereignRelics.afterDamage(event.getEntity(), event.getSource(), event.getInflictedDamage());
		WardenRelics.afterDamage(event.getEntity(), event.getSource(), event.getInflictedDamage());
	}

	private NeoForgeModEvents() {
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (BlockInteractionEvents.onRightClickBlock(event.getEntity(), event.getLevel(), event.getPos())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (!EntitySpawnEvents.onEntityJoin(event.getEntity(), event.getLevel())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		EntityDeathEvents.onLivingDeath(event.getEntity(), event.getSource());
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		EntityTickEvents.onLivingHurt(event.getEntity());
		PlayerInteractionEvents.onLivingHurt(event.getEntity(), event.getSource().getEntity());
	}
}
