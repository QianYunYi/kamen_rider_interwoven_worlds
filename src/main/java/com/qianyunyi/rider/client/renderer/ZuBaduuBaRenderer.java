package com.qianyunyi.rider.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuBaduuBaEntity;
import com.qianyunyi.rider.entity.model.ZuBaduuBaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZuBaduuBaRenderer extends GeoEntityRenderer<ZuBaduuBaEntity> {

    public ZuBaduuBaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ZuBaduuBaModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(ZuBaduuBaEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, ZuBaduuBaEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected float getDeathMaxRotation(ZuBaduuBaEntity entityLivingBaseIn) {
        return 0.0F;
    }
}