package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.BeJiminBaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BeJiminBaModel extends GeoModel<BeJiminBaEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/be_jimin_ba.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/be_jimin_ba.animation.json");

    @Override
    public ResourceLocation getModelResource(BeJiminBaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BeJiminBaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(BeJiminBaEntity animatable) {
        return ANIMATION;
    }
}