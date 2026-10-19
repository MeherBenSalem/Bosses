package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.HollowSovereignModel;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HollowSovereignRenderer extends RemnantGeoRenderer<HollowSovereignEntity> {
	public HollowSovereignRenderer(EntityRendererProvider.Context context) {
		super(context, new HollowSovereignModel(), 1.2f);
	}
}
