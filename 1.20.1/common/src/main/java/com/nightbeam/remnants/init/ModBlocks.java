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
import java.util.function.Supplier;

public final class ModBlocks {
	public static final RegistryHolder<Block> ANCIENT_ALTAR = new RegistryHolder<>("ancient_altar");
	public static final RegistryHolder<Block> ANCIENT_PEDESTAL = new RegistryHolder<>("ancient_pedestal");

	// The Eclipsed Realm.
	public static final RegistryHolder<Block> ECLIPSE_STONE = new RegistryHolder<>("eclipse_stone");
	public static final RegistryHolder<Block> COBBLED_ECLIPSE_STONE = new RegistryHolder<>("cobbled_eclipse_stone");
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICKS = new RegistryHolder<>("eclipse_stone_bricks");
	public static final RegistryHolder<Block> CRACKED_ECLIPSE_STONE_BRICKS = new RegistryHolder<>("cracked_eclipse_stone_bricks");
	public static final RegistryHolder<Block> CHISELED_ECLIPSE_STONE_BRICKS = new RegistryHolder<>("chiseled_eclipse_stone_bricks");
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICK_STAIRS = new RegistryHolder<>("eclipse_stone_brick_stairs");
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICK_SLAB = new RegistryHolder<>("eclipse_stone_brick_slab");
	public static final RegistryHolder<Block> ECLIPSE_STONE_BRICK_WALL = new RegistryHolder<>("eclipse_stone_brick_wall");
	public static final RegistryHolder<Block> GRACEROOT_LOG = new RegistryHolder<>("graceroot_log");
	public static final RegistryHolder<Block> GRACEROOT_PLANKS = new RegistryHolder<>("graceroot_planks");
	public static final RegistryHolder<Block> GILDED_LEAVES = new RegistryHolder<>("gilded_leaves");
	public static final RegistryHolder<Block> WEATHERED_GRAVESTONE = new RegistryHolder<>("weathered_gravestone");
	public static final RegistryHolder<Block> GRACE_EMBER = new RegistryHolder<>("grace_ember");
	public static final RegistryHolder<Block> ECLIPSE_WAYGATE = new RegistryHolder<>("eclipse_waygate");
	public static final RegistryHolder<Block> WAYGATE_FRAME = new RegistryHolder<>("waygate_frame");
	public static final RegistryHolder<Block> WAYGATE_VEIL = new RegistryHolder<>("waygate_veil");
	public static final RegistryHolder<Block> FOG_GATE = new RegistryHolder<>("fog_gate");

	/**
	 * A Realm block, its factory and its item: {@code itemRarity} null means no item (waygate parts). Loaders
	 * register {@link #REALM_BLOCKS} in order, so a block registers after the blocks its factory reads.
	 */
	public record Entry(RegistryHolder<Block> holder, Supplier<Block> factory, Rarity itemRarity, int loreLines) {
	}

	public static final List<Entry> REALM_BLOCKS = List.of(
			new Entry(ECLIPSE_STONE, RealmBlocks::eclipseStone, Rarity.COMMON, 0),
			new Entry(COBBLED_ECLIPSE_STONE, RealmBlocks::cobbledEclipseStone, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICKS, RealmBlocks::eclipseStoneBricks, Rarity.COMMON, 0),
			new Entry(CRACKED_ECLIPSE_STONE_BRICKS, RealmBlocks::eclipseStoneBricks, Rarity.COMMON, 0),
			new Entry(CHISELED_ECLIPSE_STONE_BRICKS, RealmBlocks::eclipseStoneBricks, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICK_STAIRS, RealmBlocks::brickStairs, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICK_SLAB, RealmBlocks::brickSlab, Rarity.COMMON, 0),
			new Entry(ECLIPSE_STONE_BRICK_WALL, RealmBlocks::brickWall, Rarity.COMMON, 0),
			new Entry(GRACEROOT_LOG, RealmBlocks::gracerootLog, Rarity.COMMON, 0),
			new Entry(GRACEROOT_PLANKS, RealmBlocks::gracerootPlanks, Rarity.COMMON, 0),
			new Entry(GILDED_LEAVES, RealmBlocks::gildedLeaves, Rarity.COMMON, 0),
			new Entry(WEATHERED_GRAVESTONE, GravestoneBlock::new, Rarity.COMMON, 0),
			new Entry(GRACE_EMBER, GraceEmberBlock::new, Rarity.EPIC, 2),
			new Entry(ECLIPSE_WAYGATE, EclipseWaygateBlock::new, Rarity.RARE, 3),
			new Entry(WAYGATE_FRAME, WaygateFrameBlock::new, null, 0),
			new Entry(WAYGATE_VEIL, WaygateVeilBlock::new, null, 0),
			new Entry(FOG_GATE, FogGateBlock::new, Rarity.EPIC, 1));

	private ModBlocks() {
	}

	public static AncientAltarBlock createAltar() {
		return new AncientAltarBlock();
	}

	public static AncientPedestalBlock createPedestal() {
		return new AncientPedestalBlock();
	}
}
