package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.HollowSovereignModel;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HollowSovereignRenderer extends GeoEntityRenderer<HollowSovereignEntity> {
	public HollowSovereignRenderer(EntityRendererProvider.Context context) {
		super(context, new HollowSovereignModel());
		this.shadowRadius = 1.2f;
	}
}
