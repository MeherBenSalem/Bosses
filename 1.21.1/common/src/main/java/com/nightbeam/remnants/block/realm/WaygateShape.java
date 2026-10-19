package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Waygate's 3 x 4 arch around its core. Offsets are (along {@code facing.getClockWise()}, up):
 * frame on both pillars and the lintel, veil in the doorway above the core.
 */
public final class WaygateShape {
	private static final int[][] FRAME = {{-1, 0}, {-1, 1}, {-1, 2}, {-1, 3}, {1, 0}, {1, 1}, {1, 2}, {1, 3}, {0, 3}};
	private static final int[][] VEIL = {{0, 1}, {0, 2}};

	private WaygateShape() {
	}

	public static BlockPos cell(BlockPos core, Direction facing, int right, int up) {
		return core.relative(facing.getClockWise(), right).above(up);
	}

	public static List<BlockPos> frame(BlockPos core, Direction facing) {
		return cells(core, facing, FRAME);
	}

	public static List<BlockPos> veil(BlockPos core, Direction facing) {
		return cells(core, facing, VEIL);
	}

	/** Every position the arch occupies, core first. */
	public static List<BlockPos> all(BlockPos core, Direction facing) {
		List<BlockPos> out = new ArrayList<>(12);
		out.add(core);
		out.addAll(frame(core, facing));
		out.addAll(veil(core, facing));
		return out;
	}

	private static List<BlockPos> cells(BlockPos core, Direction facing, int[][] offsets) {
		List<BlockPos> out = new ArrayList<>(offsets.length);
		for (int[] o : offsets) out.add(cell(core, facing, o[0], o[1]));
		return out;
	}

	/** Whether every cell is free: replaceable, inside the build height and the world border. */
	public static boolean canPlace(Level level, BlockPos core, Direction facing) {
		for (BlockPos pos : all(core, facing)) {
			if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return false;
			if (!level.getWorldBorder().isWithinBounds(pos)) return false;
			if (!pos.equals(core) && !level.getBlockState(pos).canBeReplaced()) return false;
		}
		return true;
	}

	/** Raise the pillars, lintel and doorway around a freshly placed core. */
	public static void placeParts(LevelAccessor level, BlockPos core, Direction facing, boolean open) {
		BlockState frame = ModBlocks.WAYGATE_FRAME.get().defaultBlockState();
		BlockState veil = ModBlocks.WAYGATE_VEIL.get().defaultBlockState().setValue(WaygateVeilBlock.OPEN, open);
		for (BlockPos pos : frame(core, facing)) level.setBlock(pos, frame, Block.UPDATE_ALL);
		for (BlockPos pos : veil(core, facing)) level.setBlock(pos, veil, Block.UPDATE_ALL);
	}

	/** Clear the remaining parts once the core is gone (no drops: only the core has loot). */
	public static void removeParts(LevelAccessor level, BlockPos core, Direction facing) {
		for (BlockPos pos : frame(core, facing)) {
			if (level.getBlockState(pos).is(ModBlocks.WAYGATE_FRAME.get())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (BlockPos pos : veil(core, facing)) {
			if (level.getBlockState(pos).is(ModBlocks.WAYGATE_VEIL.get())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	public static void setVeilsOpen(LevelAccessor level, BlockPos core, Direction facing, boolean open) {
		for (BlockPos pos : veil(core, facing)) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlocks.WAYGATE_VEIL.get()) && state.getValue(WaygateVeilBlock.OPEN) != open) {
				level.setBlock(pos, state.setValue(WaygateVeilBlock.OPEN, open), Block.UPDATE_ALL);
			}
		}
	}

	/** The core whose arch contains {@code part}, if any. */
	public static Optional<BlockPos> findCore(LevelAccessor level, BlockPos part) {
		for (int dy = 0; dy <= 3; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos candidate = part.offset(dx, -dy, dz);
					BlockState state = level.getBlockState(candidate);
					if (state.getBlock() instanceof EclipseWaygateBlock
							&& all(candidate, state.getValue(EclipseWaygateBlock.FACING)).contains(part)) {
						return Optional.of(candidate);
					}
				}
			}
		}
		return Optional.empty();
	}
}
