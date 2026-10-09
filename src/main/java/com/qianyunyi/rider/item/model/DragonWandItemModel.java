package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.DragonWandItem;

public class DragonWandItemModel extends GeoModel<DragonWandItem> {
	@Override
	public ResourceLocation getAnimationResource(DragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/dragon_wand.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/dragon_wand.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DragonWandItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/geo/dragon_wand.png");
	}
}
