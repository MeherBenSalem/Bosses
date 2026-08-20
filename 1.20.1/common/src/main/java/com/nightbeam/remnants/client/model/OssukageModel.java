package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class OssukageModel extends DefaultedEntityGeoModel<RemnantOssukageEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "ossukage");
	private static final ResourceLocation MODEL_ID = new ResourceLocation("remnant_bosses", "geo/entity/ossukage.geo.json");
	private static final ResourceLocation ANIMATION_ID = new ResourceLocation("remnant_bosses", "animations/entity/ossukage.animation.json");
	private static final ResourceLocation PHASE_ONE = new ResourceLocation("remnant_bosses", "textures/entities/ossukage.png");
	private static final ResourceLocation PHASE_TWO = new ResourceLocation("remnant_bosses", "textures/entities/ossukage_phase_two.png");

	public OssukageModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getModelResource(RemnantOssukageEntity animatable) {
		return MODEL_ID;
	}

	@Override
	public ResourceLocation getTextureResource(RemnantOssukageEntity animatable) {
		return animatable.isTransformed() ? PHASE_TWO : PHASE_ONE;
	}

	@Override
	public ResourceLocation getAnimationResource(RemnantOssukageEntity animatable) {
		return ANIMATION_ID;
	}
}
