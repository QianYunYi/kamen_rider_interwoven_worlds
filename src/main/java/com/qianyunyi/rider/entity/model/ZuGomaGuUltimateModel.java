package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGomaGuUltimateEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZuGomaGuUltimateModel extends GeoModel<ZuGomaGuUltimateEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/zu_goma_gu_ultimate_form.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/zu_goma_gu_ultimate_form.animation.json");

    @Override
    public ResourceLocation getModelResource(ZuGomaGuUltimateEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZuGomaGuUltimateEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZuGomaGuUltimateEntity animatable) {
        return ANIMATION;
    }
}