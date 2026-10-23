package com.nightbeam.remnants.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * Remnants GeckoLib renderer: emissive {@code _glowmask} layer, no vanilla death flop because
 * every Remnants creature has its own held death clip, and the model's own bone changes ({@link RemnantGeoModel#adjustBones}).
 */
public class RemnantGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T, EntityRenderState> {
	public RemnantGeoRenderer(EntityRendererProvider.Context context, String id, float shadow) {
		this(context, new RemnantGeoModel<>(id), shadow);
	}

	public RemnantGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadow) {
		this(context, model, shadow, true);
	}

	/** {@code glowLayer} false lets a subclass add its own glow layer. */
	protected RemnantGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadow, boolean glowLayer) {
		super(context, model);
		this.shadowRadius = shadow;
		if (glowLayer) withRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0;
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
		if (getGeoModel() instanceof RemnantGeoModel<?> model) model.adjustBones(renderPassInfo.renderState(), snapshots);
	}
}
