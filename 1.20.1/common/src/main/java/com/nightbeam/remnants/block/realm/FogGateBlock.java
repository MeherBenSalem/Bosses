package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arena fog: solid to every entity but players, so a boss and its summons stay in their
 * arena while challengers walk through, slowed, into the fight.
 */
public class FogGateBlock extends Block {
	private static final Map<UUID, Long> LAST_PASS = new ConcurrentHashMap<>();

	public FogGateBlock() {
		super(Properties.of().mapColor(MapColor.GOLD).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.WOOL)
				.lightLevel(state -> 5).noOcclusion().pushReaction(PushReaction.BLOCK)
				.isValidSpawn((s, l, p, t) -> false).isSuffocating((s, l, p) -> false)
				.isViewBlocking((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false));
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player) return Shapes.empty();
		return Shapes.block();
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
		return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
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

	@SuppressWarnings("deprecation")
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide || !(entity instanceof Player player)) return;
		long now = level.getGameTime();
		Long last = LAST_PASS.get(player.getUUID());
		if (last != null && now - last < 30 && now >= last) return;
		LAST_PASS.put(player.getUUID(), now);
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 2, false, false, false));
		level.playSound(null, pos, ModSounds.FOG_GATE_PASS.get(), SoundSource.BLOCKS, 0.9f, 0.9f + level.random.nextFloat() * 0.2f);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) return;
		level.addParticle(ModParticles.GRACE_MOTE.get(), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble(), 0, 0.01, 0);
	}
}
