package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.ArmoredGrubEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib 4 geo model for ArmoredGrubEntity.
 *
 * Model and animation assets use the default GeckoLib paths, while the texture
 * lives under the existing textures/entities folder in this project.
 */
public class ArmoredGrubModel extends DefaultedEntityGeoModel<ArmoredGrubEntity> {

    private static final ResourceLocation BASE_ID = new ResourceLocation("remnant_bosses", "armored_grub");
    private static final ResourceLocation TEXTURE_ID = new ResourceLocation("remnant_bosses", "textures/entities/armored_grub.png");

    public ArmoredGrubModel() {
        super(BASE_ID);
    }

    @Override
    public ResourceLocation getTextureResource(ArmoredGrubEntity animatable) {
        return TEXTURE_ID;
    }
}
