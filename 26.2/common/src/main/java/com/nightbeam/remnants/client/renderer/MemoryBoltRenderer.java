package com.nightbeam.remnants.client.renderer;

import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.entity.MemoryBoltEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** A violet crystal shard, turned along its flight (yaw and pitch) since it has no body rotation; always lit. */
public class MemoryBoltRenderer extends RemnantGeoRenderer<MemoryBoltEntity> {
	public MemoryBoltRenderer(EntityRendererProvider.Context context) {
		super(context, "memory_bolt", 0);
	}

	@Override
	public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	public void addRenderData(MemoryBoltEntity animatable, @Nullable Void relatedObject, EntityRenderState renderState, float partialTick) {
		RemnantBoltRenderer.faceFlight(animatable, renderState, partialTick);
	}

	@Override
	protected void applyRotations(RenderPassInfo<EntityRenderState> renderPassInfo, PoseStack poseStack, float nativeScale) {
		super.applyRotations(renderPassInfo, poseStack, nativeScale);
		RemnantBoltRenderer.pitch(renderPassInfo, poseStack);
	}

	@Override
	protected int getBlockLightLevel(MemoryBoltEntity entity, BlockPos pos) {
		return 15;
	}
}
