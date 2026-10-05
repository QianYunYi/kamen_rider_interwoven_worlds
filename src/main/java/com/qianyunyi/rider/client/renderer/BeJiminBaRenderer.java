package com.qianyunyi.rider.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qianyunyi.rider.entity.kaijin.gurongi.BeJiminBaEntity;
import com.qianyunyi.rider.entity.model.BeJiminBaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BeJiminBaRenderer extends GeoEntityRenderer<BeJiminBaEntity> {

    public BeJiminBaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BeJiminBaModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(BeJiminBaEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, BeJiminBaEntity entity,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected float getDeathMaxRotation(BeJiminBaEntity entityLivingBaseIn) {
        return 0.0F;
    }
}