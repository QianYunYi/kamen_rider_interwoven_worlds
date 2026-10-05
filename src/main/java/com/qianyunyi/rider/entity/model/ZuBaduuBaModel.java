package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.ZuBaduuBaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZuBaduuBaModel extends GeoModel<ZuBaduuBaEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/zu_baduu_ba.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/zu_baduu_ba.animation.json");

    @Override
    public ResourceLocation getModelResource(ZuBaduuBaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZuBaduuBaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZuBaduuBaEntity animatable) {
        return ANIMATION;
    }
}