package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Factories for the Realm's building blocks, which are plain vanilla block types. */
public final class RealmBlocks {
	private RealmBlocks() {
	}

	private static Properties stone(float hardness) {
		return Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
				.requiresCorrectToolForDrops().strength(hardness, 6.0f).sound(SoundType.DEEPSLATE);
	}

	public static Block eclipseStone() {
		return new Block(stone(1.5f));
	}

	public static Block cobbledEclipseStone() {
		return new Block(stone(2.0f));
	}

	public static Block eclipseStoneBricks() {
		return new Block(stone(1.5f).sound(SoundType.DEEPSLATE_BRICKS));
	}

	public static Block brickStairs() {
		Block bricks = ModBlocks.ECLIPSE_STONE_BRICKS.get();
		// StairBlock's constructor is protected in vanilla 1.20.1.
		return new StairBlock(bricks.defaultBlockState(), Properties.ofFullCopy(bricks)) {
		};
	}

	public static Block brickSlab() {
		return new SlabBlock(Properties.ofFullCopy(ModBlocks.ECLIPSE_STONE_BRICKS.get()));
	}

	public static Block brickWall() {
		return new WallBlock(Properties.ofFullCopy(ModBlocks.ECLIPSE_STONE_BRICKS.get()).forceSolidOn());
	}

	/** Graceroot does not burn: the Gracetree's wood holds its light (no flammability is registered). */
	public static Block gracerootLog() {
		return new RotatedPillarBlock(Properties.of()
				.mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? MapColor.SAND : MapColor.TERRACOTTA_WHITE)
				.instrument(NoteBlockInstrument.BASS).strength(2.0f).sound(SoundType.WOOD));
	}

	public static Block gracerootPlanks() {
		return new Block(Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS)
				.strength(2.0f, 3.0f).sound(SoundType.WOOD));
	}

	public static Block gildedLeaves() {
		return new LeavesBlock(Properties.of().mapColor(MapColor.GOLD).strength(0.2f).randomTicks()
				.sound(SoundType.AZALEA_LEAVES).noOcclusion().lightLevel(state -> 6)
				.isValidSpawn((state, level, pos, type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
				.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)
				.isRedstoneConductor((state, level, pos) -> false).pushReaction(PushReaction.DESTROY));
	}
}
