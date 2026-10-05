package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.ZuMebioDaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZuMebioDaModel extends GeoModel<ZuMebioDaEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/zu_mebio_da.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/zu_mebio_da.animation.json");

    @Override
    public ResourceLocation getModelResource(ZuMebioDaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZuMebioDaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZuMebioDaEntity animatable) {
        return ANIMATION;
    }
}