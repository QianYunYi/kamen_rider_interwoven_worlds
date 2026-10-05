package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingMightyFormKuugaItem;

public class RisingMightyFormKuugaModel extends GeoModel<RisingMightyFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingMightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_mighty_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingMightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_mighty_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingMightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_mighty_form_kuuga.png");
	}
}
