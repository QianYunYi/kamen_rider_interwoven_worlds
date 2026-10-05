package com.qianyunyi.rider.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.block.entity.ShockerOperatingTableTileEntity;

public class ShockerOperatingTableBlockModel extends GeoModel<ShockerOperatingTableTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShockerOperatingTableTileEntity animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/shocker_operating_table.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShockerOperatingTableTileEntity animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/shocker_operating_table.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShockerOperatingTableTileEntity animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/block/shocker_operating_table.png");
	}
}
