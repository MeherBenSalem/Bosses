package com.nightbeam.remnants.init;

import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block entity types. Vanilla 1.20.1 keeps BlockEntityType's supplier interface package-private, so each loader
 * builds the type itself (Fabric's builder, Forge's access-transformed vanilla builder) and binds it here.
 */
public final class ModBlockEntities {
	public static final RegistryHolder<BlockEntityType<EclipseWaygateBlockEntity>> ECLIPSE_WAYGATE = new RegistryHolder<>("eclipse_waygate");

	private ModBlockEntities() {
	}
}
