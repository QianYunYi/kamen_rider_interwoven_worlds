package com.qianyunyi.rider.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.qianyunyi.rider.block.model.KuugaSarcophagusDisplayModel;
import com.qianyunyi.rider.block.display.KuugaSarcophagusDisplayItem;

public class KuugaSarcophagusDisplayItemRenderer extends GeoItemRenderer<KuugaSarcophagusDisplayItem> {
	public KuugaSarcophagusDisplayItemRenderer() {
		super(new KuugaSarcophagusDisplayModel());
	}

	@Override
	public RenderType getRenderType(KuugaSarcophagusDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}
}
