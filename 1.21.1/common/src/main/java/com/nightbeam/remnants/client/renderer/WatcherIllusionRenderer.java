package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import com.nightbeam.remnants.entity.WatcherIllusionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * A memory illusion: the Watcher's model with the paler illusion texture (same UVs), drawn
 * translucent at alpha 200 without a shadow. Its aegis never shows and its eye flickers; the glow comes from
 * the Watcher's own glowmask.
 */
public class WatcherIllusionRenderer extends GeoEntityRenderer<WatcherIllusionEntity> {
	private static final ResourceLocation WATCHER_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/entities/forgotten_watcher.png");
	private static final Color TINT = Color.ofARGB(200, 255, 255, 255);

	public WatcherIllusionRenderer(EntityRendererProvider.Context context) {
		super(context, new RemnantGeoModel<>("forgotten_watcher", "forgotten_watcher_illusion") {
			@Override
			public void setCustomAnimations(WatcherIllusionEntity animatable, long instanceId, AnimationState<WatcherIllusionEntity> animationState) {
				super.setCustomAnimations(animatable, instanceId, animationState);
				getBone("aegis").ifPresent(bone -> bone.setHidden(true));
				if ((animatable.tickCount + animatable.getId() * 7) % 23 >= 2) return;
				getBone("eye").ifPresent(bone -> {
					bone.setScaleX(0.2f);
					bone.setScaleY(0.2f);
				});
			}
		});
		this.shadowRadius = 0;
		addRenderLayer(new AutoGlowingGeoLayer<>(this) {
			@Override
			protected ResourceLocation getTextureResource(WatcherIllusionEntity animatable) {
				return WATCHER_TEXTURE;
			}
		});
	}

	@Override
	public RenderType getRenderType(WatcherIllusionEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public Color getRenderColor(WatcherIllusionEntity animatable, float partialTick, int packedLight) {
		return TINT;
	}

	@Override
	protected float getDeathMaxRotation(WatcherIllusionEntity animatable) {
		return 0;
	}
}
