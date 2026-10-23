package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.block.realm.WaygatePhase;
import com.nightbeam.remnants.block.realm.WaygateShape;
import com.nightbeam.remnants.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Arrays;
import java.util.Optional;

/**
 * Raises an Arrival Shrine on the Realm surface: template remnant_bosses:realm/arrival_shrine,
 * 11 x 9 x 11, walking floor at local y 0, an open Waygate facing south at (5, 1, 2) and a Grace Ember at (5, 1, 7).
 */
public final class ArrivalShrine {
	public static final int SIZE = 11;
	private static final int HALF = SIZE / 2;
	public static final BlockPos CORE = new BlockPos(5, 1, 2);
	public static final BlockPos GRACE = new BlockPos(5, 1, 7);

	private ArrivalShrine() {
	}

	/** Builds the shrine centred on (x, z) and returns its Waygate core. */
	public static BlockPos build(ServerLevel realm, int x, int z) {
		for (int cx = (x - HALF - 1) >> 4; cx <= (x + HALF + 1) >> 4; cx++) {
			for (int cz = (z - HALF - 1) >> 4; cz <= (z + HALF + 1) >> 4; cz++) realm.getChunk(cx, cz);
		}
		int floor = Mth.clamp(surface(realm, x, z), realm.getMinY() + 8, realm.getMaxY() + 1 - 12);
		BlockPos origin = new BlockPos(x - HALF, floor, z - HALF);

		Optional<StructureTemplate> template = realm.getStructureManager().get(EclipsedRealm.ARRIVAL_SHRINE);
		if (template.isPresent()) {
			template.get().placeInWorld(realm, origin, origin, new StructurePlaceSettings().setIgnoreEntities(true),
					realm.getRandom(), Block.UPDATE_CLIENTS);
		} else {
			Constants.LOG.warn("Arrival Shrine template {} is missing; building a plain platform", EclipsedRealm.ARRIVAL_SHRINE);
			plainPlatform(realm, origin);
		}
		fillFoundation(realm, origin);

		BlockPos core = origin.offset(CORE);
		if (!(realm.getBlockState(core).getBlock() instanceof EclipseWaygateBlock)) {
			realm.setBlock(core, ModBlocks.ECLIPSE_WAYGATE.get().defaultBlockState()
					.setValue(EclipseWaygateBlock.FACING, Direction.SOUTH)
					.setValue(EclipseWaygateBlock.PHASE, WaygatePhase.OPEN), Block.UPDATE_ALL);
			WaygateShape.placeParts(realm, core, Direction.SOUTH, true);
		}
		BlockPos grace = origin.offset(GRACE);
		if (!realm.getBlockState(grace).is(ModBlocks.GRACE_EMBER.get())) {
			realm.setBlock(grace, ModBlocks.GRACE_EMBER.get().defaultBlockState(), Block.UPDATE_ALL);
		}
		return core;
	}

	/** The median top-block height over the footprint, so a lone tree or pit does not decide the floor. */
	private static int surface(ServerLevel realm, int x, int z) {
		int[] samples = new int[9];
		int i = 0;
		for (int dx = -HALF; dx <= HALF; dx += HALF) {
			for (int dz = -HALF; dz <= HALF; dz += HALF) {
				samples[i++] = realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
			}
		}
		Arrays.sort(samples);
		return samples[4];
	}

	/** Eclipse stone under the floor down to solid ground (at most 16 blocks), so the shrine never floats. */
	private static void fillFoundation(ServerLevel realm, BlockPos origin) {
		BlockState stone = ModBlocks.ECLIPSE_STONE.get().defaultBlockState();
		for (int dx = 0; dx < SIZE; dx++) {
			for (int dz = 0; dz < SIZE; dz++) {
				if (!onPlatform(dx, dz)) continue;
				BlockPos.MutableBlockPos pos = origin.offset(dx, -1, dz).mutable();
				for (int depth = 0; depth < 16; depth++, pos.move(Direction.DOWN)) {
					BlockState state = realm.getBlockState(pos);
					if (!state.canBeReplaced() && state.getFluidState().isEmpty()) break;
					realm.setBlock(pos, stone, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	private static boolean onPlatform(int dx, int dz) {
		double ox = dx - HALF, oz = dz - HALF;
		return ox * ox + oz * oz <= (HALF + 0.5) * (HALF + 0.5);
	}

	/** Fallback when the template is missing: a round brick floor, cleared air above, the gate and the grace. */
	private static void plainPlatform(ServerLevel realm, BlockPos origin) {
		BlockState bricks = ModBlocks.ECLIPSE_STONE_BRICKS.get().defaultBlockState();
		BlockState chiseled = ModBlocks.CHISELED_ECLIPSE_STONE_BRICKS.get().defaultBlockState();
		for (int dx = 0; dx < SIZE; dx++) {
			for (int dz = 0; dz < SIZE; dz++) {
				if (!onPlatform(dx, dz)) continue;
				int ring = (int) Math.round(Math.sqrt((dx - HALF) * (dx - HALF) + (dz - HALF) * (dz - HALF)));
				realm.setBlock(origin.offset(dx, 0, dz), ring == 3 ? chiseled : bricks, Block.UPDATE_CLIENTS);
				for (int dy = 1; dy < 9; dy++) realm.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}
}
