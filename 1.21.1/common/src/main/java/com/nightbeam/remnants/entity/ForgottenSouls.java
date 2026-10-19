package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Allegiance rules shared by the Forgotten Watcher, its illusions and the Forgotten Souls. */
public final class ForgottenSouls {
	/** Every Forgotten Soul: Lost Pilgrim, Memory Wraith, Soul Collector and the Collector's wisps. */
	public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Constants.ENTITY_NAMESPACE, "forgotten_souls"));

	private ForgottenSouls() {
	}

	public static boolean isSoul(Entity entity) {
		return entity != null && entity.getType().is(TAG);
	}

	/** The Watcher, its illusions and the Forgotten Souls never harm one another. */
	public static boolean isWatcherSide(Entity entity) {
		return entity instanceof ForgottenWatcherEntity || entity instanceof WatcherIllusionEntity || isSoul(entity);
	}

	/** A living target the Watcher's side may damage. */
	public static boolean victim(LivingEntity entity) {
		return entity.isAlive() && entity.isAttackable() && !isWatcherSide(entity) && !HollowLegion.isUntouchablePlayer(entity);
	}
}
