package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelDirector;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Frame and tick hooks for the director. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void reel$frameStart(boolean renderLevel, CallbackInfo ci) {
		ReelDirector.frameStart((Minecraft) (Object) this);
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;unbindWrite()V"))
	private void reel$frameRendered(boolean renderLevel, CallbackInfo ci) {
		ReelDirector.frameRendered((Minecraft) (Object) this);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void reel$tickStart(CallbackInfo ci) {
		ReelDirector.clientTickStart((Minecraft) (Object) this);
	}
}
