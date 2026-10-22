package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Save-migration aliases for retired entity and item IDs.
 *
 * <p>Core rule: an alias from {@code S} is applied only when {@code S} is absent from the live
 * registry <em>and</em> the resolved target is present. Resolution follows chains
 * ({@code A -> B -> C}) and stops at the first ID that is registered, so a source or intermediate ID
 * that is still live is never redirected. When no ID along the chain is registered nothing is
 * rewritten and vanilla's unknown-ID handling applies. Aliases whose targets are not registered yet are
 * therefore inert.
 */
public final class RegistryAliases {
	/** Old entity ID to successor entity ID. Pre-2.4.1 {@code remnant_bosses:} IDs chain through it. */
	public static final Map<ResourceLocation, ResourceLocation> ENTITIES;
	/** Old item ID to successor item ID. Successors may be vanilla items. */
	public static final Map<ResourceLocation, ResourceLocation> ITEMS;
	/**
	 * Short-lived helper entities (projectiles and effects) that are dropped instead of migrated.
	 * They are deliberately absent from {@link #ENTITIES}: once unregistered, vanilla logs and skips
	 * them like any unknown entity. Listed so regressions can prove nothing aliases to or from them.
	 */
	public static final Set<ResourceLocation> RETIRED_ENTITIES;

	private static final ResourceLocation ENTITY_TYPE_REGISTRY = ResourceLocation.withDefaultNamespace("entity_type");
	private static final ResourceLocation ITEM_REGISTRY = ResourceLocation.withDefaultNamespace("item");
	private static final String ENTITY_PREFIX = Constants.ENTITY_NAMESPACE + ":";
	private static final String MOD_PREFIX = Constants.MOD_ID + ":";
	private static final Predicate<ResourceLocation> LIVE_ENTITY = id -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
	private static final Predicate<ResourceLocation> LIVE_ITEM = id -> BuiltInRegistries.ITEM.containsKey(id);
	private static final Set<ResourceLocation> LOGGED = ConcurrentHashMap.newKeySet();

