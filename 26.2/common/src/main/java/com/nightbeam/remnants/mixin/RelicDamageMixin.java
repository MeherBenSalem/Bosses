package com.nightbeam.remnants.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.event.WatcherRelics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relic and armor damage rules on every loader: Fabric has no event that changes the amount. Runs first
 * in {@code LivingEntity.hurtServer}; for players that is after vanilla's difficulty scaling in
 * {@code Player.hurtServer}. The Archivist's Archive Ward cancels the hit outright.
 */
@Mixin(LivingEntity.class)
public abstract class RelicDamageMixin {
    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void remnant$archiveWard(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (WatcherRelics.wardNegates((LivingEntity) (Object) this, source)) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
    private float remnant$relicDamage(float amount, @Local(argsOnly = true) DamageSource source) {
        return WardenRelics.scaleIncoming((LivingEntity) (Object) this, source, amount);
    }
}
