package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import com.nightbeam.remnants.entity.RemnantBoltEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A family bolt: the Memory Bolt's shard model in the family's own texture ({@code ember_bolt},
 * {@code crystal_bolt}), turned along its flight like the Memory Bolt and always lit.
 */
public class RemnantBoltRenderer<T extends RemnantBoltEntity> extends RemnantGeoRenderer<T> {
	public RemnantBoltRenderer(EntityRendererProvider.Context context, String texture) {
		super(context, new RemnantGeoModel<>("memory_bolt", texture), 0);
	}

	@Override
	public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
		super.applyRotations(animatable, poseStack, ageInTicks, Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()), partialTick);
		// The model points along -Z; a positive pitch (falling bolt) tips its nose down.
		poseStack.translate(0, 0.25, 0);
		poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
		poseStack.translate(0, -0.25, 0);
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos pos) {
		return 15;
	}
}
