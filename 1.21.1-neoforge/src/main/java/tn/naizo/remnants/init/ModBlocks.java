package tn.naizo.remnants.init;

import tn.naizo.remnants.block.AncientPedestalBlock;
import tn.naizo.remnants.block.AncientAltarBlock;
import tn.naizo.remnants.RemnantBossesMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK,
			RemnantBossesMod.MODID);

	public static final DeferredHolder<Block, Block> ANCIENT_ALTAR = BLOCKS.register("ancient_altar",
			() -> new AncientAltarBlock());
	public static final DeferredHolder<Block, Block> ANCIENT_PEDESTAL = BLOCKS.register("ancient_pedestal",
			() -> new AncientPedestalBlock());
}
