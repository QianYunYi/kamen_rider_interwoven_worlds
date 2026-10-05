package com.qianyunyi.rider.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.block.display.KuugaSarcophagusDisplayItem;

public class KuugaSarcophagusDisplayModel extends GeoModel<KuugaSarcophagusDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(KuugaSarcophagusDisplayItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/kuuga_sarcophagus.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KuugaSarcophagusDisplayItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/kuuga_sarcophagus.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KuugaSarcophagusDisplayItem entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/block/kuuga_sarcophagus.png");
	}
}
