package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Allegiance rules shared by the Gravebound Warden, its helpers and the Cryptborn. */
public final class Cryptborn {
	/** Every Cryptborn mob: soldier, lurker, rat and the Burial Guardian. */
	public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation(Constants.ENTITY_NAMESPACE, "cryptborn"));

	private Cryptborn() {
	}

	public static boolean isCryptborn(Entity entity) {
		return entity != null && entity.getType().is(TAG);
	}

	/** The Warden, its chain anchors, the Cryptborn and Ossukage never harm one another. */
	public static boolean isWardenSide(Entity entity) {
		return entity instanceof GraveboundWardenEntity || entity instanceof ChainAnchorEntity || isCryptborn(entity)
				|| entity instanceof RemnantOssukageEntity;
	}

	/** A living target the Warden's side may damage. */
	public static boolean victim(LivingEntity entity) {
		return entity.isAlive() && entity.isAttackable() && !isWardenSide(entity) && !HollowLegion.isUntouchablePlayer(entity);
	}
}
