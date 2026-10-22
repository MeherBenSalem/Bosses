package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.client.realm.ScreenShake;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies {@link ScreenShake} to the camera only; the player's aim is untouched. */
@Mixin(Camera.class)
public abstract class CameraShakeMixin {
	// Invokers resolve through the refmap; Forge 1.20.1 jars keep shadowed names unmapped (MixinPolicyRegression).
	@Invoker("setRotation")
	protected abstract void remnant$setRotation(float yRot, float xRot);

	@Inject(method = "setup", at = @At("TAIL"))
	private void remnant$shake(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
		float[] offsets = ScreenShake.offsets(partialTick);
		if (offsets == null) return;
		Camera camera = (Camera) (Object) this;
		remnant$setRotation(camera.getYRot() + offsets[0], camera.getXRot() + offsets[1]);
	}
}
