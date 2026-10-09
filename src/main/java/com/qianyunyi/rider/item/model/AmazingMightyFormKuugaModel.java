package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.AmazingMightyFormKuugaItem;

public class AmazingMightyFormKuugaModel extends GeoModel<AmazingMightyFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(AmazingMightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/amazing_mighty_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AmazingMightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/amazing_mighty_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AmazingMightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/armor/amazing_mighty_form_kuuga.png");
	}
}
