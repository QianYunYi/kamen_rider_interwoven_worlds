package com.qianyunyi.rider.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 自适应亮度发光层
 *
 * 原理：
 * 1. 使用 _glowmask 后缀纹理 — 仅发光区域有像素，其余透明
 * 2. 单次渲染 — 避免重复渲染性能开销
 * 3. 自动亮度 — 根据昼夜时间切换渲染类型：
 *    - 白天：使用 entityTranslucent + 低 alpha，发光减弱
 *    - 夜晚：使用 eyes + 全发光，保持原版效果
 * 4. 通过 Minecraft.getInstance().level 获取客户端时间，不依赖实体 level
 */
public class BrightGlowingGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    /** 最小发光强度（正午） */
    private static final float MIN_ALPHA = 0.1f;
    /** 最大发光强度（午夜） */
    private static final float MAX_ALPHA = 1.0f;

    public BrightGlowingGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        ResourceLocation texture = this.renderer.getTextureLocation(animatable);
        if (texture == null) return;

        // 构建 glowmask 纹理路径: texture.png -> texture_glowmask.png
        String path = texture.getPath();
        int dotIndex = path.lastIndexOf('.');
        String glowPath = (dotIndex == -1)
                ? path + "_glowmask"
                : path.substring(0, dotIndex) + "_glowmask" + path.substring(dotIndex);
        ResourceLocation glowTexture = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), glowPath);

        // 从客户端 level 获取时间（不依赖实体 level，避免 null）
        Level level = Minecraft.getInstance().level;
        long dayTime = level != null ? level.getDayTime() % 24000 : 6000;
        // 将时间映射到余弦波，6000(正午)=1，18000(午夜)=-1
        float angle = (float) (dayTime - 6000) / 24000.0f * Mth.TWO_PI;
        float dayFactor = Mth.cos(angle);
        // nightFactor: 正午=0, 午夜=1
        float nightFactor = (1.0f - dayFactor) * 0.5f;

        if (nightFactor > 0.5f) {
            // 夜间（nightFactor > 0.5）：使用 eyes 渲染，全发光
            RenderType glowType = RenderType.eyes(glowTexture);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
            this.renderer.actuallyRender(
                    poseStack, animatable, bakedModel, glowType, bufferSource,
                    glowBuffer, true, partialTick,
                    15728880, OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, 1.0f
            );
        } else {
            // 白天（nightFactor <= 0.5）：使用 entityTranslucent + 动态 alpha，发光减弱
            float glowAlpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * nightFactor;
            RenderType glowType = RenderType.entityTranslucent(glowTexture);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
            this.renderer.actuallyRender(
                    poseStack, animatable, bakedModel, glowType, bufferSource,
                    glowBuffer, true, partialTick,
                    15728880, OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, glowAlpha
            );
        }
    }
}