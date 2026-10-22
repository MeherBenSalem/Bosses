package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.List;
import java.util.Map;

/**
 * Forge wraps the vanilla registries in ForgeRegistry, so name lookups never reach MappedRegistry.
 * Instead, retired entity and item IDs recorded in a save's registry snapshot are remapped to their
 * resolved successor; Forge then aliases the old name for every later lookup (commands, loot, nested
 * stacks). Same guard as everywhere else: only when the resolved target is registered. Retired
 * helpers have no alias and keep Forge's default missing-entry handling.
 */
public final class ForgeRegistryAliases {
	private ForgeRegistryAliases() {
	}

	public static void onMissingMappings(MissingMappingsEvent event) {
		remap(event, ForgeRegistries.Keys.ENTITY_TYPES, ForgeRegistries.ENTITY_TYPES, RegistryAliases.ENTITIES);
		remap(event, ForgeRegistries.Keys.ITEMS, ForgeRegistries.ITEMS, RegistryAliases.ITEMS);
	}

	private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> key, IForgeRegistry<T> registry,
			Map<ResourceLocation, ResourceLocation> aliases) {
		for (String namespace : List.of(Constants.ENTITY_NAMESPACE, Constants.MOD_ID)) {
			for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(key, namespace)) {
				RegistryAliases.resolve(aliases, mapping.getKey(), registry::containsKey).ifPresent(target -> {
					T value = registry.getValue(target);
					if (value == null) return;
					mapping.remap(value);
					Constants.LOG.info("Remapped retired registry entry {} to {}", mapping.getKey(), target);
				});
			}
		}
	}
}
