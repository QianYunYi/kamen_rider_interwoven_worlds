package com.qianyunyi.rider.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGoomaGuEntity;
import com.qianyunyi.rider.entity.model.ZuGoomaGuModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZuGoomaGuRenderer extends GeoEntityRenderer<ZuGoomaGuEntity> {

    public ZuGoomaGuRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ZuGoomaGuModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(ZuGoomaGuEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, ZuGoomaGuEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected float getDeathMaxRotation(ZuGoomaGuEntity entityLivingBaseIn) {
        return 0.0F;
    }
}