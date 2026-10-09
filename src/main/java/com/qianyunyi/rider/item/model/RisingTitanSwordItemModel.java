package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingTitanSwordItem;

public class RisingTitanSwordItemModel extends GeoModel<RisingTitanSwordItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingTitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_titan_sword.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingTitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_titan_sword.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingTitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/geo/rising_titan_sword.png");
	}
}
