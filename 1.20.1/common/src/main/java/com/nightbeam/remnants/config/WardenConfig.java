package com.nightbeam.remnants.config;

import java.util.Map;

/**
 * remnant/bosses/gravebound_warden (numbers) and gravebound_warden_summon (ritual strings). The bootstrap
 * writes missing keys; the 2.x kotsukage and kotsukage_summon files stay on disk unused (player edits of
 * the old ritual seed the new ritual once, see {@link ConfigMigration#CARRIES}).
 */
public final class WardenConfig {
    public static final String FILE = "gravebound_warden", SUMMON = "gravebound_warden_summon";

    private WardenConfig() {}

    public static final Map<String, Double> DEFAULTS = Map.ofEntries(
        Map.entry("max_health", 520.0), Map.entry("armor", 10.0), Map.entry("armor_toughness", 2.0),
        Map.entry("movement_speed", 0.22), Map.entry("phase_two_speed_multiplier", 1.25), Map.entry("xp_reward", 200.0),
        Map.entry("phase_two_percent", 50.0), Map.entry("attack_cooldown_ticks", 30.0),
        Map.entry("phase_two_cooldown_multiplier", 0.75),
        Map.entry("sweep_damage", 12.0), Map.entry("sweep_radius", 4.5), Map.entry("sweep_arc", 110.0),
        Map.entry("sweep_slow_ticks", 40.0),
        Map.entry("chop_damage", 18.0), Map.entry("chop_length", 6.0), Map.entry("chop_width", 2.0),
        Map.entry("chop_wedge_multiplier", 1.25),
        Map.entry("chain_speed", 1.4), Map.entry("chain_range", 14.0), Map.entry("chain_latch_ticks", 8.0),
        Map.entry("chain_pull_fraction", 0.7), Map.entry("chain_grab_cooldown_ticks", 160.0),
        Map.entry("eruption_damage", 9.0), Map.entry("eruption_count", 6.0), Map.entry("eruption_spacing", 1.5),
        Map.entry("summon_count", 2.0), Map.entry("phase_two_summon_count", 3.0), Map.entry("max_soldiers", 4.0),
        Map.entry("summon_cooldown_ticks", 400.0),
        Map.entry("chain_wall_radius", 9.0), Map.entry("anchor_health", 30.0), Map.entry("chain_damage", 6.0),
        Map.entry("chain_wall_cooldown_ticks", 700.0), Map.entry("anchor_life_ticks", 400.0),
        Map.entry("anchor_stagger_ticks", 60.0),
        Map.entry("summoning_enabled", 1.0), Map.entry("duplicate_radius", 64.0),
        Map.entry("boss_music_enabled", 1.0), Map.entry("boss_music_radius", 64.0));

    /** Offerings are east, west, south, north (pedestal one to four). */
    public static final Map<String, String> RITUAL = Map.of(
        "portal_activation_item", "remnant_bosses:crypt_key",
        "pedestal_one_activation_block", "minecraft:bone_block",
        "pedestal_two_activation_block", "minecraft:soul_soil",
        "pedestal_three_activation_block", "minecraft:chain",
        "pedestal_four_activation_block", "minecraft:iron_bars");

    public static double value(String key, double min, double max) {
        double v = JaumlConfigLib.getNumberValue("remnant/bosses", FILE, key);
        return Math.max(min, Math.min(max, Double.isFinite(v) ? v : DEFAULTS.getOrDefault(key, min)));
    }
}
