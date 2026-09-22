package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.DirgeLanternEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class DirgeLanternModel extends DefaultedEntityGeoModel<DirgeLanternEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "dirge_lantern");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/dirge_lantern.png");

	public DirgeLanternModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(DirgeLanternEntity animatable) {
		return TEXTURE_ID;
	}
}
