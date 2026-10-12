package com.nightbeam.remnants.journal;

import java.util.Locale;

/** Bound effective configuration strings before they enter a personal snapshot. */
public final class RitualValues {
    private RitualValues() {}

    public static String boundedId(String value) {
        if (value == null || value.length() > 256) return "";
        return value.toLowerCase(Locale.ROOT);
    }
}
