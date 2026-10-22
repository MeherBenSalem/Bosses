package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.client.model.RemnantGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Remnants GeckoLib renderer: emissive {@code _glowmask} layer, and no vanilla death flop because
 * every Remnants creature has its own held death clip.
 */
public class RemnantGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
	public RemnantGeoRenderer(EntityRendererProvider.Context context, String id, float shadow) {
		this(context, new RemnantGeoModel<>(id), shadow);
	}

	public RemnantGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadow) {
		super(context, model);
		this.shadowRadius = shadow;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	protected float getDeathMaxRotation(T animatable) {
		return 0;
	}
}
