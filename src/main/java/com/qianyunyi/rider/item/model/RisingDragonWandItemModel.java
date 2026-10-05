package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingDragonWandItem;

public class RisingDragonWandItemModel extends GeoModel<RisingDragonWandItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingDragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_dragon_wand.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingDragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_dragon_wand.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingDragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_dragon_wand.png");
	}
}
