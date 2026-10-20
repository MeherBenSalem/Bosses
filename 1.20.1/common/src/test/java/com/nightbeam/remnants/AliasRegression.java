package com.nightbeam.remnants;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.RegistryAliases;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Alias-table integrity, guard semantics and entity loot coverage. */
public final class AliasRegression {
    /** The loot folder Minecraft 1.20.1 loads, and the one it silently ignores. */
    private static final String LOOT = "loot_tables", IGNORED_LOOT = "loot_table";
    /**
     * Registered entities that intentionally drop nothing. Remove an entry when the entity gains a
     * loot table or is unregistered: both directions fail this check so the list cannot go stale.
     */
    private static final Set<String> NO_LOOT = Set.of("ossukage",
            // Hollow Sovereign helpers and summons.
            "sovereign_pauldron", "spectral_crescent", "royal_blade_mark", "spectral_royal_guard",
            // Gravebound Warden helpers: the thrown chain and the Chain Wall anchors.
            "warden_chain", "chain_anchor",
            // Forgotten Watcher helpers and summons: illusion decoys, bolts and wisps.
            "watcher_illusion", "memory_bolt", "soul_wisp",
            // The Ember Servitor's and the Shardling's bolts.
            "ember_bolt", "crystal_bolt");
    private static final Predicate<ResourceLocation> NONE = id -> false, ALL = id -> true;

    public static void main(String[] args) throws Exception {
        Path resources = Path.of(args[0]);
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        integrity(RegistryAliases.ENTITIES, "entity");
        integrity(RegistryAliases.ITEMS, "item");
        retiredAndKept();
        guards();
        tagRewrites();
        vanillaSuccessors();
        sliceThree();
        sliceFour();
        constructsAndRelics();
        int tables = lootCoverage(resources);
        System.out.println("Alias regressions passed: " + RegistryAliases.ENTITIES.size() + " entity and "
                + RegistryAliases.ITEMS.size() + " item aliases, " + tables + " entity loot tables in " + LOOT);
    }

    private static void integrity(Map<ResourceLocation, ResourceLocation> aliases, String kind) {
        for (Map.Entry<ResourceLocation, ResourceLocation> alias : aliases.entrySet()) {
            check(!alias.getKey().equals(alias.getValue()), kind + " alias maps to itself: " + alias.getKey());
            chain(aliases, alias.getKey());
        }
    }

    /** Every ID after {@code source} along its chain; fails on a cycle. */
    private static List<ResourceLocation> chain(Map<ResourceLocation, ResourceLocation> aliases, ResourceLocation source) {
        List<ResourceLocation> path = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>(Set.of(source));
        for (ResourceLocation next = aliases.get(source); next != null; next = aliases.get(next)) {
            check(seen.add(next), "alias chain does not terminate: " + source);
            path.add(next);
        }
        return path;
    }

    private static ResourceLocation terminal(Map<ResourceLocation, ResourceLocation> aliases, ResourceLocation source) {
        List<ResourceLocation> path = chain(aliases, source);
        return path.get(path.size() - 1);
    }

