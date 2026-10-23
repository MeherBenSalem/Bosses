package com.nightbeam.remnants.client.model;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The {@code aegis} bone shows only while the aegis is up. Its plate {@code aegis_0} stays
 * hidden (the open segment), and the bone is turned so that gap faces the synced world bearing whatever the
 * body and the {@code core} bone are doing; the {@code shield} clip's scale-up still plays.
 */
public class ForgottenWatcherModel extends RemnantGeoModel<ForgottenWatcherEntity> {
	/** The aegis' wanted Y rotation (radians, before the core's turn is taken off); absent while the aegis is down. */
	private static final DataTicket<Float> AEGIS_YAW = DataTicket.create("remnant_bosses_aegis_yaw", Float.class);

	public ForgottenWatcherModel() {
		super("forgotten_watcher");
	}

	@Override
	public void addAdditionalStateData(ForgottenWatcherEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
		float partial = renderState.getPartialTick();
		float time = animatable.level().getGameTime() + partial;
		if (!animatable.aegisShown(time)) return;
		// Plate 0 sits at model +Z; with the entity turned by (180 - body yaw), this rotation points it at the gap bearing.
		float body = Mth.rotLerp(partial, animatable.yBodyRotO, animatable.yBodyRot);
		renderState.addGeckolibData(AEGIS_YAW, (float) Math.toRadians(body - animatable.aegisGapYaw(time) - 180));
	}

	@Override
	public void adjustBones(GeoRenderState renderState, BoneSnapshots bones) {
		Float yaw = renderState.getGeckolibData(AEGIS_YAW);
		hide(bones, "aegis", yaw == null);
		if (yaw == null) return;
		hide(bones, "aegis_0", true);
		float core = bones.get("core").map(bone -> bone.getBone().baseRotY() + bone.getRotY()).orElse(0f);
		bones.ifPresent("aegis", aegis -> aegis.setRotY(yaw - core - aegis.getBone().baseRotY()));
	}
}
