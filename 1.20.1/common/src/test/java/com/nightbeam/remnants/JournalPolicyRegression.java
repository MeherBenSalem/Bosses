package com.nightbeam.remnants;
import com.nightbeam.remnants.journal.*;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
/** Real policy behavior runnable without Minecraft bootstrap. */
public final class JournalPolicyRegression {
    private static final UUID PLAYER = UUID.fromString("37f381ae-a6e8-4ec2-91ad-e54353e8856e");
    private static final UUID BOSS = UUID.fromString("b4df6eaf-a53b-4000-b2d5-a603f7db9e7a");
    private static final EncounterCredits.Position ORIGIN = pos("overworld", 0);
    public static void main(String[] args) { run(); }
    public static void run() { contribution(); multipleContributors(); policy(); System.out.println("Journal policy regressions passed"); }
    private static void contribution() {
        EncounterCredits credits = new EncounterCredits();
        check(credits.record(BOSS, 10, ORIGIN, player(pos("overworld", 64), false)), "64-block hit boundary accepted");
        check(credits.complete(BOSS, 1210, ORIGIN, id -> null).equals(Set.of(PLAYER)), "disconnected contributor at 1200 ticks");
        check(credits.complete(BOSS, 1210, ORIGIN, id -> null).isEmpty(), "completion consumes encounter");
        credits.record(BOSS, 10, ORIGIN, player(ORIGIN, false));
        check(credits.complete(BOSS, 1211, ORIGIN, id -> null).isEmpty(), "1201 ticks expires");
        check(!credits.record(BOSS, 10, ORIGIN, player(pos("overworld", 64.01), false)), "outside hit radius");
        check(!credits.record(BOSS, 10, ORIGIN, player(pos("nether", 0), false)), "cross dimension hit");
        check(!credits.record(BOSS, 10, ORIGIN, player(ORIGIN, true)), "spectator hit");
        credits.record(BOSS, 10, ORIGIN, player(ORIGIN, false));
        check(credits.complete(BOSS, 20, ORIGIN, id -> player(pos("overworld", 64.01), false)).isEmpty(), "online moved outside radius");
        credits.record(BOSS, 10, ORIGIN, player(ORIGIN, false));
        check(credits.complete(BOSS, 20, ORIGIN, id -> player(ORIGIN, true)).isEmpty(), "online spectator at completion");
        credits.record(BOSS, 10, ORIGIN, player(ORIGIN, false));
        credits.logout(player(pos("nether", 0), false));
        check(credits.complete(BOSS, 20, ORIGIN, id -> null).isEmpty(), "logout position refreshed");
        credits.record(BOSS, 100, ORIGIN, player(ORIGIN, false));
        credits.expire(1301);
        check(credits.encounterCount() == 0, "idle encounters expire");
        for (int i = 0; i < 1100; i++) credits.record(new UUID(0, i), 2000, ORIGIN, player(ORIGIN, false));
        check(credits.encounterCount() <= EncounterCredits.MAX_ENCOUNTERS, "encounters bounded");
        credits.clear();
        for (int i = 0; i < 150; i++) credits.record(BOSS, 2000, ORIGIN, new EncounterCredits.Participant(new UUID(1, i), ORIGIN, false));
        check(credits.complete(BOSS, 2000, ORIGIN, id -> null).size() <= EncounterCredits.MAX_CONTRIBUTORS, "contributors bounded");
        credits.record(BOSS, 2000, ORIGIN, player(ORIGIN, false));
        check(credits.complete(BOSS, 1999, ORIGIN, id -> null).isEmpty(), "negative elapsed time rejected");
        check(!credits.record(BOSS, 2000, ORIGIN, player(pos("overworld", Double.NaN), false)), "non-finite position rejected");
        credits.clear();
        check(credits.encounterCount() == 0, "shutdown clears temporary encounters");
    }
    private static void multipleContributors() {
        UUID secondId = UUID.fromString("c87924e5-bbfc-4a27-9990-d675b3f19b46");
        UUID uninvolvedId = UUID.fromString("1414c7a9-48e4-4610-a74b-ed2c15ac7652");
        EncounterCredits.Participant first = player(ORIGIN, false);
        EncounterCredits.Participant second = new EncounterCredits.Participant(secondId, pos("overworld", 64), false);
        EncounterCredits.Participant uninvolved = new EncounterCredits.Participant(uninvolvedId, ORIGIN, false);
        Map<UUID, EncounterCredits.Participant> online = Map.of(PLAYER, first, secondId, second, uninvolvedId, uninvolved);

        EncounterCredits credits = new EncounterCredits();
        credits.record(BOSS, 10, ORIGIN, first);
        credits.record(BOSS, 10, ORIGIN, second);
        check(credits.complete(BOSS, 1210, ORIGIN, online::get).equals(Set.of(PLAYER, secondId)),
                "all qualifying contributors credited; nearby uninvolved player excluded");

        credits.record(BOSS, 10, ORIGIN, first);
        credits.record(BOSS, 11, ORIGIN, second);
        check(credits.complete(BOSS, 1211, ORIGIN, online::get).equals(Set.of(secondId)),
                "expired first contributor must not suppress fresh second contributor");

        for (EncounterCredits.Position moved : new EncounterCredits.Position[] { pos("nether", 0), pos("overworld", 64.01) }) {
            credits.record(BOSS, 10, ORIGIN, first);
            credits.record(BOSS, 10, ORIGIN, second);
            Map<UUID, EncounterCredits.Participant> afterMove = Map.of(
                    PLAYER, new EncounterCredits.Participant(PLAYER, moved, false),
                    secondId, second, uninvolvedId, uninvolved);
            check(credits.complete(BOSS, 20, ORIGIN, afterMove::get).equals(Set.of(secondId)),
                    "ineligible first contributor must not suppress eligible second contributor");
        }
    }
    private static void policy() {
        check(EncounterCredits.canDiscover(ORIGIN, pos("overworld", 48), false, true), "discovery radius boundary");
        check(!EncounterCredits.canDiscover(ORIGIN, pos("overworld", 48.01), false, true), "discovery outside radius");
        check(!EncounterCredits.canDiscover(ORIGIN, ORIGIN, true, true), "no spectator discovery");
        check(!EncounterCredits.canDiscover(ORIGIN, ORIGIN, false, false), "requires sight");
        check(!EncounterCredits.canDiscover(ORIGIN, pos("nether", 0), false, true), "discovery same dimension");
        check(!EncounterCredits.acceptedDamage(10, 10), "cancelled damage contributes nothing");
        check(!EncounterCredits.acceptedDamage(10, 11), "healing contributes nothing");
        check(EncounterCredits.acceptedDamage(10, 9), "actual health damage accepted");
        check(!EncounterCredits.acceptedDeath(false, false), "cancelled death rejected");
        check(!EncounterCredits.acceptedDeath(true, true), "persisted death guard");
        check(EncounterCredits.acceptedDeath(true, false), "real death accepted");
        JournalRequests requests = new JournalRequests();
        check(!requests.allow(PLAYER, 0, 99, true, false), "schema mismatch rejected");
        check(!requests.allow(PLAYER, 0, 1, false, false), "dead request rejected");
        check(!requests.allow(PLAYER, 0, 1, true, true), "spectator request rejected");
        check(requests.allow(PLAYER, 0, 1, true, false), "first request accepted");
        check(!requests.allow(PLAYER, 19, 1, true, false), "request before one second rejected");
        check(requests.allow(PLAYER, 20, 1, true, false), "request at one second accepted");
    }
    private static EncounterCredits.Position pos(String dimension, double x) { return new EncounterCredits.Position(dimension, x, 0, 0); }
    private static EncounterCredits.Participant player(EncounterCredits.Position pos, boolean spectator) { return new EncounterCredits.Participant(PLAYER, pos, spectator); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
