package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.UmbrakarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class UmbrakarModel extends DefaultedEntityGeoModel<UmbrakarEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "umbrakar");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/umbrakar.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/umbrakar.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/umbrakar.png");

	public UmbrakarModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(UmbrakarEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(UmbrakarEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(UmbrakarEntity animatable) {
		return ANIMATION_ID;
	}
}
