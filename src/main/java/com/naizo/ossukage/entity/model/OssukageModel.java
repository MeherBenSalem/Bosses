package com.naizo.ossukage.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

import com.naizo.ossukage.entity.OssukageEntity;

public class OssukageModel extends GeoModel<OssukageEntity> {
	@Override
	public ResourceLocation getAnimationResource(OssukageEntity entity) {
		return ResourceLocation.parse("remnant_ossukage:animations/ninja_skeleton.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(OssukageEntity entity) {
		return ResourceLocation.parse("remnant_ossukage:geo/ninja_skeleton.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(OssukageEntity entity) {
		return ResourceLocation.parse("remnant_ossukage:textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(OssukageEntity animatable, long instanceId, AnimationState animationState) {
		GeoBone head = getAnimationProcessor().getBone("h_head");
		if (head != null) {
			EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
