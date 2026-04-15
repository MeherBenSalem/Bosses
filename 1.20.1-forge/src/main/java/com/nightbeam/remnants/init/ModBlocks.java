package com.nightbeam.remnants.init;

import com.nightbeam.remnants.block.AncientPedestalBlock;
import com.nightbeam.remnants.block.AncientAltarBlock;
import com.nightbeam.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

public class ModBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RemnantBossesMod.MODID);

	public static final RegistryObject<Block> ANCIENT_ALTAR = BLOCKS.register("ancient_altar", () -> new AncientAltarBlock());
	public static final RegistryObject<Block> ANCIENT_PEDESTAL = BLOCKS.register("ancient_pedestal", () -> new AncientPedestalBlock());
}