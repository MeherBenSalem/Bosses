package com.nightbeam.remnants.event;

import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import net.minecraft.world.entity.LivingEntity;

public final class EntityTickEvents {
	private EntityTickEvents() {
	}

	public static void updateRatAnimations(RatEntity entity) {
		int tickCount = entity.tickCount;
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;
		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		}
	}

	public static void updateOssukageAnimations(RemnantOssukageEntity entity) {
		// GeckoLib reads the synchronized combat state directly from the entity.
	}

	public static void updateSkeletonMinionAnimations(SkeletonMinionEntity entity) {
		int tickCount = entity.tickCount;
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;
		boolean isSpawning = tickCount < 120;

		if (isSpawning) {
			entity.animationState3.startIfStopped(tickCount);
		} else {
			entity.animationState3.stop();
		}

		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		}
	}

	public static void updateWraithAnimations(WraithEntity entity) {
		int tickCount = entity.tickCount;
		boolean isDead = entity.isDeadOrDying();
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;

		if (isDead) {
			entity.animationState3.startIfStopped(tickCount);
			entity.animationState0.stop();
			entity.animationState2.stop();
		} else {
			entity.animationState3.stop();
			if (isAttacking) {
				entity.animationState2.startIfStopped(tickCount);
				entity.animationState0.stop();
			} else {
				entity.animationState0.startIfStopped(tickCount);
				entity.animationState2.stop();
			}
		}
	}

	public static void updateOssukageServerTick(RemnantOssukageEntity entity) {
		// Ossukage owns its server-authoritative combat tick.
	}

	public static void onLivingHurt(LivingEntity entity) {
		if (entity instanceof RemnantOssukageEntity ossukage) {
			ossukage.onIncomingDamage();
		}
	}
}
