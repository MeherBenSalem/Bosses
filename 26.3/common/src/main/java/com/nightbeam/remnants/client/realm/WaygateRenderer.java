package com.nightbeam.remnants.client.realm;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Draws the whole Waygate arch from its core: translucent for the vortex layers, with an emissive glowmask for
 * runes, corona and vortex. GeckoLib rotates the model by the core's facing.
 */
public class WaygateRenderer extends GeoBlockRenderer<EclipseWaygateBlockEntity, BlockEntityRenderState> {
	public WaygateRenderer(BlockEntityRendererProvider.Context context) {
		super(context, new Model());
		withRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public @Nullable RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	public boolean shouldRenderOffScreen() {
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
		private static final Identifier GEO = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/eclipse_waygate");
		private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/eclipse_waygate");
		private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/block/eclipse_waygate.png");

		@Override
		public Identifier getModelResource(GeoRenderState renderState) {
			return GEO;
		}

		@Override
		public Identifier getTextureResource(GeoRenderState renderState) {
			return TEXTURE;
		}

		@Override
		public Identifier getAnimationResource(EclipseWaygateBlockEntity animatable) {
			return ANIMATION;
		}
	}
}
