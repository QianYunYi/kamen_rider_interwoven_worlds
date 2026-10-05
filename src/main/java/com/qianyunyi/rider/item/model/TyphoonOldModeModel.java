package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.TyphoonOldModeItem;

public class TyphoonOldModeModel extends GeoModel<TyphoonOldModeItem> {
	@Override
	public ResourceLocation getAnimationResource(TyphoonOldModeItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/typhoon_old_model.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(TyphoonOldModeItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/typhoon_old_model.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TyphoonOldModeItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/typhoon_old_model.png");
	}
}
