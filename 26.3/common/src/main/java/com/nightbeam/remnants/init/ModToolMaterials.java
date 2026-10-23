package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * Boss weapon materials (the 1.21.1 {@code Tier}s) and the item tags that repair them and the armor sets:
 * data/remnant_bosses/tags/item/&lt;name&gt;.json.
 */
public final class ModToolMaterials {
	public static final TagKey<Item> GRAVE_IRON_REPAIRS = tag("grave_iron_repair_materials");
	public static final TagKey<Item> ROYAL_SOUL_REPAIRS = tag("royal_soul_repair_materials");
	public static final TagKey<Item> KNOWLEDGE_REPAIRS = tag("knowledge_repair_materials");

	/** Ossukage's blade has no durability, like its 1.21.1 tier with zero uses; see {@link #unbreakableSword}. */
	public static final ToolMaterial OSSUKAGE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1, 15f, 8f, 2, GRAVE_IRON_REPAIRS);
	public static final ToolMaterial WARDENS_GRASP = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 6f, 2f, 14, GRAVE_IRON_REPAIRS);
	public static final ToolMaterial EXECUTIONER = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 1000, 7f, 2f, 14, GRAVE_IRON_REPAIRS);
	public static final ToolMaterial SOVEREIGNS_RUIN = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8f, 4f, 15, ROYAL_SOUL_REPAIRS);

	private ModToolMaterials() {
	}

	/** A sword that never takes damage: the sword components without the durability ones (null removes a component). */
	public static Item.Properties unbreakableSword(Item.Properties properties, ToolMaterial material, float attackDamage, float attackSpeed) {
		return properties.sword(material, attackDamage, attackSpeed).component(DataComponents.MAX_DAMAGE, null)
				.component(DataComponents.DAMAGE, null);
	}

	private static TagKey<Item> tag(String name) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
	}
}
