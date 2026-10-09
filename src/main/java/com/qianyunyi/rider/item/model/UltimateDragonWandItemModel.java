package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.UltimateDragonWandItem;

public class UltimateDragonWandItemModel extends GeoModel<UltimateDragonWandItem> {
	@Override
	public ResourceLocation getAnimationResource(UltimateDragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_dragon_wand.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(UltimateDragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_dragon_wand.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(UltimateDragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/geo/ultimate_dragon_wand.png");
	}
}
