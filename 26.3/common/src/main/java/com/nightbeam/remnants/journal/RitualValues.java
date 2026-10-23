package com.nightbeam.remnants.journal;

import java.util.Locale;
import java.util.Map;

/** Bound effective configuration strings before they enter a personal snapshot. */
public final class RitualValues {
    private RitualValues() {}

    /** Vanilla IDs renamed since configs were first written; a 1.21.1 config keeps working unchanged. */
    private static final Map<String, String> RENAMED = Map.of("minecraft:chain", "minecraft:iron_chain");

    public static String boundedId(String value) {
        if (value == null || value.length() > 256) return "";
        String id = value.toLowerCase(Locale.ROOT);
        return RENAMED.getOrDefault(id, id);
    }
}
