package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.realm.Waygates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Invisible pillar and lintel filler of a Waygate (the core's renderer draws the arch). Breaking or using it
 * acts on the core.
 */
public class WaygateFrameBlock extends Block {
	public WaygateFrameBlock() {
		super(Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
				.requiresCorrectToolForDrops().strength(6.0f, 1200.0f).sound(SoundType.DEEPSLATE_BRICKS)
				.noOcclusion().pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, p) -> false)
				.isSuffocating((s, l, p) -> false).isValidSpawn((s, l, p, t) -> false));
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return WaygateShape.findCore(level, pos).map(core -> Waygates.use(level, core, player, hand)).orElse(InteractionResult.PASS);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		Waygates.breakFromPart(level, pos, player);
		super.playerWillDestroy(level, pos, state, player);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock())) Waygates.partRemoved(level, pos);
		super.onRemove(state, level, pos, newState, moving);
	}

	@SuppressWarnings("deprecation")
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@SuppressWarnings("deprecation")
	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0f;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}
}
