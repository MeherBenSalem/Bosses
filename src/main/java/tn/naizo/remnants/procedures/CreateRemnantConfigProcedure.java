package tn.naizo.remnants.procedures;

import tn.naizo.jauml.JaumlConfigLib;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateRemnantConfigProcedure {
	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		execute();
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
		if (JaumlConfigLib.createConfigFile("remnant/items", "ossukage_sword")) {
			JaumlConfigLib.createConfigFile("remnant/items", "ossukage_sword");
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "dash_timer")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "dash_timer", 100);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "dash_distance")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "dash_distance", 2);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_timer")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "shuriken_timer", 50);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_damage")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "shuriken_damage", 50);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_knockback")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "shuriken_knockback", 1);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_pierce")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "shuriken_pierce", 0);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "speed_amplifier")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "speed_amplifier", 1);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/items", "ossukage_sword", "life_steal_power")) {
			JaumlConfigLib.setNumberValue("remnant/items", "ossukage_sword", "life_steal_power", 20);
		}
	}
}