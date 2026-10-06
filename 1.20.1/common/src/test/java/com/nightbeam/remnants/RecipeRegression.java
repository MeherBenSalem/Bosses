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
        System.out.println("Minecraft 1.20.1: all recipes parse; altar/pedestal craft one item; wrong ingredients rejected");
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
