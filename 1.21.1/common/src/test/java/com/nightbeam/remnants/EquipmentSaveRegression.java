package com.nightbeam.remnants;

import com.nightbeam.remnants.entity.EquipmentSaveGuard;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Ticket-0229: a Burial Guardian holding an item whose entity_data had no id crashed 1.21.1 on save
 * ("Missing id for entity in: {mob_target:...}"). The guard must turn that stack into one that saves,
 * leave good stacks alone and never touch the caller's stack.
 */
public final class EquipmentSaveRegression {
	private EquipmentSaveRegression() {
	}

	public static void main(String[] args) {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		HolderLookup.Provider registries = VanillaRegistries.createLookup();

		CompoundTag noId = new CompoundTag();
		noId.putString("mob_target", "remnants:burial_guardian");
		ItemStack broken = new ItemStack(Items.BONE, 3);
		broken.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("kept"));
		broken.set(DataComponents.ENTITY_DATA, CustomData.of(noId));
		check(!EquipmentSaveGuard.saves(broken, registries), "a stack with id-less entity_data must fail to encode (the crash)");

		ItemStack fixed = EquipmentSaveGuard.savable(broken, registries);
		check(fixed != broken, "the broken stack is replaced");
		check(EquipmentSaveGuard.saves(fixed, registries), "the repaired stack encodes");
		check(fixed.is(Items.BONE) && fixed.getCount() == 3, "item and count are kept");
		check(fixed.has(DataComponents.CUSTOM_NAME), "other components are kept");
		check(!fixed.has(DataComponents.ENTITY_DATA), "only entity_data is dropped");
		check(broken.has(DataComponents.ENTITY_DATA), "the caller's stack is not modified");

		CompoundTag withId = new CompoundTag();
		withId.putString("id", "minecraft:zombie");
		ItemStack valid = new ItemStack(Items.ZOMBIE_SPAWN_EGG);
		valid.set(DataComponents.ENTITY_DATA, CustomData.of(withId));
		check(EquipmentSaveGuard.savable(valid, registries) == valid, "valid entity_data is left alone");
		ItemStack plain = new ItemStack(Items.IRON_SWORD);
		check(EquipmentSaveGuard.savable(plain, registries) == plain, "ordinary stacks are left alone");
		check(EquipmentSaveGuard.savable(ItemStack.EMPTY, registries) == ItemStack.EMPTY, "empty slots are left alone");

		check(EquipmentSaveGuard.covers(ResourceLocation.parse("remnants:burial_guardian")), "Remnant entities are guarded");
		check(!EquipmentSaveGuard.covers(ResourceLocation.parse("minecraft:zombie")), "vanilla mobs are not guarded");
		System.out.println("Equipment save regressions passed");
	}

	private static void check(boolean ok, String message) {
		if (!ok) throw new IllegalStateException(message);
	}
}
