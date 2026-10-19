package com.nightbeam.remnants;

import com.nightbeam.remnants.config.ConfigMigration;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.config.WardenConfig;
import com.nightbeam.remnants.config.WatcherConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Upgrading a 2.x config folder (ConfigMigration): untouched 2.x defaults become the 3.0 defaults,
 * player edits stay, unrelated keys stay, and the config_version stamp makes the step run once per file.
 */
public final class ConfigMigrationRegression {
    private static final String BOSSES = "remnant/bosses", SOVEREIGN = ConfigMigration.SOVEREIGN,
            SUMMON = ConfigMigration.SOVEREIGN_SUMMON;

    public static void main(String[] args) { run(); }

    public static void run() {
        pairsMatchShippedDefaults();
        untouchedTwoXFilesUpgrade();
        playerEditsSurvive();
        runsOncePerFile();
        freshFilesOnlyGetStamped();
        kotsukageRitualSeedsTheWarden();
        umbrakarRitualSeedsTheWatcher();
        System.out.println("ConfigMigrationRegression: " + ConfigMigration.CHANGES.size()
                + " old->new default pairs; " + ConfigMigration.CARRIES.size() + " retired->successor ritual carries (Kotsukage->Warden, Umbrakar->Watcher); edits kept; stamped config_version "
                + ConfigMigration.VERSION);
    }

    /** Every new value is the shipped 3.0 default and every old value differs from it. */
    private static void pairsMatchShippedDefaults() {
        for (ConfigMigration.Change change : ConfigMigration.CHANGES) {
            require(!change.old().equals(change.current()), "pair does not change anything: " + change.key());
            Object shipped = change.file().equals(SOVEREIGN) ? SovereignConfig.DEFAULTS.get(change.key()) : SovereignConfig.RITUAL.get(change.key());
            require(shipped != null, "migrated key is not a 3.0 default: " + change.file() + "/" + change.key());
            if (shipped instanceof Double value) require(value == ((Number) change.current()).doubleValue(), "new value drifted from SovereignConfig: " + change.key());
            else require(shipped.equals(change.current()), "new value drifted from SovereignConfig: " + change.key());
        }
        for (String key : SovereignConfig.RITUAL.keySet()) {
            require(ConfigMigration.CHANGES.stream().anyMatch(c -> c.file().equals(SUMMON) && c.key().equals(key)), "ritual key not migrated: " + key);
        }
    }

    private static void untouchedTwoXFilesUpgrade() {
        MapStore store = twoX();
        List<String> migrated = ConfigMigration.migrate(store);
        require(migrated.size() == ConfigMigration.CHANGES.size(), "every untouched 2.x default migrates: " + migrated);
        require(store.number(SOVEREIGN, "max_health") == 720 && store.number(SOVEREIGN, "phase_two_percent") == 66, "boss numbers reach 3.0");
        require(store.number(SOVEREIGN, "xp_reward") == 300 && store.number(SOVEREIGN, "movement_speed") == 0.24, "xp and speed reach 3.0");
        require(store.string(SUMMON, "portal_activation_item").equals("remnant_bosses:sovereigns_seal"), "seal replaces heart of the sea");
        require(store.string(SUMMON, "pedestal_one_activation_block").equals("minecraft:gold_block")
                && store.string(SUMMON, "pedestal_two_activation_block").equals("minecraft:gold_block"), "gold blocks east and west");
        require(store.string(SUMMON, "pedestal_three_activation_block").equals("minecraft:soul_lantern")
                && store.string(SUMMON, "pedestal_four_activation_block").equals("minecraft:soul_lantern"), "soul lanterns south and north");
        require(store.number(SOVEREIGN, "beam_damage") == 24 && store.number(SOVEREIGN, "summoning_enabled") == 1
                && store.number(SOVEREIGN, "duplicate_radius") == 64, "unrelated and unchanged keys stay");
        require(ConfigMigration.version(store, BOSSES, SOVEREIGN) == 3 && ConfigMigration.version(store, BOSSES, SUMMON) == 3, "both files stamped");
    }

