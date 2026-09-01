package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.SkeletonArcherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SkeletonArcherModel extends DefaultedEntityGeoModel<SkeletonArcherEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "skeleton_archer");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/skeleton_archer.png");

	public SkeletonArcherModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonArcherEntity animatable) {
		return TEXTURE_ID;
	}
}
