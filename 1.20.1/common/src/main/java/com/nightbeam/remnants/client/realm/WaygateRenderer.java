package com.nightbeam.remnants.client.realm;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

/**
 * Draws the whole Waygate arch from its core: translucent for the vortex layers, with an emissive glowmask for
 * runes, corona and vortex. GeckoLib rotates the model by the core's facing.
 */
public class WaygateRenderer extends GeoBlockRenderer<EclipseWaygateBlockEntity> {
	public WaygateRenderer(BlockEntityRendererProvider.Context context) {
		super(new Model());
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public RenderType getRenderType(EclipseWaygateBlockEntity animatable, ResourceLocation texture,
			@Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public boolean shouldRenderOffScreen(EclipseWaygateBlockEntity blockEntity) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	/** NeoForge asks the renderer for the culling box. */
	public AABB getRenderBoundingBox(EclipseWaygateBlockEntity blockEntity) {
		return blockEntity.getRenderBoundingBox();
	}

	private static final class Model extends GeoModel<EclipseWaygateBlockEntity> {
		private static final ResourceLocation GEO = new ResourceLocation(Constants.MOD_ID, "geo/block/eclipse_waygate.geo.json");
		private static final ResourceLocation ANIMATION = new ResourceLocation(Constants.MOD_ID, "animations/block/eclipse_waygate.animation.json");
		private static final ResourceLocation TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/block/eclipse_waygate.png");

		@Override
		public ResourceLocation getModelResource(EclipseWaygateBlockEntity animatable) {
			return GEO;
		}

		@Override
		public ResourceLocation getTextureResource(EclipseWaygateBlockEntity animatable) {
			return TEXTURE;
		}

		@Override
		public ResourceLocation getAnimationResource(EclipseWaygateBlockEntity animatable) {
			return ANIMATION;
		}
	}
}
