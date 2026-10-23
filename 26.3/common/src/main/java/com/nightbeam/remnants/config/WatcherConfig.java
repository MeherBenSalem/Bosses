package com.nightbeam.remnants.config;

import java.util.Map;

/**
 * remnant/bosses/forgotten_watcher (numbers) and forgotten_watcher_summon (ritual strings). The bootstrap
 * writes missing keys; the 2.x umbrakar and umbrakar_summon files stay on disk unused (player edits of the
 * old ritual seed the new ritual once, see {@link ConfigMigration#CARRIES}).
 */
public final class WatcherConfig {
	public static final String FILE = "forgotten_watcher", SUMMON = "forgotten_watcher_summon";

	private WatcherConfig() {
	}

	public static final Map<String, Double> DEFAULTS = Map.ofEntries(
			Map.entry("max_health", 680.0), Map.entry("armor", 8.0), Map.entry("armor_toughness", 2.0),
			Map.entry("drift_speed", 0.08), Map.entry("hover_height", 0.6), Map.entry("xp_reward", 400.0),
			Map.entry("lens_radius", 9.0), Map.entry("leash_radius", 22.0),
			Map.entry("phase_two_percent", 50.0), Map.entry("attack_cooldown_ticks", 34.0),
			Map.entry("phase_two_cooldown_multiplier", 0.75),
			Map.entry("bolt_damage", 7.0), Map.entry("bolt_speed", 0.45), Map.entry("volley_bolts", 12.0),
			Map.entry("phase_two_volley_bolts", 16.0), Map.entry("volley_range", 26.0),
			Map.entry("beam_damage", 5.0), Map.entry("beam_hit_interval", 10.0), Map.entry("beam_length", 24.0),
			Map.entry("beam_sweep_degrees", 30.0),
			Map.entry("hug_distance", 4.0), Map.entry("hug_ticks", 30.0),
			Map.entry("illusion_count", 2.0), Map.entry("illusion_cooldown_ticks", 600.0), Map.entry("illusion_life_ticks", 400.0),
			Map.entry("illusion_bolt_damage", 3.0),
			Map.entry("aegis_ticks", 200.0), Map.entry("aegis_cooldown_ticks", 600.0), Map.entry("aegis_turn_ticks", 160.0),
			Map.entry("aegis_open_degrees", 35.0), Map.entry("aegis_break_hits", 3.0), Map.entry("exposed_ticks", 60.0),
			Map.entry("exposed_damage_multiplier", 1.25),
			Map.entry("summoning_enabled", 1.0), Map.entry("duplicate_radius", 64.0),
			Map.entry("boss_music_enabled", 1.0), Map.entry("boss_music_radius", 64.0));

	/** Offerings are east, west, south, north (pedestal one to four). */
	public static final Map<String, String> RITUAL = Map.of(
			"portal_activation_item", "remnant_bosses:memory_lens",
			"pedestal_one_activation_block", "minecraft:amethyst_block",
			"pedestal_two_activation_block", "minecraft:calcite",
			"pedestal_three_activation_block", "minecraft:chiseled_sandstone",
			"pedestal_four_activation_block", "minecraft:lectern");

	public static double value(String key, double min, double max) {
		double v = JaumlConfigLib.getNumberValue("remnant/bosses", FILE, key);
		return Math.max(min, Math.min(max, Double.isFinite(v) ? v : DEFAULTS.getOrDefault(key, min)));
	}
}
