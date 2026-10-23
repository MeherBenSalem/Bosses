package com.nightbeam.remnants.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

public final class EntityDeathEvents {
	private EntityDeathEvents() {
	}

	public static void onLivingDeath(LivingEntity entity, DamageSource source) {
		Level level = entity.level();
		if (level.isClientSide()) {
			return;
		}
		com.nightbeam.remnants.realm.RealmEvents.onLivingDeath(entity);
		// Nearby Soul Collectors draw the soul in.
		com.nightbeam.remnants.entity.SoulCollectorEntity.onCreatureDied(entity);
	}
}
