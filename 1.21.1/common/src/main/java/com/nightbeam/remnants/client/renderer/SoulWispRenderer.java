package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.entity.SoulWispEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** A soul flame: translucent, always lit, no shadow. */
public class SoulWispRenderer extends RemnantGeoRenderer<SoulWispEntity> {
	public SoulWispRenderer(EntityRendererProvider.Context context) {
		super(context, "soul_wisp", 0);
	}

	@Override
	public RenderType getRenderType(SoulWispEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	protected int getBlockLightLevel(SoulWispEntity entity, BlockPos pos) {
		return 15;
	}
}
