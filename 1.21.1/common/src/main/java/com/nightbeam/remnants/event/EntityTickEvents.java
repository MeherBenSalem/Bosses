package com.nightbeam.remnants.event;

import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import net.minecraft.world.entity.LivingEntity;

public final class EntityTickEvents {
	private EntityTickEvents() {
	}

	public static void onLivingHurt(LivingEntity entity) {
		if (entity instanceof RemnantOssukageEntity ossukage) {
			ossukage.onIncomingDamage();
		}
	}
}
