package com.qianyunyi.rider.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.block.display.ShockerOperatingTableDisplayItem;

public class ShockerOperatingTableDisplayModel extends GeoModel<ShockerOperatingTableDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(ShockerOperatingTableDisplayItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/shocker_operating_table.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShockerOperatingTableDisplayItem animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/shocker_operating_table.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShockerOperatingTableDisplayItem entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/block/shocker_operating_table.png");
	}
}
