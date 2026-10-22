package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import com.nightbeam.remnants.client.realm.RealmClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE, modid = Constants.MOD_ID)
public final class ForgeJournalClientEvents {
	private ForgeJournalClientEvents() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			BossHuntsClient.tick();
			RealmClient.tick();
		}
	}
}
