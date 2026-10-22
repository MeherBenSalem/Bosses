package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.entity.SpectralCrescentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Translucent spinning arc, turned to its flight direction since it has no body rotation. */
public class SpectralCrescentRenderer extends RemnantGeoRenderer<SpectralCrescentEntity> {
	public SpectralCrescentRenderer(EntityRendererProvider.Context context) {
		super(context, "spectral_crescent", 0);
	}

	@Override
	public RenderType getRenderType(SpectralCrescentEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	protected void applyRotations(SpectralCrescentEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
		super.applyRotations(animatable, poseStack, ageInTicks, Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()), partialTick);
	}

	@Override
	protected int getBlockLightLevel(SpectralCrescentEntity entity, BlockPos pos) {
		return 15;
	}
}
