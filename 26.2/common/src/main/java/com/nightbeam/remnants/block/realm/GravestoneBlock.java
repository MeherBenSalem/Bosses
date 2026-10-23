package com.nightbeam.remnants.block.realm;

import com.mojang.serialization.MapCodec;
import com.nightbeam.remnants.registry.RegistryKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A headstone found in the Realm's barrows and fields. */
public class GravestoneBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<GravestoneBlock> CODEC = simpleCodec(properties -> new GravestoneBlock());
	private static final VoxelShape NORTH_SOUTH = Block.box(3, 0, 5, 13, 13, 11);
	private static final VoxelShape EAST_WEST = Block.box(5, 0, 3, 11, 13, 13);

	public GravestoneBlock() {
		super(RegistryKeys.block().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
				.requiresCorrectToolForDrops().strength(1.5f, 6.0f).sound(SoundType.STONE)
				.noOcclusion().pushReaction(PushReaction.DESTROY));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
	}
}
