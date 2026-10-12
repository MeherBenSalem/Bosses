package com.nightbeam.remnants;

import com.nightbeam.remnants.journal.*;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Standalone regressions using Minecraft's actual NBT and production policies. */
public final class JournalRegression {
    private static final UUID PLAYER = UUID.fromString("37f381ae-a6e8-4ec2-91ad-e54353e8856e");


    public static void main(String[] args) {
        persistence();
        JournalPolicyRegression.run();
        JournalDamageRegression.run();
        RitualRegression.run();
        JournalLayoutRegression.run();
        JournalPacketRegression.run();
        System.out.println("Journal regressions passed");
    }
    private static void persistence() {
        HuntSavedData data = new HuntSavedData();
        check(data.state(PLAYER, BossHunt.OSSUKAGE) == HuntState.UNDISCOVERED, "new player starts unknown");
        for (BossHunt boss : BossHunt.values()) {
            check(BossHunt.fromId(boss.id()) == boss, "canonical boss ID");
            check(data.advance(PLAYER, boss, boss.ordinal() % 2 == 0 ? HuntState.DEFEATED : HuntState.ENCOUNTERED), "state advances");
        }
        data.setDirty(false);
        check(!data.advance(PLAYER, BossHunt.OSSUKAGE, HuntState.ENCOUNTERED), "no state regression");
        check(!data.advance(PLAYER, BossHunt.OSSUKAGE, HuntState.DEFEATED), "duplicate defeat idempotent");
        check(!data.isDirty(), "duplicate state does not dirty save");
        CompoundTag saved = data.save(new CompoundTag(), null);
        CompoundTag players = saved.getCompound("Players");
        players.getCompound(PLAYER.toString()).putString("othermod:future_boss", "retained");
        saved.putString("UnknownMetadata", "keep");
        HuntSavedData loaded = HuntSavedData.load(saved);
        for (BossHunt boss : BossHunt.values()) check(loaded.state(PLAYER, boss) == (boss.ordinal() % 2 == 0 ? HuntState.DEFEATED : HuntState.ENCOUNTERED), "NBT round-trip expected state");
        check(loaded.snapshot().getString("UnknownMetadata").equals("keep"), "unknown root metadata");
        check(loaded.snapshot().getCompound("Players").getCompound(PLAYER.toString()).getString("othermod:future_boss").equals("retained"), "unknown boss preserved");
        saved.getCompound("Players").getCompound(PLAYER.toString()).putInt(BossHunt.OSSUKAGE.id(), 999999);
        check(HuntSavedData.load(saved).state(PLAYER, BossHunt.OSSUKAGE) == HuntState.UNDISCOVERED, "invalid state bounded");
        saved.putInt("SchemaVersion", 999);
        HuntSavedData future = HuntSavedData.load(saved);
        check(!future.advance(PLAYER, BossHunt.KOTSUKAGE, HuntState.DEFEATED), "future schema read only");
        check(future.snapshot().equals(saved), "future schema lossless");
        check(BossHunt.fromId("remnant_bosses:ossukage") == null, "wrong namespace not canonical");
        CompoundTag corrupt = new CompoundTag();
        corrupt.putString("SchemaVersion", "bad");
        check(!HuntSavedData.load(corrupt).advance(PLAYER, BossHunt.OSSUKAGE, HuntState.DEFEATED), "malformed schema read only");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
