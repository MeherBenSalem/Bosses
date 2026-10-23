package com.nightbeam.remnants.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.nightbeam.remnants.client.realm.EclipseSkyRenderer;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the sky in the Eclipsed Realm with the eclipse and the Gracetree. Since 26.3 the vanilla sky draws in one
 * render pass that no other pass or upload may interrupt, so the whole sky is taken over before that pass opens.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void remnant$eclipseSky(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && EclipsedRealm.isRealm(level)) {
			RenderSystem.setShaderFog(skyFog);
			EclipseSkyRenderer.render(level, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
			ci.cancel();
		}
	}
}
