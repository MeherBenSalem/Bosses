package com.nightbeam.remnants.init;

import com.nightbeam.remnants.block.AncientAltarBlock;
import com.nightbeam.remnants.block.AncientPedestalBlock;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.level.block.Block;

public final class ModBlocks {
	public static final RegistryHolder<Block> ANCIENT_ALTAR = new RegistryHolder<>("ancient_altar");
	public static final RegistryHolder<Block> ANCIENT_PEDESTAL = new RegistryHolder<>("ancient_pedestal");

	private ModBlocks() {
	}

	public static AncientAltarBlock createAltar() {
		return new AncientAltarBlock();
	}

	public static AncientPedestalBlock createPedestal() {
		return new AncientPedestalBlock();
	}
}
