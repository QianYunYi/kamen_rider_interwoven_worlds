package com.qianyunyi.rider.client.effect.model.effect;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.effect.SkillEffectEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 技能特效实体模型
 */
public class SkillEffectEntityModel extends GeoModel<SkillEffectEntity> {

    @Override
    public ResourceLocation getModelResource(SkillEffectEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "geo/effect/" + animatable.getModelLocation() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SkillEffectEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "textures/entities/effect/" + animatable.getTextureLocation() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(SkillEffectEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "animations/effect/" + animatable.getModelLocation() + ".animation.json");
    }
}