package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * The {@code aegis} bone shows only while the aegis is up. Its plate {@code aegis_0} stays
 * hidden (the open segment), and the bone is turned so that gap faces the synced world bearing whatever the
 * body and the {@code core} bone are doing; the {@code shield} clip's scale-up still plays.
 */
public class ForgottenWatcherModel extends RemnantGeoModel<ForgottenWatcherEntity> {
	public ForgottenWatcherModel() {
		super("forgotten_watcher");
	}

	@Override
	public void setCustomAnimations(ForgottenWatcherEntity animatable, long instanceId, AnimationState<ForgottenWatcherEntity> animationState) {
		super.setCustomAnimations(animatable, instanceId, animationState);
		float partial = animationState.getPartialTick();
		float time = animatable.level().getGameTime() + partial;
		boolean shown = animatable.aegisShown(time);
		getBone("aegis").ifPresent(aegis -> {
			aegis.setHidden(!shown);
			if (!shown) return;
			getBone("aegis_0").ifPresent(gap -> gap.setHidden(true));
			// Plate 0 sits at model +Z; with the entity turned by (180 - body yaw), this rotation points it at the gap bearing.
			float body = Mth.rotLerp(partial, animatable.yBodyRotO, animatable.yBodyRot);
			float core = getBone("core").map(bone -> bone.getRotY()).orElse(0f);
			aegis.setRotY((float) Math.toRadians(body - animatable.aegisGapYaw(time) - 180) - core);
		});
	}
}
