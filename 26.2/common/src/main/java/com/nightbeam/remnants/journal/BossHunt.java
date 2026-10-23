package com.nightbeam.remnants.journal;

public enum BossHunt {
    OSSUKAGE("ossukage"), GRAVEBOUND_WARDEN("gravebound_warden"), FORGOTTEN_WATCHER("forgotten_watcher"), HOLLOW_SOVEREIGN("hollow_sovereign");
    private final String key;
    BossHunt(String key) { this.key = key; }
    public String id() { return "remnants:" + key; }
    public String key() { return key; }
    public static BossHunt fromId(String id) {
        for (BossHunt boss : values()) if (boss.id().equals(id)) return boss;
        return null;
    }
}
