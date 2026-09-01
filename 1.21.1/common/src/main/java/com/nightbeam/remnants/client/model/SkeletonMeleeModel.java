package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.SkeletonMeleeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SkeletonMeleeModel extends DefaultedEntityGeoModel<SkeletonMeleeEntity> {
	private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "skeleton_melee");
	private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/skeleton_melee.geo.json");
	private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/skeleton_melee.animation.json");
	private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/skeleton_melee.png");

	public SkeletonMeleeModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(SkeletonMeleeEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonMeleeEntity animatable) {
		return TEXTURE_ID;
	}

	@Override
	public ResourceLocation getAnimationResource(SkeletonMeleeEntity animatable) {
		return ANIMATION_ID;
	}
}
