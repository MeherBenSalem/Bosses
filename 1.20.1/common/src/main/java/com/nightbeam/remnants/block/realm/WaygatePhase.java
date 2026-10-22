package com.nightbeam.remnants.block.realm;

import net.minecraft.util.StringRepresentable;

/** The Waygate's life: placed dormant, kindled by an Eclipse Shard, then open for good. */
public enum WaygatePhase implements StringRepresentable {
	DORMANT("dormant", 4),
	KINDLING("kindling", 10),
	OPEN("open", 15);

	private final String name;
	private final int light;

	WaygatePhase(String name, int light) {
		this.name = name;
		this.light = light;
	}

	public int light() {
		return light;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
