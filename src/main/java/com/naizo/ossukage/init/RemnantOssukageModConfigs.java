package com.naizo.ossukage.init;

import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;

import com.naizo.ossukage.configuration.MainConfigConfiguration;
import com.naizo.ossukage.RemnantOssukageMod;

@EventBusSubscriber(modid = RemnantOssukageMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class RemnantOssukageModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() -> {
			ModList.get().getModContainerById("remnant_ossukage").get().registerConfig(ModConfig.Type.COMMON, MainConfigConfiguration.SPEC, "remnant_main.toml");
		});
	}
}
