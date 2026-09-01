package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.KotsukageTrapEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class KotsukageTrapModel extends DefaultedEntityGeoModel<KotsukageTrapEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "kotsukage_trap");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/kotsukage_trap.png");

	public KotsukageTrapModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(KotsukageTrapEntity animatable) {
		return TEXTURE_ID;
	}
}
