package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import com.nightbeam.remnants.block.realm.WaygatePhase;
import com.nightbeam.remnants.block.realm.WaygateShape;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server rules for Waygates: kindling with a shard, the kindle sequence, opening, touching and breaking. */
public final class Waygates {
	private Waygates() {
	}

	/** An item used on any part of the arch: an Eclipse Shard kindles a dormant gate. */
	public static InteractionResult useItem(Level level, BlockPos core, Player player, ItemStack held) {
		BlockState state = level.getBlockState(core);
		if (!(state.getBlock() instanceof EclipseWaygateBlock) || !held.is(ModItems.ECLIPSE_SHARD.get())) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		WaygatePhase phase = state.getValue(EclipseWaygateBlock.PHASE);
		if (phase != WaygatePhase.DORMANT) {
			if (!level.isClientSide()) message(player, phase == WaygatePhase.OPEN ? "open" : "kindling");
			return InteractionResult.SUCCESS;
		}
		if (level instanceof ServerLevel server) kindle(server, core, state, player, held);
		return InteractionResult.SUCCESS;
	}

	/** An empty-handed use: a dormant gate says what it needs. */
	public static InteractionResult use(Level level, BlockPos core, Player player) {
		BlockState state = level.getBlockState(core);
		if (state.getBlock() instanceof EclipseWaygateBlock && state.getValue(EclipseWaygateBlock.PHASE) == WaygatePhase.DORMANT) {
			if (!level.isClientSide()) message(player, "dormant");
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static void kindle(ServerLevel level, BlockPos core, BlockState state, Player player, ItemStack shard) {
		level.setBlock(core, state.setValue(EclipseWaygateBlock.PHASE, WaygatePhase.KINDLING), Block.UPDATE_ALL);
		if (level.getBlockEntity(core) instanceof EclipseWaygateBlockEntity gate) gate.startKindle();
		if (!player.getAbilities().instabuild) shard.shrink(1);
		level.playSound(null, core, ModSounds.WAYGATE_KINDLE.get(), SoundSource.BLOCKS, 2.0f, 1.0f);
		level.sendParticles(ModParticles.ECLIPSE_SPARK.get(), core.getX() + 0.5, core.getY() + 1.5, core.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.05);
		message(player, "kindle");
	}

	/** One tick of the kindle sequence: pillar light, motes drawn in, rune chimes. */
	public static void kindleTick(ServerLevel level, BlockPos core, BlockState state, int tick, int total) {
		Direction facing = state.getValue(EclipseWaygateBlock.FACING);
		float progress = Math.min(1.0f, tick / (float) total);
		if (tick % 2 == 0) {
			for (int side = -1; side <= 1; side += 2) {
				BlockPos base = WaygateShape.cell(core, facing, side, 0);
				double height = 4.2 * Math.min(1.0, progress * 1.6) * level.getRandom().nextDouble();
				level.sendParticles(ModParticles.ECLIPSE_SPARK.get(), base.getX() + 0.5, base.getY() + height, base.getZ() + 0.5,
						2, 0.2, 0.15, 0.2, 0.01);
			}
			double radius = 7.0 * (1.0 - progress) + 0.8;
			for (int i = 0; i < 10; i++) {
				double angle = i / 10.0 * Math.PI * 2 + tick * 0.08;
				level.sendParticles(ModParticles.GRACE_MOTE.get(), core.getX() + 0.5 + Math.cos(angle) * radius, core.getY() + 0.2,
						core.getZ() + 0.5 + Math.sin(angle) * radius, 1, 0, 0.02, 0, 0);
			}
		}
		int beat = Math.max(1, total / 6);
		if (tick % beat == 0 && tick < total) {
			level.playSound(null, core, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.6f, 0.6f + progress);
		}
		if (tick == Math.max(1, total - 20)) {
			Vec3 door = doorway(core);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, door.x, door.y, door.z, 70, 0.35, 0.8, 0.35, 0.06);
		}
	}

	/** The kindle ends: the eclipse fills the arch. */
	public static void open(ServerLevel level, BlockPos core, BlockState state) {
		level.setBlock(core, state.setValue(EclipseWaygateBlock.PHASE, WaygatePhase.OPEN), Block.UPDATE_ALL);
		WaygateShape.setVeilsOpen(level, core, state.getValue(EclipseWaygateBlock.FACING), true);
		level.playSound(null, core, ModSounds.WAYGATE_OPEN.get(), SoundSource.BLOCKS, 3.0f, 1.0f);
		Vec3 door = doorway(core);
		level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), door.x, door.y, door.z, 1, 0, 0, 0, 0);
		level.sendParticles(ModParticles.GRACE_MOTE.get(), door.x, door.y, door.z, 90, 0.7, 1.2, 0.7, 0.09);
		level.sendParticles(ModParticles.ECLIPSE_SPARK.get(), door.x, door.y, door.z, 60, 0.3, 1.0, 0.3, 0.3);
	}

	public static void touch(Level level, BlockPos core, Entity entity) {
		if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
		BlockState state = level.getBlockState(core);
		if (!(state.getBlock() instanceof EclipseWaygateBlock) || state.getValue(EclipseWaygateBlock.PHASE) != WaygatePhase.OPEN) return;
		RealmTravel.beginWaygate(player, server, core, state.getValue(EclipseWaygateBlock.FACING));
	}

	/** A player breaks a pillar or the doorway: the whole arch comes down, dropping the Waygate unless creative. */
	public static void breakFromPart(Level level, BlockPos part, Player player) {
		if (level.isClientSide()) return;
		WaygateShape.findCore(level, part).ifPresent(core -> level.destroyBlock(core, !player.getAbilities().instabuild, player));
	}

	/** A part vanished some other way (explosion, command): take the arch down with its drop. */
	public static void partRemoved(Level level, BlockPos part) {
		if (level.isClientSide()) return;
		WaygateShape.findCore(level, part).ifPresent(core -> level.destroyBlock(core, true));
	}

	public static Vec3 doorway(BlockPos core) {
		return new Vec3(core.getX() + 0.5, core.getY() + 1.6, core.getZ() + 0.5);
	}

	private static void message(Player player, String key) {
		player.sendOverlayMessage(Component.translatable("message.remnant_bosses.waygate." + key));
	}
}
