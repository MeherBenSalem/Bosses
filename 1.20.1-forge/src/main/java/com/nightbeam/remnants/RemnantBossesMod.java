package com.nightbeam.remnants;

import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import software.bernie.geckolib.GeckoLib;

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

		// Required for GeckoLib 4 on 1.20.1 (registers network + client cache hooks)
		GeckoLib.initialize();

		// Register all registries
		ModItems.ITEMS.register(modEventBus);
		ModBlocks.BLOCKS.register(modEventBus);
		ModEntities.ENTITIES.register(modEventBus);
		ModEntities.SPAWN_EGGS.register(modEventBus);
		ModSounds.SOUNDS.register(modEventBus);
		ModTabs.TABS.register(modEventBus);
		com.nightbeam.remnants.init.ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);

		// Register Config
		// CommonConfig removed in favor of JAUML

		// Setup event
		modEventBus.addListener(this::commonSetup);

		// Initialize Networking
		com.nightbeam.remnants.network.PacketHandler.init();

		// Register gameplay event handlers
		MinecraftForge.EVENT_BUS.register(com.nightbeam.remnants.event.EntitySpawnEvents.class);
		MinecraftForge.EVENT_BUS.register(com.nightbeam.remnants.event.EntityTickEvents.class);
		MinecraftForge.EVENT_BUS.register(com.nightbeam.remnants.event.EntityDeathEvents.class);
		MinecraftForge.EVENT_BUS.register(com.nightbeam.remnants.event.PlayerInteractionEvents.class);
		MinecraftForge.EVENT_BUS.register(com.nightbeam.remnants.event.BlockInteractionEvents.class);

		LOGGER.info("Remnant Bosses mod registered");
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			com.nightbeam.remnants.config.JaumlConfigBootstrap.initConfigs();
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