    private static void playerEditsSurvive() {
        MapStore store = twoX();
        store.put(SOVEREIGN, "max_health", 1200.0);
        store.put(SOVEREIGN, "phase_two_percent", 50.0);
        store.put(SUMMON, "portal_activation_item", "minecraft:nether_star");
        store.put(SUMMON, "pedestal_three_activation_block", "minecraft:diamond_block");
        store.put(SUMMON, "pedestal_one_activation_block", " Minecraft:Bone_Block ");
        List<String> migrated = ConfigMigration.migrate(store);
        require(store.number(SOVEREIGN, "max_health") == 1200 && store.number(SOVEREIGN, "phase_two_percent") == 50, "edited numbers stay");
        require(store.string(SUMMON, "portal_activation_item").equals("minecraft:nether_star")
                && store.string(SUMMON, "pedestal_three_activation_block").equals("minecraft:diamond_block"), "edited ritual stays");
        require(store.string(SUMMON, "pedestal_one_activation_block").equals("minecraft:gold_block"), "the game reads a case variant as the old default");
        require(store.number(SOVEREIGN, "armor") == 12 && store.string(SUMMON, "pedestal_four_activation_block").equals("minecraft:soul_lantern"),
                "untouched neighbours still migrate");
        require(!migrated.contains(SOVEREIGN + "/max_health") && !migrated.contains(SUMMON + "/portal_activation_item"), "edits are not reported");
    }

    private static void runsOncePerFile() {
        MapStore store = twoX();
        ConfigMigration.migrate(store);
        store.put(SOVEREIGN, "max_health", 950.0); // chosen after the upgrade: a deliberate value now
        store.put(SUMMON, "portal_activation_item", "minecraft:heart_of_the_sea");
        require(ConfigMigration.migrate(store).isEmpty(), "second run changes nothing");
        require(store.number(SOVEREIGN, "max_health") == 950 && store.string(SUMMON, "portal_activation_item").equals("minecraft:heart_of_the_sea"),
                "values set after the upgrade are kept");
        MapStore older = twoX();
        older.put(SOVEREIGN, ConfigMigration.MARKER, 2.0);
        older.put(SUMMON, ConfigMigration.MARKER, 3.0);
        List<String> migrated = ConfigMigration.migrate(older);
        require(older.number(SOVEREIGN, "max_health") == 720, "a lower stamp still migrates");
        require(older.string(SUMMON, "portal_activation_item").equals("minecraft:heart_of_the_sea") && migrated.stream().noneMatch(k -> k.startsWith(SUMMON + "/")),
                "a file already stamped 3 is left alone");
    }

    private static void freshFilesOnlyGetStamped() {
        MapStore store = new MapStore();
        require(ConfigMigration.migrate(store).isEmpty(), "nothing to migrate in new files");
        require(ConfigMigration.version(store, BOSSES, SOVEREIGN) == 3 && ConfigMigration.version(store, BOSSES, SUMMON) == 3, "new files stamped");
        require(!store.exists(BOSSES, SOVEREIGN, "max_health"), "the migration never invents keys; the bootstrap fills them");
    }

