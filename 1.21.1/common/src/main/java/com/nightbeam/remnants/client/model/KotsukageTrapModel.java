package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.KotsukageTrapEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class KotsukageTrapModel extends DefaultedEntityGeoModel<KotsukageTrapEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "kotsukage_trap");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/kotsukage_trap.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/kotsukage_trap.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/kotsukage_trap.png");

	public KotsukageTrapModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(KotsukageTrapEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(KotsukageTrapEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(KotsukageTrapEntity animatable) {
		return ANIMATION_ID;
	}
}
