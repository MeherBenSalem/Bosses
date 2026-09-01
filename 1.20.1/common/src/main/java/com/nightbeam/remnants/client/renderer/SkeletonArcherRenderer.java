package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.SkeletonArcherModel;
import com.nightbeam.remnants.entity.SkeletonArcherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SkeletonArcherRenderer extends GeoEntityRenderer<SkeletonArcherEntity> {
	public SkeletonArcherRenderer(EntityRendererProvider.Context context) {
		super(context, new SkeletonArcherModel());
		this.shadowRadius = 0.5f;
	}
}
