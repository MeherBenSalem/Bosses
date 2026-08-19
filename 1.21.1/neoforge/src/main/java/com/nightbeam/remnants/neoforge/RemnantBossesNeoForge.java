package com.nightbeam.remnants.neoforge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.JaumlConfigBootstrap;
import com.nightbeam.remnants.neoforge.network.PacketHandler;
import com.nightbeam.remnants.neoforge.platform.NeoForgeNetwork;
import com.nightbeam.remnants.neoforge.registry.NeoForgeModRegistry;
import com.nightbeam.remnants.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public class RemnantBossesNeoForge {
	public RemnantBossesNeoForge(IEventBus modEventBus) {
		Services.NETWORK = new NeoForgeNetwork();
		NeoForgeModRegistry.register(modEventBus);
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(NeoForgeModRegistry::registerAttributes);
		modEventBus.addListener(NeoForgeModRegistry::registerSpawnPlacements);
		modEventBus.addListener(PacketHandler::register);
		NeoForge.EVENT_BUS.addListener(this::onServerTick);
		NeoForge.EVENT_BUS.register(NeoForgeModEvents.class);
		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on NeoForge", Constants.MOD_NAME);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			JaumlConfigBootstrap.initConfigs();
			com.nightbeam.remnants.init.ModEntities.init();
		});
	}

	private void onServerTick(ServerTickEvent.Post event) {
		RemnantBosses.tickWorkQueue();
	}
}
