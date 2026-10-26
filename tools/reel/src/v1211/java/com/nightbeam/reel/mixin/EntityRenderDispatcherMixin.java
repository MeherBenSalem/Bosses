package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The camera operator (the local player) stays out of frame unless the shot casts them. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void reel$hidePlayer(E entity, Frustum frustum, double camX, double camY, double camZ,
			CallbackInfoReturnable<Boolean> cir) {
		if (ReelCamera.hidePlayer && entity == Minecraft.getInstance().player) cir.setReturnValue(false);
	}
}
