package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.PegasusCrossbowItem;

public class PegasusCrossbowItemModel extends GeoModel<PegasusCrossbowItem> {
	@Override
	public ResourceLocation getAnimationResource(PegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/pegasus_crossbow.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/pegasus_crossbow.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PegasusCrossbowItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/pegasus_crossbow.png");
	}
}
