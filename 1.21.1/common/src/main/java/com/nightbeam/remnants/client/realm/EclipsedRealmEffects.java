package com.nightbeam.remnants.client.realm;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * The Realm's sky type: no clouds, no sunset tint, and fog that keeps its biome colour instead of following the
 * (frozen) daylight. The sky itself is drawn by {@link EclipseSkyRenderer}.
 */
public class EclipsedRealmEffects extends DimensionSpecialEffects {
	public EclipsedRealmEffects() {
		super(Float.NaN, true, SkyType.NORMAL, false, false);
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
		return color.multiply(0.86, 0.84, 0.8);
	}

	@Override
	public boolean isFoggyAt(int x, int z) {
		return false;
	}

	@Nullable
	@Override
	public float[] getSunriseColor(float timeOfDay, float partialTick) {
		return null;
	}
}
