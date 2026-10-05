package com.qianyunyi.rider.client.effect.renderer.projectile;

import com.qianyunyi.rider.client.effect.model.projectile.GoGadoruBaWindProjectileModel;
import com.qianyunyi.rider.entity.projectile.GoGadoruBaWindProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class GoGadoruBaWindProjectileRenderer extends GeoEntityRenderer<GoGadoruBaWindProjectile> {

    public GoGadoruBaWindProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GoGadoruBaWindProjectileModel());
    }

    /**
     * 弹体的 getYRot() 由 tick() 按速度矢量设置并通过服务端移动包同步到客户端。
     */
    @Override
    protected void applyRotations(GoGadoruBaWindProjectile animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        if (animatable.getPose() == net.minecraft.world.entity.Pose.SLEEPING) {
            return;
        }
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        // 在基础偏航上先扣 90° 修正模型前向偏差，再补 180° 使弹头朝前（当前朝向正确时保持本式）
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F - yaw));
    }
}