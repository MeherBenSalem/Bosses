package com.nightbeam.remnants.client.journal;

import com.nightbeam.remnants.network.ClientboundJournalPacket;
import net.minecraft.client.Minecraft;

/** Client-thread only. Never retain progress across a world or connection change. */
public final class BossHuntsClient {
    private static Object level;
    private static Object connection;

    private BossHuntsClient() {}

    public static void receive(ClientboundJournalPacket packet) {
        Minecraft client = Minecraft.getInstance();
        tick();
        if (client.player == null || client.level == null || client.getConnection() == null) return;
        if (client.gui.screen() instanceof BossHuntsScreen screen) {
            screen.update(packet.entries());
        } else if (packet.open()) {
            client.gui.setScreen(new BossHuntsScreen(packet.entries()));
        }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (level != client.level || connection != client.getConnection()) {
            if (client.gui.screen() instanceof BossHuntsScreen) client.gui.setScreen(null);
            level = client.level;
            connection = client.getConnection();
        }
        if ((client.player == null || client.level == null || client.getConnection() == null)
                && client.gui.screen() instanceof BossHuntsScreen) client.gui.setScreen(null);
    }
}
