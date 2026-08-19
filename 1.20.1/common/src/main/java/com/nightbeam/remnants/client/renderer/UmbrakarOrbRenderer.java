package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.UmbrakarOrbModel;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UmbrakarOrbRenderer extends GeoEntityRenderer<UmbrakarOrbEntity> {
	public UmbrakarOrbRenderer(EntityRendererProvider.Context context) {
		super(context, new UmbrakarOrbModel());
		this.shadowRadius = 0.4f;
	}
}
