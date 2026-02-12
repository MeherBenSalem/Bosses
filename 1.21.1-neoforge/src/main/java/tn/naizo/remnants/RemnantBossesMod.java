package tn.naizo.remnants;

import tn.naizo.remnants.init.ModItems;
import tn.naizo.remnants.init.ModBlocks;
import tn.naizo.remnants.init.ModEntities;
import tn.naizo.remnants.init.ModSounds;
import tn.naizo.remnants.init.ModTabs;

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

	public RemnantBossesMod(IEventBus modEventBus) {
		// Register all registries
		ModItems.ITEMS.register(modEventBus);
		ModBlocks.BLOCKS.register(modEventBus);
		ModEntities.ENTITIES.register(modEventBus);
		ModEntities.SPAWN_EGGS.register(modEventBus);
		ModSounds.SOUNDS.register(modEventBus);
		ModTabs.TABS.register(modEventBus);

		// Setup event
		modEventBus.addListener(this::commonSetup);

		// Register config bootstrap
		modEventBus.register(tn.naizo.remnants.config.JaumlConfigBootstrap.class);

		// Register gameplay event handlers
		NeoForge.EVENT_BUS.register(tn.naizo.remnants.event.EntitySpawnEvents.class);
		NeoForge.EVENT_BUS.register(tn.naizo.remnants.event.EntityTickEvents.class);
		NeoForge.EVENT_BUS.register(tn.naizo.remnants.event.EntityDeathEvents.class);
		NeoForge.EVENT_BUS.register(tn.naizo.remnants.event.PlayerInteractionEvents.class);
		NeoForge.EVENT_BUS.register(tn.naizo.remnants.event.BlockInteractionEvents.class);

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
