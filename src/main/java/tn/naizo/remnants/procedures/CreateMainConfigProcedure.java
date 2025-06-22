package tn.naizo.remnants.procedures;

import tn.naizo.jauml.JaumlConfigLib;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateMainConfigProcedure {
	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		execute();
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
		if (JaumlConfigLib.createConfigFile("remnant", "main")) {
			JaumlConfigLib.createConfigFile("remnant", "main");
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant", "main", "portal_activation_item")) {
			JaumlConfigLib.setStringValue("remnant", "main", "portal_activation_item", "minecraft:nether_star");
		}
	}
}
