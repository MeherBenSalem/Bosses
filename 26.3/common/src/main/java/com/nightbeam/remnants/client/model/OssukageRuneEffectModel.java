package com.nightbeam.remnants.client.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.entity.OssukageRuneEffectEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class OssukageRuneEffectModel extends GeoModel<OssukageRuneEffectEntity> {
	private static final DataTicket<Boolean> LEAVES = DataTicket.create("remnant_bosses_rune_leaves", Boolean.class);

	private static Identifier resource(String path) {
		return Identifier.fromNamespaceAndPath("remnant_bosses", path);
	}

	@Override
	public void addAdditionalStateData(OssukageRuneEffectEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		renderState.addGeckolibData(LEAVES, animatable.getVariant() == OssukageRuneEffectEntity.LEAVES_VARIANT);
	}

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return resource(renderState.getOrDefaultGeckolibData(LEAVES, false)
				? "entity/ossukage_rune_leaves" : "entity/ossukage_rune_spawn");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return resource(renderState.getOrDefaultGeckolibData(LEAVES, false)
				? "textures/entities/ossukage_rune_leaves.png" : "textures/entities/ossukage_rune_spawn.png");
	}

	@Override
	public Identifier getAnimationResource(OssukageRuneEffectEntity animatable) {
		return resource(animatable.getVariant() == OssukageRuneEffectEntity.LEAVES_VARIANT
				? "entity/ossukage_rune_leaves" : "entity/ossukage_rune_spawn");
	}
}
