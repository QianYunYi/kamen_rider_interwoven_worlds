package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.GoGadoruBaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * 葛·伽多鲁·巴 模型。
 * <p>
 * 根据实体当前形态 / 所选武器动态控制 sword / crossbow / wand 骨骼组的可见性。
 */
public class GoGadoruBaModel extends GeoModel<GoGadoruBaEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            "kamen_rider_interwoven_worlds", "geo/go_gadoru_ba.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            "kamen_rider_interwoven_worlds", "animations/go_gadoru_ba.animation.json");

    @Override
    public ResourceLocation getModelResource(GoGadoruBaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GoGadoruBaEntity animatable) {
        // 纹理形态同名：go/go_gadoru_ba_grapple_form 等
        return ResourceLocation.fromNamespaceAndPath(
                "kamen_rider_interwoven_worlds",
                "textures/entities/gurongi/" + animatable.getTexture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoGadoruBaEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(GoGadoruBaEntity entity, long uniqueID, AnimationState<GoGadoruBaEntity> customPredicate) {
        super.setCustomAnimations(entity, uniqueID, customPredicate);

        boolean swordVisible = entity.isSwordVisible();
        boolean crossbowVisible = entity.isCrossbowVisible();
        boolean wandVisible = entity.isWandVisible();

        // 剑组（含右臂 sword 与左臂 sword2）
        setBoneHidden("sword", !swordVisible);
        setBoneHidden("sword2", !swordVisible);
        setBoneHidden("crossbow", !crossbowVisible);
        setBoneHidden("wand", !wandVisible);
    }

    private void setBoneHidden(String boneName, boolean hidden) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}