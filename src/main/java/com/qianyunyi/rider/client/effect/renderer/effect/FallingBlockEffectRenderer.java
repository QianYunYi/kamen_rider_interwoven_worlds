package com.qianyunyi.rider.client.effect.renderer.effect;


import com.mojang.blaze3d.vertex.PoseStack;
import com.qianyunyi.rider.entity.effect.FallingBlockEffectEntity;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class FallingBlockEffectRenderer extends EntityRenderer<FallingBlockEffectEntity> {
    public FallingBlockEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FallingBlockEffectEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        BlockState blockState = entity.getBlockState();
        if (blockState == null || blockState.isAir() || blockState.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        poseStack.pushPose();
        BlockPos pos = entity.blockPosition();

        // 获取实际光照值（使用方块位置上方光照）
        if (entity.level() != null) {
            int blockLight = entity.level().getBrightness(LightLayer.BLOCK, pos.above());
            int skyLight = entity.level().getBrightness(LightLayer.SKY, pos.above());
            packedLight = (skyLight << 20) | (blockLight << 4); // 手动打包光照值
        }

        // 调整位置
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        // 应用旋转
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(new Quaternionf().rotationX(entity.getRotationX() * (float) Math.PI / 180.0F));
        poseStack.mulPose(new Quaternionf().rotationY(entity.getRotationY() * (float) Math.PI / 180.0F));
        poseStack.mulPose(new Quaternionf().rotationZ(entity.getRotationZ() * (float) Math.PI / 180.0F));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        // 获取方块渲染器
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        // 渲染方块
        blockRenderer.renderSingleBlock(
                blockState,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FallingBlockEffectEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}