    /**
     * Player edits of the retired kotsukage_summon seed gravebound_warden_summon once; untouched
     * 2.x defaults (in any letter case) are not carried, so the Warden keeps its own 3.0 ritual.
     */
    private static void kotsukageRitualSeedsTheWarden() {
        String old = ConfigMigration.KOTSUKAGE_SUMMON, warden = ConfigMigration.WARDEN_SUMMON;
        require(warden.equals(WardenConfig.SUMMON), "carry target is the Warden's ritual file");
        for (ConfigMigration.Carry carry : ConfigMigration.CARRIES) {
            if (!carry.newFile().equals(warden)) continue;
            require(carry.oldFile().equals(old), "only Kotsukage's ritual seeds the Warden's: " + carry);
            require(WardenConfig.RITUAL.containsKey(carry.key()), "carried key is not a Warden ritual key: " + carry.key());
        }
        MapStore untouched = twoX();
        kotsukage(untouched);
        ConfigMigration.migrate(untouched);
        for (String key : WardenConfig.RITUAL.keySet()) require(!untouched.exists(BOSSES, warden, key), "untouched default carried: " + key);
        require(ConfigMigration.version(untouched, BOSSES, warden) == 3, "the Warden's ritual file is stamped");

        MapStore edited = twoX();
        kotsukage(edited);
        edited.put(old, "portal_activation_item", "minecraft:nether_star");
        edited.put(old, "pedestal_two_activation_block", "minecraft:crying_obsidian");
        edited.put(old, "pedestal_one_activation_block", " Minecraft:Bone_Block ");
        List<String> carried = ConfigMigration.migrate(edited);
        require(edited.string(warden, "portal_activation_item").equals("minecraft:nether_star")
                && edited.string(warden, "pedestal_two_activation_block").equals("minecraft:crying_obsidian"), "edits seed the Warden's ritual");
        require(!edited.exists(BOSSES, warden, "pedestal_one_activation_block") && !edited.exists(BOSSES, warden, "pedestal_three_activation_block"),
                "untouched neighbours are left for the bootstrap");
        require(carried.contains(old + "/portal_activation_item -> " + warden), "carry is reported: " + carried);
        require(edited.string(old, "portal_activation_item").equals("minecraft:nether_star"), "the retired file is never rewritten");
        edited.put(old, "portal_activation_item", "minecraft:diamond");
        require(ConfigMigration.migrate(edited).isEmpty() && edited.string(warden, "portal_activation_item").equals("minecraft:nether_star"),
                "the carry runs once");

        MapStore fresh = new MapStore();
        ConfigMigration.migrate(fresh);
        require(ConfigMigration.version(fresh, BOSSES, warden) == 3 && !fresh.exists(BOSSES, warden, "portal_activation_item"),
                "a world without Kotsukage's file only stamps the Warden's");
    }

    /**
     * Player edits of the retired umbrakar_summon seed forgotten_watcher_summon once; untouched 2.x
     * defaults are not carried, so the Watcher keeps its own ritual (memory lens; amethyst, calcite, chiseled
     * sandstone, lectern).
     */
    private static void umbrakarRitualSeedsTheWatcher() {
        String old = ConfigMigration.UMBRAKAR_SUMMON, watcher = ConfigMigration.WATCHER_SUMMON;
        require(watcher.equals(WatcherConfig.SUMMON), "carry target is the Watcher's ritual file");
        int carries = 0;
        for (ConfigMigration.Carry carry : ConfigMigration.CARRIES) {
            if (!carry.newFile().equals(watcher)) continue;
            carries++;
            require(carry.oldFile().equals(old), "only Umbrakar's ritual seeds the Watcher's: " + carry);
            require(WatcherConfig.RITUAL.containsKey(carry.key()), "carried key is not a Watcher ritual key: " + carry.key());
        }
        require(carries == WatcherConfig.RITUAL.size(), "every Watcher ritual key has a carry");
        MapStore untouched = twoX();
        umbrakar(untouched);
        ConfigMigration.migrate(untouched);
        for (String key : WatcherConfig.RITUAL.keySet()) require(!untouched.exists(BOSSES, watcher, key), "untouched default carried: " + key);
        require(ConfigMigration.version(untouched, BOSSES, watcher) == 3, "the Watcher's ritual file is stamped");

        MapStore edited = twoX();
        umbrakar(edited);
        edited.put(old, "portal_activation_item", "minecraft:nether_star");
        edited.put(old, "pedestal_four_activation_block", "minecraft:beacon");
        List<String> carried = ConfigMigration.migrate(edited);
        require(edited.string(watcher, "portal_activation_item").equals("minecraft:nether_star")
                && edited.string(watcher, "pedestal_four_activation_block").equals("minecraft:beacon"), "edits seed the Watcher's ritual");
        require(!edited.exists(BOSSES, watcher, "pedestal_one_activation_block"), "untouched neighbours are left for the bootstrap");
        require(carried.contains(old + "/portal_activation_item -> " + watcher), "carry is reported: " + carried);
        require(ConfigMigration.migrate(edited).isEmpty(), "the carry runs once");
    }

