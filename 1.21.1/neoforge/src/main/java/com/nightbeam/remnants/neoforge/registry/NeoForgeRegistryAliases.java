package com.nightbeam.remnants.neoforge.registry;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;

/**
 * Registry-level fallback through NeoForge's native {@code Registry#addAlias}: nested item
 * components, commands and loot resolve retired names to their successors. Runs at the lowest
 * priority of this mod's RegisterEvent so its own entries are already present, and adds only the
 * aliases whose source is unregistered and whose resolved target is registered. NeoForge itself
 * never redirects a name that is registered.
 */
public final class NeoForgeRegistryAliases {
	private NeoForgeRegistryAliases() {
	}

	public static void onRegister(RegisterEvent event) {
		apply(event, Registries.ENTITY_TYPE, RegistryAliases.ENTITIES);
		apply(event, Registries.ITEM, RegistryAliases.ITEMS);
	}

	private static <T> void apply(RegisterEvent event, ResourceKey<? extends Registry<T>> key, Map<ResourceLocation, ResourceLocation> aliases) {
		Registry<T> registry = event.getRegistry(key);
		if (registry == null) return;
		RegistryAliases.applicable(aliases, registry::containsKey).forEach((from, to) -> {
			try {
				registry.addAlias(from, to);
			} catch (IllegalStateException conflict) {
				Constants.LOG.warn("Could not alias retired ID {} to {}: {}", from, to, conflict.getMessage());
			}
		});
	}
}
