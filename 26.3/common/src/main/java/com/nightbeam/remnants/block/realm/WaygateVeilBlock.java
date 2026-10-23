package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.realm.Waygates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import com.nightbeam.remnants.registry.RegistryKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Waygate's doorway: invisible, walk-through; touching it while the gate is open starts a crossing. */
public class WaygateVeilBlock extends Block {
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	public WaygateVeilBlock() {
		super(RegistryKeys.block().mapColor(MapColor.COLOR_BLACK).strength(6.0f, 1200.0f).sound(SoundType.AMETHYST)
				.noCollision().noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
				.isViewBlocking((s, l, p, box) -> false).isSuffocating((s, l, p) -> false)
				.isValidSpawn((s, l, p, t) -> false)
				.lightLevel(state -> state.getValue(OPEN) ? 12 : 0));
		registerDefaultState(stateDefinition.any().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return WaygateShape.findCore(level, pos).map(core -> Waygates.useItem(level, core, player, stack))
				.orElse(InteractionResult.TRY_WITH_EMPTY_HAND);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return WaygateShape.findCore(level, pos).map(core -> Waygates.use(level, core, player)).orElse(InteractionResult.PASS);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
		if (state.getValue(OPEN)) WaygateShape.findCore(level, pos).ifPresent(core -> Waygates.touch(level, core, entity));
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		Waygates.breakFromPart(level, pos, player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@SuppressWarnings("deprecation")
	/** Runs once the block is gone (it changed to another block), on the server. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Waygates.partRemoved(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@SuppressWarnings("deprecation")
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

}
