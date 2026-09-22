package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.HollowSovereignEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class HollowSovereignModel extends DefaultedEntityGeoModel<HollowSovereignEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "hollow_sovereign");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/hollow_sovereign.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/hollow_sovereign.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/hollow_sovereign.png");

	public HollowSovereignModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(HollowSovereignEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(HollowSovereignEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(HollowSovereignEntity animatable) {
		return ANIMATION_ID;
	}
}
