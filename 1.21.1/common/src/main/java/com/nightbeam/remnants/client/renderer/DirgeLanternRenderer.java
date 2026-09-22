package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.DirgeLanternModel;
import com.nightbeam.remnants.entity.DirgeLanternEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DirgeLanternRenderer extends GeoEntityRenderer<DirgeLanternEntity> {
	public DirgeLanternRenderer(EntityRendererProvider.Context context) {
		super(context, new DirgeLanternModel());
		this.shadowRadius = 0.34f;
	}
}
