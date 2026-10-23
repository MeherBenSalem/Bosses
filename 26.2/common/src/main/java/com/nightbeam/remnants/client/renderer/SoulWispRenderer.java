package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.entity.SoulWispEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** A soul flame: translucent, always lit, no shadow. */
public class SoulWispRenderer extends RemnantGeoRenderer<SoulWispEntity> {
	public SoulWispRenderer(EntityRendererProvider.Context context) {
		super(context, "soul_wisp", 0);
	}

	@Override
	public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	protected int getBlockLightLevel(SoulWispEntity entity, BlockPos pos) {
		return 15;
	}
}
