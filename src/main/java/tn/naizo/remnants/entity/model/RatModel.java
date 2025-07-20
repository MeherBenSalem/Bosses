package tn.naizo.remnants.entity.model;

import tn.naizo.remnants.entity.RatEntity;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class RatModel extends GeoModel<RatEntity> {
	@Override
	public ResourceLocation getAnimationResource(RatEntity entity) {
		return new ResourceLocation("remnant_bosses", "animations/rat.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RatEntity entity) {
		return new ResourceLocation("remnant_bosses", "geo/rat.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RatEntity entity) {
		return new ResourceLocation("remnant_bosses", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(RatEntity animatable, long instanceId, AnimationState animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("head");
		if (head != null) {
			EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
