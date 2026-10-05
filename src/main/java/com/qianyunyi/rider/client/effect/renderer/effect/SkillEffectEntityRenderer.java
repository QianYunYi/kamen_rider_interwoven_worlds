package com.qianyunyi.rider.client.effect.renderer.effect;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianyunyi.rider.client.effect.model.effect.SkillEffectEntityModel;
import com.qianyunyi.rider.entity.effect.SkillEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 特殊效果渲染器
 */
public class SkillEffectEntityRenderer extends GeoEntityRenderer<SkillEffectEntity> {

    public SkillEffectEntityRenderer(EntityRendererProvider.Context renderManager, SkillEffectEntityModel model) {
        super(renderManager, model);
    }

    @Override
    public void render(SkillEffectEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isInvisible()) return;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, SkillEffectEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        // 关键空检查 - 确保实体和资源路径有效
        if (animatable.getModelLocation() == null || animatable.getTextureLocation() == null) {
            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        // 压入新的PoseStack状态
        poseStack.pushPose();

        // 计算平滑旋转
        float lerpBodyRot = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());

        // 应用旋转（180度减去平滑旋转值）
        poseStack.mulPose(new Quaternionf().rotationY((180.0F - lerpBodyRot) * Mth.DEG_TO_RAD));

        // 轻微位置偏移防止Z-fighting
        poseStack.translate(0.0F, -0.01F, 0.0F);

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // 弹出PoseStack状态
        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(SkillEffectEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SkillEffectEntity entity) {
        // 直接使用模型获取纹理路径
        return this.getGeoModel().getTextureResource(entity);
    }
}