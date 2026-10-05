package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.UltimatePegasusCrossbowItem;

public class UltimatePegasusCrossbowItemModel extends GeoModel<UltimatePegasusCrossbowItem> {
	@Override
	public ResourceLocation getAnimationResource(UltimatePegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_pegasus_crossbow.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(UltimatePegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_pegasus_crossbow.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(UltimatePegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/ultimate_pegasus_crossbow.png");
	}
}
