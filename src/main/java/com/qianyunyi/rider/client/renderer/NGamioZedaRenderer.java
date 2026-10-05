package com.qianyunyi.rider.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.entity.layer.NGamioZedaLayer;
import com.qianyunyi.rider.entity.model.NGamioZedaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NGamioZedaRenderer extends GeoEntityRenderer<NGamioZedaEntity> {

	public NGamioZedaRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new NGamioZedaModel());
		this.shadowRadius = 0.5f;
		// 添加发光层，参考哥莱姆(Gouram)的渲染方式
		this.addRenderLayer(new NGamioZedaLayer(this));
	}

	@Override
	public RenderType getRenderType(NGamioZedaEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		// 使用CutoutNoCull确保主纹理正确渲染，不受透明度排序影响
		return RenderType.entityCutoutNoCull(texture);
	}

	@Override
	public void preRender(PoseStack poseStack, NGamioZedaEntity entity, BakedGeoModel model,
						  MultiBufferSource bufferSource, VertexConsumer buffer,
						  boolean isReRender, float partialTick, int packedLight,
						  int packedOverlay, float red, float green, float blue, float alpha) {
		float scale = 1f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
				partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	protected float getDeathMaxRotation(NGamioZedaEntity entityLivingBaseIn) {
		return 0.0F;
	}

	@Override
	public ResourceLocation getTextureLocation(NGamioZedaEntity animatable) {
		// 使用模型的getTextureResource获取动态纹理
		return this.model.getTextureResource(animatable);
	}
}