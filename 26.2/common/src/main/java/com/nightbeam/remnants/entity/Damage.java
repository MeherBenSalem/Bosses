package com.nightbeam.remnants.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/** Server-side damage that is a harmless no-op on the client, like {@code Entity#hurt} before 1.21.2. */
public final class Damage {
	private Damage() {
	}

	public static boolean hurt(Entity target, DamageSource source, float amount) {
		return target.level() instanceof ServerLevel level && target.hurtServer(level, source, amount);
	}
}
