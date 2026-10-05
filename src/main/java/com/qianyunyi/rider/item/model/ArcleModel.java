package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.ArcleItem;

public class ArcleModel extends GeoModel<ArcleItem> {
	@Override
	public ResourceLocation getAnimationResource(ArcleItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/arcle.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ArcleItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/arcle.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ArcleItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/item/arcle.png");
	}
}
