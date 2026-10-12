package com.nightbeam.runtime;

import com.nightbeam.remnants.client.journal.BossHuntsScreen;
import com.nightbeam.remnants.client.journal.JournalLayout;
import com.nightbeam.remnants.journal.BossHunt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.lwjgl.glfw.GLFW;

/** Drives real screen input handlers and captures the rendered game framebuffer. */
public final class BossHuntsRuntimeClient implements ClientModInitializer {
    private final List<Path> captures = new ArrayList<>();
    private final AtomicInteger pendingCaptures = new AtomicInteger();
    private int ticks;
    private int phase;
    private int view;
    private int selected;
    private int waitUntil;
    private int capturePart;
    private int reopen;
    private BossHuntsScreen beforeClose;
    private boolean finished;
    private String finalResult;
    private int finalResultTick;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (finished) return;
        try {
            ticks++;
            client.options.pauseOnLostFocus = false;
            if (finalResult != null) {
                if (pendingCaptures.get() == 0) finishNow(client, finalResult);
                else if (ticks - finalResultTick > 1200) finishNow(client, "FAIL screenshot writes timed out: " + finalResult);
                return;
            }
            if (BossHuntsRuntimeProbe.phase < 0) {
                finish(client, "FAIL " + BossHuntsRuntimeProbe.failure);
                return;
            }
            if (ticks > 6000 || (ticks > 1800 && BossHuntsRuntimeProbe.phase == 0))
                throw new IllegalStateException("timed out waiting for disposable world/screen/server phase");
            var server = client.getSingleplayerServer();
            if (server == null || client.player == null || BossHuntsRuntimeProbe.phase == 0) return;
            if (ticks < waitUntil) return;
            if (reopen == 3) {
                BossHuntsRuntimeProbe.require(client.screen == beforeClose,
                        "repeated real item open refreshes the existing screen");
                beforeClose.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                reopen = 1;
                waitUntil = ticks + 30;
                return;
            }
            if (reopen == 1) {
                BossHuntsRuntimeProbe.require(!(client.screen instanceof BossHuntsScreen), "Escape closes real journal screen");
                server.execute(() -> BossHuntsRuntimeProbe.open(server, true));
                reopen = 2;
                waitUntil = ticks + 30;
                return;
            }
            if (reopen == 2) {
                BossHuntsRuntimeProbe.require(client.screen instanceof BossHuntsScreen && client.screen != beforeClose,
                        "journal item reopens via real server snapshot after Escape");
                finish(client, "PASS runtime probe completed: real gameplay hooks, network journal snapshots, "
                        + "owned Arrow/Wolf DamageSources (programmatic hurt), environmental final blow, "
                        + "Fabric death cancellation, GUI scales 2/3/4, keyboard selection, scroll, Escape and repeated item open. "
                        + (Boolean.getBoolean("bossHuntsRuntimeReload") ? "PASS actual restarted-world persistence. "
                        : "UNRUN actual restart (use journalRuntimeReload for second launch). ")
                        + "UNRUN multiplayer, Forge/NeoForge cancellation, dimension/reconnect scenarios.");
                return;
            }
            if (!(client.screen instanceof BossHuntsScreen screen)) {
                if (ticks % 40 == 0) server.execute(() -> BossHuntsRuntimeProbe.open(server, true));
                return;
            }
            if (phase != BossHuntsRuntimeProbe.phase) {
                phase = BossHuntsRuntimeProbe.phase;
                view = 0;
                capturePart = 0;
            }
            if (view >= 12) {
                if (phase == 1) {
                    server.execute(() -> BossHuntsRuntimeProbe.discover(server));
                    waitUntil = ticks + 120;
                } else if (phase == 2) {
                    server.execute(() -> BossHuntsRuntimeProbe.defeat(server));
                    waitUntil = ticks + 60;
                } else {
                    // A repeated request refreshes the same screen before Escape creates a new one.
                    beforeClose = screen;
                    server.execute(() -> BossHuntsRuntimeProbe.open(server, true));
                    reopen = 3;
                    waitUntil = ticks + 30;
                }
                return;
            }
            int scale = 2 + view / 4;
            int boss = view % 4;
            if (capturePart == 0) {
                client.options.guiScale().set(scale);
                client.resizeDisplay();
                while (selected != boss) {
                    BossHuntsRuntimeProbe.require(screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0), "right-arrow selection");
                    selected = (selected + 1) % 4;
                }
                screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
                capturePart = 1;
                waitUntil = ticks + 15; // let several real render frames complete after resize/input
            } else if (capturePart == 1) {
                capture(client, screen, scale, boss, "top");
                if (phase >= 2) {
                    screen.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0);
                    JournalLayout.Rect detail = JournalLayout.calculate(screen.width, screen.height).details();
                    BossHuntsRuntimeProbe.require(screen.mouseScrolled(detail.x() + 10, detail.y() + 10, -20),
                            "real detail wheel scroll handler");
                    screen.keyPressed(GLFW.GLFW_KEY_END, 0, 0);
                    capturePart = 2;
                    waitUntil = ticks + 15;
                } else { view++; capturePart = 0; }
            } else {
                capture(client, screen, scale, boss, "bottom");
                screen.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0);
                selected = Math.floorMod(selected - 1, 4);
                view++;
                capturePart = 0;
            }
        } catch (Throwable error) {
            BossHuntsRuntimeProbe.fail(error.toString());
            BossHuntsRuntimeProbe.LOG.error("Client runtime probe failure", error);
            finish(client, "FAIL " + error);
        }
    }

    private void capture(Minecraft client, BossHuntsScreen screen, int scale, int boss, String section) {
        String state = phase == 1 ? "undiscovered" : phase == 2 ? "encountered" : "defeated";
        String name = "boss-hunts-" + state + "-scale" + scale + "-" + BossHunt.values()[boss].key() + "-" + section + ".png";
        Path target = client.gameDirectory.toPath().resolve("screenshots").resolve(name);
        captures.add(target);
        pendingCaptures.incrementAndGet();
        // Screenshot.grab reads the actual Minecraft render target. No external or synthetic rendering.
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
                message -> {
                    BossHuntsRuntimeProbe.LOG.info("SCREENSHOT {} {}", name, message.getString());
                    pendingCaptures.decrementAndGet();
                });
        BossHuntsRuntimeProbe.LOG.info("VIEW {} requestedScale={} actualScale={} gui={}x{} narration={}", name,
                scale, client.getWindow().getGuiScale(), screen.width, screen.height, screen.getNarrationMessage().getString());
    }

    private void finish(Minecraft client, String result) {
        finalResult = result;
        finalResultTick = ticks;
        if (pendingCaptures.get() == 0) finishNow(client, result);
    }

    private void finishNow(Minecraft client, String result) {
        finished = true;
        try {
            StringBuilder report = new StringBuilder(result).append('\n');
            report.append("Minecraft 1.20.1 Fabric; real integrated server and client framebuffer.\n");
            report.append("World folder: BossHuntsValidation (disposable copy only).\n");
            for (Path path : captures) {
                boolean present = Files.isRegularFile(path) && Files.size(path) > 0;
                report.append(present ? "CAPTURE " : "MISSING ").append(path.toAbsolutePath()).append('\n');
                if (!present) BossHuntsRuntimeProbe.fail("screenshot file missing: " + path);
            }
            if (BossHuntsRuntimeProbe.phase < 0) report.append("FAIL ").append(BossHuntsRuntimeProbe.failure).append('\n');
            Path file = client.gameDirectory.toPath().resolve("boss-hunts-runtime-report.txt");
            Files.writeString(file, report);
            BossHuntsRuntimeProbe.LOG.info("RUNTIME REPORT {}\n{}", file.toAbsolutePath(), report);
        } catch (Exception error) {
            BossHuntsRuntimeProbe.LOG.error("Could not write runtime report", error);
        }
        // Vanilla stop exits the normal game loop; Minecraft performs its orderly server save/shutdown.
        client.stop();
    }
}
