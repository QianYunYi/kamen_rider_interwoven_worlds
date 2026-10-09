package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.TitanSwordItem;

public class TitanSwordItemModel extends GeoModel<TitanSwordItem> {
	@Override
	public ResourceLocation getAnimationResource(TitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/titan_sword.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(TitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/titan_sword.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/geo/titan_sword.png");
	}
}