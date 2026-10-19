package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.GraveboundWardenEntity;
import software.bernie.geckolib.animation.AnimationState;

/**
 * The {@code cage_door} and {@code souls} bones are never hidden. After
 * {@code cell_break} the freed souls leave the cage, so the {@code souls} bone stays at 0.2 scale for the
 * rest of the fight (the transition and death clips scale it themselves).
 */
public class GraveboundWardenModel extends RemnantGeoModel<GraveboundWardenEntity> {
	public static final float SHRUNK_SOULS = 0.2f;

	public GraveboundWardenModel() {
		super("gravebound_warden");
	}

	@Override
	public void setCustomAnimations(GraveboundWardenEntity animatable, long instanceId, AnimationState<GraveboundWardenEntity> animationState) {
		super.setCustomAnimations(animatable, instanceId, animationState);
		if (!animatable.soulsShrunk()) return;
		getBone("souls").ifPresent(bone -> {
			bone.setScaleX(SHRUNK_SOULS);
			bone.setScaleY(SHRUNK_SOULS);
			bone.setScaleZ(SHRUNK_SOULS);
		});
	}
}
