package com.nightbeam.remnants.client.model;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.Constants;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib model for the Remnants asset layout: geckolib/models/entity/&lt;geo&gt;.geo.json,
 * geckolib/animations/entity/&lt;geo&gt;.animation.json and textures/entities/&lt;texture&gt;.png. GeckoLib 5 ids name
 * those files without the folder or suffix ({@code remnant_bosses:entity/<geo>}).
 */
public class RemnantGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
	private final Identifier model, animation, texture;

	public RemnantGeoModel(String geo) {
		this(geo, geo);
	}

	public RemnantGeoModel(String geo, String texture) {
		this.model = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "entity/" + geo);
		this.animation = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "entity/" + geo);
		this.texture = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entities/" + texture + ".png");
	}

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return model;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return texture;
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		return animation;
	}

	/**
	 * Per-frame bone changes on top of the animation, from data the model captured in {@link #addAdditionalStateData}.
	 * Snapshot rotations are offsets from the bone's base rotation. Called by {@code RemnantGeoRenderer}.
	 */
	public void adjustBones(GeoRenderState renderState, BoneSnapshots bones) {
	}

	/** Hides a bone and everything under it for this frame. */
	public static void hide(BoneSnapshots bones, String name, boolean hidden) {
		bones.ifPresent(name, bone -> bone.skipRender(hidden).skipChildrenRender(hidden));
	}
}
