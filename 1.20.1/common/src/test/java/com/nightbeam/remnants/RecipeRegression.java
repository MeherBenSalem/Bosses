package com.nightbeam.remnants;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Deserializers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

/** Standalone native-parser tests. Test items stand in for mod registrations;
 * loader registration and packaged descriptors are verified separately. */
public final class RecipeRegression {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path root = Path.of(args[0]);
        Path recipeDir = root.resolve("data/remnant_bosses/recipes");
        try (var paths = Files.list(recipeDir)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
            }
        }
        require(Files.isDirectory(recipeDir), "Minecraft 1.20.1 cannot discover recipe/; use recipes/");
        verify(recipeDir, "ancient_altar", List.of(
                stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE),
                ItemStack.EMPTY, stack(Items.DIAMOND), ItemStack.EMPTY,
                stack(Items.EMERALD), stack(Items.CHISELED_DEEPSLATE), stack(Items.EMERALD)));
        verify(recipeDir, "ancient_pedestal", List.of(
                ItemStack.EMPTY, stack(Items.EMERALD), ItemStack.EMPTY,
                ItemStack.EMPTY, stack(Items.CHISELED_DEEPSLATE), ItemStack.EMPTY,
                stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE)));
        // Mod items stand in as paper, so the raw keys are checked too.
        ItemStack paper = stack(Items.PAPER);
        verifyShaped(recipeDir, "sovereigns_seal", "remnant_bosses:sovereigns_seal", List.of("RGR", "GEG", "RGR"),
                java.util.Map.of('R', "remnant_bosses:tarnished_regalia", 'G', "minecraft:gold_ingot", 'E', "minecraft:echo_shard"),
                List.of(paper, stack(Items.GOLD_INGOT), paper, stack(Items.GOLD_INGOT), stack(Items.ECHO_SHARD),
                        stack(Items.GOLD_INGOT), paper, stack(Items.GOLD_INGOT), paper));
        for (var piece : List.of(java.util.Map.entry("regalia_cuirass", Items.DIAMOND_CHESTPLATE),
                java.util.Map.entry("regalia_greaves", Items.DIAMOND_LEGGINGS), java.util.Map.entry("regalia_sabatons", Items.DIAMOND_BOOTS))) {
            verifyShaped(recipeDir, piece.getKey(), "remnant_bosses:" + piece.getKey(), List.of("RFR", "RDR", "RRR"),
                    java.util.Map.of('R', "remnant_bosses:tarnished_regalia", 'F', "remnant_bosses:royal_soul_fragment",
                            'D', BuiltInRegistries.ITEM.getKey(piece.getValue()).toString()),
                    List.of(paper, paper, paper, paper, stack(piece.getValue()), paper, paper, paper, paper));
        }
        // G G / GIG / G G: grave iron around an iron block.
        verifyShaped(recipeDir, "crypt_key", "remnant_bosses:crypt_key", List.of("G G", "GIG", "G G"),
                java.util.Map.of('G', "remnant_bosses:grave_iron", 'I', "minecraft:iron_block"),
                List.of(paper, ItemStack.EMPTY, paper, paper, stack(Items.IRON_BLOCK), paper, paper, ItemStack.EMPTY, paper));
        // MAM / ASA / MAM, and the Archivist pieces around diamond armor.
        verifyShaped(recipeDir, "memory_lens", "remnant_bosses:memory_lens", List.of("MAM", "ASA", "MAM"),
                java.util.Map.of('M', "remnant_bosses:memory_shard", 'A', "minecraft:amethyst_shard", 'S', "minecraft:spyglass"),
                List.of(paper, stack(Items.AMETHYST_SHARD), paper, stack(Items.AMETHYST_SHARD), stack(Items.SPYGLASS),
                        stack(Items.AMETHYST_SHARD), paper, stack(Items.AMETHYST_SHARD), paper));
        for (var piece : List.of(java.util.Map.entry("archivist_hood", Items.DIAMOND_HELMET),
                java.util.Map.entry("archivist_robe", Items.DIAMOND_CHESTPLATE), java.util.Map.entry("archivist_leggings", Items.DIAMOND_LEGGINGS),
                java.util.Map.entry("archivist_boots", Items.DIAMOND_BOOTS))) {
            verifyShaped(recipeDir, piece.getKey(), "remnant_bosses:" + piece.getKey(), List.of("MFM", "MDM", "MMM"),
                    java.util.Map.of('M', "remnant_bosses:memory_shard", 'F', "remnant_bosses:knowledge_fragment",
                            'D', net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(piece.getValue()).toString()),
                    List.of(paper, paper, paper, paper, stack(piece.getValue()), paper, paper, paper, paper));
        }
        for (String name : List.of("ancient_altar", "ancient_pedestal")) {
            Path loot = root.resolve("data/remnant_bosses/loot_tables/blocks/" + name + ".json");
            require(Files.isRegularFile(loot), "Missing discoverable block loot for " + name);
            parseLoot(JsonParser.parseString(Files.readString(loot)).getAsJsonObject());
        }
        if (args.length > 1) {
            try (ZipFile jar = new ZipFile(args[1])) {
                for (String name : List.of("ancient_altar", "ancient_pedestal")) {
                    String entry = "data/remnant_bosses/recipes/" + name + "_recipe.json";
                    require(jar.getEntry(entry) != null, "Missing packaged " + entry);
                    parse(JsonParser.parseString(new String(jar.getInputStream(jar.getEntry(entry)).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                    require(jar.getEntry("data/remnant_bosses/loot_tables/blocks/" + name + ".json") != null,
                            "Missing discoverable block loot for " + name);
                    var lootEntry = jar.getEntry("data/remnant_bosses/loot_tables/blocks/" + name + ".json");
                    parseLoot(JsonParser.parseString(new String(jar.getInputStream(lootEntry).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                }
            }
        }
        System.out.println("Minecraft 1.20.1: all recipes parse; altar/pedestal, Sovereign's Seal, Regalia, Crypt Key, Lens of Memory and Archivist armor craft one item; wrong ingredients rejected");
    }

    private static void parseLoot(JsonObject json) {
        require(Deserializers.createLootTableSerializer().create().fromJson(vanillaItems(json), LootTable.class) != null,
                "Block loot did not parse");
    }

    private static Recipe<?> parse(JsonObject json) {
        json = vanillaItems(json);
        return BuiltInRegistries.RECIPE_SERIALIZER.get(new ResourceLocation(json.get("type").getAsString()))
                .fromJson(new ResourceLocation("remnant_bosses", "regression"), json);
    }

    private static void verify(Path recipeDir, String name, List<ItemStack> stacks) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(recipeDir.resolve(name + "_recipe.json"))).getAsJsonObject();
        require(json.getAsJsonObject("result").get("item").getAsString().equals("remnant_bosses:" + name), "Wrong output ID " + name);
        ShapedRecipe recipe = (ShapedRecipe) parse(json);
        require(recipe.matches(grid(stacks), null), "Crafting grid does not match " + name);
        ItemStack result = recipe.assemble(grid(stacks), RegistryAccess.EMPTY);
        require(result.is(Items.PAPER), "Wrong output " + name);
        require(result.getCount() == 1, "Wrong output count " + name);
        var wrong = new java.util.ArrayList<>(stacks);
        wrong.set(4, stack(Items.DIRT));
        require(!recipe.matches(grid(wrong), null), "Wrong ingredient accepted for " + name);
    }

    private static void verifyShaped(Path recipeDir, String file, String output, List<String> pattern,
            java.util.Map<Character, String> keys, List<ItemStack> stacks) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(recipeDir.resolve(file + ".json"))).getAsJsonObject();
        require(json.getAsJsonObject("result").get("item").getAsString().equals(output), "Wrong output ID " + file);
        require(json.getAsJsonObject("result").get("count").getAsInt() == 1, "Wrong output count " + file);
        var rows = json.getAsJsonArray("pattern");
        require(rows.size() == pattern.size(), "Wrong pattern height " + file);
        for (int i = 0; i < pattern.size(); i++) require(rows.get(i).getAsString().equals(pattern.get(i)), "Wrong pattern row " + i + " in " + file);
        JsonObject key = json.getAsJsonObject("key");
        require(key.size() == keys.size(), "Unexpected key count in " + file);
        keys.forEach((symbol, item) -> require(key.getAsJsonObject(String.valueOf(symbol)).get("item").getAsString().equals(item),
                "Key " + symbol + " is not " + item + " in " + file));
        ShapedRecipe recipe = (ShapedRecipe) parse(json);
        require(recipe.matches(grid(stacks), null), "Crafting grid does not match " + file);
        require(recipe.assemble(grid(stacks), RegistryAccess.EMPTY).is(Items.PAPER), "Wrong output " + file);
        var wrong = new java.util.ArrayList<>(stacks);
        wrong.set(4, stack(Items.DIRT));
        require(!recipe.matches(grid(wrong), null), "Wrong ingredient accepted for " + file);
    }

    private static CraftingContainer grid(List<ItemStack> stacks) {
        var menu = new AbstractContainerMenu(null, 0) {
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < stacks.size(); i++) grid.setItem(i, stacks.get(i));
        return grid;
    }

    // Bootstrap freezes vanilla registries. Alias only mod item IDs to a real vanilla
    // item; leave every schema field intact. Loader registration is tested separately.
    private static JsonObject vanillaItems(JsonObject json) {
        return JsonParser.parseString(json.toString().replaceAll("remnant_bosses:[a-z_]+", "minecraft:paper")).getAsJsonObject();
    }

    private static ItemStack stack(Item item) { return new ItemStack(item); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
