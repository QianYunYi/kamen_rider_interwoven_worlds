package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGumunBaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZuGumunBaModel extends GeoModel<ZuGumunBaEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/zu_gumun_ba.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/zu_gumun_ba.animation.json");

    @Override
    public ResourceLocation getModelResource(ZuGumunBaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZuGumunBaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZuGumunBaEntity animatable) {
        return ANIMATION;
    }
}