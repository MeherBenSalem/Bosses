package com.nightbeam.remnants.journal;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Personal progression in the overworld; unknown records survive read/write unchanged. */
public final class HuntSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    private final CompoundTag records;
    private final boolean readOnly;
    final EncounterCredits encounters = new EncounterCredits();
    final JournalRequests requests = new JournalRequests();

    public HuntSavedData() {
        records = new CompoundTag();
        records.putInt("SchemaVersion", SCHEMA_VERSION);
        records.put("Players", new CompoundTag());
        readOnly = false;
    }
    private HuntSavedData(CompoundTag tag) {
        records = tag.copy();
        // Missing/invalid/unknown schema is not ours to migrate or overwrite.
        readOnly = !isInt(tag, "SchemaVersion") || tag.getIntOr("SchemaVersion", 0) != SCHEMA_VERSION;
        if (!readOnly && canonicaliseBossKeys(records, KNOWN_BOSS)) setDirty();
    }
    public static HuntSavedData load(CompoundTag tag) { return new HuntSavedData(tag); }
    public HuntState state(UUID player, BossHunt boss) {
        CompoundTag states = records.getCompoundOrEmpty("Players").getCompoundOrEmpty(player.toString());
        if (!isInt(states, boss.id())) return HuntState.UNDISCOVERED;
        int value = states.getIntOr(boss.id(), 0);
        return value >= 0 && value < HuntState.values().length ? HuntState.values()[value] : HuntState.UNDISCOVERED;
    }
    public boolean advance(UUID player, BossHunt boss, HuntState next) {
        if (readOnly || next.ordinal() <= state(player, boss).ordinal()) return false;
        // Preserve malformed/unknown containers too instead of destroying their original contents.
        if (records.contains("Players") && !isCompound(records, "Players")) return false;
        CompoundTag players = records.getCompoundOrEmpty("Players");
        String id = player.toString();
        if (players.contains(id) && !isCompound(players, id)) return false;
        CompoundTag states = players.getCompoundOrEmpty(id);
        states.putInt(boss.id(), next.ordinal());
        players.put(id, states);
        records.put("Players", players);
        setDirty();
        return true;
    }
    public CompoundTag snapshot() { return records.copy(); }
    private static final Predicate<Identifier> KNOWN_BOSS = id -> BossHunt.fromId(id.toString()) != null;
    /**
     * Renames retired boss keys through the entity alias table. Runs on every schema-1 load and is
     * idempotent. A key moves only when it is not a known boss and its resolved successor is; when
     * both keys exist the furthest-advanced state wins. Non-integer values and unknown keys stay put.
     * The schema stays 1, so older builds keep reading and writing the store (they preserve keys
     * they do not know).
     */
    public static boolean canonicaliseBossKeys(CompoundTag records, Predicate<Identifier> knownBoss) {
        if (!isCompound(records, "Players")) return false;
        CompoundTag players = records.getCompoundOrEmpty("Players");
        boolean changed = false;
        for (String player : players.keySet()) {
            if (!isCompound(players, player)) continue;
            CompoundTag states = players.getCompoundOrEmpty(player);
            for (String key : List.copyOf(states.keySet())) {
                if (!isInt(states, key)) continue;
                Identifier target = RegistryAliases.resolveEntity(Identifier.tryParse(key), knownBoss).orElse(null);
                if (target == null) continue;
                String renamed = target.toString();
                if (states.contains(renamed) && !isInt(states, renamed)) continue;
                int value = states.getIntOr(key, 0);
                if (isInt(states, renamed)) value = Math.max(bounded(value), bounded(states.getIntOr(renamed, 0)));
                states.remove(key);
                states.putInt(renamed, value);
                changed = true;
            }
        }
        return changed;
    }
    private static int bounded(int state) { return state >= 0 && state < HuntState.values().length ? state : HuntState.UNDISCOVERED.ordinal(); }
    private static boolean isInt(CompoundTag tag, String key) { return tag.get(key) instanceof IntTag; }
    private static boolean isCompound(CompoundTag tag, String key) { return tag.get(key) instanceof CompoundTag; }
    /** The whole record tree is the saved form, so keys this build does not know survive a save. */
    public static final Codec<HuntSavedData> CODEC = CompoundTag.CODEC.xmap(HuntSavedData::load, HuntSavedData::snapshot);
}
