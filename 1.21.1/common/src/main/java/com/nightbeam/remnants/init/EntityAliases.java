package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class EntityAliases {
	public record Alias(ResourceLocation from, ResourceLocation to) {
	}

	public static final List<Alias> ALL = List.of(
			alias("remnant_bosses:kunai", "remnants:kunai"),
			alias("remnant_bosses:rat", "remnants:rat"),
			alias("remnant_bosses:skeleton_minion", "remnants:skeleton_minion"),
			alias("remnant_bosses:remnant_ossukage", "remnants:ossukage"),
			alias("remnant_bosses:ossukage_rune_effect", "remnants:ossukage_rune_effect"),
			alias("remnant_bosses:wraith", "remnants:wraith"),
			alias("remnant_bosses:armored_grub", "remnants:armored_grub"),
			alias("remnant_bosses:umbrakar", "remnants:umbrakar"),
			alias("remnant_bosses:umbrakar_orb", "remnants:umbrakar_orb"));

	private EntityAliases() {
	}

	private static Alias alias(String from, String to) {
		return new Alias(ResourceLocation.parse(from), ResourceLocation.parse(to));
	}

	public static ResourceLocation remnants(String path) {
		return ResourceLocation.fromNamespaceAndPath(Constants.ENTITY_NAMESPACE, path);
	}

	public static ResourceLocation remnantBosses(String path) {
		return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
	}
}
