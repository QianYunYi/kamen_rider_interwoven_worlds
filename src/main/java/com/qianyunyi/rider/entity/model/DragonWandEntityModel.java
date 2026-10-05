package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.projectile.DragonWandEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonWandEntityModel extends GeoModel<DragonWandEntity> {
    @Override
    public ResourceLocation getAnimationResource(DragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/dragon_wand.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(DragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/dragon_wand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DragonWandEntity animatable) {
        return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/dragon_wand.png");
    }
}