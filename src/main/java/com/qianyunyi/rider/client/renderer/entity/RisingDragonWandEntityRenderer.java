package com.qianyunyi.rider.client.renderer.entity;

import com.qianyunyi.rider.entity.projectile.RisingDragonWandEntity;
import com.qianyunyi.rider.entity.model.RisingDragonWandEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class RisingDragonWandEntityRenderer extends GeoEntityRenderer<RisingDragonWandEntity> {

    public RisingDragonWandEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RisingDragonWandEntityModel());
    }

    @Override
    public void render(RisingDragonWandEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();

        stack.translate(0.0, -0.25, 0.0);

        stack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        stack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot() + 90.0F));

        stack.scale(0.69F, 0.69F, 0.69F);

        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }
}