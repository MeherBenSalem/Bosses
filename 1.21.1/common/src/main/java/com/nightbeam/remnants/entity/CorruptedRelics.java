package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Allegiance rules shared by the Corrupted Relics, the Sanctum's sacred objects animated by the Oracle. */
public final class CorruptedRelics {
	/** Every Corrupted Relic: Shardling, Ancient Vessel and Relic Stalker. */
	public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Constants.ENTITY_NAMESPACE, "corrupted_relics"));

	private CorruptedRelics() {
	}

	public static boolean isRelic(Entity entity) {
		return entity != null && entity.getType().is(TAG);
	}

	/** A living target the Relics may damage: never another relic, nor a creative or spectating player. */
	public static boolean victim(LivingEntity entity) {
		return entity.isAlive() && entity.isAttackable() && !isRelic(entity) && !HollowLegion.isUntouchablePlayer(entity);
	}
}
