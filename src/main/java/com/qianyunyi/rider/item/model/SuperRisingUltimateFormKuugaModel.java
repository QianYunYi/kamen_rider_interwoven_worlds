package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.SuperRisingUltimateFormKuugaItem;

public class SuperRisingUltimateFormKuugaModel extends GeoModel<SuperRisingUltimateFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(SuperRisingUltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/super_rising_ultimate_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SuperRisingUltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/super_rising_ultimate_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SuperRisingUltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/armor/super_rising_ultimate_form_kuuga.png");
	}
}