    private static void retiredAndKept() {
        Set<ResourceLocation> retired = RegistryAliases.RETIRED_ENTITIES;
        check(retired.containsAll(Set.of(rl("remnants:kunai"), rl("remnants:ossukage_rune_effect"),
                rl("remnants:kotsukage_trap"), rl("remnants:umbrakar_orb"))), "dropped helpers are listed as retired");
        for (ResourceLocation id : retired) {
            check(!RegistryAliases.ENTITIES.containsKey(id), "retired helper is aliased: " + id);
            check(!RegistryAliases.ENTITIES.containsValue(id), "retired helper is a successor: " + id);
            check(RegistryAliases.resolveEntity(id, ALL).isEmpty() && RegistryAliases.resolveEntity(id, NONE).isEmpty(),
                    "retired helper resolves: " + id);
        }
        RegistryAliases.ENTITIES.forEach((from, to) -> {
            check(RegistryAliases.isAliasNamespace(from.getNamespace()), "entity alias outside mod namespaces: " + from);
            check(to.getNamespace().equals(Constants.ENTITY_NAMESPACE), "entity successor outside remnants: " + to);
        });
        RegistryAliases.ITEMS.forEach((from, to) -> {
            check(from.getNamespace().equals(Constants.MOD_ID), "item alias outside remnant_bosses: " + from);
            check(to.getNamespace().equals(Constants.MOD_ID) || to.getNamespace().equals("minecraft"), "item successor namespace: " + to);
        });
        check(!RegistryAliases.ENTITIES.containsKey(rl("remnants:hollow_sovereign")), "hollow_sovereign keeps its ID");
        for (String kept : List.of("boss_journal", "hollow_sovereign_spawn_egg", "ancient_altar", "ancient_pedestal")) {
            check(!RegistryAliases.ITEMS.containsKey(rl("remnant_bosses:" + kept)), "kept item is aliased: " + kept);
        }
        check(chain(RegistryAliases.ENTITIES, rl("remnant_bosses:remnant_ossukage"))
                .equals(List.of(rl("remnants:ossukage"), rl("remnants:eclipsed_knight"))), "pre-2.4.1 ID chains through the table");
    }

    private static void guards() {
        ResourceLocation melee = rl("remnants:skeleton_melee"), sentinel = rl("remnants:hollow_sentinel");
        check(RegistryAliases.resolveEntity(melee, ALL).isEmpty(), "registered source is never rewritten");
        check(RegistryAliases.resolveEntity(melee, Set.of(melee, sentinel)::contains).isEmpty(), "registered source beats registered target");
        check(RegistryAliases.resolveEntity(melee, NONE).isEmpty(), "unregistered target is never used");
        check(RegistryAliases.resolveEntity(melee, Set.of(sentinel)::contains).equals(Optional.of(sentinel)), "alias applies");

        ResourceLocation legacy = rl("remnant_bosses:remnant_ossukage"), ossukage = rl("remnants:ossukage"), knight = rl("remnants:eclipsed_knight");
        check(RegistryAliases.resolveEntity(legacy, Set.of(ossukage)::contains).equals(Optional.of(ossukage)), "chain stops at live intermediate");
        check(RegistryAliases.resolveEntity(legacy, Set.of(ossukage, knight)::contains).equals(Optional.of(ossukage)), "live intermediate is not looked through");
        check(RegistryAliases.resolveEntity(legacy, Set.of(knight)::contains).equals(Optional.of(knight)), "chain reaches successor");
        check(RegistryAliases.resolveEntity(legacy, NONE).isEmpty(), "chain without live ID is not rewritten");
        check(RegistryAliases.resolveEntity(rl("remnants:not_an_alias"), ALL).isEmpty() && RegistryAliases.resolveEntity(null, ALL).isEmpty(), "unknown IDs");

        ResourceLocation head = rl("remnant_bosses:old_skeleton_head"), skull = rl("minecraft:skeleton_skull");
        check(RegistryAliases.resolveItem(head, Set.of(skull)::contains).equals(Optional.of(skull)), "item alias to vanilla");
        check(RegistryAliases.resolveItem(head, Set.of(head, skull)::contains).isEmpty(), "registered item is never rewritten");
        ResourceLocation iron = rl("remnant_bosses:grave_iron");
        check(RegistryAliases.resolveItem(rl("remnant_bosses:rat_fang"), Set.of(iron)::contains).equals(Optional.of(iron))
                && RegistryAliases.resolveItem(rl("remnant_bosses:old_skeleton_bone"), Set.of(iron)::contains).equals(Optional.of(iron)), "many-to-one item alias");

        // Name-lookup fallback: only ENTITY_TYPE and ITEM, only this mod's namespaces.
        check(RegistryAliases.lookupName(Registries.ITEM, head, Set.of(skull)::contains).equals(skull), "item registry lookup falls back");
        check(RegistryAliases.lookupName(Registries.ENTITY_TYPE, melee, Set.of(sentinel)::contains).equals(sentinel), "entity registry lookup falls back");
        check(RegistryAliases.lookupName(Registries.BLOCK, head, ALL) == head && RegistryAliases.lookupName(Registries.BLOCK, head, NONE) == head, "other registries untouched");
        check(RegistryAliases.lookupName(Registries.ITEM, head, Set.of(head, skull)::contains) == head, "registered name looked up as itself");
        check(RegistryAliases.lookupName(Registries.ITEM, skull, NONE) == skull && RegistryAliases.lookupName(Registries.ITEM, null, NONE) == null, "foreign namespace untouched");

        // Invariant for any registry state: a rewrite happens exactly when the source is gone and
        // some ID on its chain is live, and it lands on the first live one.
        List<Predicate<ResourceLocation>> states = List.of(NONE, ALL, registeredEntities()::contains,
                id -> !id.getNamespace().equals(Constants.MOD_ID), id -> id.getPath().contains("_"));
        for (Predicate<ResourceLocation> live : states) {
            for (Map<ResourceLocation, ResourceLocation> aliases : List.of(RegistryAliases.ENTITIES, RegistryAliases.ITEMS)) {
                for (ResourceLocation source : aliases.keySet()) {
                    Optional<ResourceLocation> expected = live.test(source) ? Optional.empty()
                            : chain(aliases, source).stream().filter(live).findFirst();
                    check(RegistryAliases.resolve(aliases, source, live).equals(expected), "guard invariant for " + source);
                    check(Objects.equals(RegistryAliases.applicable(aliases, live).get(source), expected.orElse(null)), "applicable() agrees for " + source);
                }
            }
        }
    }

