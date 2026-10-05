package com.qianyunyi.rider.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.qianyunyi.rider.block.model.ShockerOperatingTableBlockModel;
import com.qianyunyi.rider.block.entity.ShockerOperatingTableTileEntity;

public class ShockerOperatingTableTileRenderer extends GeoBlockRenderer<ShockerOperatingTableTileEntity> {
	public ShockerOperatingTableTileRenderer() {
		super(new ShockerOperatingTableBlockModel());
	}

	@Override
	public RenderType getRenderType(ShockerOperatingTableTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
