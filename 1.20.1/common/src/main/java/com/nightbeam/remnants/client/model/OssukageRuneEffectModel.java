package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.OssukageRuneEffectEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OssukageRuneEffectModel extends GeoModel<OssukageRuneEffectEntity> {
	private static ResourceLocation resource(String path) {
		return new ResourceLocation("remnant_bosses", path);
	}

	@Override
	public ResourceLocation getModelResource(OssukageRuneEffectEntity animatable) {
		return resource(animatable.getVariant() == OssukageRuneEffectEntity.LEAVES_VARIANT
				? "geo/entity/ossukage_rune_leaves.geo.json" : "geo/entity/ossukage_rune_spawn.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(OssukageRuneEffectEntity animatable) {
		return resource(animatable.getVariant() == OssukageRuneEffectEntity.LEAVES_VARIANT
				? "textures/entities/ossukage_rune_leaves.png" : "textures/entities/ossukage_rune_spawn.png");
	}

	@Override
	public ResourceLocation getAnimationResource(OssukageRuneEffectEntity animatable) {
		return resource(animatable.getVariant() == OssukageRuneEffectEntity.LEAVES_VARIANT
				? "animations/entity/ossukage_rune_leaves.animation.json" : "animations/entity/ossukage_rune_spawn.animation.json");
	}
}
