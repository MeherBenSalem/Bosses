package com.nightbeam.remnants.client.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.entity.BurialGuardianEntity;
import org.jspecify.annotations.Nullable;

/**
 * Once the Burial Guardian has unsealed (phase 2), its {@code lid} bone stays thrown
 * open at the pose the {@code unseal} clip reaches (100 degrees about Y), whatever clip plays next.
 */
public class BurialGuardianModel extends RemnantGeoModel<BurialGuardianEntity> {
	/** GeckoLib negates Bedrock X/Y rotations when it bakes keyframes; the clip's [0, 100, 0] becomes -100. */
	private static final float LID_OPEN = (float) Math.toRadians(-100);
	private static final DataTicket<Boolean> LID_OPENED = DataTicket.create("remnant_bosses_lid_open", Boolean.class);

	public BurialGuardianModel() {
		super("burial_guardian");
	}

	@Override
	public void addAdditionalStateData(BurialGuardianEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		renderState.addGeckolibData(LID_OPENED, animatable.lidOpen());
	}

	@Override
	public void adjustBones(GeoRenderState renderState, BoneSnapshots bones) {
		if (!renderState.getOrDefaultGeckolibData(LID_OPENED, false)) return;
		bones.ifPresent("lid", bone -> bone.setRotY(LID_OPEN));
	}
}
