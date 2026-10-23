package com.nightbeam.remnants.client.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class OssukageModel extends GeoModel<RemnantOssukageEntity> {
	private static final Identifier MODEL_ID = Identifier.fromNamespaceAndPath("remnant_bosses", "entity/ossukage");
	private static final Identifier ANIMATION_ID = Identifier.fromNamespaceAndPath("remnant_bosses", "entity/ossukage");
	private static final Identifier PHASE_ONE = Identifier.fromNamespaceAndPath("remnant_bosses", "textures/entities/ossukage.png");
	private static final Identifier PHASE_TWO = Identifier.fromNamespaceAndPath("remnant_bosses", "textures/entities/ossukage_phase_two.png");
	private static final DataTicket<Boolean> TRANSFORMED = DataTicket.create("remnant_bosses_ossukage_transformed", Boolean.class);

	@Override
	public void addAdditionalStateData(RemnantOssukageEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		renderState.addGeckolibData(TRANSFORMED, animatable.isTransformed());
	}

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return MODEL_ID;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return renderState.getOrDefaultGeckolibData(TRANSFORMED, false) ? PHASE_TWO : PHASE_ONE;
	}

	@Override
	public Identifier getAnimationResource(RemnantOssukageEntity animatable) {
		return ANIMATION_ID;
	}
}
