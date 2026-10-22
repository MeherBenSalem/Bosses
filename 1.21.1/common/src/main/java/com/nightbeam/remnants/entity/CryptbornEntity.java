package com.nightbeam.remnants.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The Cryptborn's shared body. It reuses the Hollow Legion's action machine
 * ({@link LegionEntity}: one synchronized action at a time, server-timed hits read from the clip table,
 * held death clip, stats from remnant/monsters/&lt;id&gt;) and swaps the allegiance: Cryptborn never
 * hit each other, the Warden or its anchors.
 */
public abstract class CryptbornEntity extends LegionEntity {
	protected CryptbornEntity(EntityType<? extends CryptbornEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean canHit(LivingEntity entity) {
		return entity != this && Cryptborn.victim(entity);
	}

	@Override
	protected boolean isFriend(Entity entity) {
		return Cryptborn.isWardenSide(entity);
	}
}
