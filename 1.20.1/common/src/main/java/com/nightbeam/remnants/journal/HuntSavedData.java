package com.nightbeam.remnants.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

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
    @Override public CompoundTag save(CompoundTag tag) { return tag.merge(snapshot()); }
}
