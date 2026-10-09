package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.MightyFormKuugaItem;

public class MightyFormKuugaModel extends GeoModel<MightyFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(MightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/mighty_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/mighty_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MightyFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/armor/mighty_form_kuuga.png");
	}
}
