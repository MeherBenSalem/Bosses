package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds no data: it gives the grace a block-entity renderer for its flame, pool of light and beam. */
public class GraceEmberBlockEntity extends BlockEntity {
	public GraceEmberBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GRACE_EMBER.get(), pos, state);
	}

	/** Forge culls block entities by this box; the light pool spreads past the block and the beam rises 8 up. */
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 10.0, 0.0);
	}
}
