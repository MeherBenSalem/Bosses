package com.nightbeam.remnants.neoforge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeJournalClientEvents {
	private NeoForgeJournalClientEvents() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		BossHuntsClient.tick();
		com.nightbeam.remnants.client.realm.RealmClient.tick();
	}
}
