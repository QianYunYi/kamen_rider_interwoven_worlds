package com.qianyunyi.rider.item.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.qianyunyi.rider.item.model.BeatChaser2000RightHandlebarItemModel;
import com.qianyunyi.rider.item.BeatChaser2000RightHandlebarItem;

import com.mojang.blaze3d.vertex.PoseStack;

public class BeatChaser2000RightHandlebarItemRenderer extends GeoItemRenderer<BeatChaser2000RightHandlebarItem> {
	public BeatChaser2000RightHandlebarItemRenderer() {
		super(new BeatChaser2000RightHandlebarItemModel());
	}

	@Override
	public RenderType getRenderType(BeatChaser2000RightHandlebarItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
		super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
	}
}