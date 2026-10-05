package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.MaskRider1OldItem;

public class MaskRider1OldModel extends GeoModel<MaskRider1OldItem> {
	@Override
	public ResourceLocation getAnimationResource(MaskRider1OldItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/masked_rider_1_old_model.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MaskRider1OldItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/masked_rider_1_old_model.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MaskRider1OldItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/masked_rider_1_old_model.png");
	}
}
