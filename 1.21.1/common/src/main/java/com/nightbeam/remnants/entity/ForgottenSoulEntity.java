package com.nightbeam.remnants.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The Forgotten Souls' shared body. It reuses the Hollow Legion's action machine
 * ({@link LegionEntity}: one synchronized action at a time, server-timed hits read from the clip table,
 * held death clip, stats from remnant/monsters/&lt;id&gt;) and swaps the allegiance: Forgotten Souls never
 * hit each other, the Forgotten Watcher or its illusions.
 */
public abstract class ForgottenSoulEntity extends LegionEntity {
	protected ForgottenSoulEntity(EntityType<? extends ForgottenSoulEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean canHit(LivingEntity entity) {
		return entity != this && ForgottenSouls.victim(entity);
	}

	@Override
	protected boolean isFriend(Entity entity) {
		return ForgottenSouls.isWatcherSide(entity);
	}
}
