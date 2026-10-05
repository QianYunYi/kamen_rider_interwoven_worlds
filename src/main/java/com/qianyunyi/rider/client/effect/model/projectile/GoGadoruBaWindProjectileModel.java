package com.qianyunyi.rider.client.effect.model.projectile;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.projectile.GoGadoruBaWindProjectile;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 葛·伽多鲁·巴 风弹模型：与天马弩子弹（blast_pegasus）一致。
 */
public class GoGadoruBaWindProjectileModel extends GeoModel<GoGadoruBaWindProjectile> {

    @Override
    public ResourceLocation getModelResource(GoGadoruBaWindProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "geo/projectile/blast_pegasus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GoGadoruBaWindProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "textures/entities/projectile/blast_pegasus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoGadoruBaWindProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "animations/projectile/blast_pegasus.animation.json");
    }
}