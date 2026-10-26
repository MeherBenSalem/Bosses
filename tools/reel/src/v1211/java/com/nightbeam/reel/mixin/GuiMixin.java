package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelCamera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD for the reel: "none" draws nothing; "boss" keeps the boss bars and the mod's realm titles and banners but drops
 * the vanilla hotbar, crosshair, hearts, chat and effects; "full" is the normal HUD.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void reel$hud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if ("none".equals(ReelCamera.hud)) {
			ci.cancel();
			return;
		}
		graphics.pose().pushPose();
		graphics.pose().translate(0, ReelCamera.hudOffset, 0);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void reel$hudEnd(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		graphics.pose().popPose();
	}

	@Inject(method = {"renderCrosshair", "renderHotbarAndDecorations", "renderExperienceLevel", "renderEffects", "renderChat",
			"renderOverlayMessage", "renderScoreboardSidebar", "renderTabList", "renderCameraOverlays", "renderDemoOverlay",
			"renderTitle"}, at = @At("HEAD"), cancellable = true)
	private void reel$vanillaLayer(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if ("boss".equals(ReelCamera.hud)) ci.cancel();
	}
}
