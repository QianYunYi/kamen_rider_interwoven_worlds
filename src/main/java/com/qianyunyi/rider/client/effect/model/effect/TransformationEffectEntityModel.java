package com.qianyunyi.rider.client.effect.model.effect;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.effect.TransformationEffectEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TransformationEffectEntityModel extends GeoModel<TransformationEffectEntity> {

    @Override
    public ResourceLocation getModelResource(TransformationEffectEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "geo/effect/" + animatable.getModelLocation() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TransformationEffectEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "textures/entities/effect/" + animatable.getTextureLocation() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(TransformationEffectEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "animations/effect/" + animatable.getModelLocation() + ".animation.json");
    }
}