package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;

/**
 * Minecraft 1.20.1 armor materials are plain {@link ArmorMaterial} implementations (1.21.1 registers
 * them and armor items take a registry holder). Values match the 1.21.1 materials.
 */
public enum ModArmorMaterials implements ArmorMaterial {
	/**
	 * Texture layers: assets/remnant_bosses/textures/models/armor/regalia_layer_1.png and _layer_2.png.
	 * The namespaced name is resolved by Forge's armor-texture lookup and by Fabric API's
	 * ArmorFeatureRenderer mixin; vanilla alone would assume the minecraft namespace.
	 */
	REGALIA(Constants.MOD_ID + ":regalia", /* durability multiplier */ 35, Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
			ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.5f, 0.05f),
	/**
	 * Rusted plate over grave cloth: defense 2 / 6 / 5 / 2, toughness 1, durability multiplier between iron (15)
	 * and diamond (33). Texture layers: textures/models/armor/warden_layer_1.png and _layer_2.png.
	 */
	WARDEN(Constants.MOD_ID + ":warden", /* durability multiplier */ 24, Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6,
			ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2), 12, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0f, 0.0f),
	/**
	 * A scholar's robe over verdigris plates: defense 3 / 7 / 6 / 3, toughness 2, enchantability 20, the same
	 * durability multiplier as diamond. Texture layers: textures/models/armor/archivist_layer_1.png and _layer_2.png.
	 */
	ARCHIVIST(Constants.MOD_ID + ":archivist", /* durability multiplier */ 33, Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7,
			ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3), 20, SoundEvents.ARMOR_EQUIP_CHAIN, 2.0f, 0.0f);

	/** Vanilla per-slot durability bases (ArmorMaterials.HEALTH_FUNCTION_FOR_TYPE; ArmorItem.Type in 1.21). */
	private static final Map<ArmorItem.Type, Integer> DURABILITY_BASE = Map.of(ArmorItem.Type.HELMET, 11,
			ArmorItem.Type.CHESTPLATE, 16, ArmorItem.Type.LEGGINGS, 15, ArmorItem.Type.BOOTS, 13);

	private final String name;
	private final int durabilityMultiplier;
	private final EnumMap<ArmorItem.Type, Integer> defense;
	private final int enchantmentValue;
	private final SoundEvent equipSound;
	private final float toughness;
	private final float knockbackResistance;

	ModArmorMaterials(String name, int durabilityMultiplier, Map<ArmorItem.Type, Integer> defense, int enchantmentValue,
			SoundEvent equipSound, float toughness, float knockbackResistance) {
		this.name = name;
		this.durabilityMultiplier = durabilityMultiplier;
		this.defense = new EnumMap<>(defense);
		this.enchantmentValue = enchantmentValue;
		this.equipSound = equipSound;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
	}

	@Override
	public int getDurabilityForType(ArmorItem.Type type) {
		return DURABILITY_BASE.get(type) * durabilityMultiplier;
	}

	@Override
	public int getDefenseForType(ArmorItem.Type type) {
		return defense.get(type);
	}

	@Override
	public int getEnchantmentValue() {
		return enchantmentValue;
	}

	@Override
	public SoundEvent getEquipSound() {
		return equipSound;
	}

	/**
	 * Royal soul fragment (Regalia), grave iron (Warden) or knowledge fragment (Archivist); resolved on demand because
	 * materials exist before items are registered.
	 */
	@Override
	public Ingredient getRepairIngredient() {
		return switch (this) {
			case REGALIA -> Ingredient.of(ModItems.ROYAL_SOUL_FRAGMENT.get());
			case WARDEN -> Ingredient.of(ModItems.GRAVE_IRON.get());
			case ARCHIVIST -> Ingredient.of(ModItems.KNOWLEDGE_FRAGMENT.get());
		};
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public float getToughness() {
		return toughness;
	}

	@Override
	public float getKnockbackResistance() {
		return knockbackResistance;
	}
}
