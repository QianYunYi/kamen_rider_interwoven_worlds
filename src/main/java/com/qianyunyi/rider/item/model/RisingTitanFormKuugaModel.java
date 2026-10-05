package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingTitanFormKuugaItem;

public class RisingTitanFormKuugaModel extends GeoModel<RisingTitanFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingTitanFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_titan_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingTitanFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_titan_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingTitanFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_titan_form_kuuga.png");
	}
}
