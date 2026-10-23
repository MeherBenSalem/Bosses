package com.nightbeam.remnants;

import com.nightbeam.remnants.journal.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Boss Hunts keys follow the entity alias table on every load, with the same guard and no schema bump. */
public final class JournalMigrationRegression {
    private static final UUID PLAYER = UUID.fromString("37f381ae-a6e8-4ec2-91ad-e54353e8856e");
    private static final UUID OTHER = UUID.fromString("0f7a2c55-3a39-4f0e-9d0e-6a1c1f6f9c11");
    private static final int ENCOUNTERED = HuntState.ENCOUNTERED.ordinal(), DEFEATED = HuntState.DEFEATED.ordinal();
    // A later roster: old keys are no longer bosses, their successors are.
    private static final Predicate<Identifier> FUTURE = id -> Set.of("remnants:hollow_sovereign",
            "remnants:eclipsed_knight", "remnants:gravebound_warden", "remnants:forgotten_watcher").contains(id.toString());
    private static final Predicate<Identifier> LIVE = id -> BossHunt.fromId(id.toString()) != null;

    public static void main(String[] args) { run(); }
    public static void run() { liveRoster(); futureRoster(); loadPath(); System.out.println("Journal migration regressions passed"); }

    private static void liveRoster() {
        CompoundTag current = store();
        CompoundTag states = player(current, PLAYER);
        for (BossHunt boss : BossHunt.values()) states.putInt(boss.id(), boss.ordinal() % 2 == 0 ? DEFEATED : ENCOUNTERED);
        states.putInt("remnants:skeleton_melee", ENCOUNTERED); // alias source, but its successor is not a boss
        states.putString("othermod:future_boss", "retained");
        CompoundTag before = current.copy();
        check(!HuntSavedData.canonicaliseBossKeys(current, LIVE), "current roster needs no rename");
        check(current.equals(before), "current roster untouched");

        CompoundTag legacy = store();
        player(legacy, PLAYER).putInt("remnant_bosses:remnant_ossukage", DEFEATED);
        player(legacy, PLAYER).putInt(BossHunt.OSSUKAGE.id(), ENCOUNTERED);
        check(HuntSavedData.canonicaliseBossKeys(legacy, LIVE), "pre-2.4.1 key renamed today");
        check(player(legacy, PLAYER).getIntOr(BossHunt.OSSUKAGE.id(), 0) == DEFEATED, "legacy collision keeps furthest state");
        check(!player(legacy, PLAYER).contains("remnant_bosses:remnant_ossukage"), "legacy key removed");

        // Umbrakar's key (and the pre-2.4.1 spelling) lands on the Forgotten Watcher.
        CompoundTag umbrakar = store();
        player(umbrakar, PLAYER).putInt("remnants:umbrakar", ENCOUNTERED);
        player(umbrakar, OTHER).putInt("remnant_bosses:umbrakar", DEFEATED);
        check(HuntSavedData.canonicaliseBossKeys(umbrakar, LIVE), "retired Umbrakar key renamed today");
        check(player(umbrakar, PLAYER).getIntOr(BossHunt.FORGOTTEN_WATCHER.id(), 0) == ENCOUNTERED
                && !player(umbrakar, PLAYER).contains("remnants:umbrakar"), "Umbrakar progress moves to the Forgotten Watcher");
        check(player(umbrakar, OTHER).getIntOr(BossHunt.FORGOTTEN_WATCHER.id(), 0) == DEFEATED, "pre-2.4.1 Umbrakar key chains to the Watcher");
    }

