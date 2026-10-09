package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.PetrifyArcleItemItem;

public class PetrifyArcleItemItemModel extends GeoModel<PetrifyArcleItemItem> {
	@Override
	public ResourceLocation getAnimationResource(PetrifyArcleItemItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/petrify_arcle.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PetrifyArcleItemItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/petrify_arcle.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PetrifyArcleItemItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/geo/petrify_arcle_item.png");
	}
}
