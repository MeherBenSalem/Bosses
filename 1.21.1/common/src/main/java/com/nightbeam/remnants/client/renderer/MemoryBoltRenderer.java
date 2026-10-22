package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.remnants.entity.MemoryBoltEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A violet crystal shard, turned along its flight (yaw and pitch) since it has no body rotation; always lit. */
public class MemoryBoltRenderer extends RemnantGeoRenderer<MemoryBoltEntity> {
	public MemoryBoltRenderer(EntityRendererProvider.Context context) {
		super(context, "memory_bolt", 0);
	}

	@Override
	public RenderType getRenderType(MemoryBoltEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	protected void applyRotations(MemoryBoltEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
		super.applyRotations(animatable, poseStack, ageInTicks, Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()), partialTick, nativeScale);
		// The model points along -Z; a positive pitch (falling bolt) tips its nose down.
		poseStack.translate(0, 0.25, 0);
		poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
		poseStack.translate(0, -0.25, 0);
	}

	@Override
	protected int getBlockLightLevel(MemoryBoltEntity entity, BlockPos pos) {
		return 15;
	}
}
