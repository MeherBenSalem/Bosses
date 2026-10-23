package com.nightbeam.remnants.client.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
import org.jspecify.annotations.Nullable;

/** Hides the pauldrons once they fly off (phase 2) and the breastplate once the soul bursts out (phase 3). */
public class HollowSovereignModel extends RemnantGeoModel<HollowSovereignEntity> {
	private static final DataTicket<Boolean> PAULDRONS = DataTicket.create("remnant_bosses_pauldrons", Boolean.class);
	private static final DataTicket<Boolean> BREASTPLATE = DataTicket.create("remnant_bosses_breastplate", Boolean.class);

	public HollowSovereignModel() {
		super("hollow_sovereign");
	}

	@Override
	public void addAdditionalStateData(HollowSovereignEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		renderState.addGeckolibData(PAULDRONS, animatable.showsPauldrons());
		renderState.addGeckolibData(BREASTPLATE, animatable.showsBreastplate());
	}

	@Override
	public void adjustBones(GeoRenderState renderState, BoneSnapshots bones) {
		boolean pauldrons = renderState.getOrDefaultGeckolibData(PAULDRONS, true);
		hide(bones, "pauldron_left", !pauldrons);
		hide(bones, "pauldron_right", !pauldrons);
		hide(bones, "breastplate", !renderState.getOrDefaultGeckolibData(BREASTPLATE, true));
	}
}
