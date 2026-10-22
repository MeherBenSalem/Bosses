package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.realm.RealmProtection;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explosions in the Realm still hurt, but never break its terrain. */
@Mixin(Explosion.class)
public abstract class RealmExplosionMixin {
	// Accessors resolve through the refmap; Forge 1.20.1 jars keep shadowed names unmapped (MixinPolicyRegression).
	@Accessor("level")
	protected abstract Level remnant$level();

	@Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)
	private void remnant$realmProtected(CallbackInfoReturnable<Boolean> cir) {
		if (RealmProtection.active(remnant$level())) cir.setReturnValue(false);
	}
}
