package com.naizo.ossukage.init;

import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.naizo.ossukage.configuration.RemnantConfigConfiguration;
import com.naizo.ossukage.RemnantOssukageMod;

@Mod.EventBusSubscriber(modid = RemnantOssukageMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RemnantOssukageModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() -> {
			ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RemnantConfigConfiguration.SPEC, "remnant_main.toml");
		});
	}
}
