package com.nightbeam.remnants;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.storage.loot.LootTable;

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
        // Parse even an obsolete directory to expose the result-field error independently.
        Path recipeDir = root.resolve("data/remnant_bosses/recipe");
        Path parseDir = Files.isDirectory(recipeDir) ? recipeDir : root.resolve("data/remnant_bosses/recipes");
        try (var paths = Files.list(parseDir)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
            }
        }
        require(Files.isDirectory(recipeDir), "Minecraft 1.21.1 cannot discover recipes/; use recipe/");
        verify(recipeDir, "ancient_altar", List.of(
                stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE),
                ItemStack.EMPTY, stack(Items.DIAMOND), ItemStack.EMPTY,
                stack(Items.EMERALD), stack(Items.CHISELED_DEEPSLATE), stack(Items.EMERALD)));
        verify(recipeDir, "ancient_pedestal", List.of(
                ItemStack.EMPTY, stack(Items.EMERALD), ItemStack.EMPTY,
                ItemStack.EMPTY, stack(Items.CHISELED_DEEPSLATE), ItemStack.EMPTY,
                stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE), stack(Items.CHISELED_DEEPSLATE)));
        for (String name : List.of("ancient_altar", "ancient_pedestal")) {
            Path loot = root.resolve("data/remnant_bosses/loot_table/blocks/" + name + ".json");
            require(Files.isRegularFile(loot), "Missing discoverable block loot for " + name);
            parseLoot(JsonParser.parseString(Files.readString(loot)).getAsJsonObject());
        }
        if (args.length > 1) {
            try (ZipFile jar = new ZipFile(args[1])) {
                for (String name : List.of("ancient_altar", "ancient_pedestal")) {
                    String entry = "data/remnant_bosses/recipe/" + name + "_recipe.json";
                    require(jar.getEntry(entry) != null, "Missing packaged " + entry);
                    parse(JsonParser.parseString(new String(jar.getInputStream(jar.getEntry(entry)).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                    require(jar.getEntry("data/remnant_bosses/loot_table/blocks/" + name + ".json") != null,
                            "Missing discoverable block loot for " + name);
                    var lootEntry = jar.getEntry("data/remnant_bosses/loot_table/blocks/" + name + ".json");
                    parseLoot(JsonParser.parseString(new String(jar.getInputStream(lootEntry).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                }
            }
        }
        System.out.println("Minecraft 1.21.1: all recipes parse; altar/pedestal craft one item; wrong ingredients rejected");
    }

    private static void parseLoot(JsonObject json) {
        LootTable.DIRECT_CODEC.parse(JsonOps.INSTANCE, vanillaItems(json)).getOrThrow();
    }

    private static Recipe<?> parse(JsonObject json) {
        return Recipe.CODEC.parse(JsonOps.INSTANCE, vanillaItems(json)).getOrThrow();
    }

    private static void verify(Path recipeDir, String name, List<ItemStack> stacks) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(recipeDir.resolve(name + "_recipe.json"))).getAsJsonObject();
        require(json.getAsJsonObject("result").get("id").getAsString().equals("remnant_bosses:" + name), "Wrong output ID " + name);
        ShapedRecipe recipe = (ShapedRecipe) parse(json);
        require(recipe.matches(CraftingInput.of(3, 3, stacks), null), "Crafting grid does not match " + name);
        ItemStack result = recipe.assemble(CraftingInput.of(3, 3, stacks), RegistryAccess.EMPTY);
        require(result.is(Items.PAPER), "Wrong output " + name);
        require(result.getCount() == 1, "Wrong output count " + name);
        var wrong = new java.util.ArrayList<>(stacks);
        wrong.set(4, stack(Items.DIRT));
        require(!recipe.matches(CraftingInput.of(3, 3, wrong), null), "Wrong ingredient accepted for " + name);
        JsonObject legacy = json.deepCopy();
        JsonObject oldResult = legacy.getAsJsonObject("result");
        oldResult.add("item", oldResult.remove("id"));
        require(Recipe.CODEC.parse(JsonOps.INSTANCE, vanillaItems(legacy)).error().isPresent(), "Obsolete result.item unexpectedly accepted");
    }

    // Bootstrap freezes vanilla registries. Alias only mod item IDs to a real vanilla
    // item; leave every schema field intact. Loader registration is tested separately.
    private static JsonObject vanillaItems(JsonObject json) {
        return JsonParser.parseString(json.toString().replaceAll("remnant_bosses:[a-z_]+", "minecraft:paper")).getAsJsonObject();
    }

    private static ItemStack stack(Item item) { return new ItemStack(item); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
