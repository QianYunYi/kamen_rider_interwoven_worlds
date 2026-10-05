package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.BeatChaser2000RightHandlebarItem;

public class BeatChaser2000RightHandlebarItemModel extends GeoModel<BeatChaser2000RightHandlebarItem> {
	@Override
	public ResourceLocation getAnimationResource(BeatChaser2000RightHandlebarItem animatable) {
		return  ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/beat_chaser_2000_right_handlebar.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BeatChaser2000RightHandlebarItem animatable) {
		return  ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/beat_chaser_2000_right_handlebar.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BeatChaser2000RightHandlebarItem animatable) {
		return  ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/item/beat_chaser_2000_right_handlebar.png");
	}
}