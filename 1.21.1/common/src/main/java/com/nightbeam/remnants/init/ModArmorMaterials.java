package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

/**
 * Minecraft 1.21 armor materials are registry entries that armor items reference by {@code Holder}.
 * Loaders register {@link #REGALIA}, {@link #WARDEN} and {@link #ARCHIVIST} first and bind their holders ({@link RegistryHolder#bindHolder});
 * 1.20.1 uses an {@code ArmorMaterial} enum instead, so this class is the only 1.21-specific piece.
 */
public final class ModArmorMaterials {
	public static final int REGALIA_DURABILITY_MULTIPLIER = 35;
	/** Texture layers: assets/remnant_bosses/textures/models/armor/regalia_layer_1.png and _layer_2.png. */
	public static final RegistryHolder<ArmorMaterial> REGALIA = new RegistryHolder<>("regalia", ModArmorMaterials::regalia);
	/** Durability multiplier between iron (15) and diamond (33). */
	public static final int WARDEN_DURABILITY_MULTIPLIER = 24;
	/** Texture layers: assets/remnant_bosses/textures/models/armor/warden_layer_1.png and _layer_2.png. */
	public static final RegistryHolder<ArmorMaterial> WARDEN = new RegistryHolder<>("warden", ModArmorMaterials::warden);
	/** Diamond's durability multiplier. */
	public static final int ARCHIVIST_DURABILITY_MULTIPLIER = 33;
	/** Texture layers: assets/remnant_bosses/textures/models/armor/archivist_layer_1.png and _layer_2.png. */
	public static final RegistryHolder<ArmorMaterial> ARCHIVIST = new RegistryHolder<>("archivist", ModArmorMaterials::archivist);

	private ModArmorMaterials() {
	}

	/** Rusted plate over grave cloth: defense 2 / 6 / 5 / 2 (iron), toughness 1, repaired with grave iron. */
	private static ArmorMaterial warden() {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.HELMET, 2);
		defense.put(ArmorItem.Type.CHESTPLATE, 6);
		defense.put(ArmorItem.Type.LEGGINGS, 5);
		defense.put(ArmorItem.Type.BOOTS, 2);
		defense.put(ArmorItem.Type.BODY, 5);
		return new ArmorMaterial(defense, 12, SoundEvents.ARMOR_EQUIP_CHAIN,
				() -> Ingredient.of(ModItems.GRAVE_IRON.get()),
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "warden"))),
				1.0f, 0.0f);
	}

	/** A scholar's robe over verdigris plates: defense 3 / 7 / 6 / 3, toughness 2, enchantability 20, repaired with knowledge fragments. */
	private static ArmorMaterial archivist() {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.HELMET, 3);
		defense.put(ArmorItem.Type.CHESTPLATE, 7);
		defense.put(ArmorItem.Type.LEGGINGS, 6);
		defense.put(ArmorItem.Type.BOOTS, 3);
		defense.put(ArmorItem.Type.BODY, 7);
		return new ArmorMaterial(defense, 20, SoundEvents.ARMOR_EQUIP_CHAIN,
				() -> Ingredient.of(ModItems.KNOWLEDGE_FRAGMENT.get()),
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "archivist"))),
				2.0f, 0.0f);
	}

	private static ArmorMaterial regalia() {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.HELMET, 3);
		defense.put(ArmorItem.Type.CHESTPLATE, 8);
		defense.put(ArmorItem.Type.LEGGINGS, 6);
		defense.put(ArmorItem.Type.BOOTS, 3);
		defense.put(ArmorItem.Type.BODY, 8);
		return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
				() -> Ingredient.of(ModItems.ROYAL_SOUL_FRAGMENT.get()),
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "regalia"))),
				2.5f, 0.05f);
	}
}
