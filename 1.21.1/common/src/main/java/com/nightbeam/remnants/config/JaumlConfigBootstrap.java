package com.nightbeam.remnants.config;

import com.nightbeam.remnants.Constants;

import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class JaumlConfigBootstrap {
	private static final Logger LOGGER = Constants.LOG;

	private JaumlConfigBootstrap() {
	}

	public static void initConfigs() {
		JaumlApi api = JaumlApi.load();
		if (api == null) {
			LOGGER.warn("JaumlConfigLib not present; skipping config bootstrap.");
			return;
		}

		if (api.createConfigFile("remnant/bosses", "ossukage_summon")) {
			if (api.createConfigFile("remnant", "main")) {
				// Populate a couple of safe defaults so main.json is not empty and is
				// discoverable by users
				if (!api.arrayKeyExists("remnant", "main", "config_version"))
					api.setStringValue("remnant", "main", "config_version", "1");
				if (!api.arrayKeyExists("remnant", "main", "generated_by"))
					api.setStringValue("remnant", "main", "generated_by", "Remnant Bosses mod - default config");
			}
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage_summon", "portal_activation_item")) {
			api.setStringValue("remnant/bosses", "ossukage_summon", "portal_activation_item", "minecraft:nether_star");
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage_summon", "pedestal_one_activation_block")) {
			api.setStringValue("remnant/bosses", "ossukage_summon", "pedestal_one_activation_block",
					"minecraft:skeleton_skull");
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage_summon", "pedestal_two_activation_block")) {
			api.setStringValue("remnant/bosses", "ossukage_summon", "pedestal_two_activation_block",
					"minecraft:skeleton_skull");
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage_summon", "pedestal_three_activation_block")) {
			api.setStringValue("remnant/bosses", "ossukage_summon", "pedestal_three_activation_block",
					"minecraft:skeleton_skull");
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage_summon", "pedestal_four_activation_block")) {
			api.setStringValue("remnant/bosses", "ossukage_summon", "pedestal_four_activation_block",
					"minecraft:skeleton_skull");
		}

		if (api.createConfigFile("remnant/items", "ossukage_sword")) {
			api.createConfigFile("remnant/items", "ossukage_sword");
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "dash_timer")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "dash_timer", 100);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "dash_distance")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "dash_distance", 2);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_timer")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "shuriken_timer", 50);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_damage")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "shuriken_damage", 50);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_knockback")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "shuriken_knockback", 1);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "shuriken_pierce")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "shuriken_pierce", 0);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "speed_amplifier")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "speed_amplifier", 1);
		}
		if (!api.arrayKeyExists("remnant/items", "ossukage_sword", "life_steal_power")) {
			api.setNumberValue("remnant/items", "ossukage_sword", "life_steal_power", 20);
		}

		if (api.createConfigFile("remnant/bosses", "ossukage")) {
			api.createConfigFile("remnant/bosses", "ossukage");
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "on_spawn_skeletons")) {
			api.setNumberValue("remnant/bosses", "ossukage", "on_spawn_skeletons", 2);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "max_health_phase_1")) {
			api.setNumberValue("remnant/bosses", "ossukage", "max_health_phase_1", 800);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "attack_damage_phase_1")) {
			api.setNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_1", 7);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "movement_speed_phase_1")) {
			api.setNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_1", 0.25);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "hp_threshold_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "hp_threshold_phase_2", 50);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "transform_delay_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "transform_delay_phase_2", 60);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "skeletons_on_transform_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "skeletons_on_transform_phase_2", 5);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "skeletons_on_dash_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "skeletons_on_dash_phase_2", 2);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "special_attack_chance_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "special_attack_chance_phase_2", 20);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "health_boost_timer_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "health_boost_timer_phase_2", 200);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "attack_damage_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "attack_damage_phase_2", 9);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "movement_speed_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "movement_speed_phase_2", 0.3);
		}
		// Boss Music Settings
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "boss_music_enabled")) {
			api.setNumberValue("remnant/bosses", "ossukage", "boss_music_enabled", 1);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "boss_music_radius")) {
			api.setNumberValue("remnant/bosses", "ossukage", "boss_music_radius", 64);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "armor_phase_1")) {
			api.setNumberValue("remnant/bosses", "ossukage", "armor_phase_1", 6);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "armor_phase_2")) {
			api.setNumberValue("remnant/bosses", "ossukage", "armor_phase_2", 8);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "follow_range")) {
			api.setNumberValue("remnant/bosses", "ossukage", "follow_range", 64);
		}
		if (!api.arrayKeyExists("remnant/bosses", "ossukage", "knockback_resistance")) {
			api.setNumberValue("remnant/bosses", "ossukage", "knockback_resistance", 0.6);
		}

		// Spawning Settings
		if (api.createConfigFile("remnant/spawning", "rat_spawns")) {
			api.createConfigFile("remnant/spawning", "rat_spawns");
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "enable_natural_spawning")) {
			api.setStringValue("remnant/spawning", "rat_spawns", "enable_natural_spawning", "true");
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "spawn_weight")) {
			api.setNumberValue("remnant/spawning", "rat_spawns", "spawn_weight", 10);
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "min_group_size")) {
			api.setNumberValue("remnant/spawning", "rat_spawns", "min_group_size", 1);
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "max_group_size")) {
			api.setNumberValue("remnant/spawning", "rat_spawns", "max_group_size", 3);
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "dimension_whitelist")) {
			api.setStringValue("remnant/spawning", "rat_spawns", "dimension_whitelist", "minecraft:overworld");
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "dimension_blacklist")) {
			api.setStringValue("remnant/spawning", "rat_spawns", "dimension_blacklist",
					"minecraft:the_nether,minecraft:the_end");
		}
		if (!api.arrayKeyExists("remnant/spawning", "rat_spawns", "biome_blacklist")) {
			api.setStringValue("remnant/spawning", "rat_spawns", "biome_blacklist", "");
		}

		// Balance Settings
		if (api.createConfigFile("remnant/balance", "rat_stats")) {
			api.createConfigFile("remnant/balance", "rat_stats");
		}
		if (!api.arrayKeyExists("remnant/balance", "rat_stats", "rat_health")) {
			api.setNumberValue("remnant/balance", "rat_stats", "rat_health", 30.0);
		}
		if (!api.arrayKeyExists("remnant/balance", "rat_stats", "rat_attack_damage")) {
			api.setNumberValue("remnant/balance", "rat_stats", "rat_attack_damage", 4.0);
		}
		if (!api.arrayKeyExists("remnant/balance", "rat_stats", "rat_armor")) {
			api.setNumberValue("remnant/balance", "rat_stats", "rat_armor", 2.0);
		}

		// Wraith Spawning Settings
		if (api.createConfigFile("remnant/spawning", "wraith_spawns")) {
			api.createConfigFile("remnant/spawning", "wraith_spawns");
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "enable_natural_spawning")) {
			api.setStringValue("remnant/spawning", "wraith_spawns", "enable_natural_spawning", "true");
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "spawn_weight")) {
			api.setNumberValue("remnant/spawning", "wraith_spawns", "spawn_weight", 5);
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "min_group_size")) {
			api.setNumberValue("remnant/spawning", "wraith_spawns", "min_group_size", 1);
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "max_group_size")) {
			api.setNumberValue("remnant/spawning", "wraith_spawns", "max_group_size", 2);
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "dimension_whitelist")) {
			api.setStringValue("remnant/spawning", "wraith_spawns", "dimension_whitelist", "minecraft:overworld");
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "dimension_blacklist")) {
			api.setStringValue("remnant/spawning", "wraith_spawns", "dimension_blacklist", "minecraft:the_nether,minecraft:the_end");
		}
		if (!api.arrayKeyExists("remnant/spawning", "wraith_spawns", "biome_blacklist")) {
			api.setStringValue("remnant/spawning", "wraith_spawns", "biome_blacklist", "");
		}

		// Wraith Balance Settings
		if (api.createConfigFile("remnant/balance", "wraith_stats")) {
			api.createConfigFile("remnant/balance", "wraith_stats");
		}
		if (!api.arrayKeyExists("remnant/balance", "wraith_stats", "wraith_health")) {
			api.setNumberValue("remnant/balance", "wraith_stats", "wraith_health", 20.0);
		}
		if (!api.arrayKeyExists("remnant/balance", "wraith_stats", "wraith_attack_damage")) {
			api.setNumberValue("remnant/balance", "wraith_stats", "wraith_attack_damage", 3.0);
		}
		if (!api.arrayKeyExists("remnant/balance", "wraith_stats", "wraith_armor")) {
			api.setNumberValue("remnant/balance", "wraith_stats", "wraith_armor", 0.0);
		}
		if (!api.arrayKeyExists("remnant/balance", "wraith_stats", "wraith_movement_speed")) {
			api.setNumberValue("remnant/balance", "wraith_stats", "wraith_movement_speed", 0.25);
		}
		if (!api.arrayKeyExists("remnant/balance", "wraith_stats", "wraith_follow_range")) {
			api.setNumberValue("remnant/balance", "wraith_stats", "wraith_follow_range", 16.0);
		}

		api.createConfigFile("remnant/bosses", "umbrakar_summon");
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar_summon", "portal_activation_item")) {
			api.setStringValue("remnant/bosses", "umbrakar_summon", "portal_activation_item", "minecraft:echo_shard");
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar_summon", "pedestal_one_activation_block")) {
			api.setStringValue("remnant/bosses", "umbrakar_summon", "pedestal_one_activation_block", "minecraft:amethyst_block");
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar_summon", "pedestal_two_activation_block")) {
			api.setStringValue("remnant/bosses", "umbrakar_summon", "pedestal_two_activation_block", "minecraft:crying_obsidian");
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar_summon", "pedestal_three_activation_block")) {
			api.setStringValue("remnant/bosses", "umbrakar_summon", "pedestal_three_activation_block", "minecraft:end_stone");
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar_summon", "pedestal_four_activation_block")) {
			api.setStringValue("remnant/bosses", "umbrakar_summon", "pedestal_four_activation_block", "minecraft:sculk");
		}
		api.createConfigFile("remnant/bosses", "umbrakar");
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "max_health")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "max_health", 750);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "attack_damage")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "attack_damage", 14);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "movement_speed")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "movement_speed", 0.32);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "armor")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "armor", 10);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "follow_range")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "follow_range", 48);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "orb_damage")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "orb_damage", 8);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "hp_threshold_phase_2")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "hp_threshold_phase_2", 40);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "attack_damage_phase_2")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "attack_damage_phase_2", 18);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "movement_speed_phase_2")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "movement_speed_phase_2", 0.38);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "bite_damage")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "bite_damage", 16);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "slam_damage")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "slam_damage", 18);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "tail_damage")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "tail_damage", 15);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "roar_damage")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "roar_damage", 8);
		}
		if (!api.arrayKeyExists("remnant/bosses", "umbrakar", "roar_range")) {
			api.setNumberValue("remnant/bosses", "umbrakar", "roar_range", 16);
		}

		api.createConfigFile("remnant/bosses", "kotsukage_summon");
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage_summon", "portal_activation_item")) {
			api.setStringValue("remnant/bosses", "kotsukage_summon", "portal_activation_item", "minecraft:wither_skeleton_skull");
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage_summon", "pedestal_one_activation_block")) {
			api.setStringValue("remnant/bosses", "kotsukage_summon", "pedestal_one_activation_block", "minecraft:bone_block");
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage_summon", "pedestal_two_activation_block")) {
			api.setStringValue("remnant/bosses", "kotsukage_summon", "pedestal_two_activation_block", "minecraft:soul_sand");
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage_summon", "pedestal_three_activation_block")) {
			api.setStringValue("remnant/bosses", "kotsukage_summon", "pedestal_three_activation_block", "minecraft:soul_soil");
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage_summon", "pedestal_four_activation_block")) {
			api.setStringValue("remnant/bosses", "kotsukage_summon", "pedestal_four_activation_block", "minecraft:nether_wart_block");
		}

		api.createConfigFile("remnant/bosses", "kotsukage");
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "max_health")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "max_health", 700);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "attack_damage")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "attack_damage", 12);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "movement_speed")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "movement_speed", 0.28);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "armor")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "armor", 8);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "follow_range")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "follow_range", 40);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "knockback_resistance")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "knockback_resistance", 0.75);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "hp_threshold_phase_2")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "hp_threshold_phase_2", 45);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "attack_damage_phase_2")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "attack_damage_phase_2", 16);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "movement_speed_phase_2")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "movement_speed_phase_2", 0.34);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "swipe_damage")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "swipe_damage", 14);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "stomp_damage")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "stomp_damage", 16);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "stomp_radius")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "stomp_radius", 5);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "poison_damage")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "poison_damage", 10);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "poison_range")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "poison_range", 8);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "poison_duration")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "poison_duration", 120);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "roar_damage")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "roar_damage", 8);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "roar_range")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "roar_range", 14);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "trap_damage")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "trap_damage", 10);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "trap_count")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "trap_count", 2);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "trap_slow_duration")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "trap_slow_duration", 80);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "minion_count")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "minion_count", 2);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "boss_music_enabled")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "boss_music_enabled", 1);
		}
		if (!api.arrayKeyExists("remnant/bosses", "kotsukage", "boss_music_radius")) {
			api.setNumberValue("remnant/bosses", "kotsukage", "boss_music_radius", 64);
		}

		api.createConfigFile("remnant/balance", "skeleton_minion_stats");
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "minion_health")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "minion_health", 24);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "minion_attack_damage")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "minion_attack_damage", 5);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "minion_movement_speed")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "minion_movement_speed", 0.32);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "minion_armor")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "minion_armor", 2);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "poison_duration")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "poison_duration", 60);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "leap_cooldown")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "leap_cooldown", 80);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "trap_chance")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "trap_chance", 18);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_minion_stats", "trap_cooldown")) {
			api.setNumberValue("remnant/balance", "skeleton_minion_stats", "trap_cooldown", 160);
		}

		api.createConfigFile("remnant/balance", "skeleton_melee_stats");
		if (!api.arrayKeyExists("remnant/balance", "skeleton_melee_stats", "max_health")) {
			api.setNumberValue("remnant/balance", "skeleton_melee_stats", "max_health", 28);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_melee_stats", "attack_damage")) {
			api.setNumberValue("remnant/balance", "skeleton_melee_stats", "attack_damage", 6);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_melee_stats", "heavy_damage")) {
			api.setNumberValue("remnant/balance", "skeleton_melee_stats", "heavy_damage", 8);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_melee_stats", "movement_speed")) {
			api.setNumberValue("remnant/balance", "skeleton_melee_stats", "movement_speed", 0.28);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_melee_stats", "armor")) {
			api.setNumberValue("remnant/balance", "skeleton_melee_stats", "armor", 4);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_melee_stats", "follow_range")) {
			api.setNumberValue("remnant/balance", "skeleton_melee_stats", "follow_range", 24);
		}

		api.createConfigFile("remnant/balance", "skeleton_archer_stats");
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "max_health")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "max_health", 22);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "attack_damage")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "attack_damage", 4);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "arrow_damage")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "arrow_damage", 4);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "charged_damage")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "charged_damage", 6);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "movement_speed")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "movement_speed", 0.26);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "armor")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "armor", 2);
		}
		if (!api.arrayKeyExists("remnant/balance", "skeleton_archer_stats", "follow_range")) {
			api.setNumberValue("remnant/balance", "skeleton_archer_stats", "follow_range", 28);
		}
	}

	private static final class JaumlApi {
		private final Method createConfigFile;
		private final Method arrayKeyExists;
		private final Method setStringValue;
		private final Method setNumberValue;
		private final Class<?> numberParamType;

		private JaumlApi(Method createConfigFile, Method arrayKeyExists, Method setStringValue, Method setNumberValue,
				Class<?> numberParamType) {
			this.createConfigFile = createConfigFile;
			this.arrayKeyExists = arrayKeyExists;
			this.setStringValue = setStringValue;
			this.setNumberValue = setNumberValue;
			this.numberParamType = numberParamType;
		}

		static JaumlApi load() {
			try {
				Class<?> clazz = Class.forName("tn.naizo.jauml.JaumlConfigLib");
				Method createConfigFile = clazz.getMethod("createConfigFile", String.class, String.class);
				Method arrayKeyExists = clazz.getMethod("arrayKeyExists", String.class, String.class, String.class);
				Method setStringValue = clazz.getMethod("setStringValue", String.class, String.class, String.class,
						String.class);
				Method setNumberValue = findSetNumberValue(clazz);
				return new JaumlApi(createConfigFile, arrayKeyExists, setStringValue, setNumberValue,
						setNumberValue.getParameterTypes()[3]);
			} catch (ClassNotFoundException e) {
				return null;
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("Failed to bind JaumlConfigLib; configs will not be generated.", e);
				return null;
			}
		}

		private static Method findSetNumberValue(Class<?> clazz) throws NoSuchMethodException {
			Method method = tryGetMethod(clazz, "setNumberValue", Number.class);
			if (method != null) {
				return method;
			}
			Class<?>[] candidates = new Class<?>[] { Double.class, double.class, Integer.class, int.class, Float.class,
					float.class, Long.class, long.class };
			for (Class<?> type : candidates) {
				method = tryGetMethod(clazz, "setNumberValue", type);
				if (method != null) {
					return method;
				}
			}
			throw new NoSuchMethodException("setNumberValue(String,String,String,Number|primitive)");
		}

		private static Method tryGetMethod(Class<?> clazz, String name, Class<?> numberType) {
			try {
				return clazz.getMethod(name, String.class, String.class, String.class, numberType);
			} catch (NoSuchMethodException e) {
				return null;
			}
		}

		boolean createConfigFile(String path, String name) {
			try {
				return (boolean) createConfigFile.invoke(null, path, name);
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("JaumlConfigLib.createConfigFile failed for {}/{}", path, name, e);
				return false;
			}
		}

		boolean arrayKeyExists(String path, String name, String key) {
			try {
				return (boolean) arrayKeyExists.invoke(null, path, name, key);
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("JaumlConfigLib.arrayKeyExists failed for {}/{}/{}", path, name, key, e);
				return false;
			}
		}

		void setStringValue(String path, String name, String key, String value) {
			try {
				setStringValue.invoke(null, path, name, key, value);
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("JaumlConfigLib.setStringValue failed for {}/{}/{}", path, name, key, e);
			}
		}

		void setNumberValue(String path, String name, String key, Number value) {
			try {
				setNumberValue.invoke(null, path, name, key, coerceNumber(value));
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("JaumlConfigLib.setNumberValue failed for {}/{}/{}", path, name, key, e);
			}
		}

		private Object coerceNumber(Number value) {
			if (numberParamType == Integer.class || numberParamType == int.class) {
				return value.intValue();
			}
			if (numberParamType == Double.class || numberParamType == double.class) {
				return value.doubleValue();
			}
			if (numberParamType == Float.class || numberParamType == float.class) {
				return value.floatValue();
			}
			if (numberParamType == Long.class || numberParamType == long.class) {
				return value.longValue();
			}
			return value;
		}
	}
}
