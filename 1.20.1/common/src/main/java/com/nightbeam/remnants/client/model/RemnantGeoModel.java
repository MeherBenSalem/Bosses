package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.Constants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Remnants asset layout: geo/entity/&lt;geo&gt;.geo.json,
 * animations/entity/&lt;geo&gt;.animation.json and textures/entities/&lt;texture&gt;.png.
 */
public class RemnantGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
	private final ResourceLocation model, animation, texture;

	public RemnantGeoModel(String geo) {
		this(geo, geo);
	}

	public RemnantGeoModel(String geo, String texture) {
		this.model = new ResourceLocation(Constants.MOD_ID, "geo/entity/" + geo + ".geo.json");
		this.animation = new ResourceLocation(Constants.MOD_ID, "animations/entity/" + geo + ".animation.json");
		this.texture = new ResourceLocation(Constants.MOD_ID, "textures/entities/" + texture + ".png");
	}

	@Override
	public ResourceLocation getModelResource(T animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(T animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(T animatable) {
		return animation;
	}
}