    /** Umbrakar's ritual exactly as every 2.x release wrote it. */
    private static void umbrakar(MapStore store) {
        String old = ConfigMigration.UMBRAKAR_SUMMON;
        store.put(old, "portal_activation_item", "minecraft:echo_shard");
        store.put(old, "pedestal_one_activation_block", "minecraft:amethyst_block");
        store.put(old, "pedestal_two_activation_block", "minecraft:crying_obsidian");
        store.put(old, "pedestal_three_activation_block", "minecraft:end_stone");
        store.put(old, "pedestal_four_activation_block", "minecraft:sculk");
    }

    /** Kotsukage's ritual exactly as every 2.x release wrote it. */
    private static void kotsukage(MapStore store) {
        String old = ConfigMigration.KOTSUKAGE_SUMMON;
        store.put(old, "portal_activation_item", "minecraft:wither_skeleton_skull");
        store.put(old, "pedestal_one_activation_block", "minecraft:bone_block");
        store.put(old, "pedestal_two_activation_block", "minecraft:soul_sand");
        store.put(old, "pedestal_three_activation_block", "minecraft:soul_soil");
        store.put(old, "pedestal_four_activation_block", "minecraft:nether_wart_block");
    }

    /** Exactly what 2.6.x wrote on first start (fixture config/remnant/bosses). */
    private static MapStore twoX() {
        MapStore store = new MapStore();
        Object[] numbers = {"max_health", 950.0, "attack_damage", 16.0, "armor", 8.0, "movement_speed", 0.25,
                "phase_two_percent", 45.0, "phase_two_damage_multiplier", 1.3, "attack_cooldown_ticks", 35.0,
                "slam_damage", 20.0, "beam_damage", 24.0, "eruption_damage", 18.0, "spell_range", 24.0,
                "wave_radius", 12.0, "summoning_enabled", 1.0, "duplicate_radius", 64.0, "xp_reward", 150.0};
        for (int i = 0; i < numbers.length; i += 2) store.put(SOVEREIGN, (String) numbers[i], numbers[i + 1]);
        store.put(SUMMON, "portal_activation_item", "minecraft:heart_of_the_sea");
        store.put(SUMMON, "pedestal_one_activation_block", "minecraft:bone_block");
        store.put(SUMMON, "pedestal_two_activation_block", "minecraft:bone_block");
        store.put(SUMMON, "pedestal_three_activation_block", "minecraft:skeleton_skull");
        store.put(SUMMON, "pedestal_four_activation_block", "minecraft:skeleton_skull");
        return store;
    }

    /** Jauml-like storage: missing numbers read as 0, as JaumlConfigLib.getNumberValue does. */
    private static final class MapStore implements ConfigMigration.Store {
        private final Map<String, Object> values = new HashMap<>();

        void put(String file, String key, Object value) { values.put(BOSSES + "/" + file + "/" + key, value); }
        double number(String file, String key) { return number(BOSSES, file, key); }
        String string(String file, String key) { return string(BOSSES, file, key); }

        @Override public boolean exists(String path, String file, String key) { return values.containsKey(path + "/" + file + "/" + key); }
        @Override public double number(String path, String file, String key) {
            return values.get(path + "/" + file + "/" + key) instanceof Number n ? n.doubleValue() : 0;
        }
        @Override public String string(String path, String file, String key) {
            Object value = values.get(path + "/" + file + "/" + key);
            return value == null ? null : value.toString();
        }
        @Override public void setNumber(String path, String file, String key, double value) { values.put(path + "/" + file + "/" + key, value); }
        @Override public void setString(String path, String file, String key, String value) { values.put(path + "/" + file + "/" + key, value); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
