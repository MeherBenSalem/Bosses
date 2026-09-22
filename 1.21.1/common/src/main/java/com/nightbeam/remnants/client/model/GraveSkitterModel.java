package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.GraveSkitterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GraveSkitterModel extends DefaultedEntityGeoModel<GraveSkitterEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "grave_skitter");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/grave_skitter.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/grave_skitter.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/grave_skitter.png");

	public GraveSkitterModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(GraveSkitterEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(GraveSkitterEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(GraveSkitterEntity animatable) {
		return ANIMATION_ID;
	}
}
