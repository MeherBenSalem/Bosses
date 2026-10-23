package com.nightbeam.remnants;

import com.nightbeam.remnants.journal.HealthWriteTracker;

/** Fixtures model accepted health writes, not the enclosing cancellable damage events. */
public final class JournalDamageRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        HealthWriteTracker tracker = new HealthWriteTracker();
        // A pre-damage listener deals 4 damage; the enclosing event is then cancelled.
        // Cancelled events never reach setHealth, so only the nested write is measured.
        tracker.beforeWrite(20);
        check(tracker.afterWrite(16) == 4, "nested pre-event damage belongs to nested source");
        check(tracker.afterWrite(16) == 0, "cancelled outer hit has no health-write frame");

        // Outer damage is recorded before a post-damage listener's zero-damage call.
        tracker.beforeWrite(20);
        float outer = tracker.afterWrite(15);
        tracker.beforeWrite(15);
        float nested = tracker.afterWrite(15);
        check(outer == 5 && nested == 0, "nested zero hit must not suppress outer accepted damage");

        // If another mod reenters during setHealth itself, do not double-count its loss.
        tracker.beforeWrite(20);
        tracker.beforeWrite(20);
        check(tracker.afterWrite(17) == 3, "nested write records its three health");
        check(tracker.afterWrite(12) == 5, "outer write excludes nested loss");

        tracker.beforeWrite(12);
        tracker.beforeWrite(12);
        check(tracker.afterWrite(10) == 2, "nested loss accepted");
        check(tracker.afterWrite(10) == 0, "unchanged outer write cannot claim nested loss");
        tracker.beforeWrite(10);
        check(tracker.afterWrite(11) == 0, "healing is not damage");
        tracker.beforeWrite(Float.NaN);
        check(tracker.afterWrite(0) == 0, "invalid health is not damage");
        System.out.println("Journal damage-write regressions passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
