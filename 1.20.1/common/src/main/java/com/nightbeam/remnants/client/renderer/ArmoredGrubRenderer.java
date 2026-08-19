package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.client.model.ArmoredGrubModel;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib 4 renderer for ArmoredGrubEntity.
 *
 * GeoEntityRenderer takes care of:
 *   - Loading the geo model and animation JSON via ArmoredGrubModel
 *   - Applying the texture
 *   - Running the animation controller each render frame
 *
 * No model layer definitions are required - GeckoLib bypasses vanilla's layer system.
 */
public class ArmoredGrubRenderer extends GeoEntityRenderer<ArmoredGrubEntity> {

    public ArmoredGrubRenderer(EntityRendererProvider.Context context) {
        super(context, new ArmoredGrubModel());
    }

    @Override
    public Vec3 getRenderOffset(ArmoredGrubEntity entity, float partialTicks) {
        return new Vec3(0.0D, -0.5D, 0.0D);
    }
}
