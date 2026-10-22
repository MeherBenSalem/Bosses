package com.nightbeam.remnants.item;

import com.nightbeam.remnants.config.WardenConfig;

/**
 * Crypt Key: the Gravebound Warden's altar activation item. Like the
 * Sovereign's Seal, the tooltip names the configured pedestal offerings.
 */
public class CryptKeyItem extends SovereignsSealItem {
	public CryptKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	protected String summonFile() {
		return WardenConfig.SUMMON;
	}
}
