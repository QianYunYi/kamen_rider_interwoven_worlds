package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingPegasusCrossbowItem;

public class RisingPegasusCrossbowItemModel extends GeoModel<RisingPegasusCrossbowItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingPegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_pegasus_crossbow.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingPegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_pegasus_crossbow.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingPegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_pegasus_crossbow.png");
	}
}
