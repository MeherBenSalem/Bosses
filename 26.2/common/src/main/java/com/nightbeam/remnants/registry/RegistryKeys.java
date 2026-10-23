package com.nightbeam.remnants.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Properties that carry the registry key of the entry being built: items, blocks and entity types must know
 * their id before they are constructed. Only valid inside a {@link RegistryHolder} factory.
 */
public final class RegistryKeys {
	private RegistryKeys() {
	}

	public static Item.Properties item() {
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, RegistryHolder.building()));
	}

	/** A block's item, named after the block. */
	public static Item.Properties blockItem() {
		return item().useBlockDescriptionPrefix();
	}

	public static BlockBehaviour.Properties block() {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, RegistryHolder.building()));
	}

	public static BlockBehaviour.Properties copy(Block block) {
		return BlockBehaviour.Properties.ofFullCopy(block).setId(ResourceKey.create(Registries.BLOCK, RegistryHolder.building()));
	}

	public static ResourceKey<EntityType<?>> entity() {
		return ResourceKey.create(Registries.ENTITY_TYPE, RegistryHolder.building());
	}
}
