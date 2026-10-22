package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Allegiance and hit-shape rules shared by the Hollow Sovereign, its helpers and the Hollow Legion. */
public final class HollowLegion {
	public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Constants.ENTITY_NAMESPACE, "hollow_legion"));

	private HollowLegion() {
	}

	/** Tagged Legion mobs, except spectral guards bound to a player's Sigil. */
	public static boolean isLegion(Entity entity) {
		if (entity == null || !entity.getType().is(TAG)) return false;
		return !(entity instanceof SpectralRoyalGuardEntity guard && guard.isPlayerAllied());
	}

	/** The Sovereign, its pauldrons and the Legion never harm one another. */
	public static boolean isSovereignSide(Entity entity) {
		return entity instanceof HollowSovereignEntity || entity instanceof SovereignPauldronEntity || isLegion(entity);
	}

	public static boolean isUntouchablePlayer(Entity entity) {
		return entity instanceof Player player && (player.isCreative() || player.isSpectator());
	}

	/** A living target the Sovereign's side may damage. */
	public static boolean sovereignVictim(LivingEntity entity) {
		return entity.isAlive() && entity.isAttackable() && !isSovereignSide(entity) && !isUntouchablePlayer(entity);
	}

	/** Tamed animals, Sigil guards and anything else that names a player as its owner. */
	public static boolean ownedByAPlayer(Entity entity) {
		return entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
	}

	/** Targets in front of {@code attacker} within {@code reach} and a horizontal arc of {@code arcDegrees}. */
	public static List<LivingEntity> arc(Mob attacker, double reach, double arcDegrees, Predicate<LivingEntity> victim) {
		Vec3 forward = Vec3.directionFromRotation(0, attacker.getYRot());
		double minDot = Math.cos(Math.toRadians(arcDegrees / 2));
		List<LivingEntity> hits = new ArrayList<>();
		AABB box = attacker.getBoundingBox().inflate(reach + 1, 1.5, reach + 1);
		for (LivingEntity entity : attacker.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != attacker && victim.test(e))) {
			Vec3 offset = entity.position().subtract(attacker.position()).multiply(1, 0, 1);
			double distance = offset.length() - entity.getBbWidth() / 2;
			if (distance > reach) continue;
			if (offset.lengthSqr() > 1.0E-4 && offset.normalize().dot(forward) < minDot && distance > attacker.getBbWidth() / 2) continue;
			if (!attacker.hasLineOfSight(entity)) continue;
			hits.add(entity);
		}
		return hits;
	}

	/** Targets inside a straight strip in front of {@code attacker}. */
	public static List<LivingEntity> line(Mob attacker, Vec3 origin, Vec3 direction, double length, double halfWidth,
			Predicate<LivingEntity> victim) {
		Vec3 dir = direction.multiply(1, 0, 1).normalize();
		List<LivingEntity> hits = new ArrayList<>();
		AABB box = new AABB(origin, origin.add(dir.scale(length))).inflate(halfWidth + 1, 2, halfWidth + 1);
		for (LivingEntity entity : attacker.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != attacker && victim.test(e))) {
			Vec3 offset = entity.position().subtract(origin).multiply(1, 0, 1);
			double along = offset.dot(dir);
			double side = offset.subtract(dir.scale(along)).length();
			if (along < -0.5 || along > length || side > halfWidth + entity.getBbWidth() / 2) continue;
			if (Math.abs(entity.getY() - origin.y) > 2.5) continue;
			hits.add(entity);
		}
		return hits;
	}

	/** Yaw (degrees, mob convention) that faces from {@code from} toward {@code to}. */
	public static float yawToward(Vec3 from, Vec3 to) {
		return (float) (Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0);
	}
}
