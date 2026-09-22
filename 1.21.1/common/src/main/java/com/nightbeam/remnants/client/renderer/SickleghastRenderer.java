package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.SickleghastModel;
import com.nightbeam.remnants.entity.SickleghastEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SickleghastRenderer extends GeoEntityRenderer<SickleghastEntity> {
	public SickleghastRenderer(EntityRendererProvider.Context context) {
		super(context, new SickleghastModel());
		this.shadowRadius = 0.36f;
	}
}
