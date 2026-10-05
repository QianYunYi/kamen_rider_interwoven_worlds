package com.qianyunyi.rider.client.effect.renderer.effect;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

import com.qianyunyi.rider.client.effect.model.effect.TransformationEffectEntityModel;
import com.qianyunyi.rider.entity.effect.TransformationEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 特殊效果渲染器
 */
public class TransformationEffectEntityRenderer extends GeoEntityRenderer<TransformationEffectEntity> {

    public TransformationEffectEntityRenderer(EntityRendererProvider.Context renderManager, TransformationEffectEntityModel model) {
        super(renderManager, model);
    }


    @Override
    public void preRender(PoseStack poseStack, TransformationEffectEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        // 关键空检查 - 确保实体和资源路径有效
        if (animatable.getModelLocation() == null || animatable.getTextureLocation() == null) {
            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        // 注意：GeckoLib 渲染器外层已经调用了 pushPose()，
        // 此处不再 pushPose/popPose，否则旋转会被提前撤销

        // 计算平滑旋转（基于 partialTick 插值，消除 20TPS 到 60FPS 的延迟）
        float lerpBodyRot = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        float lerpPitch = Mth.rotLerp(partialTick, animatable.xRotO, animatable.getXRot());

        // 应用 Y 轴旋转，使模型正面朝向玩家朝向
        // Minecraft 坐标系中，模型默认朝向 Z 负方向，使用 (180° - yaw) 修正
        poseStack.mulPose(new Quaternionf().rotationY((360.0F - lerpBodyRot) * Mth.DEG_TO_RAD));

        // 应用 X 轴旋转（俯仰），跟随玩家视角上下
        poseStack.mulPose(new Quaternionf().rotationX(lerpPitch * Mth.DEG_TO_RAD));

        // 轻微位置偏移防止Z-fighting
        poseStack.translate(0.0F, -0.01F, 0.0F);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public RenderType getRenderType(TransformationEffectEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(TransformationEffectEntity entity) {
        // 直接使用模型获取纹理路径
        return this.getGeoModel().getTextureResource(entity);
    }
}