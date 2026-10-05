package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingDragonFormKuugaItem;

public class RisingDragonFormKuugaModel extends GeoModel<RisingDragonFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingDragonFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_dragon_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingDragonFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_dragon_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingDragonFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_dragon_form_kuuga.png");
	}
}
