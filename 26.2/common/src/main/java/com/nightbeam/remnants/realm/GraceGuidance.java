package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.init.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The guidance of grace: after resting, a trail of golden motes drifts from the grace toward
 * the nearest Remnant arena. The structure search is cached per grace for ten minutes.
 */
public final class GraceGuidance {
	private static final long CACHE_TICKS = 20 * 60 * 10;
	private static final int STEPS = 18;
	private static final Map<GlobalPos, Cached> CACHE = new HashMap<>();

	private record Cached(Optional<BlockPos> target, long at) {
	}

	private GraceGuidance() {
	}

	public static void show(ServerPlayer player, ServerLevel level, BlockPos grace) {
		if (!EclipsedRealm.isRealm(level)) return;
		Optional<BlockPos> target = nearestArena(level, grace);
		if (target.isEmpty()) return;
		Vec3 from = Vec3.atCenterOf(grace).add(0, 0.6, 0);
		Vec3 to = Vec3.atCenterOf(target.get());
		Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
		if (flat.lengthSqr() < 1.0e-4) return;
		Vec3 dir = flat.normalize();
		for (int i = 0; i < STEPS; i++) {
			int step = i;
			RemnantBosses.queueServerWork(2 + i * 2, () -> {
				if (player.isRemoved() || player.level() != level) return;
				Vec3 p = from.add(dir.scale(1.0 + step * 0.85)).add(0, Math.sin(step * 0.45) * 0.25 + step * 0.04, 0);
				level.sendParticles(player, ModParticles.GRACE_MOTE.get(), true, false, p.x, p.y, p.z, 4, 0.06, 0.06, 0.06, 0.004);
			});
		}
	}

	private static Optional<BlockPos> nearestArena(ServerLevel level, BlockPos grace) {
		GlobalPos key = GlobalPos.of(level.dimension(), grace);
		long now = level.getGameTime();
		Cached cached = CACHE.get(key);
		if (cached != null && now - cached.at() < CACHE_TICKS && now >= cached.at()) return cached.target();
		int radius = (int) RealmConfig.value("guidance_radius_chunks", 0, 100);
		Optional<BlockPos> found = radius <= 0 ? Optional.empty()
				: Optional.ofNullable(level.findNearestMapStructure(EclipsedRealm.ARENAS, grace, radius, false));
		if (CACHE.size() > 512) CACHE.clear();
		CACHE.put(key, new Cached(found, now));
		return found;
	}
}
