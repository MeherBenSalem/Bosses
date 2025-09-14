
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.block.AncientPedestalBlock;
import tn.naizo.remnants.block.AncientAltarBlock;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

public class RemnantBossesModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, RemnantBossesMod.MODID);
	public static final RegistryObject<Block> ANCIENT_ALTAR = REGISTRY.register("ancient_altar", () -> new AncientAltarBlock());
	public static final RegistryObject<Block> ANCIENT_PEDESTAL = REGISTRY.register("ancient_pedestal", () -> new AncientPedestalBlock());
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
