package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModParticles;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A site of grace: rest, respawn, guidance and travel. Unbreakable outside creative. */
public class GraceEmberBlock extends Block {
	private static final VoxelShape OUTLINE = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(6.5, 0, 6.5, 9.5, 15, 9.5));
	private static final VoxelShape COLLISION = Block.box(2, 0, 2, 14, 3, 14);

	public GraceEmberBlock() {
		super(Properties.of().mapColor(MapColor.GOLD).strength(-1.0f, 3600000.0f).noLootTable()
				.sound(SoundType.AMETHYST).lightLevel(state -> 13).noOcclusion().pushReaction(PushReaction.BLOCK)
				.isValidSpawn((s, l, p, t) -> false));
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			GraceRegistry.use(serverPlayer, server, pos, hand);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double x = pos.getX() + 0.5, y = pos.getY() + 0.75, z = pos.getZ() + 0.5;
		for (int i = 0; i < 2; i++) {
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
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) GraceRegistry.removed(server, pos);
		super.onRemove(state, level, pos, newState, moving);
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
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}
}
