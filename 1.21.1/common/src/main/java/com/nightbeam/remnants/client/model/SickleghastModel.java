package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.SickleghastEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SickleghastModel extends DefaultedEntityGeoModel<SickleghastEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "sickleghast");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/sickleghast.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/sickleghast.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/sickleghast.png");

	public SickleghastModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(SickleghastEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(SickleghastEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(SickleghastEntity animatable) {
		return ANIMATION_ID;
	}
}
