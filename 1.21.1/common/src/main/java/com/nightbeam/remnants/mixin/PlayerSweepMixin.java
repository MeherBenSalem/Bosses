package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.event.WardenRelics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Warden's Grasp sweeps slow their victims. Neither loader has a common "after the sweep" hook, so this
 * runs where vanilla (and NeoForge's patched copy) plays the sweep, after the sweep victims were hurt.
 */
@Mixin(Player.class)
public abstract class PlayerSweepMixin {
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;sweepAttack()V"))
    private void remnant$graspSweep(Entity target, CallbackInfo ci) {
        WardenRelics.onSweep((Player) (Object) this, target);
    }
}
