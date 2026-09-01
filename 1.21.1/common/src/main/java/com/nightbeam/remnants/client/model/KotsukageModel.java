package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.KotsukageEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class KotsukageModel extends DefaultedEntityGeoModel<KotsukageEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "kotsukage");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/kotsukage.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/kotsukage.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/kotsukage.png");

	public KotsukageModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(KotsukageEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(KotsukageEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(KotsukageEntity animatable) {
		return ANIMATION_ID;
	}
}
