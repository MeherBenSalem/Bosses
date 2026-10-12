package com.nightbeam.remnants;

import com.nightbeam.remnants.client.journal.JournalLayout;

/** Hand-derived viewport and scrolling boundaries; no Minecraft runtime required. */
public final class JournalLayoutRegression {
    public static void main(String[] args) { run(); }

    public static void run() {
        for (int[] size : new int[][] {{160, 100}, {240, 135}, {320, 180}, {440, 252}, {640, 360}, {1920, 1080}}) {
            JournalLayout layout = JournalLayout.calculate(size[0], size[1]);
            inside(layout.panel(), size[0], size[1]);
            inside(layout.details(), size[0], size[1]);
            inside(layout.footer(), size[0], size[1]);
            for (int i = 0; i < 4; i++) {
                JournalLayout.Rect entry = layout.entry(i);
                inside(entry, size[0], size[1]);
                check(!overlap(entry, layout.details()), "entry covers detail viewport");
                check(!overlap(entry, layout.footer()), "entry covers footer controls");
            }
            check(!overlap(layout.footer(), layout.details()), "footer covers detail viewport");
        }
        check(JournalLayout.calculate(240, 135).compact(), "narrow screen requires compact navigation");
        check(!JournalLayout.calculate(640, 360).compact(), "normal screen requires split pages");
        check(JournalLayout.clampScroll(-12, 300, 100) == 0, "negative scroll");
        check(JournalLayout.clampScroll(999, 300, 100) == 200, "bottom scroll must stop at last line");
        check(JournalLayout.clampScroll(80, 40, 100) == 0, "short detail must not scroll");
        check(JournalLayout.clampScroll(180, 300, 200) == 100, "resize must clamp old scroll");
        System.out.println("Journal layout regression passed");
    }

    private static void inside(JournalLayout.Rect rect, int width, int height) {
        check(rect.width() > 0 && rect.height() > 0, "empty viewport");
        check(rect.x() >= 0 && rect.y() >= 0 && rect.right() <= width && rect.bottom() <= height,
                "viewport outside " + width + "x" + height + ": " + rect);
    }

    private static boolean overlap(JournalLayout.Rect a, JournalLayout.Rect b) {
        return a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
