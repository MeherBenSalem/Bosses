package com.nightbeam.remnants.init;

import com.nightbeam.remnants.block.AncientAltarBlock;
import com.nightbeam.remnants.block.AncientPedestalBlock;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.block.realm.FogGateBlock;
import com.nightbeam.remnants.block.realm.GraceEmberBlock;
import com.nightbeam.remnants.block.realm.GravestoneBlock;
import com.nightbeam.remnants.block.realm.RealmBlocks;
import com.nightbeam.remnants.block.realm.WaygateFrameBlock;
import com.nightbeam.remnants.block.realm.WaygateVeilBlock;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class ModBlocks {
	public static final RegistryHolder<Block> ANCIENT_ALTAR = new RegistryHolder<>("ancient_altar", AncientAltarBlock::new);
	public static final RegistryHolder<Block> ANCIENT_PEDESTAL = new RegistryHolder<>("ancient_pedestal", AncientPedestalBlock::new);

	// The Eclipsed Realm.
	public static final RegistryHolder<Block> ECLIPSE_STONE = new RegistryHolder<>("eclipse_stone", RealmBlocks::eclipseStone);
	public static final RegistryHolder<Block> COBBLED_ECLIPSE_STONE = new RegistryHolder<>("cobbled_eclipse_stone", RealmBlocks::cobbledEclipseStone);
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICKS = new RegistryHolder<>("eclipse_stone_bricks", RealmBlocks::eclipseStoneBricks);
	public static final RegistryHolder<Block> CRACKED_ECLIPSE_STONE_BRICKS = new RegistryHolder<>("cracked_eclipse_stone_bricks", RealmBlocks::eclipseStoneBricks);
	public static final RegistryHolder<Block> CHISELED_ECLIPSE_STONE_BRICKS = new RegistryHolder<>("chiseled_eclipse_stone_bricks", RealmBlocks::eclipseStoneBricks);
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICK_STAIRS = new RegistryHolder<>("eclipse_stone_brick_stairs", RealmBlocks::brickStairs);
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICK_SLAB = new RegistryHolder<>("eclipse_stone_brick_slab", RealmBlocks::brickSlab);
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICK_WALL = new RegistryHolder<>("eclipse_stone_brick_wall", RealmBlocks::brickWall);
	public static final RegistryHolder<Block> GRACEROOT_LOG = new RegistryHolder<>("graceroot_log", RealmBlocks::gracerootLog);
	public static final RegistryHolder<Block> GRACEROOT_PLANKS = new RegistryHolder<>("graceroot_planks", RealmBlocks::gracerootPlanks);
	public static final RegistryHolder<Block> GILDED_LEAVES = new RegistryHolder<>("gilded_leaves", RealmBlocks::gildedLeaves);
	public static final RegistryHolder<Block> WEATHERED_GRAVESTONE = new RegistryHolder<>("weathered_gravestone", GravestoneBlock::new);
	public static final RegistryHolder<Block> GRACE_EMBER = new RegistryHolder<>("grace_ember", GraceEmberBlock::new);
	public static final RegistryHolder<Block> ECLIPSE_WAYGATE = new RegistryHolder<>("eclipse_waygate", EclipseWaygateBlock::new);
	public static final RegistryHolder<Block> WAYGATE_FRAME = new RegistryHolder<>("waygate_frame", WaygateFrameBlock::new);
	public static final RegistryHolder<Block> WAYGATE_VEIL = new RegistryHolder<>("waygate_veil", WaygateVeilBlock::new);
	public static final RegistryHolder<Block> FOG_GATE = new RegistryHolder<>("fog_gate", FogGateBlock::new);

	/**
	 * A Realm block and its item: {@code itemRarity} null means no item (waygate parts). Loaders register
	 * {@link #REALM_BLOCKS} in order, so a block registers after the blocks its factory reads.
	 */
	public record Entry(RegistryHolder<Block> holder, Rarity itemRarity, int loreLines) {
	}

	public static final List<Entry> REALM_BLOCKS = List.of(
			new Entry(ECLIPSE_STONE, Rarity.COMMON, 0),
			new Entry(COBBLED_ECLIPSE_STONE, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICKS, Rarity.COMMON, 0),
			new Entry(CRACKED_ECLIPSE_STONE_BRICKS, Rarity.COMMON, 0),
			new Entry(CHISELED_ECLIPSE_STONE_BRICKS, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICK_STAIRS, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICK_SLAB, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICK_WALL, Rarity.COMMON, 0),
			new Entry(GRACEROOT_LOG, Rarity.COMMON, 0),
			new Entry(GRACEROOT_PLANKS, Rarity.COMMON, 0),
			new Entry(GILDED_LEAVES, Rarity.COMMON, 0),
			new Entry(WEATHERED_GRAVESTONE, Rarity.COMMON, 0),
			new Entry(GRACE_EMBER, Rarity.EPIC, 2),
			new Entry(ECLIPSE_WAYGATE, Rarity.RARE, 3),
			new Entry(WAYGATE_FRAME, null, 0),
			new Entry(WAYGATE_VEIL, null, 0),
			new Entry(FOG_GATE, Rarity.EPIC, 1));

	private ModBlocks() {
	}
}
