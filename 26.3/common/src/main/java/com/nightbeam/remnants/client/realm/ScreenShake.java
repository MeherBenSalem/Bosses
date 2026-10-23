package com.nightbeam.remnants.client.realm;

import com.nightbeam.remnants.config.RealmConfig;
import net.minecraft.util.Mth;

/**
 * Camera shake as decaying "trauma" (0..1): sources add to it, it fades every tick, and the camera mixin turns
 * trauma squared into small layered-sine yaw and pitch offsets.
 */
public final class ScreenShake {
	private static final float DECAY = 0.035f;
	private static final float MAX_YAW = 2.6f;
	private static final float MAX_PITCH = 1.9f;
	private static float trauma;
	private static float previous;
	private static int ticks;

	private ScreenShake() {
	}

	public static void add(float amount) {
		trauma = Mth.clamp(trauma + amount, 0.0f, 1.0f);
	}

	/** Hold the shake at no less than {@code level} (used by sustained sources like a kindling Waygate). */
	public static void atLeast(float level) {
		trauma = Math.max(trauma, Mth.clamp(level, 0.0f, 1.0f));
	}

	public static void tick() {
		previous = trauma;
		trauma = Math.max(0.0f, trauma - DECAY);
		ticks++;
	}

	public static void reset() {
		trauma = previous = 0;
	}

	/** Yaw and pitch offsets in degrees for this frame, or null when still. */
	public static float[] offsets(float partialTick) {
		float t = Mth.lerp(partialTick, previous, trauma);
		if (t <= 0.001f || !RealmConfig.clientSwitch("camera_shake")) return null;
		float shake = t * t;
		float time = (ticks + partialTick) * 0.9f;
		float yaw = (Mth.sin(time * 1.7f) * 0.6f + Mth.sin(time * 3.3f + 1.3f) * 0.4f) * MAX_YAW * shake;
		float pitch = (Mth.sin(time * 2.3f + 0.7f) * 0.6f + Mth.sin(time * 4.1f + 2.1f) * 0.4f) * MAX_PITCH * shake;
		return new float[]{yaw, pitch};
	}
}
