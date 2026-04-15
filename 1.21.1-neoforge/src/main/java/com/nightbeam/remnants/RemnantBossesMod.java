package com.nightbeam.remnants;

import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@Mod("remnant_bosses")
public class RemnantBossesMod {
	public static final Logger LOGGER = LogManager.getLogger(RemnantBossesMod.class);
	public static final String MODID = "remnant_bosses";

	private static final List<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ArrayList<>();

	public RemnantBossesMod(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
		// Register all registries
		ModItems.ITEMS.register(modEventBus);
		ModBlocks.BLOCKS.register(modEventBus);
		ModEntities.ENTITIES.register(modEventBus);
		ModEntities.SPAWN_EGGS.register(modEventBus);
		ModSounds.SOUNDS.register(modEventBus);
		ModTabs.TABS.register(modEventBus);
		com.nightbeam.remnants.init.ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);

		// Setup event
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(com.nightbeam.remnants.init.ModEntities::init);
		modEventBus.addListener(com.nightbeam.remnants.init.ModEntities::registerAttributes);
		modEventBus.addListener(com.nightbeam.remnants.init.ModEntities::registerSpawnPlacements);
		modEventBus.addListener(com.nightbeam.remnants.network.PacketHandler::register);

		// Register config bootstrap
		modEventBus.register(com.nightbeam.remnants.config.JaumlConfigBootstrap.class);

		// Register gameplay event handlers
		NeoForge.EVENT_BUS.register(com.nightbeam.remnants.event.EntitySpawnEvents.class);
		NeoForge.EVENT_BUS.register(com.nightbeam.remnants.event.EntityTickEvents.class);
		NeoForge.EVENT_BUS.register(com.nightbeam.remnants.event.EntityDeathEvents.class);
		NeoForge.EVENT_BUS.register(com.nightbeam.remnants.event.PlayerInteractionEvents.class);
		NeoForge.EVENT_BUS.register(com.nightbeam.remnants.event.BlockInteractionEvents.class);

		LOGGER.info("Remnant Bosses mod registered");
	}

	private void commonSetup(FMLCommonSetupEvent event) {
	}

	/**
	 * Queue work to be executed on the next server tick.
	 */
	public static void queueServerWork(int delay, Runnable runnable) {
		workQueue.add(new AbstractMap.SimpleEntry<>(runnable, delay));
	}

	@EventBusSubscriber
	private static class ServerTickHandler {
		private ServerTickHandler() {
		}

		@SubscribeEvent
		public static void onServerTick(ServerTickEvent.Post event) {
			List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
			workQueue.forEach(work -> {
				work.setValue(work.getValue() - 1);
				if (work.getValue() == 0)
					actions.add(work);
			});
			actions.forEach(e -> e.getKey().run());
			workQueue.removeAll(actions);
		}
	}
}
