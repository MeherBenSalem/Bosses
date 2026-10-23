package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.realm.RealmProtection;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explosions in the Realm still hurt, but never break its terrain. */
@Mixin(ServerExplosion.class)
public abstract class RealmExplosionMixin {

	@Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)
	private void remnant$realmProtected(CallbackInfoReturnable<Boolean> cir) {
		if (RealmProtection.active(((ServerExplosion) (Object) this).level())) cir.setReturnValue(false);
	}
}
