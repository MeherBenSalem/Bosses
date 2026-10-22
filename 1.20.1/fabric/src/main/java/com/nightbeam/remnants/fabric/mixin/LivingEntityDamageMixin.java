package com.nightbeam.remnants.fabric.mixin;

import com.nightbeam.remnants.event.SovereignRelics;
import com.nightbeam.remnants.event.WardenRelics;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric API 0.92 (Minecraft 1.20.1) has no {@code ServerLivingEntityEvents.AFTER_DAMAGE}. This stands in
 * for it for the Regalia "Unbowed" bonus and the Warden armor's Jailer's Hold: after an accepted
 * {@code hurt}, it reports the amount left once shields and helmet/freeze adjustments ran, which is the
 * {@code damageTaken} the 1.21.1 Fabric build receives from AFTER_DAMAGE. {@code hurt} parameters are read
 * at the return, so blocking (amount 0) is visible.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Inject(method = "hurt", at = @At("RETURN"))
    private void remnants$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!cir.getReturnValueZ() || self.level().isClientSide || self.isDeadOrDying()) return;
        SovereignRelics.afterDamage(self, source, amount);
        WardenRelics.afterDamage(self, source, amount);
    }
}
