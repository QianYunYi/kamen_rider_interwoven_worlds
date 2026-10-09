package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.GrowingFormKuugaItemItem;

public class GrowingFormKuugaItemModel extends GeoModel<GrowingFormKuugaItemItem> {
	@Override
	public ResourceLocation getAnimationResource(GrowingFormKuugaItemItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/growing_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GrowingFormKuugaItemItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/growing_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GrowingFormKuugaItemItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/armor/growing_form_kuuga.png");
	}
}
