package com.nightbeam.remnants.entity.anim;

import com.geckolib.animation.RawAnimation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cached GeckoLib animations built from {@link RemnantClips}, played with the clip's own loop mode. */
public final class RemnantAnims {
	private static final Map<RemnantClips.Clip, RawAnimation> CACHE = new ConcurrentHashMap<>();

	private RemnantAnims() {
	}

	public static RawAnimation of(RemnantClips.Sheet sheet, String clip) {
		return of(sheet.clip(clip));
	}

	public static RawAnimation of(RemnantClips.Clip clip) {
		return CACHE.computeIfAbsent(clip, c -> switch (c.mode()) {
			case LOOP -> RawAnimation.begin().thenLoop(c.id());
			case ONCE -> RawAnimation.begin().thenPlay(c.id());
			case HOLD -> RawAnimation.begin().thenPlayAndHold(c.id());
		});
	}
}
