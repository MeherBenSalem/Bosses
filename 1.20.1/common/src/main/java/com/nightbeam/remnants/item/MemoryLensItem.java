package com.nightbeam.remnants.item;

import com.nightbeam.remnants.config.WatcherConfig;

/**
 * Lens of Memory: the Forgotten Watcher's altar activation item. Like the
 * Sovereign's Seal, the tooltip names the configured pedestal offerings.
 */
public class MemoryLensItem extends SovereignsSealItem {
	public MemoryLensItem(Properties properties) {
		super(properties);
	}

	@Override
	protected String summonFile() {
		return WatcherConfig.SUMMON;
	}
}
