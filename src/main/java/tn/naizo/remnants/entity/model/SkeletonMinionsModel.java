package tn.naizo.remnants.entity.model;

import tn.naizo.remnants.entity.SkeletonMinionsEntity;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class SkeletonMinionsModel extends GeoModel<SkeletonMinionsEntity> {
	@Override
	public ResourceLocation getAnimationResource(SkeletonMinionsEntity entity) {
		return new ResourceLocation("remnant_bosses", "animations/skeleton_minion.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SkeletonMinionsEntity entity) {
		return new ResourceLocation("remnant_bosses", "geo/skeleton_minion.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonMinionsEntity entity) {
		return new ResourceLocation("remnant_bosses", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(SkeletonMinionsEntity animatable, long instanceId, AnimationState animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("head");
		if (head != null) {
			EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
