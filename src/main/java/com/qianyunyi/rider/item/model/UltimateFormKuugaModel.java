package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.UltimateFormKuugaItem;

public class UltimateFormKuugaModel extends GeoModel<UltimateFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(UltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/ultimate_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(UltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/ultimate_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(UltimateFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/ultimate_form_kuuga.png");
	}
}
