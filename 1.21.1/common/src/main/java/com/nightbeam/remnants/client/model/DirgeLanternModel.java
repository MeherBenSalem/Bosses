package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.DirgeLanternEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class DirgeLanternModel extends DefaultedEntityGeoModel<DirgeLanternEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "dirge_lantern");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/dirge_lantern.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/dirge_lantern.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/dirge_lantern.png");

	public DirgeLanternModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(DirgeLanternEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(DirgeLanternEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(DirgeLanternEntity animatable) {
		return ANIMATION_ID;
	}
}
