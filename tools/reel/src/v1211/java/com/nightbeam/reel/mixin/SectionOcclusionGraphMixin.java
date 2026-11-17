package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelClock;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Future;

/** Under time control the visibility graph is rebuilt in the same frame the camera moved, never a frame late. */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
	@Shadow private Future<?> fullUpdateTask;

	@Inject(method = "update", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;runPartialUpdate(ZLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;Lnet/minecraft/world/phys/Vec3;)V"))
	private void reel$waitForGraph(boolean smartCull, Camera camera, Frustum frustum, List<SectionRenderDispatcher.RenderSection> sections,
			CallbackInfo ci) {
		if (!ReelClock.timeControl || fullUpdateTask == null) return;
		try {
			fullUpdateTask.get();
		} catch (Exception ignored) {
		}
	}
}
