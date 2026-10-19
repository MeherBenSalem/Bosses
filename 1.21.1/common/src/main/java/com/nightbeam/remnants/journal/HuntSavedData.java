package com.nightbeam.remnants.journal;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
        readOnly = !tag.contains("SchemaVersion", Tag.TAG_INT) || tag.getInt("SchemaVersion") != SCHEMA_VERSION;
        if (!readOnly && canonicaliseBossKeys(records, KNOWN_BOSS)) setDirty();
    }
    public static HuntSavedData load(CompoundTag tag) { return new HuntSavedData(tag); }
    public HuntState state(UUID player, BossHunt boss) {
        CompoundTag states = records.getCompound("Players").getCompound(player.toString());
        if (!states.contains(boss.id(), Tag.TAG_INT)) return HuntState.UNDISCOVERED;
        int value = states.getInt(boss.id());
        return value >= 0 && value < HuntState.values().length ? HuntState.values()[value] : HuntState.UNDISCOVERED;
    }
    public boolean advance(UUID player, BossHunt boss, HuntState next) {
        if (readOnly || next.ordinal() <= state(player, boss).ordinal()) return false;
        // Preserve malformed/unknown containers too instead of destroying their original contents.
        if (records.contains("Players") && !records.contains("Players", Tag.TAG_COMPOUND)) return false;
        CompoundTag players = records.getCompound("Players");
        String id = player.toString();
        if (players.contains(id) && !players.contains(id, Tag.TAG_COMPOUND)) return false;
        CompoundTag states = players.getCompound(id);
        states.putInt(boss.id(), next.ordinal());
        players.put(id, states);
        records.put("Players", players);
        setDirty();
        return true;
    }
    public CompoundTag snapshot() { return records.copy(); }
    private static final Predicate<ResourceLocation> KNOWN_BOSS = id -> BossHunt.fromId(id.toString()) != null;
    /**
     * Renames retired boss keys through the entity alias table. Runs on every schema-1 load and is
     * idempotent. A key moves only when it is not a known boss and its resolved successor is; when
     * both keys exist the furthest-advanced state wins. Non-integer values and unknown keys stay put.
     * The schema stays 1, so older builds keep reading and writing the store (they preserve keys
     * they do not know).
     */
    public static boolean canonicaliseBossKeys(CompoundTag records, Predicate<ResourceLocation> knownBoss) {
        if (!records.contains("Players", Tag.TAG_COMPOUND)) return false;
        CompoundTag players = records.getCompound("Players");
        boolean changed = false;
        for (String player : players.getAllKeys()) {
            if (!players.contains(player, Tag.TAG_COMPOUND)) continue;
            CompoundTag states = players.getCompound(player);
            for (String key : List.copyOf(states.getAllKeys())) {
                if (!states.contains(key, Tag.TAG_INT)) continue;
                ResourceLocation target = RegistryAliases.resolveEntity(ResourceLocation.tryParse(key), knownBoss).orElse(null);
                if (target == null) continue;
                String renamed = target.toString();
                if (states.contains(renamed) && !states.contains(renamed, Tag.TAG_INT)) continue;
                int value = states.getInt(key);
                if (states.contains(renamed, Tag.TAG_INT)) value = Math.max(bounded(value), bounded(states.getInt(renamed)));
                states.remove(key);
                states.putInt(renamed, value);
                changed = true;
            }
        }
        return changed;
    }
    private static int bounded(int state) { return state >= 0 && state < HuntState.values().length ? state : HuntState.UNDISCOVERED.ordinal(); }
    public static HuntSavedData load(CompoundTag tag, HolderLookup.Provider registries) { return load(tag); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { return tag.merge(snapshot()); }
}
