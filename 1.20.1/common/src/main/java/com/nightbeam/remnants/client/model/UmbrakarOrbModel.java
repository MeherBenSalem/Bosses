package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class UmbrakarOrbModel extends DefaultedEntityGeoModel<UmbrakarOrbEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "umbrakar_orb");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/umbrakar_orb.png");

	public UmbrakarOrbModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(UmbrakarOrbEntity animatable) {
		return TEXTURE_ID;
	}
}
