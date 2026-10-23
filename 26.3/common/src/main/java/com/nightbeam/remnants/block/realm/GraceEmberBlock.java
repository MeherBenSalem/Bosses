package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.registry.RegistryKeys;
import com.nightbeam.remnants.realm.GraceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A site of grace: rest, respawn, guidance and travel. Unbreakable outside creative. */
public class GraceEmberBlock extends Block implements EntityBlock {
	private static final VoxelShape OUTLINE = Block.box(2, 0, 2, 14, 5, 14);
	private static final VoxelShape COLLISION = Block.box(2, 0, 2, 14, 1, 14);

	public GraceEmberBlock() {
		super(RegistryKeys.block().mapColor(MapColor.GOLD).strength(-1.0f, 3600000.0f).noLootTable()
				.sound(SoundType.AMETHYST).lightLevel(state -> 13).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
				.isValidSpawn((s, l, p, t) -> false));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		// Resting works with anything in hand (a name tag renames); the off hand defers to the main hand.
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			GraceRegistry.use(serverPlayer, server, pos, hand);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			GraceRegistry.use(serverPlayer, server, pos, InteractionHand.MAIN_HAND);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * A grace saved before the glow renderer has no block entity, and a client only builds one when the block changes.
	 * Add it on the client and redraw the section so the renderer picks it up. Called from animateTick and on rest.
	 */
	public static void ensureBlockEntity(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof GraceEmberBlock)) return;
		if (level.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) instanceof GraceEmberBlockEntity) return;
		level.getBlockEntity(pos);
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_IMMEDIATE);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	/** Gives graces saved before the glow renderer their block entity on the server, so it is saved and sent with the chunk. */
	@SuppressWarnings("deprecation")
	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.getBlockEntity(pos);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		ensureBlockEntity(level, pos);
		double x = pos.getX() + 0.5, y = pos.getY() + 0.75, z = pos.getZ() + 0.5;
		for (int i = 0; i < 1; i++) {
			level.addParticle(ModParticles.GRACE_MOTE.get(), x + (random.nextDouble() - 0.5) * 0.35, y + random.nextDouble() * 0.3,
					z + (random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.01, 0.015 + random.nextDouble() * 0.02,
					(random.nextDouble() - 0.5) * 0.01);
		}
		if (random.nextInt(12) == 0) {
			level.addParticle(ParticleTypes.END_ROD, x, y + 0.2, z, 0, 0.03, 0);
		}
		if (random.nextInt(40) == 0) {
			level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.35f, 1.4f + random.nextFloat() * 0.3f, false);
		}
	}

	@SuppressWarnings("deprecation")
	/** Runs once the block is gone (it changed to another block), on the server. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		GraceRegistry.removed(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return OUTLINE;
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GraceEmberBlockEntity(pos, state);
	}
}
