package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.KotsukageEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class KotsukageModel extends DefaultedEntityGeoModel<KotsukageEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "kotsukage");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/kotsukage.png");

	public KotsukageModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(KotsukageEntity animatable) {
		return TEXTURE_ID;
	}
}
