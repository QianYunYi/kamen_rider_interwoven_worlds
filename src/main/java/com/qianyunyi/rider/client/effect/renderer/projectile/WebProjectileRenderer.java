package com.qianyunyi.rider.client.effect.renderer.projectile;

import com.qianyunyi.rider.entity.projectile.WebProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 蜘蛛吐丝弹射物的渲染器
 * 使用白色蛛网纹理渲染一个小型弹射物
 */
@OnlyIn(Dist.CLIENT)
public class WebProjectileRenderer extends EntityRenderer<WebProjectile> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "kamen_rider_interwoven_worlds", "textures/entity/projectile/web_projectile.png");

    public WebProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.15f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WebProjectile entity) {
        return TEXTURE;
    }
}