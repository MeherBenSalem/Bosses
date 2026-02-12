package tn.naizo.remnants.config;

import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Safe wrapper for JaumlConfigLib that uses reflection to avoid hard dependency.
 * Allows procedures to call Jauml config methods safely even if library isn't present.
 */
public class JaumlConfigLib {
	private static final Logger LOGGER = LogManager.getLogger(JaumlConfigLib.class);
	private static Object configLibInstance = null;
	private static Method getNumberMethod = null;
	private static Method getStringMethod = null;
	
	static {
		try {
			Class<?> jaumlClass = Class.forName("tn.naizo.jauml.JaumlConfigLib");
			configLibInstance = jaumlClass;
			getNumberMethod = jaumlClass.getMethod("getNumberValue", String.class, String.class, String.class);
			getStringMethod = jaumlClass.getMethod("getStringValue", String.class, String.class, String.class);
		} catch (Exception e) {
			LOGGER.debug("JaumlConfigLib not found in classpath - config values will return defaults");
		}
	}
	
	/**
	 * Get a number value from Jauml config, with fallback to default.
	 * Returns the result as a double for compatibility.
	 */
	public static double getNumberValue(String category, String file, String key) {
		if (getNumberMethod == null) {
			// Return reasonable defaults if Jauml not available
			return getDefaultNumber(category, file, key);
		}
		try {
			Object result = getNumberMethod.invoke(null, category, file, key);
			if (result instanceof Number n) {
				return n.doubleValue();
			}
			return getDefaultNumber(category, file, key);
		} catch (Exception e) {
			LOGGER.warn("Failed to get config value {}/{}/{}", category, file, key, e);
			return getDefaultNumber(category, file, key);
		}
	}
	
	/**
	 * Get a string value from Jauml config, with fallback to default.
	 */
	public static String getStringValue(String category, String file, String key) {
		if (getStringMethod == null) {
			return getDefaultString(category, file, key);
		}
		try {
			return (String) getStringMethod.invoke(null, category, file, key);
		} catch (Exception e) {
			LOGGER.warn("Failed to get config value {}/{}/{}", category, file, key, e);
			return getDefaultString(category, file, key);
		}
	}
	
	/**
	 * Provide sensible fallback defaults for config values.
	 */
	private static double getDefaultNumber(String category, String file, String key) {
		// Return configuration defaults
		if ("remnant/items".equals(category) && "ossukage_sword".equals(file)) {
			return switch(key) {
				case "dash_timer" -> 100.0;
				case "dash_distance" -> 2.0;
				case "shuriken_timer" -> 50.0;
				case "shuriken_damage" -> 50.0;
				case "shuriken_knockback" -> 1.0;
				case "shuriken_pierce" -> 0.0;
				default -> 1.0;
			};
		} else if ("remnant/bosses".equals(category) && "ossukage".equals(file)) {
			return switch(key) {
				case "on_spawn_skeletons" -> 2.0;
				case "max_health_phase_1" -> 800.0;
				case "attack_damage_phase_1" -> 7.0;
				case "movement_speed_phase_1" -> 0.25;
				case "hp_threshold_phase_2" -> 50.0;
				case "transform_delay_phase_2" -> 60.0;
				case "skeletons_on_transform_phase_2" -> 5.0;
				case "health_boost_timer_phase_2" -> 200.0;
				case "attack_damage_phase_2" -> 9.0;
				case "movement_speed_phase_2" -> 0.3;
				case "skeletons_on_dash_phase_2" -> 2.0;
				case "special_attack_chance_phase_2" -> 20.0;
				default -> 1.0;
			};
		}
		return 1.0;
	}
	
	/**
	 * Provide sensible fallback defaults for string values.
	 */
	private static String getDefaultString(String category, String file, String key) {
		return "";
	}
}
