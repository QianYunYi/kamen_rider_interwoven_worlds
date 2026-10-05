package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.projectile.UltimateDragonWandEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UltimateDragonWandEntityModel extends GeoModel<UltimateDragonWandEntity> {
    @Override
    public ResourceLocation getAnimationResource(UltimateDragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_dragon_wand.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(UltimateDragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_dragon_wand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(UltimateDragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/ultimate_dragon_wand.png");
    }
}