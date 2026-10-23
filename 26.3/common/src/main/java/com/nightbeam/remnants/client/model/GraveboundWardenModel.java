package com.nightbeam.remnants.client.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.entity.GraveboundWardenEntity;
import org.jspecify.annotations.Nullable;

/**
 * The {@code cage_door} and {@code souls} bones are never hidden. After
 * {@code cell_break} the freed souls leave the cage, so the {@code souls} bone stays at 0.2 scale for the
 * rest of the fight (the transition and death clips scale it themselves).
 */
public class GraveboundWardenModel extends RemnantGeoModel<GraveboundWardenEntity> {
	public static final float SHRUNK_SOULS = 0.2f;
	private static final DataTicket<Boolean> SOULS_SHRUNK = DataTicket.create("remnant_bosses_souls_shrunk", Boolean.class);

	public GraveboundWardenModel() {
		super("gravebound_warden");
	}

	@Override
	public void addAdditionalStateData(GraveboundWardenEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		renderState.addGeckolibData(SOULS_SHRUNK, animatable.soulsShrunk());
	}

	@Override
	public void adjustBones(GeoRenderState renderState, BoneSnapshots bones) {
		if (!renderState.getOrDefaultGeckolibData(SOULS_SHRUNK, false)) return;
		bones.ifPresent("souls", bone -> bone.setScale(SHRUNK_SOULS, SHRUNK_SOULS, SHRUNK_SOULS));
	}
}
