package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.init.RegistryAliases;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Saved entities in chunks, spawner data and passengers all resolve their type through
 * {@code EntityType.by(CompoundTag)}. A retired ID is rewritten in place so the entity loads, and
 * is saved again, as its successor. Unknown and retired-helper IDs are left for vanilla to skip.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeAliasMixin {
    @Inject(method = "by(Lnet/minecraft/nbt/CompoundTag;)Ljava/util/Optional;", at = @At("HEAD"))
    private static void remnants$migrateSavedId(CompoundTag tag, CallbackInfoReturnable<Optional<EntityType<?>>> cir) {
        RegistryAliases.rewriteEntityId(tag);
    }
}
