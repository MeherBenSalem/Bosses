package com.nightbeam.remnants.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nightbeam.remnants.client.model.OssukageModel;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class RemnantOssukageRenderer extends GeoEntityRenderer<RemnantOssukageEntity, EntityRenderState> {
	public RemnantOssukageRenderer(EntityRendererProvider.Context context) {
		super(context, new OssukageModel());
		this.shadowRadius = 0.8f;
	}
}
