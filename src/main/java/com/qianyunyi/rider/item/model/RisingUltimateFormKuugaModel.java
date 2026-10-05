package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingUltimateFormKuugaItem;

public class RisingUltimateFormKuugaModel extends GeoModel<RisingUltimateFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingUltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_ultimate_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingUltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_ultimate_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingUltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_ultimate_form_kuuga.png");
	}
}
