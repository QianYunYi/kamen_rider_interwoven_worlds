package com.qianyunyi.rider.client.effect.renderer.projectile;

import com.qianyunyi.rider.client.effect.model.projectile.PegasusBowgunProjectileModel;
import com.qianyunyi.rider.entity.projectile.PegasusBowgunProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class PegasusBowgunProjectileRenderer extends GeoEntityRenderer<PegasusBowgunProjectile> {

    public PegasusBowgunProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PegasusBowgunProjectileModel());
    }

    /**
     * 让弹体模型跟随其飞行朝向（非生物弹体默认不被 GeckoLib 朝向，这里显式按 getYRot 偏航）。
     * 偏航修正量 270° 与本模型 blast_pegasus.geo.json 的前向偏差一致（天马风弹与葛·伽多鲁·巴共用）。
     */
    @Override
    protected void applyRotations(PegasusBowgunProjectile animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        if (animatable.getPose() == net.minecraft.world.entity.Pose.SLEEPING) {
            return;
        }
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F - yaw));
    }
}