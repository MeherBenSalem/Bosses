package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.HollowSovereignEntity;
import software.bernie.geckolib.animation.AnimationState;

/** Hides the pauldrons once they fly off (phase 2) and the breastplate once the soul bursts out (phase 3). */
public class HollowSovereignModel extends RemnantGeoModel<HollowSovereignEntity> {
	public HollowSovereignModel() {
		super("hollow_sovereign");
	}

	@Override
	public void setCustomAnimations(HollowSovereignEntity animatable, long instanceId, AnimationState<HollowSovereignEntity> animationState) {
		super.setCustomAnimations(animatable, instanceId, animationState);
		boolean pauldrons = animatable.showsPauldrons();
		getBone("pauldron_left").ifPresent(bone -> bone.setHidden(!pauldrons));
		getBone("pauldron_right").ifPresent(bone -> bone.setHidden(!pauldrons));
		getBone("breastplate").ifPresent(bone -> bone.setHidden(!animatable.showsBreastplate()));
	}
}
