package tn.naizo.remnants.init;

import tn.naizo.remnants.configuration.MainConfigConfiguration;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = RemnantBossesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RemnantBossesModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() -> {
			ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MainConfigConfiguration.SPEC, "remnant_main.toml");
		});
	}
}
