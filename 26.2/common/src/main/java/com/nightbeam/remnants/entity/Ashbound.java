package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Allegiance rules shared by the Ashbound Constructs, Kharum's furnace-hearted machines. */
public final class Ashbound {
	/** Every Ashbound Construct: Ashbound Crawler, Ember Servitor and Rusted Guardian. */
	public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(Constants.ENTITY_NAMESPACE, "ashbound"));

	private Ashbound() {
	}

	public static boolean isAshbound(Entity entity) {
		return entity != null && entity.is(TAG);
	}

	/** A living target the Ashbound may damage: never another construct, nor a creative or spectating player. */
	public static boolean victim(LivingEntity entity) {
		return entity.isAlive() && entity.isAttackable() && !isAshbound(entity) && !HollowLegion.isUntouchablePlayer(entity);
	}

	/** Sets {@code entity} burning for at least {@code ticks}. Constructs never light blocks: only creatures burn. */
	public static void ignite(Entity entity, int ticks) {
		if (ticks > 0) entity.igniteForTicks(ticks);
	}
}
