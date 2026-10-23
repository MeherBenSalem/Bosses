package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.client.realm.RealmOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Eclipsed Realm's banners and crossing over the HUD, the same way on every loader. */
@Mixin(Hud.class)
public abstract class GuiRealmOverlayMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void remnant$realmOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		RealmOverlay.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
	}
}
