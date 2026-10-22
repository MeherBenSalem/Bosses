package com.nightbeam.remnants.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The Corrupted Relics' walking body. It reuses the Hollow Legion's action machine
 * ({@link LegionEntity}: one synchronized action at a time, server-timed hits read from the clip table,
 * held death clip, stats from remnant/monsters/&lt;id&gt;) and swaps the allegiance: relics never hit each other.
 */
public abstract class CorruptedRelicEntity extends LegionEntity {
	protected CorruptedRelicEntity(EntityType<? extends CorruptedRelicEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean canHit(LivingEntity entity) {
		return entity != this && CorruptedRelics.victim(entity);
	}

	@Override
	protected boolean isFriend(Entity entity) {
		return CorruptedRelics.isRelic(entity);
	}
}
