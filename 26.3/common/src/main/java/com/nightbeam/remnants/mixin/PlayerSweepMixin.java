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
 * runs at the end of the sweep, after its victims were hurt.
 */
@Mixin(Player.class)
public abstract class PlayerSweepMixin {
    // No descriptor: NeoForge's attack calls its own doSweepAttack overload with a sweep hit box.
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;doSweepAttack", shift = At.Shift.AFTER))
    private void remnant$graspSweep(Entity target, CallbackInfo ci) {
        WardenRelics.onSweep((Player) (Object) this, target);
    }
}
