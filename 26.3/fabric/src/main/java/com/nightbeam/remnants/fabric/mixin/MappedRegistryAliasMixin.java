package com.nightbeam.remnants.fabric.mixin;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Registry-level alias for every name lookup: saved entities and item stacks decode through the registries'
 * name codecs since 1.21.5, as do item components nested in codecs, commands, loot tables and spawn-egg entity data. Only ENTITY_TYPE
 * and ITEM names in this mod's namespaces are considered, and only when the name itself is not
 * registered and its resolved target is ({@code containsKey} is deliberately left alone so that guard
 * stays exact). Defaulted registries reach these through {@code super.get}.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryAliasMixin<T> {
    @ModifyVariable(method = {
            "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;",
            "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;"
    }, at = @At("HEAD"), argsOnly = true)
    private Identifier remnants$aliasName(Identifier name) {
        return RegistryAliases.lookupName((Registry<?>) (Object) this, name);
    }

    @SuppressWarnings("unchecked")
    @ModifyVariable(method = {
            "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"
    }, at = @At("HEAD"), argsOnly = true)
    private ResourceKey<T> remnants$aliasKey(ResourceKey<T> key) {
        return RegistryAliases.lookupKey((Registry<T>) (Object) this, key);
    }
}
