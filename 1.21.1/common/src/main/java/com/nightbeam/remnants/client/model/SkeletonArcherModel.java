package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.SkeletonArcherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SkeletonArcherModel extends DefaultedEntityGeoModel<SkeletonArcherEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "skeleton_archer");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/skeleton_archer.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/skeleton_archer.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/skeleton_archer.png");

	public SkeletonArcherModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(SkeletonArcherEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonArcherEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(SkeletonArcherEntity animatable) {
		return ANIMATION_ID;
	}
}
