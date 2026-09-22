package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.GraveSkitterModel;
import com.nightbeam.remnants.entity.GraveSkitterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GraveSkitterRenderer extends GeoEntityRenderer<GraveSkitterEntity> {
	public GraveSkitterRenderer(EntityRendererProvider.Context context) {
		super(context, new GraveSkitterModel());
		this.shadowRadius = 0.64f;
	}
}
