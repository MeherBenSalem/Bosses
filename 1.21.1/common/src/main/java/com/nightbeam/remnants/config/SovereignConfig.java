package com.nightbeam.remnants.config;

import java.util.Map;

/**
 * remnant/bosses/hollow_sovereign (numbers) and hollow_sovereign_summon (ritual strings).
 * The bootstrap writes missing keys; values a 2.x file still holds at their 2.x default are first
 * rewritten once by {@link ConfigMigration} (player edits stay). Keys from 2.x that the Sovereign no
 * longer reads (beam_damage, wave_radius, ...) stay on disk unused.
 */
public final class SovereignConfig {
    private SovereignConfig() {}
    public static final Map<String, Double> DEFAULTS = Map.ofEntries(
        Map.entry("max_health", 720.0), Map.entry("armor", 12.0), Map.entry("armor_toughness", 4.0),
        Map.entry("movement_speed", 0.24), Map.entry("run_speed_multiplier", 1.35), Map.entry("xp_reward", 300.0),
        Map.entry("phase_two_percent", 66.0), Map.entry("phase_three_percent", 33.0), Map.entry("last_command_percent", 10.0),
        Map.entry("attack_cooldown_ticks", 30.0), Map.entry("phase_two_cooldown_multiplier", 0.75),
        Map.entry("phase_three_cooldown_multiplier", 0.6),
        Map.entry("combo_damage", 9.0), Map.entry("combo_finisher_multiplier", 1.45),
        Map.entry("slam_damage", 14.0), Map.entry("slam_radius", 9.0), Map.entry("slam_ground_height", 1.2),
        Map.entry("riposte_damage", 16.0), Map.entry("guard_break_multiplier", 1.25),
        Map.entry("cleave_damage", 12.0), Map.entry("cleave_length", 12.0),
        Map.entry("summon_cooldown_ticks", 500.0),
        Map.entry("spectral_slash_damage", 10.0), Map.entry("crescent_speed", 1.1),
        Map.entry("soul_lunge_damage", 15.0), Map.entry("soul_lunge_distance", 9.0),
        Map.entry("last_command_damage", 12.0), Map.entry("last_command_radius", 2.5),
        Map.entry("kneel_damage_multiplier", 1.5),
        Map.entry("pauldron_health", 40.0), Map.entry("pauldron_dive_damage", 8.0),
        Map.entry("summoning_enabled", 1.0), Map.entry("duplicate_radius", 64.0),
        Map.entry("boss_music_enabled", 1.0), Map.entry("boss_music_radius", 64.0));
    public static final Map<String, String> RITUAL = Map.of(
        "portal_activation_item", "remnant_bosses:sovereigns_seal",
        "pedestal_one_activation_block", "minecraft:gold_block",
        "pedestal_two_activation_block", "minecraft:gold_block",
        "pedestal_three_activation_block", "minecraft:soul_lantern",
        "pedestal_four_activation_block", "minecraft:soul_lantern");
    public static double value(String key, double min, double max) {
        double v = JaumlConfigLib.getNumberValue("remnant/bosses", "hollow_sovereign", key);
        return Math.max(min, Math.min(max, Double.isFinite(v) ? v : DEFAULTS.getOrDefault(key, min)));
    }
    public static double visualDensity() {
        double n = JaumlConfigLib.getNumberValue("remnant/client", "presentation", "vfx_density");
        return Double.isFinite(n) ? Math.max(0, Math.min(1, n)) : 1;
    }
}
