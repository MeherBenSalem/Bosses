package com.nightbeam.remnants.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nightbeam.remnants.client.model.OssukageRuneEffectModel;
import com.nightbeam.remnants.entity.OssukageRuneEffectEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class OssukageRuneEffectRenderer extends GeoEntityRenderer<OssukageRuneEffectEntity, EntityRenderState> {
	public OssukageRuneEffectRenderer(EntityRendererProvider.Context context) {
		super(context, new OssukageRuneEffectModel());
		this.shadowRadius = 0.0f;
	}
}
