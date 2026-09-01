package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.SkeletonMeleeModel;
import com.nightbeam.remnants.entity.SkeletonMeleeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SkeletonMeleeRenderer extends GeoEntityRenderer<SkeletonMeleeEntity> {
	public SkeletonMeleeRenderer(EntityRendererProvider.Context context) {
		super(context, new SkeletonMeleeModel());
		this.shadowRadius = 0.55f;
	}
}
