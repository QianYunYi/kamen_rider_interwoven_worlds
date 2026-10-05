package com.qianyunyi.rider.client.effect.model.projectile;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.projectile.PegasusBowgunProjectile;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PegasusBowgunProjectileModel extends GeoModel<PegasusBowgunProjectile> {

    @Override
    public ResourceLocation getModelResource(PegasusBowgunProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "geo/projectile/blast_pegasus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PegasusBowgunProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "textures/entities/projectile/blast_pegasus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PegasusBowgunProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID,
                "animations/projectile/blast_pegasus.animation.json");
    }
}