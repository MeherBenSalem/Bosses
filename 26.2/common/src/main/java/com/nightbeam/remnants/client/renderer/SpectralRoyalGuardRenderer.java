package com.nightbeam.remnants.client.renderer;

import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.util.RenderUtil;
import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import com.nightbeam.remnants.entity.SpectralRoyalGuardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** The sentinel model with the spectral texture (same UVs), drawn translucent; eyes glow from the sentinel glowmask. */
public class SpectralRoyalGuardRenderer extends RemnantGeoRenderer<SpectralRoyalGuardEntity> {
	private static final Identifier SENTINEL_GLOWMASK = RenderUtil.getEmissiveResource(
			Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entities/hollow_sentinel.png"));
	private static final int TINT = 0xAAFFFFFF;

	public SpectralRoyalGuardRenderer(EntityRendererProvider.Context context) {
		super(context, new RemnantGeoModel<>("hollow_sentinel", "hollow_sentinel_spectral"), 0.35f, false);
		withRenderLayer(new AutoGlowingGeoLayer<>(this) {
			@Override
			protected Identifier getTextureResource(EntityRenderState renderState) {
				return SENTINEL_GLOWMASK;
			}
		});
	}

	@Override
	public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	public int getRenderColor(SpectralRoyalGuardEntity animatable, @Nullable Void relatedObject, float partialTick) {
		return TINT;
	}

	@Override
	protected AABB getBoundingBoxForCulling(SpectralRoyalGuardEntity entity) {
		return entity.cullingBox();
	}
}
