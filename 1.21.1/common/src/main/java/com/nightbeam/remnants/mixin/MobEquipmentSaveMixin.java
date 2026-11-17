package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.entity.EquipmentSaveGuard;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Before a Remnant entity writes its equipment, any stack that cannot be encoded is repaired or cleared
 * (see {@link EquipmentSaveGuard}). The lists are edited directly so no equip sound or game event fires
 * while a chunk is being saved.
 */
@Mixin(Mob.class)
public abstract class MobEquipmentSaveMixin {
	@Accessor("handItems") protected abstract NonNullList<ItemStack> remnant$handItems();
	@Accessor("armorItems") protected abstract NonNullList<ItemStack> remnant$armorItems();
	@Accessor("bodyArmorItem") protected abstract ItemStack remnant$bodyArmorItem();
	@Accessor("bodyArmorItem") protected abstract void remnant$setBodyArmorItem(ItemStack stack);

	@Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
	private void remnant$repairUnsavableEquipment(CompoundTag tag, CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (!EquipmentSaveGuard.covers(mob)) return;
		HolderLookup.Provider registries = mob.registryAccess();
		EquipmentSaveGuard.repair(mob, remnant$handItems(), registries);
		EquipmentSaveGuard.repair(mob, remnant$armorItems(), registries);
		ItemStack body = remnant$bodyArmorItem();
		ItemStack fixed = EquipmentSaveGuard.repair(mob, body, registries);
		if (fixed != body) remnant$setBodyArmorItem(fixed);
	}
}
