package com.nightbeam.remnants.client.renderer;

import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.client.model.ArmoredGrubModel;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ArmoredGrubRenderer extends GeoEntityRenderer<ArmoredGrubEntity> {

    public ArmoredGrubRenderer(EntityRendererProvider.Context context) {
        super(context, new ArmoredGrubModel());
    }

    @Override
    public Vec3 getRenderOffset(ArmoredGrubEntity entity, float partialTick) {
        return new Vec3(0.0D, -0.5D, 0.0D);
    }
}
