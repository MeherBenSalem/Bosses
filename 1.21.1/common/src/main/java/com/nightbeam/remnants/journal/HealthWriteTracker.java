package com.nightbeam.remnants.journal;

import java.util.ArrayDeque;

/** Measures only accepted health writes; enclosing damage events may cancel or reenter. */
public final class HealthWriteTracker {
    private final ArrayDeque<Write> writes = new ArrayDeque<>();
    private static final class Write {
        final float before;
        float nestedLoss;
        Write(float before) { this.before = before; }
    }
    public void beforeWrite(float health) { writes.push(new Write(health)); }
    public float afterWrite(float health) {
        if (writes.isEmpty()) return 0;
        Write write = writes.pop();
        if (!Float.isFinite(write.before) || !Float.isFinite(health)) return 0;
        float totalLoss = Math.max(0, write.before - health);
        if (!writes.isEmpty()) writes.peek().nestedLoss += totalLoss;
        return Math.max(0, totalLoss - write.nestedLoss);
    }
}
