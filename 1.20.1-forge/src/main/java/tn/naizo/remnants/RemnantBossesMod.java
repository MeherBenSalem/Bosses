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

@Mod("remnant_bosses")
public class RemnantBossesMod {
	public static final Logger LOGGER = LogManager.getLogger(RemnantBossesMod.class);
	public static final String MODID = "remnant_bosses";

	public RemnantBossesMod(FMLJavaModLoadingContext context) {
		IEventBus modEventBus = context.getModEventBus();

		// Register all registries
		ModItems.ITEMS.register(modEventBus);
		ModBlocks.BLOCKS.register(modEventBus);
		ModEntities.ENTITIES.register(modEventBus);
		ModEntities.SPAWN_EGGS.register(modEventBus);
		ModSounds.SOUNDS.register(modEventBus);
		ModTabs.TABS.register(modEventBus);

		// Setup event
		modEventBus.addListener(this::commonSetup);

		// Register gameplay event handlers
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.EntitySpawnEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.EntityTickEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.EntityDeathEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.PlayerInteractionEvents.class);
		MinecraftForge.EVENT_BUS.register(tn.naizo.remnants.event.BlockInteractionEvents.class);

		LOGGER.info("Remnant Bosses mod registered");
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		LOGGER.info("Remnant Bosses mod loaded successfully");
	}
}