    private static void futureRoster() {
        CompoundTag records = store();
        CompoundTag a = player(records, PLAYER);
        a.putInt("remnants:ossukage", DEFEATED);
        a.putInt("remnants:eclipsed_knight", ENCOUNTERED);
        a.putInt("remnant_bosses:remnant_ossukage", ENCOUNTERED);
        a.putInt("remnants:kotsukage", ENCOUNTERED);
        a.putInt("remnants:umbrakar", 999999);
        a.putInt("remnants:hollow_sovereign", DEFEATED);
        a.putInt("remnants:rat", DEFEATED); // successor catacomb_rat is not a boss
        a.putInt("remnants:kunai", DEFEATED); // retired helper: never aliased
        a.putString("othermod:future_boss", "retained");
        CompoundTag b = player(records, OTHER);
        b.putInt("remnants:ossukage", ENCOUNTERED);
        b.putInt("remnants:eclipsed_knight", DEFEATED);
        b.putInt("remnants:kotsukage", DEFEATED);
        b.putString("remnants:gravebound_warden", "corrupt");
        records.getCompoundOrEmpty("Players").putString("not-a-player", "kept");

        check(HuntSavedData.canonicaliseBossKeys(records, FUTURE), "retired boss keys renamed");
        a = player(records, PLAYER);
        check(a.getIntOr("remnants:eclipsed_knight", 0) == DEFEATED, "collision keeps furthest state (old ahead)");
        check(!a.contains("remnants:ossukage") && !a.contains("remnant_bosses:remnant_ossukage"), "chain sources removed");
        check(a.getIntOr("remnants:gravebound_warden", 0) == ENCOUNTERED && !a.contains("remnants:kotsukage"), "plain rename");
        check(a.getIntOr("remnants:forgotten_watcher", 0) == 999999, "rename without collision keeps the raw value");
        check(a.getIntOr("remnants:hollow_sovereign", 0) == DEFEATED, "kept boss untouched");
        check(a.getIntOr("remnants:rat", 0) == DEFEATED && !a.contains("remnants:catacomb_rat"), "non-boss successor not renamed");
        check(a.getIntOr("remnants:kunai", 0) == DEFEATED, "retired helper untouched");
        check(a.getStringOr("othermod:future_boss", "").equals("retained"), "unknown key preserved");
        b = player(records, OTHER);
        check(b.getIntOr("remnants:eclipsed_knight", 0) == DEFEATED && !b.contains("remnants:ossukage"), "collision keeps furthest state (new ahead)");
        check(b.getIntOr("remnants:kotsukage", 0) == DEFEATED && b.getStringOr("remnants:gravebound_warden", "").equals("corrupt"),
                "malformed target leaves both keys untouched");
        check(records.getCompoundOrEmpty("Players").getStringOr("not-a-player", "").equals("kept"), "malformed player preserved");
        check(records.getIntOr("SchemaVersion", 0) == HuntSavedData.SCHEMA_VERSION && HuntSavedData.SCHEMA_VERSION == 1, "schema stays 1");
        CompoundTag once = records.copy();
        check(!HuntSavedData.canonicaliseBossKeys(records, FUTURE) && records.equals(once), "canonicalisation is idempotent");
    }

    private static void loadPath() {
        CompoundTag saved = store();
        player(saved, PLAYER).putInt("remnant_bosses:remnant_ossukage", DEFEATED);
        HuntSavedData loaded = HuntSavedData.load(saved);
        check(loaded.state(PLAYER, BossHunt.OSSUKAGE) == HuntState.DEFEATED, "load canonicalises legacy key");
        check(loaded.isDirty(), "renamed store is saved");
        check(loaded.snapshot().getIntOr("SchemaVersion", 0) == 1, "load keeps schema 1");
        check(player(saved, PLAYER).contains("remnant_bosses:remnant_ossukage"), "caller's tag not mutated");

        CompoundTag clean = store();
        player(clean, PLAYER).putInt(BossHunt.GRAVEBOUND_WARDEN.id(), ENCOUNTERED);
        check(!HuntSavedData.load(clean).isDirty(), "nothing to rename leaves store clean");

        CompoundTag future = saved.copy();
        future.putInt("SchemaVersion", 999);
        HuntSavedData readOnly = HuntSavedData.load(future);
        check(readOnly.snapshot().equals(future) && !readOnly.isDirty(), "unknown schema is not canonicalised");
    }

    private static CompoundTag store() {
        CompoundTag records = new CompoundTag();
        records.putInt("SchemaVersion", HuntSavedData.SCHEMA_VERSION);
        records.put("Players", new CompoundTag());
        return records;
    }
    private static CompoundTag player(CompoundTag records, UUID id) {
        CompoundTag players = records.getCompoundOrEmpty("Players");
        if (!players.contains(id.toString())) players.put(id.toString(), new CompoundTag());
        return players.getCompoundOrEmpty(id.toString());
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
