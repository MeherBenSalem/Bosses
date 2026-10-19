package com.nightbeam.remnants.config;

import java.util.Map;

/**
 * remnant/realm/realm: the Eclipsed Realm's tunables. Client presentation switches
 * (realm_titles, camera_shake) live in remnant/client/presentation with the other client options.
 */
public final class RealmConfig {
	public static final Map<String, Double> DEFAULTS = Map.ofEntries(
			Map.entry("kindle_ticks", 120.0),
			Map.entry("crossing_ticks", 40.0),
			Map.entry("crossing_cooldown_ticks", 100.0),
			Map.entry("arrival_search_radius", 48.0),
			Map.entry("spawn_thinning", 0.4),
			Map.entry("spawn_block_light_max", 7.0),
			Map.entry("rituals_realm_only", 1.0),
			Map.entry("guidance_radius_chunks", 48.0),
			Map.entry("felled_banner_radius", 64.0));
	public static final String[] CLIENT_SWITCHES = {"realm_titles", "camera_shake"};

	private RealmConfig() {
	}

	public static double value(String key, double min, double max) {
		double v = JaumlConfigLib.getNumberValue("remnant/realm", "realm", key);
		if (!Double.isFinite(v)) v = DEFAULTS.getOrDefault(key, min);
		return Math.max(min, Math.min(max, v));
	}

	public static int kindleTicks() {
		return (int) value("kindle_ticks", 20, 1200);
	}

	public static int crossingTicks() {
		return (int) value("crossing_ticks", 10, 200);
	}

	public static int crossingCooldown() {
		return (int) value("crossing_cooldown_ticks", 20, 1200);
	}

	public static boolean ritualsRealmOnly() {
		return value("rituals_realm_only", 0, 1) > 0;
	}

	public static boolean clientSwitch(String key) {
		double v = JaumlConfigLib.getNumberValue("remnant/client", "presentation", key);
		return !Double.isFinite(v) || v > 0;
	}
}
