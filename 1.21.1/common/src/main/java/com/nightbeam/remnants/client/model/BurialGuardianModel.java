package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.BurialGuardianEntity;
import software.bernie.geckolib.animation.AnimationState;

/**
 * Once the Burial Guardian has unsealed (phase 2), its {@code lid} bone stays thrown
 * open at the pose the {@code unseal} clip reaches (100 degrees about Y), whatever clip plays next.
 */
public class BurialGuardianModel extends RemnantGeoModel<BurialGuardianEntity> {
	/** GeckoLib negates Bedrock X/Y rotations when it bakes keyframes; the clip's [0, 100, 0] becomes -100. */
	private static final float LID_OPEN = (float) Math.toRadians(-100);

	public BurialGuardianModel() {
		super("burial_guardian");
	}

	@Override
	public void setCustomAnimations(BurialGuardianEntity animatable, long instanceId, AnimationState<BurialGuardianEntity> animationState) {
		super.setCustomAnimations(animatable, instanceId, animationState);
		if (!animatable.lidOpen()) return;
		getBone("lid").ifPresent(bone -> bone.setRotY(bone.getInitialSnapshot().getRotY() + LID_OPEN));
	}
}