    private static void tagRewrites() {
        ResourceLocation sentinel = rl("remnants:hollow_sentinel");
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "remnants:skeleton_melee");
        entity.putFloat("Health", 5f);
        check(RegistryAliases.rewriteEntityId(entity, Set.of(sentinel)::contains), "entity tag rewritten");
        check(entity.getString("id").equals("remnants:hollow_sentinel") && entity.getFloat("Health") == 5f, "entity tag keeps its data");
        CompoundTag live = new CompoundTag();
        live.putString("id", "remnants:skeleton_melee");
        check(!RegistryAliases.rewriteEntityId(live, ALL) && live.getString("id").equals("remnants:skeleton_melee"), "live entity untouched");
        for (String id : List.of("remnants:kunai", "minecraft:zombie", "remnants:Bad Id", "")) {
            CompoundTag other = new CompoundTag();
            other.putString("id", id);
            check(!RegistryAliases.rewriteEntityId(other, NONE.negate()) && !RegistryAliases.rewriteEntityId(other, NONE), "not rewritten: " + id);
        }
        check(!RegistryAliases.rewriteEntityId(new CompoundTag(), ALL) && !RegistryAliases.rewriteEntityId(null, ALL), "missing id");

        CompoundTag item = new CompoundTag();
        item.putString("id", "remnant_bosses:fang_on_a_stick");
        item.putByte("Count", (byte) 1);
        CompoundTag extra = new CompoundTag();
        extra.putInt("Damage", 7);
        item.put("tag", extra);
        CompoundTag before = item.copy();
        CompoundTag migrated = RegistryAliases.rewriteItemId(item, Set.of(rl("minecraft:stone_sword"))::contains);
        check(migrated != item && item.equals(before), "item tag copied, caller's tag untouched");
        check(migrated.getString("id").equals("minecraft:stone_sword") && migrated.getByte("Count") == 1
                && migrated.getCompound("tag").getInt("Damage") == 7, "item keeps count and durability");
        check(RegistryAliases.rewriteItemId(item, ALL) == item, "registered item returns the same tag");
    }

    private static void vanillaSuccessors() {
        Predicate<ResourceLocation> vanillaItems = BuiltInRegistries.ITEM::containsKey;
        RegistryAliases.ITEMS.forEach((from, to) -> {
            if (to.getNamespace().equals("minecraft")) {
                check(vanillaItems.test(to), "vanilla successor does not exist: " + to);
                // Mod items are not registered in this JVM, so the real registry stands in for "after removal".
                check(RegistryAliases.resolveItem(from, vanillaItems).equals(Optional.of(to)), "real registry resolves " + from);
            }
        });
    }

    /**
     * Gravebound Warden migration: no predecessor is registered, every successor is, and each old ID
     * resolves to its successor against the live registrations. Kunai and the Ossukage rune stay
     * registered because Ossukage is.
     */
    private static void sliceThree() {
        Set<ResourceLocation> entities = registeredEntities();
        Map.of("kotsukage", "gravebound_warden", "skeleton_minion", "gravebound_soldier",
                "grave_skitter", "crypt_lurker", "rat", "catacomb_rat").forEach((old, successor) -> {
            ResourceLocation from = rl("remnants:" + old), to = rl("remnants:" + successor);
            check(!entities.contains(from), "slice 3 predecessor is still registered: " + from);
            check(entities.contains(to), "slice 3 successor is not registered: " + to);
            check(RegistryAliases.resolveEntity(from, entities::contains).equals(Optional.of(to)), "predecessor does not resolve: " + from);
        });
        check(!entities.contains(rl("remnants:kotsukage_trap")), "the dropped Kotsukage trap is still registered");
        check(entities.containsAll(Set.of(rl("remnants:kunai"), rl("remnants:ossukage_rune_effect"))), "Ossukage's helpers stay until slice 5");
        check(RegistryAliases.resolveEntity(rl("remnant_bosses:rat"), entities::contains).equals(Optional.of(rl("remnants:catacomb_rat")))
                && RegistryAliases.resolveEntity(rl("remnant_bosses:skeleton_minion"), entities::contains).equals(Optional.of(rl("remnants:gravebound_soldier"))),
                "pre-2.4.1 IDs chain to the slice 3 successors");

        Set<ResourceLocation> items = registeredItems();
        Predicate<ResourceLocation> live = id -> items.contains(id) || BuiltInRegistries.ITEM.containsKey(id);
        Map.of("rat_fang", "remnant_bosses:grave_iron", "old_skeleton_bone", "remnant_bosses:grave_iron",
                "old_skeleton_head", "minecraft:skeleton_skull", "fang_on_a_stick", "minecraft:stone_sword",
                "kotsukage_spawn_egg", "remnant_bosses:gravebound_warden_spawn_egg",
                "skeleton_minion_spawn_egg", "remnant_bosses:gravebound_soldier_spawn_egg",
                "grave_skitter_spawn_egg", "remnant_bosses:crypt_lurker_spawn_egg",
                "rat_spawn_egg", "remnant_bosses:catacomb_rat_spawn_egg").forEach((old, successor) -> {
            ResourceLocation from = rl("remnant_bosses:" + old), to = rl(successor);
            check(!items.contains(from), "slice 3 predecessor item is still registered: " + from);
            check(live.test(to), "slice 3 successor item is not registered: " + to);
            check(RegistryAliases.resolveItem(from, live).equals(Optional.of(to)), "predecessor item does not resolve: " + from);
        });
        for (String kept : List.of("crypt_key", "wardens_grasp", "gravebound_executioner", "crypt_sigil", "warden_helm",
                "warden_hauberk", "warden_tassets", "warden_boots", "burial_guardian_spawn_egg")) {
            check(items.contains(rl("remnant_bosses:" + kept)), "slice 3 item is not registered: " + kept);
        }
    }

    /**
     * Forgotten Watcher migration (Umbrakar, the wraith and the dirge lantern): no predecessor is
     * registered, every successor is, each old ID resolves to its successor against the live
     * registrations, and the dropped Umbrakar orb is gone without an alias.
     */
    private static void sliceFour() {
        Set<ResourceLocation> entities = registeredEntities();
        Map.of("umbrakar", "forgotten_watcher", "wraith", "memory_wraith", "dirge_lantern", "soul_collector").forEach((old, successor) -> {
            ResourceLocation from = rl("remnants:" + old), to = rl("remnants:" + successor);
            check(!entities.contains(from), "slice 4 predecessor is still registered: " + from);
            check(entities.contains(to), "slice 4 successor is not registered: " + to);
            check(RegistryAliases.resolveEntity(from, entities::contains).equals(Optional.of(to)), "predecessor does not resolve: " + from);
        });
        check(!entities.contains(rl("remnants:umbrakar_orb")), "the dropped Umbrakar orb is still registered");
        check(entities.containsAll(Set.of(rl("remnants:lost_pilgrim"), rl("remnants:watcher_illusion"), rl("remnants:memory_bolt"),
                rl("remnants:soul_wisp"))), "slice 4 helpers and the Lost Pilgrim are registered");
        check(RegistryAliases.resolveEntity(rl("remnant_bosses:umbrakar"), entities::contains).equals(Optional.of(rl("remnants:forgotten_watcher")))
                && RegistryAliases.resolveEntity(rl("remnant_bosses:wraith"), entities::contains).equals(Optional.of(rl("remnants:memory_wraith"))),
                "pre-2.4.1 IDs chain to the slice 4 successors");

        Set<ResourceLocation> items = registeredItems();
        Map.of("umbrakar_spawn_egg", "forgotten_watcher_spawn_egg", "wraith_spawn_egg", "memory_wraith_spawn_egg",
                "dirge_lantern_spawn_egg", "soul_collector_spawn_egg").forEach((old, successor) -> {
            ResourceLocation from = rl("remnant_bosses:" + old), to = rl("remnant_bosses:" + successor);
            check(!items.contains(from), "slice 4 predecessor item is still registered: " + from);
            check(items.contains(to), "slice 4 successor item is not registered: " + to);
            check(RegistryAliases.resolveItem(from, items::contains).equals(Optional.of(to)), "predecessor item does not resolve: " + from);
        });
        for (String kept : List.of("memory_shard", "knowledge_fragment", "memory_lens", "memory_staff", "watchers_eye", "archivist_hood",
                "archivist_robe", "archivist_leggings", "archivist_boots", "lost_pilgrim_spawn_egg")) {
            check(items.contains(rl("remnant_bosses:" + kept)), "slice 4 item is not registered: " + kept);
        }
    }

    /**
     * The Ashbound Constructs and the Corrupted Relics roam the Realm ahead of their boss slices: the armored grub
     * and the sickleghast are retired, each old ID (and the pre-2.4.1 grub) resolves to its successor, the eggs
     * follow, and both family materials are registered.
     */
    private static void constructsAndRelics() {
        Set<ResourceLocation> entities = registeredEntities();
        Map.of("armored_grub", "ashbound_crawler", "sickleghast", "relic_stalker").forEach((old, successor) -> {
            ResourceLocation from = rl("remnants:" + old), to = rl("remnants:" + successor);
            check(!entities.contains(from), "retired family predecessor is still registered: " + from);
            check(entities.contains(to), "family successor is not registered: " + to);
            check(RegistryAliases.resolveEntity(from, entities::contains).equals(Optional.of(to)), "predecessor does not resolve: " + from);
        });
        check(RegistryAliases.resolveEntity(rl("remnant_bosses:armored_grub"), entities::contains).equals(Optional.of(rl("remnants:ashbound_crawler"))),
                "pre-2.4.1 armored grub chains to the Ashbound Crawler");
        check(entities.containsAll(Set.of(rl("remnants:ember_servitor"), rl("remnants:rusted_guardian"), rl("remnants:shardling"),
                rl("remnants:ancient_vessel"), rl("remnants:ember_bolt"), rl("remnants:crystal_bolt"))), "new family members are registered");

        Set<ResourceLocation> items = registeredItems();
        Map.of("armored_grub_spawn_egg", "ashbound_crawler_spawn_egg", "sickleghast_spawn_egg", "relic_stalker_spawn_egg").forEach((old, successor) -> {
            ResourceLocation from = rl("remnant_bosses:" + old), to = rl("remnant_bosses:" + successor);
            check(!items.contains(from), "retired family egg is still registered: " + from);
            check(items.contains(to), "family successor egg is not registered: " + to);
            check(RegistryAliases.resolveItem(from, items::contains).equals(Optional.of(to)), "predecessor egg does not resolve: " + from);
        });
        for (String kept : List.of("ashen_cog", "relic_shard", "ember_servitor_spawn_egg", "rusted_guardian_spawn_egg",
                "shardling_spawn_egg", "ancient_vessel_spawn_egg")) {
            check(items.contains(rl("remnant_bosses:" + kept)), "family item is not registered: " + kept);
        }
    }

    private static int lootCoverage(Path resources) throws Exception {
        Path data = resources.resolve("data");
        Path lootDir = data.resolve(Constants.ENTITY_NAMESPACE).resolve(LOOT).resolve("entities");
        Set<String> registered = registeredEntities().stream().map(ResourceLocation::getPath).collect(Collectors.toSet());
        Set<String> helpers = RegistryAliases.RETIRED_ENTITIES.stream()
                .filter(id -> id.getNamespace().equals(Constants.ENTITY_NAMESPACE)).map(ResourceLocation::getPath).collect(Collectors.toSet());
        for (String path : registered) {
            if (helpers.contains(path)) continue;
            boolean exists = Files.isRegularFile(lootDir.resolve(path + ".json"));
            if (NO_LOOT.contains(path)) check(!exists, "allowlisted entity now has loot; drop it from NO_LOOT: " + path);
            else check(exists, "registered entity has no loot table in " + LOOT + ": remnants:" + path);
        }
        for (String path : NO_LOOT) check(registered.contains(path), "NO_LOOT names an unregistered entity: " + path);
        for (ResourceLocation source : RegistryAliases.ENTITIES.keySet()) {
            ResourceLocation successor = terminal(RegistryAliases.ENTITIES, source);
            check(!NO_LOOT.contains(successor.getPath()), "a successor may not skip loot: " + successor);
            if (registered.contains(successor.getPath())) {
                check(Files.isRegularFile(lootDir.resolve(successor.getPath() + ".json")), "successor has no loot table: " + successor);
            }
        }
        int tables = 0;
        try (Stream<Path> files = Files.list(lootDir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                parseLoot(JsonParser.parseString(Files.readString(file)).getAsJsonObject(), file);
                tables++;
            }
        }
        try (Stream<Path> namespaces = Files.list(data)) {
            for (Path namespace : namespaces.toList()) {
                check(!Files.exists(namespace.resolve(IGNORED_LOOT)), "Minecraft 1.20.1 ignores " + data.relativize(namespace.resolve(IGNORED_LOOT)));
            }
        }
        // Entity loot resolves as remnants:entities/<id>; remnant_bosses copies are never read.
        for (String folder : List.of(LOOT, IGNORED_LOOT)) {
            check(!Files.exists(data.resolve(Constants.MOD_ID).resolve(folder).resolve("entities")), "dead remnant_bosses entity loot in " + folder);
        }
        return tables;
    }

    private static void parseLoot(JsonObject json, Path file) {
        // Bootstrap froze vanilla registries: stand mod items in with a vanilla item, keep the schema.
        JsonObject vanilla = JsonParser.parseString(json.toString().replaceAll("remnant_bosses:[a-z_]+", "minecraft:paper")).getAsJsonObject();
        check(Deserializers.createLootTableSerializer().create().fromJson(vanilla, LootTable.class) != null, "loot table does not parse: " + file);
    }

    /** Entity IDs that ModEntities registers, read from its RegistryHolder fields. */
    private static Set<ResourceLocation> registeredEntities() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        try {
            for (Field field : ModEntities.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != RegistryHolder.class) continue;
                RegistryHolder<?> holder = (RegistryHolder<?>) field.get(null);
                if (holder.namespace().equals(Constants.ENTITY_NAMESPACE)) ids.add(holder.id());
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        check(ids.size() >= 10, "ModEntities registrations not found");
        return ids;
    }

    /** Item IDs that ModItems and ModEntities (spawn eggs) register, read from their RegistryHolder fields. */
    private static Set<ResourceLocation> registeredItems() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        try {
            for (Class<?> owner : List.of(ModItems.class, ModEntities.class)) {
                for (Field field : owner.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) || field.getType() != RegistryHolder.class) continue;
                    RegistryHolder<?> holder = (RegistryHolder<?>) field.get(null);
                    if (holder.namespace().equals(Constants.MOD_ID)) ids.add(holder.id());
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        check(ids.size() >= 10, "ModItems registrations not found");
        return ids;
    }

    private static ResourceLocation rl(String id) { return new ResourceLocation(id); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
