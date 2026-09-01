package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.KotsukageModel;
import com.nightbeam.remnants.entity.KotsukageEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KotsukageRenderer extends GeoEntityRenderer<KotsukageEntity> {
	public KotsukageRenderer(EntityRendererProvider.Context context) {
		super(context, new KotsukageModel());
		this.shadowRadius = 1.4f;
	}
}
