package com.nightbeam.remnants.journal;

import java.util.*;
import java.util.function.Function;

/** Server-thread-only, bounded temporary combat attribution. No team or tracking-list credit. */
public final class EncounterCredits {
    public static final int MAX_ENCOUNTERS = 1024;
    public static final int MAX_CONTRIBUTORS = 128;
    public static final long WINDOW = 1200;
    private final LinkedHashMap<UUID, LinkedHashMap<UUID, Contribution>> encounters = new LinkedHashMap<>();
    public record Position(String dimension, double x, double y, double z) {}
    public record Participant(UUID id, Position position, boolean spectator) {}
    private record Contribution(long hitTick, Participant player) {}

    public static boolean acceptedDamage(float before, float after) {
        return Float.isFinite(before) && Float.isFinite(after) && before > after;
    }
    public static boolean acceptedDeath(boolean dead, boolean processed) { return dead && !processed; }
    private static boolean within(Position a, Position b, double radius) {
        if (!a.dimension().equals(b.dimension())) return false;
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
    public static boolean canDiscover(Position boss, Position player, boolean spectator, boolean lineOfSight) {
        return !spectator && lineOfSight && within(boss, player, 48);
    }
    public boolean record(UUID boss, long tick, Position location, Participant player) {
        if (player == null || player.spectator() || !within(location, player.position(), 64)) return false;
        LinkedHashMap<UUID, Contribution> participants = encounters.remove(boss);
        if (participants == null) participants = new LinkedHashMap<>();
        participants.remove(player.id());
        if (participants.size() >= MAX_CONTRIBUTORS) participants.remove(participants.keySet().iterator().next());
        participants.put(player.id(), new Contribution(tick, player));
        if (encounterCount() >= MAX_ENCOUNTERS) encounters.remove(encounters.keySet().iterator().next());
        encounters.put(boss, participants);
        return true;
    }
    public Set<UUID> complete(UUID boss, long tick, Position location, Function<UUID, Participant> online) {
        Map<UUID, Contribution> participants = encounters.remove(boss);
        if (participants == null) return Set.of();
        Set<UUID> credited = new HashSet<>();
        participants.forEach((id, hit) -> {
            long elapsed = tick - hit.hitTick();
            Participant player = online.apply(id);
            if (player == null) player = hit.player();
            if (elapsed >= 0 && elapsed <= WINDOW && !player.spectator() && within(location, player.position(), 64)) credited.add(id);
        });
        return credited;
    }
    public void logout(Participant player) {
        for (Map<UUID, Contribution> participants : encounters.values()) {
            Contribution hit = participants.get(player.id());
            if (hit != null) participants.put(player.id(), new Contribution(hit.hitTick(), player));
        }
    }
    public void expire(long tick) {
        encounters.values().removeIf(participants -> {
            participants.values().removeIf(hit -> tick < hit.hitTick() || tick - hit.hitTick() > WINDOW);
            return participants.isEmpty();
        });
    }
    public int encounterCount() { return encounters.size(); }
    public void clear() { encounters.clear(); }
}
