package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.projectile.RisingDragonWandEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RisingDragonWandEntityModel extends GeoModel<RisingDragonWandEntity> {
    @Override
    public ResourceLocation getAnimationResource(RisingDragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_dragon_wand.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RisingDragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_dragon_wand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RisingDragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/geo/rising_dragon_wand.png");
    }
}