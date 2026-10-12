package com.nightbeam.remnants;

import com.nightbeam.remnants.journal.RitualValues;

public final class RitualValueRegression {
    public static void main(String[] args) { run(); }

    public static void run() {
        require(RitualValues.boundedId(null).equals(""), "Null configuration must remain unavailable");
        require(RitualValues.boundedId("x".repeat(257)).equals(""), "Oversized config must not enter a packet");
        require(RitualValues.boundedId("Minecraft:ECHO_SHARD").equals("minecraft:echo_shard"), "Case normalization");
        require(RitualValues.boundedId("unknown:offering").equals("unknown:offering"), "Do not invent a default");
        System.out.println("RitualValueRegression: missing/oversized/case-normalized config passes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
