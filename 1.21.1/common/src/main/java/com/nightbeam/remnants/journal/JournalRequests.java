package com.nightbeam.remnants.journal;

import java.util.LinkedHashMap;
import java.util.UUID;

/** Refresh limits are temporary and scoped to one world's SavedData instance. */
public final class JournalRequests {
    private static final int MAX_PLAYERS = 4096;
    private final LinkedHashMap<UUID, Long> accepted = new LinkedHashMap<>();
    public boolean allow(UUID player, long tick, int version, boolean alive, boolean spectator) {
        if (version != 1 || !alive || spectator) return false;
        Long previous = accepted.get(player);
        if (previous != null && tick >= previous && tick - previous < 20) return false;
        accepted.remove(player);
        if (accepted.size() >= MAX_PLAYERS) accepted.remove(accepted.keySet().iterator().next());
        accepted.put(player, tick);
        return true;
    }
    public void expire(long tick) { accepted.values().removeIf(previous -> tick < previous || tick - previous > 1200); }
    public void logout(UUID player) { accepted.remove(player); }
    public void clear() { accepted.clear(); }
}
