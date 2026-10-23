package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModBlockEntities;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import com.nightbeam.remnants.registry.RegistryKeys;
import com.nightbeam.remnants.realm.RealmLinks;
import com.nightbeam.remnants.realm.Waygates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/** The Waygate core: bottom centre of the arch, owner of the block entity that renders and kindles it. */
public class EclipseWaygateBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<WaygatePhase> PHASE = EnumProperty.create("phase", WaygatePhase.class);
	private static final VoxelShape PLINTH = Block.box(0, 0, 0, 16, 3, 16);

	public EclipseWaygateBlock() {
		super(RegistryKeys.block().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
				.requiresCorrectToolForDrops().strength(6.0f, 1200.0f).sound(SoundType.DEEPSLATE_BRICKS)
				.noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
				.lightLevel(state -> state.getValue(PHASE).light()));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PHASE, WaygatePhase.DORMANT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PHASE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Level level = context.getLevel();
		if (!WaygateShape.canPlace(level, context.getClickedPos(), facing)) {
			if (!level.isClientSide() && context.getPlayer() != null) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.remnant_bosses.waygate.space"));
			}
			return null;
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			WaygateShape.placeParts(level, pos, state.getValue(FACING), state.getValue(PHASE) == WaygatePhase.OPEN);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			net.minecraft.world.entity.player.Player player, InteractionHand hand, BlockHitResult hit) {
		return Waygates.useItem(level, pos, player, stack);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			net.minecraft.world.entity.player.Player player, BlockHitResult hit) {
		return Waygates.use(level, pos, player);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
		Waygates.touch(level, pos, entity);
	}

	@SuppressWarnings("deprecation")
	/** Runs once the block is gone (it changed to another block), on the server. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		WaygateShape.removeParts(level, pos, state.getValue(FACING));
		RealmLinks.get(level.getServer()).forget(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PLINTH;
	}

	@SuppressWarnings("deprecation")
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EclipseWaygateBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModBlockEntities.ECLIPSE_WAYGATE.get(),
				level.isClientSide() ? EclipseWaygateBlockEntity::clientTick : EclipseWaygateBlockEntity::serverTick);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
