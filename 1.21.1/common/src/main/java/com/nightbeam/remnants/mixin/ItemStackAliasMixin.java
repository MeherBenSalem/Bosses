package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Inventories, containers and item entities load their stacks through {@code ItemStack.parse} and
 * {@code parseOptional}. A retired item ID is replaced by its successor on a copy of the tag; the
 * caller's tag is untouched. Nested stacks decoded straight from codecs rely on the loader's
 * registry-level alias instead.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAliasMixin {
    @ModifyVariable(method = "parse(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Ljava/util/Optional;",
            at = @At("HEAD"), argsOnly = true)
    private static Tag remnants$migrateParsedId(Tag tag) {
        return tag instanceof CompoundTag compound ? RegistryAliases.rewriteItemId(compound) : tag;
    }

    @ModifyVariable(method = "parseOptional(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true)
    private static CompoundTag remnants$migrateOptionalId(CompoundTag tag) {
        return RegistryAliases.rewriteItemId(tag);
    }
}
