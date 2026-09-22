package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.SickleghastEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SickleghastModel extends DefaultedEntityGeoModel<SickleghastEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "sickleghast");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/sickleghast.png");

	public SickleghastModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(SickleghastEntity animatable) {
		return TEXTURE_ID;
	}
}
