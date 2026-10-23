package com.nightbeam.remnants.client.realm;

import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import com.nightbeam.remnants.block.realm.WaygatePhase;
import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Client-side Waygate ambience: kindle shake, the open flash, the doorway's swirl and its hum. */
public final class WaygateClientFx {
	private static final double SHAKE_RANGE = 24;
	private static final double FLASH_RANGE = 32;

	private WaygateClientFx() {
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EclipseWaygateBlockEntity gate) {
		WaygatePhase phase = state.getValue(EclipseWaygateBlock.PHASE);
		Direction facing = state.getValue(EclipseWaygateBlock.FACING);
		Player player = Minecraft.getInstance().player;
		Vec3 door = new Vec3(pos.getX() + 0.5, pos.getY() + 1.55, pos.getZ() + 0.5);
		double distance = player == null ? Double.MAX_VALUE : player.position().distanceTo(door);
		if (gate.clientPhase != phase) {
			if (gate.clientPhase == WaygatePhase.KINDLING && phase == WaygatePhase.OPEN && distance < FLASH_RANGE) {
				float near = (float) (1 - distance / FLASH_RANGE);
				RealmOverlay.flash(0.25f + 0.6f * near);
				ScreenShake.add(0.5f + 0.4f * near);
			}
			gate.clientPhase = phase;
			gate.clientKindleTicks = 0;
		}
		RandomSource random = level.getRandom();
		Direction right = facing.getClockWise();
		switch (phase) {
			case DORMANT -> {
				if (random.nextInt(14) == 0) {
					double side = random.nextBoolean() ? 1 : -1;
					level.addParticle(ModParticles.GRACE_MOTE.get(), door.x + right.getStepX() * side, door.y + random.nextDouble() * 1.8,
							door.z + right.getStepZ() * side, 0, 0.01, 0);
				}
			}
			case KINDLING -> {
				gate.clientKindleTicks++;
				float progress = Math.min(1, gate.clientKindleTicks / (float) RealmConfig.kindleTicks());
				if (distance < SHAKE_RANGE) ScreenShake.atLeast((0.18f + 0.42f * progress) * (float) (1 - distance / SHAKE_RANGE));
				swirl(level, door, right, random, 1 + (int) (progress * 3), 1.4 - progress * 0.6);
			}
			case OPEN -> {
				swirl(level, door, right, random, 2, 0.9);
				if ((level.getGameTime() + Math.floorMod(pos.asLong(), 80)) % 80 == 0 && distance < 20) {
					level.playLocalSound(door.x, door.y, door.z, ModSounds.WAYGATE_HUM.get(), SoundSource.BLOCKS, 0.55f, 1.0f, false);
				}
			}
		}
	}

	/** Sparks drawn inward on a spiral across the doorway plane. */
	private static void swirl(Level level, Vec3 door, Direction right, RandomSource random, int count, double radius) {
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double r = radius * (0.6 + random.nextDouble() * 0.4);
			double side = Math.cos(angle) * r * 0.55, up = Math.sin(angle) * r;
			double x = door.x + right.getStepX() * side, y = door.y + up, z = door.z + right.getStepZ() * side;
			double inward = 0.06;
			double tangent = 0.05;
			double vSide = -Math.cos(angle) * inward - Math.sin(angle) * tangent;
			double vUp = -Math.sin(angle) * inward + Math.cos(angle) * tangent;
			level.addParticle(ModParticles.ECLIPSE_SPARK.get(), x, y, z, right.getStepX() * vSide, vUp, right.getStepZ() * vSide);
		}
	}
}
