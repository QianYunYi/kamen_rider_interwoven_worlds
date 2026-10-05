package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.GoBadaaBaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

public class GoBadaaBaModel extends GeoModel<GoBadaaBaEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/go_badaa_ba.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/go_badaa_ba.animation.json");

    @Override
    public ResourceLocation getModelResource(GoBadaaBaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GoBadaaBaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoBadaaBaEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(GoBadaaBaEntity entity, long uniqueID, software.bernie.geckolib.core.animation.AnimationState<GoBadaaBaEntity> customPredicate) {
        super.setCustomAnimations(entity, uniqueID, customPredicate);

        // 控制horse骨骼组可见性（默认隐藏，血量低于90召马后显示）
        CoreGeoBone horse = this.getAnimationProcessor().getBone("horse");
        if (horse != null) {
            horse.setHidden(!entity.isHorseVisible());
        }
    }
}