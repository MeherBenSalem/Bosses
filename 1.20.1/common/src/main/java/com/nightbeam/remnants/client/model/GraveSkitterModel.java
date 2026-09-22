package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.GraveSkitterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GraveSkitterModel extends DefaultedEntityGeoModel<GraveSkitterEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "grave_skitter");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/grave_skitter.png");

	public GraveSkitterModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(GraveSkitterEntity animatable) {
		return TEXTURE_ID;
	}
}
