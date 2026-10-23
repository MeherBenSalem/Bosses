package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * Armor materials are plain values since 1.21.2: the item carries its stats as components and its look as an
 * equipment asset (assets/remnant_bosses/equipment/&lt;name&gt;.json). Repair tags live in
 * {@link ModToolMaterials}.
 */
public final class ModArmorMaterials {
	/** Rusted plate over grave cloth: defense 2 / 6 / 5 / 2 (iron), toughness 1, repaired with grave iron. */
	public static final ArmorMaterial WARDEN = new ArmorMaterial(24, defense(2, 6, 5, 2, 5), 12, SoundEvents.ARMOR_EQUIP_CHAIN,
			1.0f, 0.0f, ModToolMaterials.GRAVE_IRON_REPAIRS, asset("warden"));
	/** A scholar's robe over verdigris plates: defense 3 / 7 / 6 / 3, toughness 2, enchantability 20, repaired with knowledge fragments. */
	public static final ArmorMaterial ARCHIVIST = new ArmorMaterial(33, defense(3, 7, 6, 3, 7), 20, SoundEvents.ARMOR_EQUIP_CHAIN,
			2.0f, 0.0f, ModToolMaterials.KNOWLEDGE_REPAIRS, asset("archivist"));
	public static final ArmorMaterial REGALIA = new ArmorMaterial(35, defense(3, 8, 6, 3, 8), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
			2.5f, 0.05f, ModToolMaterials.ROYAL_SOUL_REPAIRS, asset("regalia"));

	private ModArmorMaterials() {
	}

	private static Map<ArmorType, Integer> defense(int helmet, int chestplate, int leggings, int boots, int body) {
		EnumMap<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
		defense.put(ArmorType.HELMET, helmet);
		defense.put(ArmorType.CHESTPLATE, chestplate);
		defense.put(ArmorType.LEGGINGS, leggings);
		defense.put(ArmorType.BOOTS, boots);
		defense.put(ArmorType.BODY, body);
		return defense;
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
	}
}
