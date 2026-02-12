package tn.naizo.remnants.event;

import tn.naizo.remnants.entity.RatEntity;
import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.entity.SkeletonMinionEntity;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Handles entity tick updates for custom entities.
 * Replaces the deleted animation and tick logic procedures.
 * Handles animation state updates and entity AI behavior.
 */
@Mod.EventBusSubscriber(modid = "remnant_bosses")
public class EntityTickEvents {

	@SubscribeEvent
	public static void onLivingTick(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		Level level = entity.level();

		// Only run on client for animation updates
		if (!level.isClientSide) {
			// Server-side tick logic can go here if needed
			if (entity instanceof RemnantOssukageEntity ossukage) {
				updateOssukageServerTick(ossukage);
			}
			return;
		}

		// Client-side animation updates
		if (entity instanceof RatEntity rat) {
			updateRatAnimations(rat);
		}

		if (entity instanceof RemnantOssukageEntity ossukage) {
			updateOssukageAnimations(ossukage);
		}

		if (entity instanceof SkeletonMinionEntity skeleton) {
			updateSkeletonMinionAnimations(skeleton);
		}
	}

	/**
	 * Update animation states for Rat entity.
	 * Replaces CheckIsIdleAnimProcedure and CheckAttackAnimProcedure.
	 */
	private static void updateRatAnimations(RatEntity entity) {
		int tickCount = entity.tickCount;

		// Idle animation - play when entity is not attacking
		boolean isIdle = entity.getTarget() == null;
		entity.animationState0.animateWhen(isIdle, tickCount);

		// Attack animation - play when entity is attacking
		boolean isAttacking = entity.getTarget() != null;
		entity.animationState2.animateWhen(isAttacking, tickCount);
	}

	/**
	 * Update animation states for Ossukage entity.
	 * Replaces multiple animation condition procedures.
	 */
	private static void updateOssukageAnimations(RemnantOssukageEntity entity) {
		int tickCount = entity.tickCount;

		// Get entity state for animation selection
		String state = entity.getEntityState();

		// Idle animation - play when entity is idle
		boolean isIdle = state.equals("idle") || state.isEmpty();
		entity.animationState0.animateWhen(isIdle, tickCount);

		// Attack animation - play when entity is attacking
		boolean isAttacking = entity.isAttacking() || state.equals("attack");
		entity.animationState2.animateWhen(isAttacking, tickCount);

		// Leap animation - play during special move
		boolean isLeaping = state.equals("leap");
		entity.animationState3.animateWhen(isLeaping, tickCount);

		// Transform animation - play during transformation
		boolean isTransforming = entity.isTransformed();
		entity.animationState4.animateWhen(isTransforming, tickCount);

		// Additional animation state
		entity.animationState5.animateWhen(false, tickCount);
	}

	/**
	 * Update animation states for Skeleton Minion entity.
	 * Replaces CheckIsIdleAnimProcedure and related procedures.
	 */
	private static void updateSkeletonMinionAnimations(SkeletonMinionEntity entity) {
		int tickCount = entity.tickCount;

		// Idle animation - play when entity is not attacking
		boolean isIdle = entity.getTarget() == null;
		entity.animationState0.animateWhen(isIdle, tickCount);

		// Attack animation - play when entity is attacking (use synced field on client)
		boolean isAttacking = entity.isAttacking();
		entity.animationState2.animateWhen(isAttacking, tickCount);

		// Spawn animation - play when newly spawned
		boolean isSpawn = entity.isSpawned();
		entity.animationState3.animateWhen(isSpawn, tickCount);
	}

	/**
	 * Update Ossukage server-side tick logic.
	 * This would handle AI state transitions and behavior.
	 */
	private static void updateOssukageServerTick(RemnantOssukageEntity entity) {
		// Placeholder for server-side AI logic
		// This would include state machine updates, phase transitions, etc.
		// The original NinjaSkeletonOnEntityTickUpdateProcedure logic would go here
	}
}
