package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class UmbrakarOrbModel extends DefaultedEntityGeoModel<UmbrakarOrbEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "umbrakar_orb");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/umbrakar_orb.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/umbrakar_orb.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/umbrakar_orb.png");

	public UmbrakarOrbModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(UmbrakarOrbEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(UmbrakarOrbEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(UmbrakarOrbEntity animatable) {
		return ANIMATION_ID;
	}
}
