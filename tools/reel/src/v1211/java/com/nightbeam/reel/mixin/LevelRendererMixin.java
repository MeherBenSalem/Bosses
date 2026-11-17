package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelClock;
import com.nightbeam.reel.ReelDirector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Under time control every dirty visible section is meshed synchronously before the frame is drawn, so a captured
 * frame never shows a hole where terrain is still compiling.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow private ClientLevel level;
	@Shadow private SectionRenderDispatcher sectionRenderDispatcher;
	@Shadow @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

	@Inject(method = "compileSections", at = @At("HEAD"), cancellable = true)
	private void reel$compileAll(Camera camera, CallbackInfo ci) {
		if (!ReelClock.timeControl) return;
		RenderRegionCache cache = new RenderRegionCache();
		int waiting = 0;
		for (SectionRenderDispatcher.RenderSection section : visibleSections) {
			if (!section.isDirty()) continue;
			if (level.getLightEngine().lightOnInSection(SectionPos.of(section.getOrigin()))) {
				sectionRenderDispatcher.rebuildSectionSync(section, cache);
				section.setNotDirty();
			} else {
				waiting++;
			}
		}
		sectionRenderDispatcher.uploadAllPendingUploads();
		ReelDirector.unlitSections = waiting;
		ci.cancel();
	}
}
