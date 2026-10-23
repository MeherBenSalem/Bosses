package com.nightbeam.remnants;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public final class RemnantBosses {
	private static final List<AbstractMap.SimpleEntry<Runnable, Integer>> WORK_QUEUE = new ArrayList<>();

	private RemnantBosses() {
	}

	public static void init() {
		Constants.LOG.info("Initializing {}", Constants.MOD_NAME);
	}

	public static void queueServerWork(int delay, Runnable runnable) {
		WORK_QUEUE.add(new AbstractMap.SimpleEntry<>(runnable, delay));
	}

	public static void tickWorkQueue() {
		List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
		WORK_QUEUE.forEach(work -> {
			work.setValue(work.getValue() - 1);
			if (work.getValue() == 0) {
				actions.add(work);
			}
		});
		actions.forEach(e -> e.getKey().run());
		WORK_QUEUE.removeAll(actions);
	}
}
