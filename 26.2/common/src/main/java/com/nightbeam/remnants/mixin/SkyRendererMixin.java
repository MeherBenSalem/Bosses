package com.nightbeam.remnants.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.client.realm.EclipseSkyRenderer;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the sky in the Eclipsed Realm with the eclipse and the Gracetree: the disc draws it, the rest is skipped. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
	private void remnant$eclipseSky(int skyColor, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && EclipsedRealm.isRealm(level)) {
			EclipseSkyRenderer.render(level, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
			ci.cancel();
		}
	}

	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
	private void remnant$noSunset(PoseStack poseStack, float sunAngle, int color, CallbackInfo ci) {
		if (remnant$inRealm()) ci.cancel();
	}

	@Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
	private void remnant$noCelestials(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase,
			float rainBrightness, float starBrightness, CallbackInfo ci) {
		if (remnant$inRealm()) ci.cancel();
	}

	@Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
	private void remnant$noDarkDisc(CallbackInfo ci) {
		if (remnant$inRealm()) ci.cancel();
	}

	private static boolean remnant$inRealm() {
		ClientLevel level = Minecraft.getInstance().level;
		return level != null && EclipsedRealm.isRealm(level);
	}
}
