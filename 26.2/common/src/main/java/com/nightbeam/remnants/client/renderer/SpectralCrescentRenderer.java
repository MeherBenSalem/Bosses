package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.entity.SpectralCrescentEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Translucent spinning arc, turned to its flight direction since it has no body rotation. */
public class SpectralCrescentRenderer extends RemnantGeoRenderer<SpectralCrescentEntity> {
	public SpectralCrescentRenderer(EntityRendererProvider.Context context) {
		super(context, "spectral_crescent", 0);
	}

	@Override
	public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	public void addRenderData(SpectralCrescentEntity animatable, @Nullable Void relatedObject, EntityRenderState renderState, float partialTick) {
		RemnantBoltRenderer.faceFlight(animatable, renderState, partialTick);
	}

	@Override
	protected int getBlockLightLevel(SpectralCrescentEntity entity, BlockPos pos) {
		return 15;
	}
}
