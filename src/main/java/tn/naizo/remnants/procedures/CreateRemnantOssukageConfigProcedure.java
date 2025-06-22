package tn.naizo.remnants.procedures;

import tn.naizo.jauml.JaumlConfigLib;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateRemnantOssukageConfigProcedure {
	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		execute();
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
		if (JaumlConfigLib.createConfigFile("remnant/bosses", "ossukage")) {
			JaumlConfigLib.createConfigFile("remnant/bosses", "ossukage");
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "max_health_phase_1")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "max_health_phase_1", 800);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "attack_damage_phase_1")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_1", 7);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "movement_speed_phase_1")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_1", 0.25);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "hp_threshold_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "hp_threshold_phase_2", 50);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "transform_delay_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "transform_delay_phase_2", 40);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "skeletons_on_transform_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "skeletons_on_transform_phase_2", 5);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "skeletons_on_dash_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "skeletons_on_dash_phase_2", 2);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "special_attack_chance_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "special_attack_chance_phase_2", 20);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "invisibility_timer_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "invisibility_timer_phase_2", 200);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "attack_damage_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_2", 9);
		}
		if (!JaumlConfigLib.arrayKeyExists("remnant/bosses", "ossukage", "movement_speed_phase_2")) {
			JaumlConfigLib.setNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_2", 0.3);
		}
	}
}
