package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keyframed field of view, and the held item hidden when the director says so. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void reel$fov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
		if (!Double.isNaN(ReelCamera.fov) && useFovSetting) cir.setReturnValue(ReelCamera.fov);
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void reel$hand(Camera camera, float partialTick, Matrix4f projection, CallbackInfo ci) {
		if (!ReelCamera.hand) ci.cancel();
	}
}
