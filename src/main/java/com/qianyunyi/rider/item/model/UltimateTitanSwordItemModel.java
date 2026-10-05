package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.UltimateTitanSwordItem;

public class UltimateTitanSwordItemModel extends GeoModel<UltimateTitanSwordItem> {
	@Override
	public ResourceLocation getAnimationResource(UltimateTitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_titan_sword.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(UltimateTitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_titan_sword.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(UltimateTitanSwordItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/ultimate_titan_sword.png");
	}
}
