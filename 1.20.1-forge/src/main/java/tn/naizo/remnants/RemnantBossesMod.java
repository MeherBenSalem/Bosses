package tn.naizo.remnants;

import tn.naizo.remnants.init.ModItems;
import tn.naizo.remnants.init.ModBlocks;
import tn.naizo.remnants.init.ModEntities;
import tn.naizo.remnants.init.ModSounds;
import tn.naizo.remnants.init.ModTabs;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@Mod("remnant_bosses")
public class RemnantBossesMod {
	public static final Logger LOGGER = LogManager.getLogger(RemnantBossesMod.class);
	public static final String MODID = "remnant_bosses";

	private static final List<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ArrayList<>();

	public RemnantBossesMod(FMLJavaModLoadingContext context) {
		IEventBus modEventBus = context.getModEventBus();

		// Register all registries
		ModItems.ITEMS.register(modEventBus);
		ModBlocks.BLOCKS.register(modEventBus);
		ModEntities.ENTITIES.register(modEventBus);
		ModEntities.SPAWN_EGGS.register(modEventBus);
		ModSounds.SOUNDS.register(modEventBus);
		ModTabs.TABS.register(modEventBus);
		tn.naizo.remnants.init.ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);

		// Register Config
		// CommonConfig removed in favor of JAUML

		// Setup event
		modEventBus.addListener(this::commonSetup);

		// Initialize Networking
		tn.naizo.remnants.network.PacketHandler.init();

		// Register gameplay event handlers
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.EntitySpawnEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.EntityTickEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.EntityDeathEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.PlayerInteractionEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.BlockInteractionEvents.class);

		LOGGER.info("Remnant Bosses mod registered");
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			tn.naizo.remnants.config.JaumlConfigBootstrap.initConfigs();
		});
		LOGGER.info("Remnant Bosses mod loaded successfully");
	}

	/**
	 * Queue work to be executed on the next server tick.
	 */
	public static void queueServerWork(int delay, Runnable runnable) {
		workQueue.add(new AbstractMap.SimpleEntry<>(runnable, delay));
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
	private static class ServerTickHandler {
		private ServerTickHandler() {
		}

		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
			if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
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
}