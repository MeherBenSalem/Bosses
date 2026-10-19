package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Inventories, containers and item entities load their stacks through {@code ItemStack.of}.
 * A retired item ID is replaced by its successor on a copy of the tag; the caller's tag is untouched.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAliasMixin {
    @ModifyVariable(method = "of(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), argsOnly = true)
    private static CompoundTag remnants$migrateSavedId(CompoundTag tag) {
        return RegistryAliases.rewriteItemId(tag);
    }
}
