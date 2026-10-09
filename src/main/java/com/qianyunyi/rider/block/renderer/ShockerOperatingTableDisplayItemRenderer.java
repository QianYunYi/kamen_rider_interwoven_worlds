package com.qianyunyi.rider.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.qianyunyi.rider.block.model.ShockerOperatingTableDisplayModel;
import com.qianyunyi.rider.block.display.ShockerOperatingTableDisplayItem;

public class ShockerOperatingTableDisplayItemRenderer extends GeoItemRenderer<ShockerOperatingTableDisplayItem> {
	public ShockerOperatingTableDisplayItemRenderer() {
		super(new ShockerOperatingTableDisplayModel());
	}

	@Override
	public RenderType getRenderType(ShockerOperatingTableDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}
}
