
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import com.naizo.ossukage.block.AncientRuinBlockBlock;
import com.naizo.ossukage.RemnantOssukageMod;

public class RemnantOssukageModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(RemnantOssukageMod.MODID);
	public static final DeferredBlock<Block> ANCIENT_RUIN_BLOCK = REGISTRY.register("ancient_ruin_block", AncientRuinBlockBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
