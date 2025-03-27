
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

import com.naizo.ossukage.block.AncientRuinBlockBlock;
import com.naizo.ossukage.RemnantOssukageMod;

public class RemnantOssukageModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, RemnantOssukageMod.MODID);
	public static final RegistryObject<Block> ANCIENT_RUIN_BLOCK = REGISTRY.register("ancient_ruin_block", () -> new AncientRuinBlockBlock());
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