	static {
		Map<ResourceLocation, ResourceLocation> entities = new LinkedHashMap<>();
		// Remnants redesign.
		alias(entities, "remnants:skeleton_melee", "remnants:hollow_sentinel");
		alias(entities, "remnants:skeleton_archer", "remnants:hollow_arbalist");
		alias(entities, "remnants:kotsukage", "remnants:gravebound_warden");
		alias(entities, "remnants:skeleton_minion", "remnants:gravebound_soldier");
		alias(entities, "remnants:grave_skitter", "remnants:crypt_lurker");
		alias(entities, "remnants:rat", "remnants:catacomb_rat");
		alias(entities, "remnants:umbrakar", "remnants:forgotten_watcher");
		alias(entities, "remnants:wraith", "remnants:memory_wraith");
		alias(entities, "remnants:dirge_lantern", "remnants:soul_collector");
		alias(entities, "remnants:ossukage", "remnants:eclipsed_knight");
		alias(entities, "remnants:armored_grub", "remnants:ashbound_crawler");
		alias(entities, "remnants:sickleghast", "remnants:relic_stalker");
		// Pre-2.4.1 entity namespace. These were declared in 2.4.1 but never wired into a loader.
		alias(entities, "remnant_bosses:rat", "remnants:rat");
		alias(entities, "remnant_bosses:skeleton_minion", "remnants:skeleton_minion");
		alias(entities, "remnant_bosses:remnant_ossukage", "remnants:ossukage");
		alias(entities, "remnant_bosses:wraith", "remnants:wraith");
		alias(entities, "remnant_bosses:armored_grub", "remnants:armored_grub");
		alias(entities, "remnant_bosses:umbrakar", "remnants:umbrakar");
		ENTITIES = Collections.unmodifiableMap(entities);

		Map<ResourceLocation, ResourceLocation> items = new LinkedHashMap<>();
		alias(items, "remnant_bosses:skeleton_melee_spawn_egg", "remnant_bosses:hollow_sentinel_spawn_egg");
		alias(items, "remnant_bosses:skeleton_archer_spawn_egg", "remnant_bosses:hollow_arbalist_spawn_egg");
		alias(items, "remnant_bosses:kotsukage_spawn_egg", "remnant_bosses:gravebound_warden_spawn_egg");
		alias(items, "remnant_bosses:skeleton_minion_spawn_egg", "remnant_bosses:gravebound_soldier_spawn_egg");
		alias(items, "remnant_bosses:grave_skitter_spawn_egg", "remnant_bosses:crypt_lurker_spawn_egg");
		alias(items, "remnant_bosses:rat_spawn_egg", "remnant_bosses:catacomb_rat_spawn_egg");
		alias(items, "remnant_bosses:rat_fang", "remnant_bosses:grave_iron");
		alias(items, "remnant_bosses:old_skeleton_bone", "remnant_bosses:grave_iron");
		alias(items, "remnant_bosses:old_skeleton_head", "minecraft:skeleton_skull");
		alias(items, "remnant_bosses:fang_on_a_stick", "minecraft:stone_sword"); // damage carries over
		alias(items, "remnant_bosses:umbrakar_spawn_egg", "remnant_bosses:forgotten_watcher_spawn_egg");
		alias(items, "remnant_bosses:wraith_spawn_egg", "remnant_bosses:memory_wraith_spawn_egg");
		alias(items, "remnant_bosses:dirge_lantern_spawn_egg", "remnant_bosses:soul_collector_spawn_egg");
		alias(items, "remnant_bosses:remnant_ossukage_spawn_egg", "remnant_bosses:eclipsed_knight_spawn_egg");
		alias(items, "remnant_bosses:ossukage_sword", "remnant_bosses:eclipsed_blade");
		alias(items, "remnant_bosses:armored_grub_spawn_egg", "remnant_bosses:ashbound_crawler_spawn_egg");
		alias(items, "remnant_bosses:sickleghast_spawn_egg", "remnant_bosses:relic_stalker_spawn_egg");
		ITEMS = Collections.unmodifiableMap(items);

		Set<ResourceLocation> retired = new LinkedHashSet<>();
		for (String path : new String[] {"kunai", "ossukage_rune_effect", "kotsukage_trap", "umbrakar_orb"}) {
			retired.add(ResourceLocation.fromNamespaceAndPath(Constants.ENTITY_NAMESPACE, path));
		}
		// Pre-2.4.1 spellings of the same helpers.
		for (String path : new String[] {"kunai", "ossukage_rune_effect", "umbrakar_orb"}) {
			retired.add(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
		}
		RETIRED_ENTITIES = Collections.unmodifiableSet(retired);
	}

	private RegistryAliases() {
	}

	private static void alias(Map<ResourceLocation, ResourceLocation> map, String from, String to) {
		if (map.put(ResourceLocation.parse(from), ResourceLocation.parse(to)) != null) {
			throw new IllegalStateException("Duplicate alias source " + from);
		}
	}

	public static Optional<ResourceLocation> resolveEntity(ResourceLocation id, Predicate<ResourceLocation> registered) {
		return resolve(ENTITIES, id, registered);
	}

	public static Optional<ResourceLocation> resolveItem(ResourceLocation id, Predicate<ResourceLocation> registered) {
		return resolve(ITEMS, id, registered);
	}

	/**
	 * Resolves {@code source} through {@code aliases}. Empty unless the source is unregistered and
	 * some ID along its chain is registered; the first registered ID wins.
	 */
	public static Optional<ResourceLocation> resolve(Map<ResourceLocation, ResourceLocation> aliases,
			ResourceLocation source, Predicate<ResourceLocation> registered) {
		if (source == null || !aliases.containsKey(source) || registered.test(source)) return Optional.empty();
		ResourceLocation current = source;
		// A chain visits each edge at most once; the bound also stops a (regression-forbidden) cycle.
		for (int step = 0; step < aliases.size(); step++) {
			ResourceLocation next = aliases.get(current);
			if (next == null) return Optional.empty();
			if (registered.test(next)) return Optional.of(next);
			current = next;
		}
		return Optional.empty();
	}

	/** Every source whose alias currently applies, mapped to its resolved target. */
	public static Map<ResourceLocation, ResourceLocation> applicable(Map<ResourceLocation, ResourceLocation> aliases,
			Predicate<ResourceLocation> registered) {
		Map<ResourceLocation, ResourceLocation> applied = new LinkedHashMap<>();
		for (ResourceLocation source : aliases.keySet()) {
			resolve(aliases, source, registered).ifPresent(target -> applied.put(source, target));
		}
		return applied;
	}

	/** The alias table for a vanilla registry key, or an empty map for registries without aliases. */
	public static Map<ResourceLocation, ResourceLocation> forRegistry(ResourceKey<? extends Registry<?>> registry) {
		// Compared by name so registry lookups never trigger Registries/BuiltInRegistries class init.
		ResourceLocation name = registry.location();
		if (ENTITY_TYPE_REGISTRY.equals(name)) return ENTITIES;
		if (ITEM_REGISTRY.equals(name)) return ITEMS;
		return Map.of();
	}

	/** Cheap pre-filter: only this mod's two namespaces carry aliases. */
	public static boolean isAliasNamespace(String namespace) {
		return Constants.ENTITY_NAMESPACE.equals(namespace) || Constants.MOD_ID.equals(namespace);
	}

	/**
	 * Name-lookup fallback for a registry: {@code name} itself, or its alias target when the alias
	 * applies. {@code registered} must test the registry's own entries without consulting aliases.
	 */
	public static ResourceLocation lookupName(ResourceKey<? extends Registry<?>> registry, ResourceLocation name,
			Predicate<ResourceLocation> registered) {
		if (name == null || !isAliasNamespace(name.getNamespace())) return name;
		ResourceLocation target = resolve(forRegistry(registry), name, registered).orElse(null);
		if (target == null) return name;
		logOnce(name, target);
		return target;
	}

	/** {@link #lookupName(ResourceKey, ResourceLocation, Predicate)} against a live registry. */
	public static ResourceLocation lookupName(Registry<?> registry, ResourceLocation name) {
		if (name == null || !isAliasNamespace(name.getNamespace())) return name;
		return lookupName(registry.key(), name, registry::containsKey);
	}

	/** Key form of {@link #lookupName(Registry, ResourceLocation)}; returns {@code key} when no alias applies. */
	public static <T> ResourceKey<T> lookupKey(Registry<T> registry, ResourceKey<T> key) {
		if (key == null) return null;
		ResourceLocation name = key.location();
		ResourceLocation resolved = lookupName(registry, name);
		return resolved == name ? key : ResourceKey.create(registry.key(), resolved);
	}

	/** Rewrites a saved entity tag's {@code id} in place against the live entity registry. */
	public static boolean rewriteEntityId(CompoundTag tag) {
		return rewriteEntityId(tag, LIVE_ENTITY);
	}

	/** Rewrites a saved entity tag's {@code id} in place. Returns whether it changed. */
	public static boolean rewriteEntityId(CompoundTag tag, Predicate<ResourceLocation> registered) {
		ResourceLocation target = successor(ENTITIES, tag, registered);
		if (target == null) return false;
		tag.putString("id", target.toString());
		return true;
	}

	/** Item form against the live item registry; see {@link #rewriteItemId(CompoundTag, Predicate)}. */
	public static CompoundTag rewriteItemId(CompoundTag tag) {
		return rewriteItemId(tag, LIVE_ITEM);
	}

	/**
	 * Returns {@code tag} itself when no alias applies, otherwise a copy whose {@code id} names the
	 * successor. Every other field (count, damage, nested data) is kept. The input is never mutated.
	 */
	public static CompoundTag rewriteItemId(CompoundTag tag, Predicate<ResourceLocation> registered) {
		ResourceLocation target = successor(ITEMS, tag, registered);
		if (target == null) return tag;
		CompoundTag copy = tag.copy();
		copy.putString("id", target.toString());
		return copy;
	}

	private static ResourceLocation successor(Map<ResourceLocation, ResourceLocation> aliases, CompoundTag tag,
			Predicate<ResourceLocation> registered) {
		if (tag == null || !tag.contains("id", Tag.TAG_STRING)) return null;
		String raw = tag.getString("id");
		if (!raw.startsWith(ENTITY_PREFIX) && !raw.startsWith(MOD_PREFIX)) return null;
		ResourceLocation source = ResourceLocation.tryParse(raw);
		ResourceLocation target = resolve(aliases, source, registered).orElse(null);
		if (target != null) logOnce(source, target);
		return target;
	}

	private static void logOnce(ResourceLocation source, ResourceLocation target) {
		if (LOGGED.add(source)) Constants.LOG.info("Migrating retired ID {} to {}", source, target);
	}
}
