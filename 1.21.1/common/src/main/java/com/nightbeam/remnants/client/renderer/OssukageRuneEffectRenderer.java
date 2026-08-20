package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.OssukageRuneEffectModel;
import com.nightbeam.remnants.entity.OssukageRuneEffectEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OssukageRuneEffectRenderer extends GeoEntityRenderer<OssukageRuneEffectEntity> {
	public OssukageRuneEffectRenderer(EntityRendererProvider.Context context) {
		super(context, new OssukageRuneEffectModel());
		this.shadowRadius = 0.0f;
	}
}
