package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.SkeletonMeleeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SkeletonMeleeModel extends DefaultedEntityGeoModel<SkeletonMeleeEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "skeleton_melee");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/skeleton_melee.png");

	public SkeletonMeleeModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonMeleeEntity animatable) {
		return TEXTURE_ID;
	}
}
