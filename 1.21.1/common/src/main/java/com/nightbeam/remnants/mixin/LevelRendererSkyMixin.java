package com.nightbeam.remnants.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.client.realm.EclipseSkyRenderer;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.Nullable;

/** Replaces the sky in the Eclipsed Realm with the eclipse and the Gracetree. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyMixin {
	@Shadow
	@Nullable
	private ClientLevel level;

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void remnant$eclipseSky(Matrix4f frustum, Matrix4f projection, float partialTick, Camera camera, boolean foggy,
			Runnable fogSetup, CallbackInfo ci) {
		if (level != null && EclipsedRealm.isRealm(level)) {
			PoseStack poseStack = new PoseStack();
			poseStack.mulPose(frustum);
			EclipseSkyRenderer.render(poseStack, projection, partialTick, camera, level, fogSetup);
			ci.cancel();
		}
	}
}
