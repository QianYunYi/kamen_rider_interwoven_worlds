package com.qianyunyi.rider.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianyunyi.rider.entity.kaijin.gurongi.GoGadoruBaEntity;
import com.qianyunyi.rider.entity.model.GoGadoruBaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GoGadoruBaRenderer extends GeoEntityRenderer<GoGadoruBaEntity> {

    public GoGadoruBaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GoGadoruBaModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(GoGadoruBaEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, GoGadoruBaEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}