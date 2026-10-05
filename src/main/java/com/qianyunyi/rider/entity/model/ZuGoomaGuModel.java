package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGoomaGuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZuGoomaGuModel extends GeoModel<ZuGoomaGuEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/zu_gooma_gu.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/zu_gooma_gu.animation.json");

    @Override
    public ResourceLocation getModelResource(ZuGoomaGuEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZuGoomaGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZuGoomaGuEntity animatable) {
        return ANIMATION;
    }
}