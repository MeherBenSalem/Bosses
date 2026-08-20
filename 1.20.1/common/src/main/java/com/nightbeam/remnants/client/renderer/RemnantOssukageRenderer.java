package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.OssukageModel;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RemnantOssukageRenderer extends GeoEntityRenderer<RemnantOssukageEntity> {
	public RemnantOssukageRenderer(EntityRendererProvider.Context context) {
		super(context, new OssukageModel());
		this.shadowRadius = 0.8f;
	}
}
