package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.GarumeReEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GarumeReModel extends GeoModel<GarumeReEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/garume_re.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/garume_re.animation.json");

    @Override
    public ResourceLocation getModelResource(GarumeReEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GarumeReEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GarumeReEntity animatable) {
        return ANIMATION;
    }
}