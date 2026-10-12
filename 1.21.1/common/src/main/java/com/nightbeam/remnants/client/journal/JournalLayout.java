package com.nightbeam.remnants.client.journal;

/** GUI-pixel geometry shared by rendering and hit testing, independent of Minecraft. */
public record JournalLayout(Rect panel, Rect navigation, Rect details, Rect footer, boolean compact) {
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }
    }

    public static JournalLayout calculate(int width, int height) {
        int panelWidth = Math.max(40, Math.min(840, width - 12));
        int panelHeight = Math.max(70, Math.min(460, height - 12));
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        Rect panel = new Rect(x, y, panelWidth, panelHeight);
        Rect footer = new Rect(x + 8, panel.bottom() - 24, panelWidth - 16, 18);
        boolean compact = panelWidth < 420 || panelHeight < 240;
        int contentY = y + 32;
        int contentHeight = footer.y() - contentY - 6;
        if (compact) {
            Rect navigation = new Rect(x + 8, contentY, panelWidth - 16, 24);
            Rect details = new Rect(x + 8, contentY + 28, panelWidth - 16, Math.max(1, contentHeight - 28));
            return new JournalLayout(panel, navigation, details, footer, true);
        }
        int listWidth = Math.min(210, panelWidth / 3);
        Rect navigation = new Rect(x + 8, contentY, listWidth, contentHeight);
        Rect details = new Rect(navigation.right() + 8, contentY,
                panelWidth - listWidth - 24, contentHeight);
        return new JournalLayout(panel, navigation, details, footer, false);
    }

    public Rect entry(int index) {
        if (compact) {
            int left = navigation.x() + index * navigation.width() / 4;
            int right = navigation.x() + (index + 1) * navigation.width() / 4;
            return new Rect(left, navigation.y(), right - left - 2, navigation.height());
        }
        int rowHeight = Math.min(44, (navigation.height() - 12) / 4);
        return new Rect(navigation.x(), navigation.y() + index * (rowHeight + 4), navigation.width(), rowHeight);
    }

    public static int clampScroll(int scroll, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewportHeight)));
    }
}
