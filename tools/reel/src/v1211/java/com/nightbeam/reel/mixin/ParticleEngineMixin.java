package com.nightbeam.reel.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nightbeam.reel.ReelCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Skips particles right in front of the lens, which a scripted camera can fly through and smear across the frame. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private void reel$clip(Particle particle, VertexConsumer buffer, Camera camera, float partialTick) {
		double clip = ReelCamera.particleClip;
		if (clip > 0 && particle.getBoundingBox().getCenter().distanceToSqr(camera.getPosition()) < clip * clip) return;
		particle.render(buffer, camera, partialTick);
	}
}
