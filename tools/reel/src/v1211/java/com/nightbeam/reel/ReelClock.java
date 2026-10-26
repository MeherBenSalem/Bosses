package com.nightbeam.reel;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.locks.LockSupport;

/**
 * Virtual time for offline capture. While {@link #timeControl} is on, the client timer advances by exactly
 * {@link #ticksThisFrame} game ticks per rendered frame instead of by wall-clock time, so a slow software renderer
 * still yields perfectly even motion. While {@link #lockstep} is on, the integrated server runs one tick only when
 * the client asks for it, so server-driven motion (bosses, projectiles, particles) stays in step with those frames.
 */
public final class ReelClock {
	public static volatile boolean timeControl;
	public static volatile boolean lockstep;
	/** Game ticks to advance on the next frame (fractional; 0 holds time still). */
	public static volatile double ticksThisFrame;
	/** Fractional game ticks advanced since time control began. */
	public static double gameTicks;
	/** Set while the server thread waits for a permit, so chunk work may use the wait. */
	public static volatile boolean serverWaiting;
	/** Record-relative index of the tick the server is (or will be) running. */
	public static volatile int serverTickIndex;

	private static final Object LOCK = new Object();
	private static long granted, completed;

	private ReelClock() {
	}

	/** Called by the timer mixin: the integer ticks to run this frame; stores the partial tick in {@code residual[0]}. */
	public static int advance(float[] residual) {
		double before = gameTicks;
		gameTicks += ticksThisFrame;
		residual[0] = (float) (gameTicks - Math.floor(gameTicks));
		return (int) (Math.floor(gameTicks) - Math.floor(before));
	}

	public static void reset() {
		gameTicks = 0;
		synchronized (LOCK) {
			granted = completed = 0;
			LOCK.notifyAll();
		}
	}

	/** Client side: lets the server run one tick and waits until it has, then drains the packets it sent. */
	public static void stepServer(Minecraft client, int tickIndex) {
		// A pause screen stops the integrated server's ticks; waiting for one would stall.
		if (client.isPaused()) return;
		long target;
		synchronized (LOCK) {
			serverTickIndex = tickIndex;
			target = ++granted;
			LOCK.notifyAll();
		}
		long deadline = System.nanoTime() + 30_000_000_000L;
		synchronized (LOCK) {
			while (completed < target && lockstep) {
				if (System.nanoTime() > deadline) throw new IllegalStateException("integrated server did not tick within 30 s");
				try {
					LOCK.wait(2);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		// The local connection hands packets to a netty thread; give it a moment, then run them before this tick.
		LockSupport.parkNanos(3_000_000L);
		while (client.pollTask()) {
		}
	}

	/** Server side, at the head of every tick. */
	public static void awaitPermit(MinecraftServer server) {
		if (!lockstep) return;
		serverWaiting = true;
		try {
			server.managedBlock(() -> {
				synchronized (LOCK) {
					return !lockstep || completed < granted;
				}
			});
		} finally {
			serverWaiting = false;
		}
	}

	/** Server side, at the end of every tick. */
	public static void tickDone() {
		synchronized (LOCK) {
			if (completed < granted) completed++;
			LOCK.notifyAll();
		}
	}

	/** Stops lockstep and releases a server that may be waiting for a permit. */
	public static void release() {
		lockstep = false;
		synchronized (LOCK) {
			completed = granted;
			LOCK.notifyAll();
		}
	}
}
