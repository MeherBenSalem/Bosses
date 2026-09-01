package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.KotsukageTrapModel;
import com.nightbeam.remnants.entity.KotsukageTrapEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KotsukageTrapRenderer extends GeoEntityRenderer<KotsukageTrapEntity> {
	public KotsukageTrapRenderer(EntityRendererProvider.Context context) {
		super(context, new KotsukageTrapModel());
		this.shadowRadius = 0.9f;
	}
}
