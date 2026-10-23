package com.nightbeam.remnants.client.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.util.RenderUtil;
import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import com.nightbeam.remnants.entity.WatcherIllusionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * A memory illusion: the Watcher's model with the paler illusion texture (same UVs), drawn
 * translucent at alpha 200 without a shadow. Its aegis never shows and its eye flickers; the glow comes from
 * the Watcher's own glowmask.
 */
public class WatcherIllusionRenderer extends RemnantGeoRenderer<WatcherIllusionEntity> {
	private static final Identifier WATCHER_GLOWMASK = RenderUtil.getEmissiveResource(
			Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entities/forgotten_watcher.png"));
	private static final int TINT = 0xC8FFFFFF;
	private static final DataTicket<Boolean> FLICKER = DataTicket.create("remnant_bosses_illusion_flicker", Boolean.class);

	public WatcherIllusionRenderer(EntityRendererProvider.Context context) {
		super(context, new RemnantGeoModel<>("forgotten_watcher", "forgotten_watcher_illusion") {
			@Override
			public void addAdditionalStateData(WatcherIllusionEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
				renderState.addGeckolibData(FLICKER, (animatable.tickCount + animatable.getId() * 7) % 23 < 2);
			}

			@Override
			public void adjustBones(GeoRenderState renderState, BoneSnapshots bones) {
				hide(bones, "aegis", true);
				if (!renderState.getOrDefaultGeckolibData(FLICKER, false)) return;
				bones.ifPresent("eye", bone -> bone.setScaleX(0.2f).setScaleY(0.2f));
			}
		}, 0, false);
		withRenderLayer(new AutoGlowingGeoLayer<>(this) {
			@Override
			protected Identifier getTextureResource(EntityRenderState renderState) {
				return WATCHER_GLOWMASK;
			}
		});
	}

	@Override
	public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	public int getRenderColor(WatcherIllusionEntity animatable, @Nullable Void relatedObject, float partialTick) {
		return TINT;
	}
}
