package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.UmbrakarModel;
import com.nightbeam.remnants.entity.UmbrakarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UmbrakarRenderer extends GeoEntityRenderer<UmbrakarEntity> {
	public UmbrakarRenderer(EntityRendererProvider.Context context) {
		super(context, new UmbrakarModel());
		this.shadowRadius = 3.6f;
	}
}
