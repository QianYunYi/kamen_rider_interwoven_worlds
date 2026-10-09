package com.qianyunyi.rider.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.qianyunyi.rider.block.model.KuugaSarcophagusBlockModel;
import com.qianyunyi.rider.block.entity.KuugaSarcophagusTileEntity;

public class KuugaSarcophagusTileRenderer extends GeoBlockRenderer<KuugaSarcophagusTileEntity> {
	public KuugaSarcophagusTileRenderer() {
		super(new KuugaSarcophagusBlockModel());
	}

	@Override
	public void preRender(PoseStack poseStack, KuugaSarcophagusTileEntity tile, BakedGeoModel model,
						  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
						  float partialTick, int packedLight, int packedOverlay,
						  float red, float green, float blue, float alpha) {
		applyNbtToModel(tile);
		super.preRender(poseStack, tile, model, bufferSource, buffer, isReRender,
				partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}

	private void applyNbtToModel(KuugaSarcophagusTileEntity tile) {
		if (tile == null) return;

		boolean isOpen = tile.getPersistentData().getBoolean("open");
		boolean hasArcle = tile.getPersistentData().getBoolean("arcle");

		this.getGeoModel().getBone("coffinLid").ifPresent(bone -> bone.setHidden(isOpen));
		this.getGeoModel().getBone("coffinLidOpen").ifPresent(bone -> bone.setHidden(!isOpen));
		this.getGeoModel().getBone("Arcle").ifPresent(bone -> bone.setHidden(hasArcle));
	}

	@Override
	public RenderType getRenderType(KuugaSarcophagusTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}
}