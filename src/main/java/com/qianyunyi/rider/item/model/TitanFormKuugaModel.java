package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.TitanFormKuugaItem;

public class TitanFormKuugaModel extends GeoModel<TitanFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(TitanFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/titan_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(TitanFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/titan_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TitanFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/titan_form_kuuga.png");
	}
}
