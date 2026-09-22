package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.HollowSovereignEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class HollowSovereignModel extends DefaultedEntityGeoModel<HollowSovereignEntity> {
	private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "hollow_sovereign");
	private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/hollow_sovereign.png");

	public HollowSovereignModel() {
		super(BASE_ID);
	}

	@Override
	public ResourceLocation getTextureResource(HollowSovereignEntity animatable) {
		return TEXTURE_ID;
	}
}
