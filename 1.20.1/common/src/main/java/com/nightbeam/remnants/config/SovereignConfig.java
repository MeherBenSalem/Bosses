package com.nightbeam.remnants.config;

import java.util.Map;

public final class SovereignConfig {
    private SovereignConfig() {}
    public static final Map<String, Double> DEFAULTS = Map.ofEntries(
        Map.entry("max_health", 950.0), Map.entry("attack_damage", 16.0),
        Map.entry("armor", 8.0), Map.entry("movement_speed", 0.25),
        Map.entry("phase_two_percent", 45.0), Map.entry("phase_two_damage_multiplier", 1.3),
        Map.entry("attack_cooldown_ticks", 35.0), Map.entry("slam_damage", 20.0),
        Map.entry("beam_damage", 24.0), Map.entry("eruption_damage", 18.0),
        Map.entry("spell_range", 24.0), Map.entry("wave_radius", 12.0),
        Map.entry("summoning_enabled", 1.0), Map.entry("duplicate_radius", 64.0),
        Map.entry("xp_reward", 150.0));
    public static final Map<String, String> RITUAL = Map.of(
        "portal_activation_item", "minecraft:heart_of_the_sea",
        "pedestal_one_activation_block", "minecraft:bone_block",
        "pedestal_two_activation_block", "minecraft:bone_block",
        "pedestal_three_activation_block", "minecraft:skeleton_skull",
        "pedestal_four_activation_block", "minecraft:skeleton_skull");
    public static double value(String key, double min, double max) {
        double v = JaumlConfigLib.getNumberValue("remnant/bosses", "hollow_sovereign", key);
        return Math.max(min, Math.min(max, Double.isFinite(v) ? v : DEFAULTS.getOrDefault(key, min)));
    }
    public static double visualDensity() {
        double n = JaumlConfigLib.getNumberValue("remnant/client", "presentation", "vfx_density");
        return Double.isFinite(n) ? Math.max(0, Math.min(1, n)) : 1;
    }
}
