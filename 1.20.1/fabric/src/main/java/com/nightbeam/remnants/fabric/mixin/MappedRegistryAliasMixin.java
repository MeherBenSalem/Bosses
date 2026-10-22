package com.nightbeam.remnants.fabric.mixin;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Registry-level fallback for name lookups that bypass {@code EntityType.by} and {@code ItemStack.of}
 * (commands, loot tables, codecs, spawn-egg entity tags). Only ENTITY_TYPE and ITEM names in this
 * mod's namespaces are considered, and only when the name itself is not registered and its resolved
 * target is ({@code containsKey} is deliberately left alone so that guard stays exact).
 * Defaulted registries reach these through {@code super.get}.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryAliasMixin<T> {
    @ModifyVariable(method = "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation remnants$aliasName(ResourceLocation name) {
        return RegistryAliases.lookupName((Registry<?>) (Object) this, name);
    }

    @SuppressWarnings("unchecked")
    @ModifyVariable(method = {
            "get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"
    }, at = @At("HEAD"), argsOnly = true)
    private ResourceKey<T> remnants$aliasKey(ResourceKey<T> key) {
        return RegistryAliases.lookupKey((Registry<T>) (Object) this, key);
    }
}
