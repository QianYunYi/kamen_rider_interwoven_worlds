package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.DragonFormKuugaItem;

public class DragonFormKuugaModel extends GeoModel<DragonFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(DragonFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/dragon_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DragonFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/dragon_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DragonFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/dragon_form_kuuga.png");
	}
}
