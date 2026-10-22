package com.nightbeam.remnants.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-time "replace untouched old defaults" step for config files that 3.0 gave new defaults.
 *
 * <p>The bootstrap only writes missing keys, so a 2.x world keeps every value its files already hold,
 * including the 2.x defaults the player never touched (for example the Hollow Sovereign's 950 health and
 * the heart-of-the-sea ritual). For each listed file whose {@value #MARKER} is below {@value #VERSION},
 * every listed key that still holds exactly its 2.x shipped default is rewritten to the 3.0 default;
 * any other value (a player edit) stays. The file then gets {@code config_version: 3}, so the step runs
 * once per file: a file created fresh by 3.0 is stamped without changes, and a deleted file is simply
 * regenerated with 3.0 defaults. A 2.x value that was set on purpose but equals the old default cannot
 * be told apart from an untouched one and is migrated too.
 *
 * <p>{@link #CARRIES} covers retired 2.x files: a retired file (kotsukage_summon) seeds its 3.0 successor
 * (gravebound_warden_summon) once, the first time the successor is seen without a stamp. A key whose old
 * value is still the untouched 2.x default is not carried (the successor gets its own 3.0 default from the
 * bootstrap); a player edit is copied over. The retired file itself is never written or deleted and is not
 * read again afterwards, so a downgrade still finds it intact.
 *
 * <p>Plain Java on purpose: {@code ConfigMigrationRegression} drives it with an in-memory store.
 */
public final class ConfigMigration {
	public static final int VERSION = 3;
	public static final String MARKER = "config_version";
	public static final String SOVEREIGN = "hollow_sovereign", SOVEREIGN_SUMMON = "hollow_sovereign_summon";
	public static final String KOTSUKAGE_SUMMON = "kotsukage_summon", WARDEN_SUMMON = "gravebound_warden_summon";
	public static final String UMBRAKAR_SUMMON = "umbrakar_summon", WATCHER_SUMMON = "forgotten_watcher_summon";
	private static final String BOSSES = "remnant/bosses";

	/** One file's key: the default 2.x shipped (2.6.x, the only 2.x release with these files) and the 3.0 one. */
	public record Change(String path, String file, String key, Object old, Object current) {
		boolean numeric() {
			return old instanceof Number;
		}
	}

	/**
	 * Old to new default pairs. Keys whose default did not change (summoning_enabled 1, duplicate_radius 64)
	 * and 2.x keys the Sovereign no longer reads (attack_damage, phase_two_damage_multiplier, beam_damage,
	 * eruption_damage, spell_range, wave_radius) are left alone; keys a 2.x file lacks are added by the
	 * bootstrap.
	 */
	public static final List<Change> CHANGES = List.of(
			// remnant/bosses/hollow_sovereign.json
			number(SOVEREIGN, "max_health", 950, 720),
			number(SOVEREIGN, "armor", 8, 12),
			number(SOVEREIGN, "movement_speed", 0.25, 0.24),
			number(SOVEREIGN, "phase_two_percent", 45, 66),
			number(SOVEREIGN, "attack_cooldown_ticks", 35, 30),
			number(SOVEREIGN, "slam_damage", 20, 14),
			number(SOVEREIGN, "xp_reward", 150, 300),
			// remnant/bosses/hollow_sovereign_summon.json: offerings are east, west, south, north.
			string(SOVEREIGN_SUMMON, "portal_activation_item", "minecraft:heart_of_the_sea", "remnant_bosses:sovereigns_seal"),
			string(SOVEREIGN_SUMMON, "pedestal_one_activation_block", "minecraft:bone_block", "minecraft:gold_block"),
			string(SOVEREIGN_SUMMON, "pedestal_two_activation_block", "minecraft:bone_block", "minecraft:gold_block"),
			string(SOVEREIGN_SUMMON, "pedestal_three_activation_block", "minecraft:skeleton_skull", "minecraft:soul_lantern"),
			string(SOVEREIGN_SUMMON, "pedestal_four_activation_block", "minecraft:skeleton_skull", "minecraft:soul_lantern"));

	/**
	 * A key of a retired file whose player edit moves to the successor file. {@code old} is the value 2.x
	 * shipped; an unreadable or untouched old value is never carried.
	 */
	public record Carry(String path, String oldFile, String newFile, String key, Object old) {
		boolean numeric() {
			return old instanceof Number;
		}
	}

	/**
	 * Kotsukage's ritual (2.x defaults, written by every 2.x release) seeds the Warden's, and Umbrakar's seeds
	 * the Forgotten Watcher's. The retired bosses' numbers (kotsukage.json, umbrakar.json) describe retired
	 * kits and are not carried. The 2.x rat and wraith spawn settings (rat_spawns, wraith_spawns) have no
	 * successor: Remnants creatures spawn naturally only in the Eclipsed Realm.
	 */
	public static final List<Carry> CARRIES = List.of(
			carry(BOSSES, KOTSUKAGE_SUMMON, WARDEN_SUMMON, "portal_activation_item", "minecraft:wither_skeleton_skull"),
			carry(BOSSES, KOTSUKAGE_SUMMON, WARDEN_SUMMON, "pedestal_one_activation_block", "minecraft:bone_block"),
			carry(BOSSES, KOTSUKAGE_SUMMON, WARDEN_SUMMON, "pedestal_two_activation_block", "minecraft:soul_sand"),
			carry(BOSSES, KOTSUKAGE_SUMMON, WARDEN_SUMMON, "pedestal_three_activation_block", "minecraft:soul_soil"),
			carry(BOSSES, KOTSUKAGE_SUMMON, WARDEN_SUMMON, "pedestal_four_activation_block", "minecraft:nether_wart_block"),
			carry(BOSSES, UMBRAKAR_SUMMON, WATCHER_SUMMON, "portal_activation_item", "minecraft:echo_shard"),
			carry(BOSSES, UMBRAKAR_SUMMON, WATCHER_SUMMON, "pedestal_one_activation_block", "minecraft:amethyst_block"),
			carry(BOSSES, UMBRAKAR_SUMMON, WATCHER_SUMMON, "pedestal_two_activation_block", "minecraft:crying_obsidian"),
			carry(BOSSES, UMBRAKAR_SUMMON, WATCHER_SUMMON, "pedestal_three_activation_block", "minecraft:end_stone"),
			carry(BOSSES, UMBRAKAR_SUMMON, WATCHER_SUMMON, "pedestal_four_activation_block", "minecraft:sculk"));

	/** The config storage the migration reads and writes (Jauml in game, a map in the regression). */
	public interface Store {
		boolean exists(String path, String file, String key);

		double number(String path, String file, String key);

		String string(String path, String file, String key);

		void setNumber(String path, String file, String key, double value);

		void setString(String path, String file, String key, String value);

		/** Writes the {@value #MARKER} stamp; defaults to a number. */
		default void setVersion(String path, String file, int version) {
			setNumber(path, file, MARKER, version);
		}
	}

	private ConfigMigration() {
	}

	private static Change number(String file, String key, double old, double current) {
		return new Change(BOSSES, file, key, old, current);
	}

	private static Change string(String file, String key, String old, String current) {
		return new Change(BOSSES, file, key, old, current);
	}

	private static Carry carry(String path, String oldFile, String newFile, String key, Object old) {
		return new Carry(path, oldFile, newFile, key, old);
	}

	/**
	 * Runs the step for every file below {@link #VERSION} and stamps it. Call it after the files exist and
	 * before missing keys are filled. Returns the rewritten keys as {@code file/key}, for the log.
	 */
	public static List<String> migrate(Store store) {
		Map<String, List<Change>> byFile = new LinkedHashMap<>();
		for (Change change : CHANGES) byFile.computeIfAbsent(change.path() + "/" + change.file(), f -> new ArrayList<>()).add(change);
		List<String> migrated = new ArrayList<>();
		for (List<Change> changes : byFile.values()) {
			String path = changes.get(0).path(), file = changes.get(0).file();
			if (version(store, path, file) >= VERSION) continue;
			for (Change change : changes) {
				if (!store.exists(path, file, change.key())) continue;
				if (change.numeric()) {
					double value = store.number(path, file, change.key());
					if (sameNumber(value, ((Number) change.old()).doubleValue())) {
						store.setNumber(path, file, change.key(), ((Number) change.current()).doubleValue());
						migrated.add(file + "/" + change.key());
					}
				} else if (sameId(store.string(path, file, change.key()), (String) change.old())) {
					store.setString(path, file, change.key(), (String) change.current());
					migrated.add(file + "/" + change.key());
				}
			}
			store.setVersion(path, file, VERSION);
		}
		migrated.addAll(carry(store));
		return migrated;
	}

	/**
	 * Copies player edits of retired files into unstamped successors, then stamps them. Returns the carried
	 * keys as {@code old/key -> new}. Call it before the bootstrap fills the successor's missing keys.
	 */
	public static List<String> carry(Store store) {
		Map<String, List<Carry>> byFile = new LinkedHashMap<>();
		for (Carry carry : CARRIES) byFile.computeIfAbsent(carry.path() + "/" + carry.newFile(), f -> new ArrayList<>()).add(carry);
		List<String> carried = new ArrayList<>();
		for (List<Carry> carries : byFile.values()) {
			String path = carries.get(0).path(), file = carries.get(0).newFile();
			if (version(store, path, file) >= VERSION) continue;
			for (Carry carry : carries) {
				if (!store.exists(path, carry.oldFile(), carry.key())) continue;
				String raw = store.string(path, carry.oldFile(), carry.key());
				if (carry.numeric()) {
					double number = store.number(path, carry.oldFile(), carry.key());
					// Numeric text counts as its number; any other text is unreadable and treated as untouched.
					if (raw != null && isNumber(raw)) number = Double.parseDouble(raw.trim());
					else if (raw != null && !raw.isBlank()) continue;
					if (!Double.isFinite(number) || sameNumber(number, ((Number) carry.old()).doubleValue())) continue;
					store.setNumber(path, file, carry.key(), number);
				} else if (raw == null) {
					// Jauml returns no text for a number: a number where 2.x wrote text is an edit, carried as is.
					double number = store.number(path, carry.oldFile(), carry.key());
					if (!Double.isFinite(number)) continue;
					store.setNumber(path, file, carry.key(), number);
				} else {
					if (sameText(raw, (String) carry.old())) continue;
					store.setString(path, file, carry.key(), raw.trim());
				}
				carried.add(carry.oldFile() + "/" + carry.key() + " -> " + file);
			}
			store.setVersion(path, file, VERSION);
		}
		return carried;
	}

	private static boolean isNumber(String raw) {
		try {
			Double.parseDouble(raw.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/** Text and ID values compare trimmed and case-insensitively, as the game reads them. */
	private static boolean sameText(String value, String old) {
		return value.trim().toLowerCase(Locale.ROOT).equals(old.toLowerCase(Locale.ROOT));
	}

	/** The file's stamp; 0 when it has none (every 2.x file). */
	public static int version(Store store, String path, String file) {
		if (!store.exists(path, file, MARKER)) return 0;
		double value = store.number(path, file, MARKER);
		return Double.isFinite(value) ? (int) Math.floor(value) : 0;
	}

	private static boolean sameNumber(double value, double old) {
		return Double.isFinite(value) && Math.abs(value - old) <= 1.0E-9 * Math.max(1, Math.abs(old));
	}

	/** Registry IDs compare as the game reads them (RitualValues lower-cases them). */
	private static boolean sameId(String value, String old) {
		return value != null && value.trim().toLowerCase(Locale.ROOT).equals(old);
	}
}
