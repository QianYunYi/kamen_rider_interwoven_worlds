package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGomaGuEnhancedEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZuGomaGuEnhancedModel extends GeoModel<ZuGomaGuEnhancedEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/zu_goma_gu_enhanced_form.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/zu_goma_gu_enhanced_form.animation.json");

    @Override
    public ResourceLocation getModelResource(ZuGomaGuEnhancedEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZuGomaGuEnhancedEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZuGomaGuEnhancedEntity animatable) {
        return ANIMATION;
    }
}