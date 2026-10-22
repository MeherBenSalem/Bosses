package com.nightbeam.remnants.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;

/**
 * The Ashbound Constructs' walking body. It reuses the Hollow Legion's action machine
 * ({@link LegionEntity}: one synchronized action at a time, server-timed hits read from the clip table,
 * held death clip, stats from remnant/monsters/&lt;id&gt;) and swaps the allegiance: constructs never hit
 * each other. Their entity types are fire-immune (furnace hearts), and nothing they do lights a block.
 */
public abstract class AshboundEntity extends LegionEntity {
	protected AshboundEntity(EntityType<? extends AshboundEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean canHit(LivingEntity entity) {
		return entity != this && Ashbound.victim(entity);
	}

	@Override
	protected boolean isFriend(Entity entity) {
		return Ashbound.isAshbound(entity);
	}

	/** Constructs and relics are vessels, not the dead: smite and healing potions treat them as ordinary creatures. */
	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}
}
