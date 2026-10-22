package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import com.nightbeam.remnants.entity.SpectralRoyalGuardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.core.object.Color;

/** The sentinel model with the spectral texture (same UVs), drawn translucent; eyes glow from the sentinel glowmask. */
public class SpectralRoyalGuardRenderer extends GeoEntityRenderer<SpectralRoyalGuardEntity> {
	private static final ResourceLocation SENTINEL_TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/entities/hollow_sentinel.png");
	private static final Color TINT = Color.ofRGBA(255, 255, 255, 170);

	public SpectralRoyalGuardRenderer(EntityRendererProvider.Context context) {
		super(context, new RemnantGeoModel<>("hollow_sentinel", "hollow_sentinel_spectral"));
		this.shadowRadius = 0.35f;
		addRenderLayer(new AutoGlowingGeoLayer<>(this) {
			@Override
			protected ResourceLocation getTextureResource(SpectralRoyalGuardEntity animatable) {
				return SENTINEL_TEXTURE;
			}
		});
	}

	@Override
	public RenderType getRenderType(SpectralRoyalGuardEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public Color getRenderColor(SpectralRoyalGuardEntity animatable, float partialTick, int packedLight) {
		return TINT;
	}

	@Override
	protected float getDeathMaxRotation(SpectralRoyalGuardEntity animatable) {
		return 0;
	}
}
