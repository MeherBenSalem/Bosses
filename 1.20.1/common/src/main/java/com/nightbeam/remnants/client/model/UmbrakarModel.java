package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.UmbrakarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class UmbrakarModel extends DefaultedEntityGeoModel<UmbrakarEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "umbrakar");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/umbrakar.png");

	public UmbrakarModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(UmbrakarEntity animatable) {
		return TEXTURE_ID;
	}
}
