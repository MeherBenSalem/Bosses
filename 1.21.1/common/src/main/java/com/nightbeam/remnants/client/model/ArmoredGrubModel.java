package com.nightbeam.remnants.client.model;

import com.nightbeam.remnants.entity.ArmoredGrubEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ArmoredGrubModel extends DefaultedEntityGeoModel<ArmoredGrubEntity> {

    private static final ResourceLocation BASE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "armored_grub");
    private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "geo/entity/armored_grub.geo.json");
    private static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "animations/entity/armored_grub.animation.json");
    private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("remnant_bosses", "textures/entities/armored_grub.png");

    public ArmoredGrubModel() {
        super(BASE_ID);
    }

    @Override
    public ResourceLocation getModelResource(ArmoredGrubEntity animatable) {
        return MODEL_ID;
    }

    @Override
    public ResourceLocation getTextureResource(ArmoredGrubEntity animatable) {
        return TEXTURE_ID;
    }

    @Override
    public ResourceLocation getAnimationResource(ArmoredGrubEntity animatable) {
        return ANIMATION_ID;
    }
}
