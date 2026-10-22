package com.nightbeam.remnants.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public final class JaumlConfigBootstrap {
	private static final Logger LOGGER = LogManager.getLogger(JaumlConfigBootstrap.class);

	private JaumlConfigBootstrap() {
	}

	public static void initConfigs() {
		JaumlApi api = JaumlApi.load();
		if (api == null) {
			LOGGER.warn("JaumlConfigLib not present; skipping config bootstrap.");
			return;
		}

		BoneMonsterConfig.DEFAULTS.forEach((file,defaults) -> {
            api.createConfigFile("remnant/monsters",file);
            defaults.forEach((key,value) -> {
                if(!api.arrayKeyExists("remnant/monsters",file,key)) api.setNumberValue("remnant/monsters",file,key,value);
            });
        });
        api.createConfigFile("remnant/bosses", "hollow_sovereign");
        api.createConfigFile("remnant/bosses", "hollow_sovereign_summon");
        api.createConfigFile("remnant/bosses", WardenConfig.FILE);
        api.createConfigFile("remnant/bosses", WardenConfig.SUMMON);
        api.createConfigFile("remnant/bosses", WatcherConfig.FILE);
        api.createConfigFile("remnant/bosses", WatcherConfig.SUMMON);
        // Rewrite values still at their 2.x default once per file, then add new keys.
        ConfigMigration.Store store = api.store();
        if (store == null) {
            LOGGER.warn("JaumlConfigLib cannot read values; 2.x defaults were not updated or carried over.");
        } else {
            java.util.List<String> migrated = ConfigMigration.migrate(store);
            if (!migrated.isEmpty()) LOGGER.info("Updated untouched 2.x config defaults to 3.0: {}", String.join(", ", migrated));
        }
        SovereignConfig.DEFAULTS.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", "hollow_sovereign", key)) api.setNumberValue("remnant/bosses", "hollow_sovereign", key, value);
        });
        SovereignConfig.RITUAL.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", "hollow_sovereign_summon", key)) api.setStringValue("remnant/bosses", "hollow_sovereign_summon", key, value);
        });
        // The 2.x kotsukage, kotsukage_summon, rat_spawns, rat_stats and skeleton_minion_stats files are no
        // longer written; existing copies stay on disk.
        WardenConfig.DEFAULTS.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", WardenConfig.FILE, key)) api.setNumberValue("remnant/bosses", WardenConfig.FILE, key, value);
        });
        WardenConfig.RITUAL.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", WardenConfig.SUMMON, key)) api.setStringValue("remnant/bosses", WardenConfig.SUMMON, key, value);
        });
        // The 2.x umbrakar, umbrakar_summon, wraith_spawns and wraith_stats files are no longer written;
        // existing copies stay on disk.
        WatcherConfig.DEFAULTS.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", WatcherConfig.FILE, key)) api.setNumberValue("remnant/bosses", WatcherConfig.FILE, key, value);
        });
        WatcherConfig.RITUAL.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", WatcherConfig.SUMMON, key)) api.setStringValue("remnant/bosses", WatcherConfig.SUMMON, key, value);
        });
        api.createConfigFile("remnant/client", "presentation");
        for (String key : new String[]{"custom_boss_bars", "vfx_density"}) {
            if (!api.arrayKeyExists("remnant/client", "presentation", key)) api.setNumberValue("remnant/client", "presentation", key, 1);
        }
        for (String key : RealmConfig.CLIENT_SWITCHES) {
            if (!api.arrayKeyExists("remnant/client", "presentation", key)) api.setNumberValue("remnant/client", "presentation", key, 1);
        }
        api.createConfigFile("remnant/realm", "realm");
        RealmConfig.DEFAULTS.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/realm", "realm", key)) api.setNumberValue("remnant/realm", "realm", key, value);
        });
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
	}

	private static final class JaumlApi {
		private final Method createConfigFile;
		private final Method arrayKeyExists;
		private final Method setStringValue;
		private final Method setNumberValue;
		private final Class<?> numberParamType;
		/** Optional (ConfigMigration): absent getters disable the default migration, not the bootstrap. */
		private final Method getNumberValue, getStringValue, setIntValue;

		private JaumlApi(Method createConfigFile, Method arrayKeyExists, Method setStringValue, Method setNumberValue,
				Class<?> numberParamType, Method getNumberValue, Method getStringValue, Method setIntValue) {
			this.createConfigFile = createConfigFile;
			this.arrayKeyExists = arrayKeyExists;
			this.setStringValue = setStringValue;
			this.setNumberValue = setNumberValue;
			this.numberParamType = numberParamType;
			this.getNumberValue = getNumberValue;
			this.getStringValue = getStringValue;
			this.setIntValue = setIntValue;
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
						setNumberValue.getParameterTypes()[3], optional(clazz, "getNumberValue"),
						optional(clazz, "getStringValue"), optional(clazz, "setIntValue", int.class));
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

		private static Method optional(Class<?> clazz, String name, Class<?>... extra) {
			Class<?>[] parameters = new Class<?>[3 + extra.length];
			java.util.Arrays.fill(parameters, 0, 3, String.class);
			System.arraycopy(extra, 0, parameters, 3, extra.length);
			try {
				return clazz.getMethod(name, parameters);
			} catch (NoSuchMethodException e) {
				return null;
			}
		}

		/** Read/write view for {@link ConfigMigration}; null when this Jauml build cannot read values. */
		ConfigMigration.Store store() {
			if (getNumberValue == null || getStringValue == null) return null;
			return new ConfigMigration.Store() {
				@Override
				public boolean exists(String path, String file, String key) {
					return arrayKeyExists(path, file, key);
				}

				@Override
				public double number(String path, String file, String key) {
					try {
						return getNumberValue.invoke(null, path, file, key) instanceof Number n ? n.doubleValue() : Double.NaN;
					} catch (ReflectiveOperationException e) {
						LOGGER.warn("JaumlConfigLib.getNumberValue failed for {}/{}/{}", path, file, key, e);
						return Double.NaN;
					}
				}

				@Override
				public String string(String path, String file, String key) {
					try {
						return getStringValue.invoke(null, path, file, key) instanceof String value ? value : null;
					} catch (ReflectiveOperationException e) {
						LOGGER.warn("JaumlConfigLib.getStringValue failed for {}/{}/{}", path, file, key, e);
						return null;
					}
				}

				@Override
				public void setNumber(String path, String file, String key, double value) {
					setNumberValue(path, file, key, value);
				}

				@Override
				public void setString(String path, String file, String key, String value) {
					setStringValue(path, file, key, value);
				}

				@Override
				public void setVersion(String path, String file, int version) {
					if (setIntValue == null) {
						setNumberValue(path, file, ConfigMigration.MARKER, version);
						return;
					}
					try {
						setIntValue.invoke(null, path, file, ConfigMigration.MARKER, version);
					} catch (ReflectiveOperationException e) {
						LOGGER.warn("JaumlConfigLib.setIntValue failed for {}/{}", path, file, e);
					}
				}
			};
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
