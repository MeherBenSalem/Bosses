
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.block.AncientRuinBlockBlock;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

public class RemnantBossesModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, RemnantBossesMod.MODID);
	public static final RegistryObject<Block> ANCIENT_RUIN_BLOCK = REGISTRY.register("ancient_ruin_block", () -> new AncientRuinBlockBlock());
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
