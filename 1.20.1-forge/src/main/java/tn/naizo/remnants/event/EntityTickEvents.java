package tn.naizo.remnants.event;

import tn.naizo.remnants.entity.RatEntity;
import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.entity.SkeletonMinionEntity;
import tn.naizo.remnants.entity.WraithEntity;
import tn.naizo.remnants.procedures.NinjaSkeletonOnEntityTickUpdateProcedure;
import tn.naizo.remnants.procedures.NinjaSkeletonEntityIsHurtProcedure;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Handles entity tick updates for custom entities.
 * Replaces the deleted animation and tick logic procedures.
 * Handles animation state updates and entity AI behavior.
 */
@Mod.EventBusSubscriber(modid = "remnant_bosses", bus = Mod.EventBusSubscriber.Bus.FORGE)
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

		if (entity instanceof WraithEntity wraith) {
			updateWraithAnimations(wraith);
		}
	}

	/**
	 * Update animation states for Rat entity.
	 * Replaces CheckIsIdleAnimProcedure and CheckAttackAnimProcedure.
	 */
	private static void updateRatAnimations(RatEntity entity) {
		int tickCount = entity.tickCount;

		// Only treat as attacking during actual attack swing
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;

		// Properly manage idle and attack animations - only one should play at a time
		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		}
	}

	/**
	 * Update animation states for Ossukage entity.
	 * Replaces multiple animation condition procedures.
	 */
	private static void updateOssukageAnimations(RemnantOssukageEntity entity) {
		int tickCount = entity.tickCount;

		// Get entity state for animation selection
		String state = entity.getEntityState();

		// Only treat as attacking during actual attack swing
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;
		boolean isTransforming = entity.isTransformed();
		boolean isSpawning = tickCount < 120;
		boolean isLeaping = state.equals("leap");
		boolean isIdle = !isAttacking && (state.equals("idle") || state.isEmpty());

		// Spawn animation takes priority (first 120 ticks)
		if (isSpawning) {
			entity.animationState5.startIfStopped(tickCount);
		} else {
			entity.animationState5.stop();
		}

		// Transform animation when transformed
		if (isTransforming) {
			entity.animationState4.startIfStopped(tickCount);
		} else {
			entity.animationState4.stop();
		}

		// Leap animation during special move
		if (isLeaping) {
			entity.animationState3.startIfStopped(tickCount);
		} else {
			entity.animationState3.stop();
		}

		// Attack and Idle animations - mutually exclusive
		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else if (isIdle) {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		} else {
			// Neither attacking nor idle
			entity.animationState0.stop();
			entity.animationState2.stop();
		}
	}

	/**
	 * Update animation states for Skeleton Minion entity.
	 * Replaces CheckIsIdleAnimProcedure and related procedures.
	 */
	private static void updateSkeletonMinionAnimations(SkeletonMinionEntity entity) {
		int tickCount = entity.tickCount;

		// Only treat as attacking during actual attack swing
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;
		boolean isSpawning = tickCount < 120;

		// Spawn animation takes priority (first 120 ticks)
		if (isSpawning) {
			entity.animationState3.startIfStopped(tickCount);
		} else {
			entity.animationState3.stop();
		}

		// Attack and Idle animations - mutually exclusive
		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		}
	}

	/**
	 * Update animation states for Wraith entity.
	 * Controls idle, walk, attack, and death animations.
	 */
	private static void updateWraithAnimations(WraithEntity entity) {
		int tickCount = entity.tickCount;

		// Check if entity is dead
		boolean isDead = entity.isDeadOrDying();

		// Only treat as attacking during actual attack swing
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;

		// Death animation takes priority
		if (isDead) {
			entity.animationState3.startIfStopped(tickCount);
			entity.animationState0.stop();
			entity.animationState2.stop();
		} else {
			// Stop death animation if not dead
			entity.animationState3.stop();

			// Attack and Idle animations - mutually exclusive
			if (isAttacking) {
				entity.animationState2.startIfStopped(tickCount);
				entity.animationState0.stop();
			} else {
				entity.animationState0.startIfStopped(tickCount);
				entity.animationState2.stop();
			}
		}
		// Walk animation is handled by renderer's animateWalk
	}

	/**
	 * Update Ossukage server-side tick logic.
	 * This handles AI state transitions, attack timers, and special behaviors.
	 */
	private static void updateOssukageServerTick(RemnantOssukageEntity entity) {
		NinjaSkeletonOnEntityTickUpdateProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	/**
	 * Handle Ossukage entity taking damage.
	 * Triggers transformation when health threshold is reached.
	 */
	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity() instanceof RemnantOssukageEntity ossukage) {
			NinjaSkeletonEntityIsHurtProcedure.execute(ossukage.level(), ossukage.getX(), ossukage.getY(), ossukage.getZ(), ossukage);
		}
	}
}
